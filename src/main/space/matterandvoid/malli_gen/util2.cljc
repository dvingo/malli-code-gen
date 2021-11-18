(ns space.matterandvoid.malli-gen.util2
  (:require
    [malli.core :as m]
    [malli.util :as mu])
  #?(:clj (:import (clojure.lang ExceptionInfo)
                   (java.util Map))))


;; in 2021 we all know this is 2x faster than the core's merge
#?(:cljs (defn ^IMap assign
               "Primitive and faster left merge"
               [^IMap m1, ^IMap m2]
               (persistent!
                 (reduce-kv
                   (fn [m k v] (assoc! m k v))
                   (transient (or m1 {}))
                   m2)))
   :clj (defn ^Map assign
          "Primitive and faster left merge"
          [^Map m1, ^Map m2]
          (persistent!
            (reduce-kv
              (fn [m k v] (assoc! m k v))
              (transient (or m1 {}))
              m2))))



(def atomic-types-set
  #{'zero? 'false? 'true? 'inst? 'uri?
    'string? :string :re
    'boolean? :boolean
    'keyword? :keyword
    'qualified-keyword? :qualified-keyword
    'int? :int
    'nil? :nil
    'pos-int? :pos-int
    'neg-int? :neg-int
    'nat-int? :nat-int
    'double? :double
    'decimal?
    'float? :float
    'symbol? :symbol
    'qualified-symbol? :qualified-symbol
    'uuid? :uuid})


(def malli-coll-types
  ;; https://github.com/metosin/malli#sequence-schemas
  #{:vector :sequential :set
    :cat :catn :alt :altn
    :? :* :+ :repeat})

(def composite-schema-types
  #{:vector :map :sequential :set ::m/schema :and})


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
  (ref-schema? common.spec/schema:user)
  (-> common.spec/schema:shipping-order m/deref m/deref m/children (nth 2))
  (-> common.spec/schema:shipping-order m/deref m/deref m/children (nth 1)
      last m/deref)
  (-> common.spec/schema:shipping-order m/deref m/deref m/children (nth 1)
      last ref-schema?))

(defn dderef [schema]
  (m/deref (m/deref schema)))

(defn get-from-registry [root-schema reffed-name]
  (let [root-props (m/properties root-schema)
        reg        (:registry root-props)
        reg-def    (get reg reffed-name)]
    (if reg-def
      (m/deref (m/schema [:schema root-props reffed-name])))))

(comment
  (m/schema (get-from-registry ts2/schema:task ::task)))

(defn form-options [schema]
  (let [form (m/form schema)
        opts (second form)]
    (if (map? opts) opts)))

(defn schema-type [schema]
  (try
    (m/type (m/deref (m/deref schema)))
    (catch #?(:clj ExceptionInfo :cljs :error) e
      (if (= ::m/invalid-schema (:type (ex-data e)))
        nil
        (throw e)))))

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


(defn seq-schema->predicate
  "get content predicate from a seq schema"
  [seq-schema-def]
  ;(def s3 seq-schema-def)
  (first (m/children seq-schema-def)))
(assert (= [:enum "ee"] (m/form (seq-schema->predicate (m/schema [:vector [:enum "ee"]])))))
(comment
  (seq-schema->predicate s3))


(defn enum-ref? [schema-ref]
  (= :enum (m/type (dderef schema-ref))))

(def assert-reg {::thing-enum [:enum ::thing-1 ::thing-1]
                 ::map1 [:map [::th1 ::thing-enum]]})

(assert (enum-ref? (m/schema [:schema {:registry assert-reg} ::thing-enum])))
(assert (not (enum-ref? (m/schema [:schema {:registry assert-reg} ::map1]))))


(defn try-get-atomic-from-and [and-schema-def]
  (reduce
    (fn [_ child-pred]
      (if (atomic-types-set (m/type child-pred))
        (reduced child-pred)))
    nil
    (m/children and-schema-def)))

(defn try-get-atomic
  "tries to extract an atomic predicate schema from input-schema"
  [input-schema]
  (let [schema-def (-> input-schema m/deref m/deref)
        s-type (m/type schema-def)]
    (cond
      (atomic-types-set s-type) s-type
      (= :and s-type) (try-get-atomic-from-and schema-def)
      :else nil)))


(defn seq-schema->contents-atomic-predicate [seq-schema]
  ;(def s1 seq-schema)
  (let [seq-schema-def (m/deref (m/deref seq-schema))
        main-pred (seq-schema->predicate seq-schema-def)
        pred-type (m/type main-pred)]
    (cond
      (atomic-types-set pred-type) pred-type
      :else (some-> (try-get-atomic main-pred) m/type))))
(comment
  (seq-schema->contents-atomic-predicate s1))
(assert (= 'int? (m/form (seq-schema->contents-atomic-predicate (m/schema [:vector int?])))))
(assert (= 'int? (seq-schema->contents-atomic-predicate (m/schema [:vector [:and int? pos?]]))))


(defn is-seq-of-refs? [schema]
  (let [s (m/deref (m/deref schema))]
    (and
      (contains? malli-coll-types (m/type s))
      (= (count (m/children s)) 1)
      (ref-schema? (first (m/children s))))))

(defn composite-schema? [schema]
  (contains? composite-schema-types (m/type (m/deref (m/deref schema)))))

(defn seq-schema?
  "identifies list-like and regex sequence schemas"
  [schema]
  (contains? malli-coll-types (m/type (m/deref (m/deref schema)))))

(defn atomic-type-schema? [schema]
  ;; todo add support for [:and int? [:> 100]]
  (contains? atomic-types-set (m/form schema)))

(defn simple-seq-schema?
  "helps to find [:vector int?] [:set :string] kind of stuff"
  [schema-or-ref]
  (let [schema-def (m/deref (m/deref schema-or-ref))
        ;; double deref because single deref on a [:schema {:registry x} :e] will return just :e, not its definition
        ;; deref on a definition is idempotent, so no worries about excessive deref
        sch-type (m/type schema-def)
        content-schema (first (m/children schema-def))]
    #_(prn ::ct content-schema (type content-schema))
    (and (contains? malli-coll-types sch-type)
         (atomic-type-schema? content-schema))))

(assert (simple-seq-schema? (m/schema [:vector inst?])))
(assert (not (simple-seq-schema? (m/schema [:vector [:and int? [:> 0]]]))))
(assert (not (simple-seq-schema? (m/schema [:vector :map]))))

(comment
  s2
  (m/children (m/deref (m/deref s1)))
  (simple-seq-schema? s1)
  (atomic-type-schema? (first (m/children s1)))
  (type (m/form (m/schema inst?))))


(defn leaf-schema?
  "a single predicate like 'int? is considered an atomic schema"
  [schema]
  (let [comp-type? (composite-schema? schema)
        ref-vec? (is-seq-of-refs? schema)]
    ;(prn ::schema-atomic? comp-type? ref-vec?)
    (not (or comp-type? ref-vec?))))

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
        (m/deref) (m/deref)
        (m/children) (first)
        (m/children) (first))))


(defn coll-schema->pred-symbol [coll-schema]
  (let [?symbol (-> coll-schema (m/children) (first) (m/type))]
    (if (symbol? ?symbol)
      ?symbol)))

(assert (= 'string? (coll-schema->pred-symbol (m/schema [:set string?]))))


(defn is-ref-coll?
  ; fixme add maps support
  [prop-name et-schema]
  (let [prop-schema (m/deref (m/deref (mu/get et-schema prop-name)))
        is-coll? (contains? malli-coll-types (m/type prop-schema))]
    (boolean
      (when is-coll?
        (let [children (m/children prop-schema)
              first-child (first children)
              single-child-and-ref? (and (= 1 (count children))
                                         (= :ref (m/type first-child)))]
          single-child-and-ref?)))))

