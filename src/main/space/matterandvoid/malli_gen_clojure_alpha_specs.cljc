(ns space.matterandvoid.malli-gen-clojure-alpha-specs
  "Generate clojure/spec-alpha2 specs from malli specs")


(malli.core/walk
  ())

(defn gen-clojure-spec-alpha [schema]
  1)

(gen-clojure-spec-alpha [:schema {:registry ts1/registry:main} ::task])
