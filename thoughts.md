Given a malli schema generate lots of useful code and data structures; things like:

- pathom resolvers
- fulcro queries
- fulcro components
- the usual crud operations but with guardrails specs annotating them.
- generate sample data that is valid as per the data schema 


Designed to be as flexible as possible, I am thinking to achieve this you can 
use the interceptor pattern.

```clojure
(schema->code Task)
```
Takes malli schema as input and returns code as data (a list).
```clojure
(sieppari.core/execute {::schema MyEntityMalliSchema}
   (schema->resolver)
   (schema->fulcro-query)
   (schema->crux-pull-syntax))
```

*_update_*:

malli of course has a built in solution instead of custom interceptors:

https://github.com/metosin/malli#value-transformation


This relies on the structure of your data.

The \*/id attribute of any entity you are defining schema for is special in that its value will be used as a pointer when
storing graph data in a normalized form.


I would like to support the use of idents as the pointer storage format, but use just a plain uuid for the id of each entity.
As far as I know the built in crux pull does not support this, this port of the datascript pull does:

https://github.com/dvingo/my-clj-utils/blob/master/src/main/dv/crux_pull.clj#L145

https://github.com/dvingo/my-clj-utils/blob/master/src/main/dv/crux_util.clj#L176

This should be made into a protocol or multi-method to allow open extension of how to map a pointer to an ID to an ID.
This would allow the helper to be customized.

When generating make-entity helpers, we do want to include nil values:

> storing explicit nil values in the source documents means the indexes can speed up certain queries,
> whereas if the attribute is simply missing then your query needs to use a rule-based approach (inevitably slower, and probably relying on full 
> index scanning), such as with this or-join https://github.com/juxt/crux/blob/06bc04139efabad5d0fe3dd779e76bd64bb42f46/crux-test/test/crux/query_test.clj#L1830-L1834

https://juxt-oss.zulipchat.com/#narrow/stream/194466-crux/topic/Querying.20for.20entities.20that.20may.20have.20a.20particular.20attribute/near/226542408

# Inspiration & prior art

malli -> clj-kondo config

https://github.com/metosin/malli#clj-kondo

https://github.com/metosin/malli#visualizing-schemas

Clojure European Summer Time - Data Driven RAD with Malli, by Arne Brasseur

https://www.youtube.com/watch?v=ww9yR_rbgQs

Clojure Remote - Keynote: Designing with Data (Michael Drogalis)

https://youtu.be/kP8wImz-x4w?t=3091

Similar idea, schema stored in datascript instead of malli:

https://vvvvalvalval.github.io/posts/2018-07-23-datascript-as-a-lingua-franca-for-domain-modeling.html

# Editors 
This strategy makes heavy use of generated symbols, and editors have issues resolving these things.

If you use cursive you can disable unknown symbols via, resolve-as :none

https://github.com/cursive-ide/cursive/issues/2417

To deal with the fact that vars which are generated within a macro cannot be discovered by editors (code completion, jump to source, doc strings)
some ideas are:

add: `(declare symbol-here)` in the target namespace for each generated var.

Use the code-gen but still def a var:
```clojure
(defresolver my-task-resolver (malli-code-gen/gen-pathom2-resolver MyMalliSchema))

(def create-task (malli-code-gen/gen-crud-create MyMalliSchema))
(def delete-task (malli-code-gen/gen-crud-delete MyMalliSchema))
(s/def ::task (malli-code-gen/gen-clojure-spec MyMalliSchema))
;; etc ....
```

# Code sketches
Playing around with recursive schema.

Using global names (fully qualified keywords) in order to generate clojure.spec.alpha specifications.

```clojure
(ns co.my-org.my-app.task
  (:require [co.my-org.my-app.task.db :as db]))
  
(def registry
  {::id uuid?
     ::description string?
     ::db/updated-at inst?
     ::db/created-at inst?
     ::task
       [:map
         ::id
         ::description
         [::sub-tasks {:optional true}
         
         ;; this is where different malli registries may be very useful.
         ;; in some contexts (on the frontend for example) we want a nested tree of hashmaps of tasks
         ;; in others (when persisting to the db) we want refs/idents [::task/id #uuid ""])
         ;; and when/if we want pathom to traverse the relationship we want a hashmap {::task/id #uuid ""}
         ;; but we probably don't want to support mixing them together.
         
           [:vector
           [:or [:ref ::task]
                [:tuple [:enum ::id] uuid?] 
                [:map [::id]]]]]
          [::db/updated-at {:optional true}]
          [::db/created-at {:optional true}]]})

(m/validate [:schema {:registry registry} ::task]
  {::id #uuid "514e5101-6212-4aa0-8042-148ca79b1a5a"
     ::db/updated-at (tick.alpha.api/now)
     ::sub-tasks [{::id #uuid "514e5101-6212-4aa0-8042-148ca79b1a59"
                   ::db/created-at (tick.alpha.api/now)
                   ::description "some description"}
                  [::id (java.util.UUID/randomUUID)]
                  {::id (java.util.UUID/randomUUID)}]
     ::description "some description"})
```

# Desired output

Given a task with the following malli schema:

```clojure
  {::id uuid?
     ::description string?
     ::duration  [:fn tick.alpha.api/duration?]
     ;; global tasks show up for all users
     ::global?  boolean?
     ::db/updated-at inst?
     ::db/created-at inst?
     ::task
       [:map
         ::id
         ::description
         [::global {:optional true}]
         [::sub-tasks {:optional true}
           [:vector [:ref ::task]]]
         [::db/updated-at {:optional true}]
         [::db/created-at {:optional true}]]})
 ```

## Clojure.spec.alpha 

Generate clojure specs, something like:

```clojure
(gen-clojure-spec-alpha [:schema {:registry registry} ::task])
```

```clojure
(>def ::task any?)
(>def :task/id uuid?)
(>def :task/description string?)
(>def :task/duration tick.alpha.api/duration?)
(>def :task/subtasks (s/nilable (s/coll-of ::task :type vector?)))
(>def :task/global? (s/nilable boolean?))
(>def ::task (s/keys :req [::id ::description ::duration] :opt [::global ::db/created-at ::db/updated-at ::subtasks]]))
```

## Pull vector

```clojure
(defn task-pull-vector 
  [subtasks-depth]
  [::task/id
   ::task/description
   ::task/duration
   ::task/global?
   {::task/subtasks (or subtasks-depth '...)}
   ::db/updated-at
   ::db/created-at])
```

Anywhere there is recursion we will want to parameterize the pull expression.

## Pathom resolver

pathom2 resolvers (and mutations) are maps, we can generate this form which would allow users to assoc and dissoc properties 
as they see fit and even wrap the generated function that does the mutation and resolution.

We probably don't want to generate code, but this is something to keep in mind for enabling a reloaded workflow for resolvers
and mutations:

Note from Tony Kay via clojurians slack on how to get reload friendly resolvers:

> Here’s what I do: Make your own macro for defresolver and defmutation that do the following:

    Copy the body into a function with an alternate symbol (e.g. my-resolver-impl)
    Emit the resolver with the desired symbol, but have it just call the function.

Now when you reload the resolver it redefines the symbol for the generated function, which is what the embedded resolver will call.

```clojure
(defmacro defresolver [& args]
  (let [{:keys [sym arglist doc config body]} (futil/conform! ::mutation-args args)
        internal-fn-sym (symbol (str (name sym) "__internal-fn__"))
        config          (dissoc config :check :ex-return)
        env-arg         (first arglist)
        params-arg      (second arglist)]
    `(do
       ;; Use this internal function so we can dynamically update a resolver in
       ;; dev without having to restart the whole pathom parser.
       (defn ~internal-fn-sym [env# params#]
         (let [~env-arg env#
               ~params-arg params#
               result# (do ~@body)]
           result#))
       (pc/defresolver ~sym [env# params#]
         ~config
         (~internal-fn-sym env# params#)))))
```

## Fulcro query

For sake of example, imagine adding to-many notes to the task above.

Pretty much the same as the pull expression, but any joins use a component

Some attributes elided for clarity.

```clojure
{::id uuid?
 ::description string?
 ::duration  [:fn tick.alpha.api/duration?]
 ::note/content string? 
 ::note/id uuid?
 ::note [:map ::note/content ::note/id]
 ::task
  [:map
    ;; Proably use properties to specify a UI component name?
    [::notes {:optional true ::fulcro-component 'my-app/Note} ;; <-- something like this to specify the fulcro component to join with.
      [:vector [:ref ::note]]
    [::sub-tasks {:optional true :recur '...} ;; <-- allows specifying recursion depth (could also be an integer).
      [:vector [:ref ::task]]]
    [::db/updated-at {:optional true}]
    [::db/created-at {:optional true}]]})
```

```clojure
(defsc Note [_ _]
  {:query (fn [_] (malli-gen/gen-fulcro-query ::note)}

;; expands to:
 
(defsc Note [_ _]
  {:query (fn [_] [::note/id ::note/content]}
  
;; -----------

(defsc Task [_ _]
  {:query (fn [_] (malli-gen/gen-fulcro-query ::task)}
  
;; expands to:
  
(defsc Task [_ _]
  {:query (fn [_]
    [::task/id ::task/description ::task/duration ::task/global? 
      {::task/sub-tasks '...} ;; <-- value of :recur above
      {::task/notes (com.fulcrologic.fulcro.components/get-query Note)
      ::db/updated-at ::db/created-at}
      ]}
```

This allows a user to add extra props:
```clojure
:query (fn [_] (conj  (malli-gen/gen-fulcro-query ::note) :ui/open? :ui/editing?) ;; etc
```

And if you have a use-case where you want to remove some props you can use a schema transformer
`(mu/dissoc ::prop)` before calling gen-fulcro-query to do so.

## DB helpers

create

Current thought is to generate clojure.spec.alpha specs solely for guardrails useage. Ideally leverage aave to not need to do this.
Idea for output:
```clojure
(>def ::task);;; see spec above
(>defn create-task 
  [m] 
  [map? => ::task]
  (let [task (-> m m/default-value-transformer optional->nil-transformer)]
    task
  ))
```

the fictional `optional->nil-transformer` adds all properties specified in the schema but sets missing props to nil to improve crux query performance.

Sample invocation:

```clojure
(create-task {::task/description "description"})
; =>
{::task/id #uuid ".."
 ::task/description "description"
 ...
 }
```

read 

update

delete
