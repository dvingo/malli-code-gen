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

Scrap Your Query Boilerplate With Specql – Tatu Tarvainen

https://www.youtube.com/watch?v=qEXNyZ5FJN4&list=PLetHPRQvX4a9iZk-buMQfdxZm72UnP3C9&index=4

Hodur

https://github.com/hodur-org


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

defresolver macro:
https://github.com/wilkerlucio/pathom/blob/master/src/com/wsscode/pathom/connect.cljc#L1637

resolver fn:
https://github.com/wilkerlucio/pathom/blob/master/src/com/wsscode/pathom/connect.cljc#L1540

defmutation macro:
https://github.com/wilkerlucio/pathom/blob/master/src/com/wsscode/pathom/connect.cljc#L1774

```clojure
;; Example code a user would author:

(pc/defresolver task-resolver [{:keys [crux-node] {:keys [params]} :ast} input]
 {::pc/output (malli-gen/gen-pathom-output-vector ::task)
  ::pc/transform pc/transform-batch-resolver}
  (let [pull-depth (or (:pull-depth params) 5)]
   (mapv (fn [task-id] (generated-task-pull crux-node task-id) input)))
   
 ;; the generated pull line is generated from another helper for crux pull.
 
 ;; interesting part:
  (malli-gen/gen-pathom-output-vector ::task)
  ;; =>
  [::task/id ::task/description ::task/duration {::task/sub-tasks [::task/id]}
   {::task/notes [::note/id]} ::db/created-at ::db/updated-at]
```

See also the resolvers generated by fulcro rad: 

https://github.com/fulcrologic/fulcro-rad/blob/develop/src/main/com/fulcrologic/rad/resolvers.cljc

https://blog.wsscode.com/pathom/v2/pathom/2.2.0/connect/resolvers.html

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
    ;; Probably use properties to specify a UI component name?
    [::notes {:optional true ::fulcro-component 'my-app/Note} ;; <-- something like this to specify the fulcro component to join with.
      [:vector [:ref ::note]]
    [::sub-tasks {:optional true :recur '...} ;; <-- allows specifying recursion depth (could also be an integer).
      [:vector [:ref ::task]]]
    [::db/updated-at {:optional true}]
    [::db/created-at {:optional true}]]})
```

```clojure
(defsc Note [_ _]
  {:query (fn [_] (malli-gen/gen-fulcro-query ::note))})

;; expands to:
 
(defsc Note [_ _]
  {:query (fn [_] [::note/id ::note/content]}))
  
;; -----------

(defsc Task [_ _]
  {:query (fn [_] (malli-gen/gen-fulcro-query ::task)}))
  
;; expands to:
  
(defsc Task [_ _]
  {:query (fn [_]
    [::task/id ::task/description ::task/duration ::task/global? 
      {::task/sub-tasks '...} ;; <-- value of :recur above
      {::task/notes (com.fulcrologic.fulcro.components/get-query Note)
      ::db/updated-at ::db/created-at}]}
```

This allows a user to add extra props:
```clojure
:query (fn [_] (conj (malli-gen/gen-fulcro-query ::note) :ui/open? :ui/editing?) ;; etc
```

And if you have a use-case where you want to remove some props you can use a schema transformer
`(mu/dissoc ::prop)` before calling gen-fulcro-query to do so.

## Helix integration with fulcro queries

It's mentioned at the bottom of this document:

https://github.com/lilactown/helix/blob/master/docs/motivation.md

> Some ideas of things you might want to build on top of helix’s component macro:
>
> Integration with a design library in order to document and test components
> Hiccup parsing
> Integration with a data loading solution that associates GraphQL queries with components
> Adding your own team’s code style checking for components

https://github.com/lilactown/helix/blob/master/docs/pro-tips.md#create-a-custom-macro

Then could combine with the new anonymous component feature in fulcro 3.5

https://github.com/fulcrologic/fulcro/tree/feature/fulcro-3.5

https://github.com/fulcrologic/fulcro/blob/feature/fulcro-3.5/src/main/com/fulcrologic/fulcro/raw/components.cljc

The above fulcro query examples would be even simpler to generate then:


https://github.com/fulcrologic/fulcro-rad/blob/develop/src/main/com/fulcrologic/rad/resolvers.cljc
```clojure 

;; integrating with helix example:                                                      

(def Note
  ;; [::note/id ::note/content]
  (nc (malli-gen/gen-fulcro-query ::note)))

(def Task
  ;; (malli-gen/gen-fulcro-query ::task)
  (nc [::task/id ::task/description ::task/duration ::task/global? 
      {::task/sub-tasks '...} ;; <-- value of :recur above
      {::task/notes (com.fulcrologic.fulcro.components/get-query Note) ::db/updated-at ::db/created-at} ]))

(defnc my-component
  []
  {:helix/features {:check-invalid-hooks-usage true}}
  (with-fulcro fulcro-app
    (let [props (fulcro/use-tree app Task {:ident ::task/id)]
      (for [n (range 10)]
        (let [[count set-count] (hooks/use-state 0)]
          (d/button {:on-click #(set-count inc)} count))))))
```

## DB helpers

create

Current thought is to generate clojure.spec.alpha specs solely for guardrails usage. Ideally leverage aave to not need to do this.
Could try this:
https://github.com/setzer22/malli-instrument

Idea for output:
```clojure
(>def ::task);;; see spec above
(>defn create-task 
  [m] 
  [map? => ::task]
  (let [task (-> m m/default-value-transformer optional->nil-transformer)]
    (assert (m/validate task))
    (let [tasks (task-tree->vec task)]
      (crux-util/put-all tasks))))
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

## Domain assumptions / questions

Should we consider schema migrations?
If we change field's name or delete one. Then we update our spec files. Are we doing PLOP?
Or we will create a new set of schemas?
Probably initial product should leave migration in the hands of the end user, but if we are going for file
based schemas and file emission – then maybe consider design for versioning.

# Visionary UI idea

Imagine a public database of fully qualified key names that define a malli :map schema. Like a maven central of schemas.

If the shape of that schema is known then you can enable a public set of utilities for this schema.

The first one that comes to mind is an open set of UI components.

Given: malli :map schema
Output: hiccup

Or, provide a symbol of a ui component that knows how to render the provided schema.
Then you could provide a UI to select a component from a global database of UI components which
work with the given schema.

Imagine browsing to a github repo that contains a UI component that advertises that it can render a :co.thing.xzy/entity

You include that component in your deps and when you pass it data that satisfies the schema for :co.thing.xzy/entity
it will render it and could even add mutation support for whichever UI library it is written in (re-frame, reagent ratom,
 fulcro, react useState hook, etc.)

# Purpose

The point of the generators in this library are to get leverage.
The default output should be sane defaults that give you a working system with no code, given only a malli schema.

I want to iterate quickly on applications without needing to deal with persistence, validation,
authorization - these are cross cutting concerns and the end goal is a system of utilities that 
give you sane defaults and flexiblity to adapt them.


The system is open to extension due the design of malli's properties features - any schema can have arbitrary key value pairs in this map.


Things needed for a basic web app:

1. DB layer
  - normalization when persisting (convert nested maps to pointers)
  - validation before saving
2. Query layer
  - pathom resolvers and mutations
3. Authentication and authorization
  - currently implemented as pathom-transform
  - Properties of a malli schema can indicate the access rights of a property and the helper code
    can use the schema to verify.
4. Transit - when transmitting data
5. FE
  - The pervasive style of state change in cljs is fn of map -> map
     the generators can have form support built in that deals with this structure
    optionally could use or persistence like datascript (or crux cljs (!))
  - for forms - client-side mutations for whichever UI lib you use.
  - this part would have to be per UI library. whichever react wrapper in cljs etc.
6. FE mutations - how the system evolves.
