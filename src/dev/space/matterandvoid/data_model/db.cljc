(ns space.matterandvoid.data-model.db
  (:require
    [space.matterandvoid.malli-registry :as reg]))

(def db-schema
  {::updated-at inst?
   ::created-at inst?})

(reg/register! db-schema)
(comment
  (require '[malli.core :as m])
  (m/schema ::updated-at)
  )
