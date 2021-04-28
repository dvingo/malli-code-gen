(ns malli-code-gen.gen-spec-alpha
  (:require [malli-code-gen.test-schema :as ts1]))


(defn gen-clojure-spec-alpha [schema]
  1)

(gen-clojure-spec-alpha [:schema {:registry ts1/registry:main} ::task])
