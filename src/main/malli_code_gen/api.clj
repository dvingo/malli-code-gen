(ns malli-code-gen.api
  (:require [malli-code-gen.gen-eql]))


(defn schema->eql
  "Given a schema generates an EQL pull vector for it

  Schema can be a [:schema ...] or [:map ...]"
  [schema opts]
  (malli-code-gen.gen-eql/schema->eql-pull schema opts))
