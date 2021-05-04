(ns dan-play
  (:require
    [clojure.java.io :as io]
    [malli-study.dot :as md]
    [malli.clj-kondo :as mk]
    [malli.core :as m]
    [malli.generator :as mg]
    [malli.provider :as mp]
    [malli.registry :as mr]
    [malli.transform :as mt]
    [malli.util :as mu]
    [space.matterandvoid.data-model.comment :as comment]
    [space.matterandvoid.data-model.db :as db]
    [space.matterandvoid.data-model.task :as task]
    [space.matterandvoid.malli-gen-eql-pull :as gen-eql]
    [space.matterandvoid.malli-registry :as reg])
  (:import [java.util Date UUID]))


;--------------------------------------------------------------------------------
; malli function schemas + instrument + clj-kondo warnings
;--------------------------------------------------------------------------------

(comment
  ;(reg/register! {::task/task (mu/update-in ) [::] comment-pull-5})
  (gen-eql/map->eql-pull-vector ::comment/comment)

  (reg/reset-registry!)

  (m/deref ::task/task)
  (gen-eql/map->eql-pull-vector ::task/task)

  (m/properties (m/deref comment-pull-5))
  (m/properties ::comment/comment)
  (= (map first (m/children
                  [:map {:doc-string "A comment is a textual content that can be attached to another entity."}
                   :space.matterandvoid.data-model.comment/id
                   :space.matterandvoid.data-model.comment/content
                   [:space.matterandvoid.data-model.comment/replies {:optional true}]
                   :space.matterandvoid.data-model.db/updated-at
                   :space.matterandvoid.data-model.db/created-at]))


    (map first (m/children [:map {:doc-string "A comment is a textual content that can be attached to another entity.", :pull-depth 5}
                            [:space.matterandvoid.data-model.comment/id :space.matterandvoid.data-model.comment/id]
                            [:space.matterandvoid.data-model.comment/content :space.matterandvoid.data-model.comment/content]
                            [:space.matterandvoid.data-model.comment/replies {:optional true} :space.matterandvoid.data-model.comment/replies]
                            [:space.matterandvoid.data-model.db/updated-at :space.matterandvoid.data-model.db/updated-at]
                            [:space.matterandvoid.data-model.db/created-at :space.matterandvoid.data-model.db/created-at]])))


  (mu/get-in ::comment/comment [0])
  (gen-eql/get-map-schema (m/deref comment2))
  (gen-eql/get-map-schema comment2)
  (m/deref comment2)

  (gen-eql/get-map-schema (m/deref ::task/task2))
  (gen-eql/get-map-schema ::task/task2)

  (m/type ::comment/comment)
  (m/type (m/deref ::comment/comment))


  (gen-eql/get-map-schema ::comment/comment)


  (gen-eql/map->eql-pull-vector ::task/task2)
  (gen-eql/map->eql-pull-vector ::task/task)
  (task/set-pull-depth! '...)
  (gen-eql/map->eql-pull-vector ::comment/comment)
  )

; aave – malli powered code checking for Clojure.
; https://github.com/teknql/aave

; can have registries on different levels

(def registry:main
  {:e/id         pos-int?
   :e/address    [:map
                  {:registry {:e.address/street string?
                              :e.address/zip    string?}}
                  [:zip :e.address/zip]
                  [:street :e.address/street]]

   ::id          uuid?
   ::description string?
   ;::duration      [:fn tick.alpha.api/duration?]
   ;; global tasks show up for all users
   ::global?     boolean?
   ::updated-at  inst?
   ::created-at  inst?
   ::username    string?
   ::user
                 [:map
                  {:e/type :e.type/user}
                  ::id ::username :e/address]

   ::task
                 [:map
                  {:e/type :e.type/task}
                  ::id
                  ::user
                  ::description
                  [::global? {:optional true}]
                  [:sub-tasks {:optional true}
                   [:vector [:ref ::task]]]
                  [::updated-at {:optional true}]
                  [::created-at {:optional true}]]})

(def schema:task
  [:schema
   {:registry registry:main}
   ::task])
;
(def schema:user
  [:schema
   {:registry registry:main}
   ::user])

(def spec:task
  [:map
   [:goog/id [:qualified-keyword {:namespace :goog}]]
   [:id pos-int?]
   [:gist string?]])

;(comment
;  (mu/subschemas schema:task)
;  (m/children schema:task)
;  (m/entries schema:task)
;
;  (m/type schema:task))
;
;(def subschemas1
;  (mu/subschemas schema:task))
;
;;; albeit subschemas :schema will be printed as vectors – they aren't
;;; they are :malli.core/schema
;
;
;(def maps1
;  (filterv
;    (fn [subschema]
;      (let [local-schema (:schema subschema)
;            props        (m/properties local-schema)]
;        (and (vector? local-schema)
;          (= :map (first local-schema)))))
;    subschemas1))
;
;(comment
;  (m/properties (get-in subschemas1 [0 :schema]))
;
;  (mu/to-map-syntax
;    schema:task))
;
;
;(comment
;  (mu/find-first
;    schema:task
;    (fn [schema path options]
;      (prn path)
;      (-> schema m/properties :e/type)))
;
;  (mu/required-keys schema:task)
;  (mu/optional-keys schema:task))
;
;
;(comment
;  (m/walk
;    (m/deref (m/deref schema:task))
;    (m/schema-walker
;      (fn [schema]
;        (doto schema prn)))))
;
;
;(m/walk
;  schema:task
;  (m/schema-walker
;    (fn [schema]
;      (prn ::schema schema)
;      schema)))
;
;;--------------------------------------------------------------------------------
;; custom mutable registry (see deps.edn for jvm-opts needed to enable this).
;;--------------------------------------------------------------------------------

(def custom-registry
  {::id          uuid?
   ::description string?
   ::updated-at  inst?
   ::created-at  inst?
   ::sub-tasks   [:vector [:ref ::task]]
   ::comments    [:vector [:ref ::comment]]

   ::comment-id  uuid?
   ::comment-txt string?
   ::comment     [:map
                  ::comment-id
                  ::comment-txt
                  [::other {:optional true} [:vector [:ref ::comment]]]
                  [::comment-replies {:optional true} [:vector [:ref ::comment]]]]
   ::task
                 [:map
                  ::id
                  ::description
                  ::comments
                  [::other [:vector [:ref ::comment]]]
                  [::sub-tasks {:optional true}]
                  [::updated-at {:optional true}]
                  [::created-at {:optional true}]]})

;(comment (m/validate ::description "hello" {:registry my-registry}))
;(m/validate ::description "hello")

(comment
  (m/validate #_[:schema {:registry registry} ::task]
    ::task
    {::id          #uuid "514e5101-6212-4aa0-8042-148ca79b1a5a"
     ::updated-at  (Date.)
     ::sub-tasks   [{::id          #uuid "514e5101-6212-4aa0-8042-148ca79b1a59"
                     ::created-at  (Date.)
                     ::description "some description"}
                    [::id (UUID/randomUUID)]
                    {::id (UUID/randomUUID)}]
     ::description "some description"} {:registry my-registry}))

;--------------------------------------------------------------------------------
; clj-kondo example
;--------------------------------------------------------------------------------

(defn sample-task-fn [t] t)
(comment
  (m/=> sample-task-fn [:=> [:cat ::task] ::task])
  (comment (mk/collect *ns*)))

(comment
  (satisfies? m/RefSchema (m/schema ::task))
  (satisfies? m/RefSchema (m/deref (m/schema ::task)))

  (m/walk
    (m/deref ::task)
    (m/schema-walker
      (fn [x] (prn :x' x) x)))

  (m/deref-all (m/schema (::task x)))

  (m/deref (m/schema (::task x)))
  (m/deref [:schema (::task x)])
  (m/walk x (fn [i] (println i)))

  (mu/to-map-syntax))

(def ref-schema? (partial satisfies? m/RefSchema))
(def ref-type-schema? #{::m/schema})

(comment (m/type ::task)
  (= ::m/schema (m/type uuid?)))

(defn schema-data [s]
  [(m/type s) (m/properties s)] (m/children s))

(defn schema->map [s]
  {:type (m/type s) :props (m/properties s) :children (m/children s)})

(comment
  (schema-map ::task)
  (walk-to-eql* (m/schema ::task))
  (schema-data ::task))

(defn ->pull-vec [s out]
  (let [{:keys [type props children] :as m} (schema->map s)]
    (println "m: ") (prn m)
    (cond
      (ref-type-schema? type)
      (->pull-vec (m/deref s) out)

      (= :map type)
      (reduce
        (fn [out* child]
          (println "recur with: " child)
          (println "schema: ") (prn (m/schema child))
          (println "after schema ")
          (conj out* (->pull-vec child out*)))
        out
        children)

      :else out)
    m))

(comment
  (->pull-vec ::task [])

  (m/form (m/deref ::task))
  (m/type (m/schema [:map [:id string?]]))
  (m/type (m/schema ::task))
  (->
    (mu/to-map-syntax [:map [:id string?]])
    :children first
    (m/schema)
    )
  [[:id nil {:type string?}]]
  (mu/to-map-syntax [:id string?])
  (mu/to-map-syntax [:map [:id string?]])
  (mu/to-map-syntax (m/deref ::task))
  (m/schema [:id nil {:type string?}])

  (walk-to-eql [:map [:id string?]])
  (m/schema string?)
  (mu/to-map-syntax [:map []]))

(comment
  (ref-schema? (m/schema ::task))
  (ref-schema? [:map [:id string?]])
  (walk-to-eql [:map [:id string?]])
  (walk-to-eql ::task))

(comment
  (mu/to-map-syntax (m/deref ::task)))

(defn walk-it [schema]
  (let [schema (m/deref schema)]
    (m/walk
      schema
      (fn [schema path walked-children options]
        (println "--------------------------------------------------------------------------------")
        (print "schema: ") (prn schema)
        (when (ref-schema? schema)
          (print "derefed schema: ")
          (prn (m/deref schema)))
        (print "path: ") (prn path)
        (print "walked-children: ") (prn walked-children)
        (print "options: ") (prn options)
        {:type            (m/type schema)
         :walked-children walked-children}))))
(comment
  (walk-it ::task))

(comment

  (md/transform (m/deref ::task)))


(comment

  (let [s (m/schema ::task)]
    s
    )

  )

(def composite-schema-types
  #{:vector :map :list :set ::m/schema})

(defn ->schema [s] (cond-> s (not (m/schema? s)) m/deref))

(defn schema-type
  [s]
  (try
    (m/type (m/deref s))
    (catch clojure.lang.ExceptionInfo e
      (if
        (= ::m/invalid-schema (:type (ex-data e)))
        nil
        (throw e)))))

(defn is-map-schema? [s]
  (= :map (schema-type s)))

(defn is-vec-of-refs? [s]
  (let [s (m/deref s)]
    (and
      (= :vector (m/type s))
      (do (println "it is a vector") true)
      (= (count (m/children s)) 1)
      (ref-schema? (first (m/children s))))))

(comment
  (schema-type ::sub-tasks)
  (is-vec-of-refs? ::sub-tasks)
  (m/children ::sub-tasks))

(defn is-prop-atomic? [prop-name schema]
  (println "\n\n schema; ") (pr schema " " (->schema schema) " \n")
  (println "type: " (pr-str (m/type (->schema schema))))

  (prn ::is-atomic prop-name schema (m/type (m/deref schema)))
  (prn "not composite: " (not (composite-schema-types (m/type (m/deref schema)))))
  (prn "(not (is-vec-of-refs? schema))" (not (is-vec-of-refs? schema)))
  (not
    (and
      (composite-schema-types (m/type (m/deref schema)))
      (is-vec-of-refs? schema))))

(comment
  (is-map-schema? string?)
  (is-map-schema? [])
  (is-map-schema? ::task)
  (is-map-schema? (m/schema [:map [:id string?]]))
  (is-map-schema? [:map [:id string?]])
  (is-map-schema? (m/deref (m/schema [:map [:id string?]])))
  (m/schema? ::task))

(defn ref-coll->reffed [ref-coll-schema]
  (def s1 ref-coll-schema)
  (-> ref-coll-schema
    (m/deref)
    (m/children) (first)
    (m/children) (first)))

(defn map->eql-pull-vector
  "Given a map spec – returns a query vector
  Crux pull https://opencrux.com/reference/queries.html#pull "
  ([schema] (map->eql-pull-vector schema {:mcg/max-nest 3}))
  ([orig-schema
    {:mcg/keys [max-nest]
     :or       {max-nest '...}}]
   (prn ::pull-vector orig-schema)
   (let [schema (m/deref orig-schema)
         entry->pull-item
                (fn ->pull [entry]
                  (let [[^keyword spec-item-id
                         ^map spec-item-options
                         ^malli.core/schema child-schema] entry]
                    (if (is-prop-atomic? spec-item-id child-schema)
                      spec-item-id
                      (let [rs         (ref-coll->reffed child-schema)
                            recursive? (= rs orig-schema)]
                        (if recursive?
                          {spec-item-id max-nest}
                          (cond
                            (= :vector (schema-type child-schema))
                            (do
                              (def x rs)
                              {spec-item-id (map->eql-pull-vector rs) #_(mapv #(->pull %) (m/children ref-type))})
                            :else
                            spec-item-id))))))]

     (prn "chidren of schema: " (m/children schema))
     (mapv entry->pull-item (m/children schema)))))

(defn uuid [] (UUID/randomUUID))
(comment
  (m/children (m/deref ::task))
  (->
    (m/children (m/deref ::task))
    (nth 2)
    (nth 2)
    (ref-schema?)
    ;(m/deref )
    ;(m/children)
    )
  (m/validate ::comment
    {::comment-id      (uuid)
     ::comment-txt     "lskdj"
     ::comment-replies [{::comment-id      (uuid)
                         ::comment-txt     "lskdj"
                         ::comment-replies [{}]}]

     })
  (m/children (m/schema (m/deref x)))
  (is-prop-atomic? ::name ::sub-tasks)
  (m/deref (m/schema ::sub-tasks))
  (is-vec-of-refs? ::sub-tasks)
  (m/schema ::task)
  (map->eql-pull-vector ::task {}))



(comment
  (m/schema ::comment)
  (m/schema
    ::comment
    {:registry custom-registry}

    ))
(comment
  (mg/generate ::task)
  (mg/genn)

  )

(comment
  (let [s (m/schema ::task/task nil)]
    (satisfies? m/RefSchema s)
    (-> s m/deref m/type)
    )
  (md/-lift
    (m/schema ::task/task nil))
  (md/transform
    ::comment/comment)
  (md/transform
    [:schema
     {:registry {"comment" (m/form (m/deref ::comment/comment))}}])
  (md/transform ::comment/comment)
  )

(defmacro my-when [test & body]
  `(if ~test (do ~@body)) nil)
(defmacro my-thing [hi]
  hi)

(comment
  (my-thing 'hi)
  (macroexpand
    '(my-when [1 2 34] body)))

(comment

  (m/properties (m/schema ::comment/comment))
  (m/properties-schema (m/schema ::task/task))
  (m/properties (m/schema ::task/task))
  (m/-ref (m/schema ::comment/comment))
  (m/-type-properties (m/schema ::comment/comment))
  (md/transform
    [:schema
     {:registry {"comment" (m/form (m/deref ::comment/comment))}}])
  )
