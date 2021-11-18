(ns ivan-play2
  (:require [malli.core :as m]
            [malli.util :as mu]
            [malli.transform :as mt]
            [space.matterandvoid.malli-study.kondo :as mk]
            [space.matterandvoid.malli-gen.api :as mcg]
            [space.matterandvoid.malli-gen.test-schema4 :as ts4]
            [clojure.java.io :as io]
            [crux.api :as crux] ;; XTDB now
            [space.matterandvoid.malli-gen.util :as u])
  (:import (java.util UUID)))


;; malli map prop anatomy
[:prop-name {:prop-opt 1} :e/prop-schema-ref]
:e/prop-schema-ref ;; -> references prop schema -> references schema definition

;; key concepts
;; - schema ref – can be used to track schema identity
;; RefSchema
;; - schema def
;; - schema form
;; - schema children and property schemas
;;

(def avro-ex
  {:namespace "example.avro",
   :type      "record",
   :name      "user",
   :fields    [{:name "name", :type "string"},
               {:name "favorite_number", :type "int"},
               {:name "favorite_color", :type "string", :default "green"}]})


(def malli-ex
  (m/schema
    [:map
     {:avro/type      "record"
      :avro/namespace "example.avro"
      :avro/name      "user"
      :closed         true}
     [:name string?]
     [:favorite_number int?]
     [:favorite_color {:default "green"} string?]]))

(def malli-ex-2
  (m/schema
    [:map
     {:avro/type      "record"
      :avro/namespace "example.avro"
      :avro/name      "user"
      :closed         true}
     [:name {:optional true} string?]
     [:favorite_number int?]
     [:favorite_color {:default "green"} string?]]))

(->> malli-ex m/deref m/properties)

(type malli-ex)
(type (m/form malli-ex))

;; { "name": "username",
;;   "type": ["null", "string"],
;;   "default": null }



(defn malli-prop-def->avro-field-type
  [[prop-name prop-opts prop-schema-ref]]
  (let [prop-schema-def (m/deref-all prop-schema-ref)
        schema-form (m/form prop-schema-def)
        opt? (:optional prop-opts)
        base-type
        (cond
          (#{'string? :string} schema-form) "string"
          (#{'int? :int} schema-form) "int"
          :else (throw (ex-info "Type cast not implemented" {:prop-ref prop-schema-ref})))]
    (if opt?
      ["null" base-type]
      base-type)))

(defn malli-prop-def->avro-field-def
  [[prop-name prop-opts prop-schema-ref :as prop-def]]
  (let [field-type (malli-prop-def->avro-field-type prop-def)]
    (merge {:name (name prop-name) :type field-type}
           (select-keys prop-opts [:default]))))

(defn malli-schema->avro-schema
  [malli-schema]
  (let [schema-def (m/deref-all malli-schema)
        schema-props (m/properties schema-def)
        avro-fields (mapv malli-prop-def->avro-field-def (m/children schema-def))]
    {:namespace (:avro/namespace schema-props)
     :name      (:avro/name schema-props)
     :type      (:avro/type schema-props)
     :fields    avro-fields}))

(->> (m/children malli-ex)
     (first) (last) m/form)

(->> (m/children malli-ex)
     (first) (last) m/form)

(->> (m/children malli-ex)
     (mapv malli-prop-def->avro-field-def))

(assert
  (= {:namespace "example.avro",
      :name      "user",
      :type      "record",
      :fields    [{:name "name", :type "string"}
                  {:name "favorite_number", :type "int"}
                  {:name "favorite_color", :type "string", :default "green"}]}
     (malli-schema->avro-schema malli-ex)))

(assert
  (= {:namespace "example.avro",
      :name      "user",
      :type      "record",
      :fields    [{:name "name", :type ["null" "string"]}
                  {:name "favorite_number", :type "int"}
                  {:name "favorite_color", :type "string", :default "green"}]}
     (malli-schema->avro-schema malli-ex-2)))


