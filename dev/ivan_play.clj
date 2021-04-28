(ns ivan-play
  (:require [malli.core :as m]
            [malli.clj-kondo :as mk]
            [malli.util :as mu]
            [crux.api :as crux]
            [malli-code-gen.api :as mcg]
            [malli.transform :as mt]
            [clojure.java.io :as io]
            [crux.api :as crux])
  (:import (java.util UUID)))

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


#_(m/explain
    spec:task
    {:goog/id :goog/thing
     :id      3,
     :gist    ""})



(mu/subschemas schema:task)
(m/children schema:task)
(m/entries schema:task)

(m/type schema:task)

(def subschemas1
  (mu/subschemas schema:task))

; albeit subschemas :schema will be printed as vectors – they aren't
; they are :malli.core/schema


(def maps1
  (filterv
    (fn [subschema]
      (let [local-schema (:schema subschema)
            props (m/properties local-schema)]
        (and (vector? local-schema)
             (= :map (first local-schema)))))
    subschemas1))

(m/properties (get-in subschemas1 [0 :schema]))

(mu/to-map-syntax
  schema:task)

(comment
  (mu/find-first
    schema:task
    (fn [schema path options]
      (prn path)
      (-> schema m/properties :e/type))))

(mu/required-keys schema:task)
(mu/optional-keys schema:task)


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


(defonce crux-node
  (start-crux!))

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

