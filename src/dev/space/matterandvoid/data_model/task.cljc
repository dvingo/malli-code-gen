(ns space.matterandvoid.data-model.task
  (:require
    [malli.core :as m]
    [malli.util :as mu]
    [space.matterandvoid.data-model.comment :as comment]
    [space.matterandvoid.data-model.db :as db]
    [space.matterandvoid.malli-registry :as reg]))

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
                  ::comments
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

(defn set-pull-depth! [d]
  (reg/register!
    {::task
     (mu/update-properties (m/deref ::task) assoc :pull-depth d)}))

(comment
  (require
    '[malli.generator :as mg])
  (m/schema ::description)
  (m/validate ::description "hi")
  (mg/generate ::task {:size 3})
  (m/deref ::task)
  (m/schema ::task))
