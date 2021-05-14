(ns space.matterandvoid.malli-gen.test-schema4
  "Dev scaffolding, to be removed later")


(def e-types
  [:enum
   :e.type/task
   :e.type/user
   :e.type/goal
   :e.type/habit])


(def registry:main
  {:e/id           pos-int?
   :e/address      [:map
                    {:registry {:e.address/street string?
                                :e.address/zip    string?}}
                    :e.address/zip
                    :e.address/street]

   ::type          e-types
   ::id            uuid?
   ::description   string?
   ;::duration      [:fn tick.alpha.api/duration?]
   ;; global tasks show up for all users
   ::global?     boolean?
   ::updated-at  inst?
   ::user-prop-type [:enum :int :double :boolean]
   ::goal-props [:map-of string? [:tuple ::user-prop-type string?]]
   ::created-at  inst?
   ::username    string?
   ::subtasks    [:vector [:ref ::task]]
   ::collaborators [:sequential [:ref ::user]]
   ::tags        [:set string?]


   ::user        [:map
                  {:e/type :e.type/user}
                  ::id ::username
                  #_:e/address]

   ::task        [:map
                  {:e/type :e.type/task}
                  ::id
                  ::type
                  ::user
                  ::tags
                  ::collaborators
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

(def schema:user
  "spec with external registry
  satisfies Schema"
  [:schema
   {:registry registry:main}
   ::user])


(def spec:task
  "simple map spec"
  [:map
   [:goog/id [:qualified-keyword {:namespace :goog}]]
   [:id pos-int?]
   [:gist string?]])


