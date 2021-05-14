(ns space.matterandvoid.malli-gen.api
  (:require [space.matterandvoid.malli-gen.eql :as geql]
            [space.matterandvoid.malli-gen.clojure-alpha-specs :as spec-gen]))


(defn schema->eql
  "Given a schema generates an EQL pull vector for it

  Schema can be a [:schema ...] or [:map ...]"
  [schema opts]
  (geql/schema->eql-pull schema opts))

(defn schemas->specs
  "Takes a sequence of malli RefSchemas or their source vectors
   and returns a list clojure/specs-alpha2 defs
   [[:schema ...]]"
  [schemas]
  (spec-gen/schemas->all-specs schemas))
