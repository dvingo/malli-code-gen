(ns malli-play
  (:require [malli.core :as m]
            [malli.transform :as mt]))

; aave – malli powered code checking for Clojure.
; https://github.com/teknql/aave

(def spec:task
  [:map
   [:goog/id [:qualified-keyword {:namespace :goog}]]
   [:id pos-int?]
   [:gist string?]])

(m/explain
  spec:task
  {:goog/id :goog/thing
   :id      3,
   :gist    ""})


(defn map->eql-pull-vector
  "Given a map spec – returns a query vector
  Crux pull https://opencrux.com/reference/queries.html#pull"
  [et-spec]
  (let [[spec-type & props-specs] et-spec]
    (assert (= :map spec-type) "Expects a map spec")
    (mapv first props-specs)))


(assert
  (= [:goog/id :id :gist] (map->eql-pull-vector spec:task)))