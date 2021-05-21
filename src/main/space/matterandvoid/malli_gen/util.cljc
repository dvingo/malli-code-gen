(ns space.matterandvoid.malli-gen.util
  (:require
    [malli.core :as m]
    [malli.util :as mu]
    [space.matterandvoid.malli-gen.test-schema2 :as ts2]
    [space.matterandvoid.malli-gen.test-schema3 :as ts3])
  #?(:clj (:import (clojure.lang ExceptionInfo))))

(def composite-schema-types
  #{:vector :map :sequential :set ::m/schema :and})

(def wal
  #{:vector :map :sequential :set ::m/schema :and})


(def malli-coll-types
  #{:vector :sequential :set})

(defn prn-walk [schema]
  (m/walk schema (m/schema-walker #(doto % prn))))

(defn ref-schema? [x]
  (try
    (satisfies? m/RefSchema (m/schema x))
    (catch #?(:clj ExceptionInfo :cljs :error) e
      (if (= ::m/invalid-schema (:type (ex-data e)))
        false
        (throw e)))))
(comment
  (ref-schema? ts2/schema:task))


(defn get-from-registry [root-schema reffed-name]
  (let [root-props (m/properties root-schema)
        reg        (:registry root-props)
        reg-def    (get reg reffed-name)]
    (if reg-def
      (m/deref (m/schema [:schema root-props reffed-name])))))

(comment
  (m/schema (get-from-registry ts2/schema:task ::ts2/task)))


(defn schema-type [schema]
  (try
    (m/type (m/deref schema))
    (catch #?(:clj ExceptionInfo :cljs :error) e
      (if (= ::m/invalid-schema (:type (ex-data e)))
        nil
        (throw e)))))

(assert (= :map
          (-> (m/deref (m/deref ts2/schema:task))
              (mu/get ::ts2/user) (m/deref)
              (schema-type))))

(defn map-schema? [ref-coll-schema]
  (= :map (schema-type ref-coll-schema)))


(defn get-map-schema-from-seq
  "Given a seq of schemas, asserts there is only one :map schema and returns that."
  [s]
  (let [map-schemas (filter #(= (m/type %) :map) s)]
    (assert (= (count map-schemas) 1))
    (first map-schemas)))

(defn get-map-schema [s]
  (let [s      (m/deref (m/deref s))
        s-type (m/type s)]
    (cond (= :map s-type) s
          (#{:or :and} s-type)
          (get-map-schema-from-seq (m/children s)))))


(defn is-seq-of-refs? [schema]
  (let [s (m/deref schema)]
    (and
      (contains? malli-coll-types (m/type s))
      (= (count (m/children s)) 1)
      (ref-schema? (first (m/children s))))))


(defn leaf-schema?
  "a single predicate like 'int? is considered an atomic schema"
  [schema]
  (let [comp-type? (composite-schema-types (m/type (m/deref schema)))
        ref-vec? (is-seq-of-refs? schema)]
    ;(prn ::schema-atomic? comp-type? ref-vec?)
    (not (or comp-type? ref-vec?))))

(assert (not (leaf-schema?
               (-> (get-map-schema ts3/schema:task)
                   (mu/get ::ts3/tags)))))

(defn walkable-schema?
  "is schema walkable?"
  [schema]
  pop)


(defn map-schemas-equal?
  "Duck-typing equality.
  Treats two map schemas as being equivalent if they have all the same keys in the top level.
  'm1' and 'm2' can be literal map schemas or RefSchemas to map schemas, or mix of both."
  [m1 m2]
  (let [children1 (map first (m/children (get-map-schema m1)))
        children2 (map first (m/children (get-map-schema m2)))]
    (= children1 children2)))

(comment
  (map-schemas-equal?
    ts2/schema:task
    ts2/schema:task))


(defn ref-coll->reffed
  "Takes a ref coll schema like [:vector [:ref :my-ns/entity-type]] and
  returns the referenced entity-type keyword (:my-ns/entity-type)"
  ; todo this probably needs to be updated to support refs where you want to
  ; add :and constraints just like the root schema.
  [ref-coll-schema]
  (when-not (map-schema? ref-coll-schema)
    (-> ref-coll-schema
      (m/deref)
      (m/children) (first)
      (m/children) (first))))

(assert (= ::ts2/task
           (-> (get-map-schema ts2/schema:task)
               (mu/get ::ts2/subtasks) (m/deref)
               (ref-coll->reffed))))


(defn coll-schema->pred-symbol [coll-schema]
  (let [?symbol (-> coll-schema (m/children) (first) (m/type))]
    (if (symbol? ?symbol)
      ?symbol)))

(assert (= 'string? (coll-schema->pred-symbol (m/schema [:set string?]))))
