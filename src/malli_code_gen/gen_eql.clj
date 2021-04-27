(ns malli-code-gen.gen-eql
  (:require [malli.util :as mu]
            [malli.core :as m]))


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

(def schema:user
  [:schema
   {:registry registry:main}
   ::user])

(def spec:task
  [:map
   [:goog/id [:qualified-keyword {:namespace :goog}]]
   [:id pos-int?]
   [:gist string?]])


#_(m/explain
    spec:task
    {:goog/id :goog/thing
     :id      3,
     :gist    ""})

(mu/to-map-syntax spec:task)

(m/type spec:task)
(m/type schema:task)


(mu/to-map-syntax schema:task)

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
         is-prop-atomic?
         (fn [])
         entry->pull-item
         (fn [[e-name options schema :as entry]]
           e-name)]
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


(-> (m/children (m/deref (m/deref schema:task)))
    (first)
    (nth 0)
    (type))

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
