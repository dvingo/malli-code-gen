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


; in a REPL malli stuff looks like Clojure data, but it isn't
; it's malli's data types and you need malli.core and malli.util
; to operate on them


; malli.core/walk on a :schema schema works "dumb"
; use m/deref m/deref to convert :schema to :map


; What I tried
(m/walk
  schema:task
  (m/schema-walker
    (fn [schema]
      (prn schema)
      schema)))

; what I needed
; (why is it double deref? * laughs in Russian *)
(m/walk
  (m/deref (m/deref schema:task))
  (m/schema-walker
    (fn [schema]
      (prn schema)
      schema)))

; I didn't define subtasks in a registry, so they didn't appear in walking

; walking doesn't seem more efficient than mapping.

;walking over schema with fully defined properties is the same as just mapping over it.
;If you don't define ::subtasks in the registry then yes, you can see [:vector [:ref ::task]] in the walking f, but you won't see [:subtasks [:vector]]

;I'm sticking with map and recursion for the time being and I'll ask malli devs to review this.


(-> (mu/get schema-map:task ::ts1/subtasks)
    (ref-coll->reffed)
    (m/schema?))

(m/properties schema:task)


(-> (mu/get schema-map:task ::ts1/subtasks)
    (m/deref))

(-> (mu/get schema-map:task ::ts1/subtasks)
    (m/deref)
    (m/children)
    (first)
    (m/type)
    (= :ref))

; when I got to [:ref ::task], the ::task wasn't a schema,
; but a simple keyword

; I may have underexplored schema walk
; but in our case we would benefit from the post order tra














