(ns malli-play
  (:require
    [malli.clj-kondo :as mk]
    [malli.core :as m]
    [malli.registry :as mr]
    [malli.transform :as mt]
    [crux.api :as crux]
    [clojure.java.io :as io]
    [malli-code-gen.api :as mcg]
    [malli.util :as mu])
  (:import [java.util Date UUID]))

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

(def schema:user
  [:schema
   {:registry registry:main}
   ::user])

(def spec:task
  [:map
   [:goog/id [:qualified-keyword {:namespace :goog}]]
   [:id pos-int?]
   [:gist string?]])

(comment
  (mu/subschemas schema:task)
  (m/children schema:task)
  (m/entries schema:task)

  (m/type schema:task))

(def subschemas1
  (mu/subschemas schema:task))

;; albeit subschemas :schema will be printed as vectors – they aren't
;; they are :malli.core/schema


(def maps1
  (filterv
    (fn [subschema]
      (let [local-schema (:schema subschema)
            props        (m/properties local-schema)]
        (and (vector? local-schema)
          (= :map (first local-schema)))))
    subschemas1))

(comment
  (m/properties (get-in subschemas1 [0 :schema]))

  (mu/to-map-syntax
    schema:task))


(comment
  (mu/find-first
    schema:task
    (fn [schema path options]
      (prn path)
      (-> schema m/properties :e/type)))

  (mu/required-keys schema:task)
  (mu/optional-keys schema:task))


(comment
  (m/walk
    schema:task
    (m/schema-walker
      (fn [schema]
        (doto schema prn))))

  (m/walk
    schema:task
    (m/schema-walker
      (fn [schema]
        (prn ::schema schema)
        schema))))

(defn start-crux! []
  (letfn [(kv-store [dir]
            {:kv-store {:crux/module 'crux.rocksdb/->kv-store
                        :db-dir      (io/file dir)
                        :sync?       true}})]
    (crux/start-node
      {}
      #_{:crux/tx-log              (kv-store "data/dev/tx-log")
         :crux/document-store      (kv-store "data/dev/doc-store")
         :crux/index-store         (kv-store "data/dev/index-store")})))

(comment
  (defonce crux-node (start-crux!)))

(defn stop-crux! []
  (.close crux-node))

(def task-eql
  (mcg/schema->eql schema:task))

(comment
  (crux/submit-tx
    crux-node
    [[:crux.tx/put
      {:crux.db/id   (UUID/randomUUID)
       ::id          :local-id
       ::created-at  #inst"2020-03-20"
       :sub-tasks    [#uuid"20fa70ab-d86e-4a02-8f72-2a8d1ce81dd7"]
       ::description "A parent task"}]])

  (crux/q
     (crux/db crux-node)
     {:find  [(list 'pull 'e (conj task-eql :crux.db/id))]
      :where '[[e :crux.db/id]]}))

        (doto schema prn)))))


(m/walk
  schema:task
  (m/schema-walker
    (fn [schema]
      (prn ::schema schema)
      schema)))

;--------------------------------------------------------------------------------
; custom mutable registry (see deps.edn for jvm-opts needed to enable this).
;--------------------------------------------------------------------------------

(def registry-atom (atom (m/default-schemas)))
(def my-registry (mr/mutable-registry registry-atom))

(def custom-registry
  {::id            uuid?
   ::description   string?
   ::updated-at inst?
   ::created-at inst?
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
                    [::updated-at {:optional true}]
                    [::created-at {:optional true}]]})

(swap! registry-atom merge custom-registry)

(mr/set-default-registry! my-registry)

(m/validate ::description "hello" {:registry my-registry})

(m/validate #_[:schema {:registry registry} ::task]
  ::task
  {::id            #uuid "514e5101-6212-4aa0-8042-148ca79b1a5a"
   ::updated-at (Date.)
   ::sub-tasks     [{::id            #uuid "514e5101-6212-4aa0-8042-148ca79b1a59"
                     ::created-at (Date.)
                     ::description   "some description"}
                    [::id (UUID/randomUUID)]
                    {::id (UUID/randomUUID)}]
   ::description   "some description"} {:registry my-registry})

;--------------------------------------------------------------------------------
; clj-kondo example
;--------------------------------------------------------------------------------

(defn sample-task-fn [t] t)
(m/=> sample-task-fn [:=> [:cat ::task] ::task])
(comment
  (mk/collect *ns*))
