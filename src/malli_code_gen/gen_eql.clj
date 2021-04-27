(ns malli-code-gen.gen-eql
  "Generate EQL pull vectors from schemas

  EQL ref https://github.com/edn-query-language/eql#eql-for-selections"
  (:require [malli.util :as mu]
            [malli.core :as m]))

; https://github.com/edn-query-language/eql#eql-for-selections


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

(def spec:task
  [:map
   [:goog/id [:qualified-keyword {:namespace :goog}]]
   [:id pos-int?]
   [:gist string?]])



(defn is-prop-atomic? [prop-name map-schema root-schema]
  (prn (m/type map-schema))
  (assert (= :map (m/type map-schema)))
  (let [prop-schema (mu/get map-schema prop-name)
        schema? (= ::m/schema (m/type prop-schema))
        prop-schema (cond-> prop-schema schema? m/deref)]
    true))

(m/type
  (is-prop-atomic?
    ::id
    (m/deref (m/deref schema:task))
    nil))

(defn map->eql-pull-vector
  "Given a map spec – returns a query vector
  Crux pull https://opencrux.com/reference/queries.html#pull"
  ([et-spec] (map->eql-pull-vector et-spec {:mcg/max-nest 3}))
  ([et-spec
    {:mcg/keys [root-schema max-nest cur-nest]
     :as options
     :or {max-nest 3
          cur-nest 0}}]
   (assert (#{:map ::m/schema} (m/type et-spec)) "Expecting a :map spec")
   (let [root-schema (or root-schema et-spec)
         props-specs (m/children et-spec)

         entry->pull-item
         (fn [[^keyword e-name
               ^map options
               ^malli.core/schema schema
               :as entry]]
           (if (is-prop-atomic? e-name schema root-schema)
             e-name
             (let [can-nest? (< cur-nest max-nest)]
               (if can-nest?
                 {e-name (map->eql-pull-vector
                           schema
                           {:mcg/root-schema root-schema
                            :mcg/cur-nest (inc cur-nest)
                            :mcg/max-nest max-nest})}
                 e-name))))]

     (mapv entry->pull-item props-specs))))


(assert
  (= [:goog/id :id :gist] (map->eql-pull-vector spec:task)))


(defn schema->eql-pull
  "Generates an EQL pull vector from a schema"
  [schema]
  (assert (#{:schema ::m/schema} (m/type schema)) "Expecting a schema")
  (let [schema* (m/schema schema)
        map-schema (m/deref (m/deref schema*))]
    (map->eql-pull-vector
      map-schema
      {:mcg/root-schema schema*
       :mcg/max-nest 3})))

(schema->eql-pull schema:task)

(m/type (m/deref (m/deref schema:task)))

(m/deref
  (mu/get
    (m/deref (m/deref schema:task))
    ::id))


(-> (m/type (m/deref (m/deref schema:task))))

(-> (m/deref (m/deref schema:task))
    (m/entries))

(type (first (m/children schema:task)))

(m/children schema:task)

(defn task-pull-vector
  [subtasks-depth]
  [::id
   ::description
   ::duration
   ::global?
   {::subtasks (or subtasks-depth '...)}
   ::updated-at
   ::created-at])
