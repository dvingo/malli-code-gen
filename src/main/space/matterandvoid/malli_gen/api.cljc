(ns space.matterandvoid.malli-gen.api
  (:require [space.matterandvoid.malli-gen.eql :as geql]))


(defn schema->eql
  "Given a schema generates an EQL pull vector for it

  Schema can be a [:schema ...] or [:map ...]"
  [schema opts]
  (geql/schema->eql-pull schema opts))
