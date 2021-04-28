(ns malli-code-gen.util
  (:require [malli.core :as m]
            [malli-code-gen.test-schema :as ts1]))


(defn prn-walk [schema]
  (m/walk schema (m/schema-walker #(doto % prn))))


(defn get-from-registry [root-schema reffed-name]
  (let [root-props (m/properties root-schema)
        reg (:registry root-props)
        reg-def (get reg reffed-name)]
    (if reg-def
      (m/deref (m/schema [:schema root-props reffed-name])))))

(comment
  (m/schema (get-from-registry ts1/schema:task ::ts1/task)))
