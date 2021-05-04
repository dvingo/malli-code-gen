(ns space.matterandvoid.data-model.task
  (:require
    [malli.core :as m]
    [malli.util :as mu]
    [space.matterandvoid.data-model.comment :as comment]
    [space.matterandvoid.data-model.db :as db]
    [space.matterandvoid.malli-registry :as reg]
    [space.matterandvoid.util :as u]))

(def task-schema
  {::id          uuid?
   ::description string?
   ::comments    [:vector [:ref ::comment/comment]]
   ::sub-tasks   [:vector [:ref ::task]]
   ::sub-tasks2  [:vector [:ref ::task2]]
   ::task
                 [:map
                  ::id
                  ::description
                  [::comments {:default []} ]
                  [::sub-tasks {:optional true}]
                  [::db/updated-at {:optional true}]
                  [::db/created-at {:optional true}]]

   ::task2       [:and
                  [:map
                   ::id
                   ::description
                   ::comments
                   [::sub-tasks2 {:optional true}]
                   [::db/updated-at {:optional true}]
                   [::db/created-at {:optional true}]]
                  [:fn (fn [{::db/keys [created-at updated-at]}]
                         #?(:clj  (<= (.compareTo created-at updated-at) 0)
                            :cljs (<= created-at updated-at)))]]})

(reg/register! task-schema)

(comment
  (tap> task-schema)
  [:and
   [:map
    ::id
    ::description
    ::comments
    [::sub-tasks {:optional true}]
    [::db/updated-at {:optional true}]
    [::db/created-at {:optional true}]]
   [:fn (fn [{::db/keys [created-at updated-at]}]
          #?(:clj  (<= (.compareTo created-at updated-at) 0)
             :cljs (<= created-at updated-at)))]]
  )

(defn set-pull-depth! [d]
  (reg/register!
    {::task
     (mu/update-properties (m/deref ::task) assoc :pull-depth d)}))


(defn create-task
  "Create a task from a map containing an open set of keys.
  All arguments keywords are fully qualified of the current namespace.
  This allows the option in the future to make more intracate names."
  [m]
  ;; 1. select-keys - get list of keys that ::task uses using the malli API
  ;; 2. default-value coercion
  ;; 3. validate ::task
  ;;
  )
(comment (create-task {::description "Wash the dishes."})
  {::id #uuid"6ff02977-66bd-4a98-ba4a-6a5b9b6beb70"
   ::description "Wash the dishes."
   })

(comment
  (require
    '[malli.generator :as mg])
  (m/schema ::description)
  (m/validate ::description "hi")
  (mg/generate ::task {:size 3})
  (m/deref ::task)
  (m/schema ::task))
