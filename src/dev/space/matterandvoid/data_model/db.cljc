(ns space.matterandvoid.data-model.db
  (:require
    [space.matterandvoid.malli-registry :as mr]))

(def db-schema
  {::updated-at inst?
   ::created-at inst?})

(mr/register! db-schema)
(comment
  (require '[malli.core :as m])
  (m/schema ::updated-at)
  )
