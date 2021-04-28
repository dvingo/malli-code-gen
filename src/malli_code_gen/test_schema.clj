(ns malli-code-gen.test-schema)


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
   ::subtasks    [:vector [:ref ::task]]


   ::user        [:map
                  {:e/type :e.type/user}
                  ::id ::username :e/address]

   ::task        [:map
                  {:e/type :e.type/task}
                  ::id
                  ::user
                  ::description
                  [::global? {:optional true}]
                  [::subtasks {:optional true}]
                  [::updated-at {:optional true}]
                  [::created-at {:optional true}]]})


(def schema:task
  "spec with external registry
  satisfies Schema"
  [:schema
   {:registry registry:main}
   ::task])


(def spec:task
  "simple map spec"
  [:map
   [:goog/id [:qualified-keyword {:namespace :goog}]]
   [:id pos-int?]
   [:gist string?]])


