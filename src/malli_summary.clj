(ns malli-summary
  "Summary for Tommi Reiman video
  https://www.youtube.com/watch?v=bQDkuF6-py4&t=58s&ab_channel=LondonClojurians"
  (:require [malli.core :as m]
            [malli.clj-kondo :as mk]
            [malli.transform :as mt]))

(def spec:task--closed
  [:map
   {:closed            true
    :task.meta/title   "Task %s"
    :task.meta/summary "summary for a task"}
   [:e/id pos-int?]

   [:e/tags
    {:optional true}
    [:set keyword?]]

   [:e/address
    [:map {:closed true}
     [:street string?]
     [:zip {:default "3800"} string?]
     [:latlon [:tuple double? double?]]]]])

; has for Refs
[:origin [:maybe "Country"]]


; can have registries on different levels

(def schema:main
  [:schema
   {:registry
    {:e/id pos-int?
     :e/address
           [:map
            {:registry {:e.address/street string?
                        :e.address/zip string?}}
            [:street :e.address/street]]}}
   :e/address])

(def spec:task--closed
  [:map
   {:closed true
    :task.meta/title "Task %s"
    :task.meta/summary "summary for a task"}
   [:e/id pos-int?]

   [:e/tags
    {:optional true}
    [:set keyword?]]

   [:e/address
    [:map {:closed true}
     [:street string?]
     [:zip {:default "3800"} string?]
     [:latlon [:tuple double? double?]]]]])

; has maybe (at least for Refs)
[:origin [:maybe "Country"]]


; can chain different value transformations
(mt/transformer
  (mt/strip-extra-keys-transformer)
  (mt/default-value-transformer)
  (mt/json-transformer))

(m/walk)

(m/schema-walker)


; malli provider can infer specs from values



; schema serialization tools
; https://github.com/borkdude/edamame
; https://github.com/borkdude/sci

(comment
  "schema tools:
   - modification
   - lookups"
  'malli.util/subschemas
  'malli.util/assoc-in)


; can transform into JSON/Swagger Schema and