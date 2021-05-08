(ns space.matterandvoid.util2
  (:require
    [malli.core :as m]
    [malli.util :as mu]
    [space.matterandvoid.test-schema :as ts1])
  #?(:clj (:import
            [clojure.lang ExceptionInfo]
            [java.util UUID])))


(def composite-schema-types
  #{:vector :map :list :set ::m/schema :and})

(def coll-types
  #{:vector :list :set})

#?(:cljs (defn uuid
           "Without args gives random UUID.
            With args, builds UUID based on input (useful in tests)."
           ([] (random-uuid))
           ([s] (cljs.core/uuid s)))

   :clj  (defn uuid
           "Without args gives random UUID.
           With args, builds UUID based on input (useful in tests)."
           ([] (UUID/randomUUID))
           ([int-or-str]
            (if (int? int-or-str)
              (UUID/fromString
                (format "ffffffff-ffff-ffff-ffff-%012d" int-or-str))
              (UUID/fromString int-or-str)))))

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
  (satisfies? m/RefSchema ts1/schema:task)
  (ref-schema? (m/deref ts1/schema:task)))


(defn get-from-registry [root-schema reffed-name]
  (let [root-props (m/properties root-schema)
        reg        (:registry root-props)
        reg-def    (get reg reffed-name)]
    (if reg-def
      (m/deref (m/schema [:schema root-props reffed-name])))))

(comment
  (m/schema (get-from-registry ts1/schema:task ::ts1/task)))


(defn schema-type [schema]
  (try
    (m/type (m/deref schema))
    (catch #?(:clj ExceptionInfo :cljs :error) e
      (if (= ::m/invalid-schema (:type (ex-data e)))
        nil
        (throw e)))))

(assert (= :map
          (-> (m/deref (m/deref ts1/schema:task))
            (mu/get ::ts1/user) (m/deref)
            (schema-type))))

(defn map-schema? [ref-coll-schema]
  (= :map (schema-type ref-coll-schema)))


(defn is-vec-of-refs? [schema]
  (let [s (m/deref schema)]
    (and
      (= :vector (m/type s))
      (= (count (m/children s)) 1)
      (ref-schema? (first (m/children s))))))

(defn schema-atomic? [schema]
  (not
    (or
      (composite-schema-types (m/type (m/deref schema)))
      (is-vec-of-refs? schema))))

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
    ts1/schema:task
    ts1/schema:task))


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

(assert (= ::ts1/task
          (-> (get-map-schema ts1/schema:task)
            (mu/get ::ts1/subtasks) (m/deref)
            (ref-coll->reffed))))


(defn coll-schema->pred-symbol [coll-schema]
  (let [?symbol (-> coll-schema (m/children) (first) (m/type))]
    (if (symbol? ?symbol)
      ?symbol)))

(assert (= 'string? (coll-schema->pred-symbol (m/schema [:set string?]))))
