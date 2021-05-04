(ns space.matterandvoid.node-js
  (:require
    [malli.clj-kondo :as mk]
    [malli.core :as m]
    [malli.error :as me]
    [malli.generator :as mg]
    [malli.provider :as mp]
    [malli.registry :as mr]
    [malli.transform :as mt]
    [malli.util :as mu]
    [space.matterandvoid.data-model.comment :as comment]
    [space.matterandvoid.data-model.db :as db]
    [space.matterandvoid.data-model.task :as task]
    [space.matterandvoid.malli-gen-eql-pull :as gen-eql]
    [space.matterandvoid.malli-registry :as reg]
    [taoensso.timbre :as log]))

(defn main [& args]
  (log/info "in main"))

(comment
  (m/properties (m/schema ::comment/comment))
  (m/properties-schema (m/schema ::task/task))
  (m/properties (m/schema ::task/task))
  (type (m/schema ::comment/comment))
  (m/-ref (m/schema ::comment/comment))
  (m/-type-properties  ::comment/comment)

  )
