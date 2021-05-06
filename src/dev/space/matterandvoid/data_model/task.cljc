(ns space.matterandvoid.data-model.task
  (:require
    [malli.core :as m]
    [malli.util :as mu]
    [space.matterandvoid.data-model.comment :as comment]
    [space.matterandvoid.data-model.db :as db]
    [space.matterandvoid.malli-registry :as reg]
    [space.matterandvoid.util2 :as u]
    [malli.transform :as mt]
    [malli.error :as me]))

(def task-schema
  {::id          :uuid
   ::description :string
   ::comments    [:vector [:ref ::comment/comment]]
   ::sub-tasks   [:vector [:ref ::task]]
   ::sub-tasks2  [:vector [:ref ::task2]]
   ::task
                 [:map
                  ::id
                  ::description
                  [::comments {:default []}]
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

(type (m/form ::id))
(comment
  (m/decode ::task {} (mt/default-value-transformer
                        {:defaults {:map               (fn [v] {:default? true})
                                    :malli.core/schema (fn [v] (println "id v: " v)
                                                         (println "type: " (type v))
                                                         (condp = (m/form v)
                                                           ::id (u/uuid)
                                                           nil))
                                    ::another          (constantly {})
                                    :uuid              (fn [v] (println "v: " v)
                                                         (u/uuid))}})))
(reg/register! task-schema)

(def task-defaults
  {:defaults {:malli.core/schema
              #(condp = (m/form %)
                 ::id (u/uuid)
                 nil)}})

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
             :cljs (<= created-at updated-at)))]])


(defn set-pull-depth! [d]
  (reg/register!
    {::task
     (mu/update-properties (m/deref ::task) assoc :pull-depth d)}))


;; example of code to generate

;; in cljs can use (goog-define check-fns? false)
;; and set via closure-defines

(def check-fns? true)

(defn create-task
  "Create a task from a map containing an open set of keys.
  All arguments keywords are fully qualified of the current namespace.
  This allows the option in the future to make more intracate names."
  [m]
  (let [task (m/decode ::task m
               (mt/transformer
                 (mt/default-value-transformer task-defaults)
                 mt/strip-extra-keys-transformer))]
    (when check-fns?
      (when-not (m/validate ::task task)
        (throw (ex-info
                 (str "Invalid task: " (pr-str task))
                 {:task   task
                  :errors (me/humanize (m/explain ::task task))}))))
    task))

(m/=> create-task [:=> [:cat :map] ::task])

(comment
  (me/humanize (m/explain ::task {}))
  (create-task {::description "Wash the dishes."})
  (create-task {:extra-key 5 ::description "Wash the dishes."})

  {::id          #uuid"6ff02977-66bd-4a98-ba4a-6a5b9b6beb70"
   ::description "Wash the dishes."})


(comment
  (require
    '[malli.generator :as mg])
  (m/schema ::description)
  (m/validate ::description "hi")
  (mg/generate ::task {:size 3})
  (m/deref ::task)
  (m/schema ::task))
