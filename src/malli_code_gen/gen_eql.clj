(ns malli-code-gen.gen-eql)


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



(defn map->eql-pull-vector
  "Given a map spec – returns a query vector
  Crux pull https://opencrux.com/reference/queries.html#pull"
  [et-spec]
  (let [[spec-type & props-specs] et-spec]
    (assert (= :map spec-type) "Expects a map spec")
    (mapv first props-specs)))


(assert
  (= [:goog/id :id :gist] (map->eql-pull-vector spec:task)))

