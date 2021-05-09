(ns space.matterandvoid.malli-registry
  (:require
    [malli.core :as m]
    #_[malli.registry :as mr]))

(defonce registry-atom (atom (m/default-schemas)))
;(mr/set-default-registry! custom-registry)

(defn register!
  "Takes a map of key -> schema."
  [schema]
  (assert (map? schema) "You must provide of map of schema keys to schemas.")
  (swap! registry-atom merge schema))

;; After you invoke this you have to reload the namespaces
;; that register the schemas.
(defn reset-registry!
  "Resets the custom default schema to malli.core/default-schemas"
  []
  (reset! registry-atom (m/default-schemas)))
