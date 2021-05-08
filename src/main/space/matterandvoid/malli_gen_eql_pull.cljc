(ns space.matterandvoid.malli-gen-eql-pull
  (:require
    [malli.core :as m])
  #?(:clj (:import [clojure.lang ExceptionInfo])))

(defn ref-coll->reffed
  "Takes a ref schema, this probably needs to be updated to support refs where you want to
  add :and constraints just like the root schema."
  [ref-coll-schema]
  (-> ref-coll-schema
    (m/deref)
    (m/children) (first)
    (m/children) (first)))

(defn ->schema
  [s]
  (cond-> s (not (m/schema? s)) m/deref))

(defn schema-type
  [s]
  (try
    (m/type (m/deref s))
    (catch #?(:clj ExceptionInfo :cljs :error) e
      (if
        (= ::m/invalid-schema (:type (ex-data e)))
        nil
        (throw e)))))

(defn ref-schema? [x] (satisfies? m/RefSchema x))

(def composite-schema-types
  #{:vector :map :list :set ::m/schema :and})

(defn is-vec-of-refs? [s]
  (let [s (m/deref s)]
    (and
      (= :vector (m/type s))
      (= (count (m/children s)) 1)
      (ref-schema? (first (m/children s))))))

(defn atomic-prop? [prop-name schema]
  (not
    (and
      (composite-schema-types (m/type (m/deref schema)))
      (is-vec-of-refs? schema))))

(defn get-map-schema-from-seq
  "Given a seq of schemas, asserts there is only one :map schema and returns that."
  [s]
  (let [map-schemas (filter #(= (m/type %) :map) s)]
    (assert (= (count map-schemas) 1))
    (first map-schemas)))

(defn get-map-schema [s]
  (let [s      (m/deref s)
        s-type (m/type s)]
    (cond (= :map s-type) s
          (#{:and} s-type)
          (get-map-schema-from-seq (m/children s)))))

(def supported-schema-types #{:map :and})

;; need a helper function that takes two schemas which are map schemas,
;; but may be refs - so you deref both first
;; they are equal if they have all the same keys

(defn map-schemas-equal?
  "Treats two map schemas as being equivalent if they have all the same keys in the top level.
  'm1' and 'm2' can be literal map schemas or RefSchemas to map schemas, or mix of both."
  [m1 m2]
  (let [children1 (map first (m/children (m/deref m1)))
        children2 (map first (m/children (m/deref m2)))]
    (= children1 children2)))

(defn filter-by-ns
  "a-ns is either a set of keywords or one keyword."
  [a-ns m]
  (let [ns-set (if (keyword? a-ns) #{(name a-ns)} (into #{} (map name) a-ns))]
    (println "ns set: " ns-set)
    (->>
      (keys m)
      (filter #(contains? ns-set (namespace %)))
      (select-keys m))))


(defn filter-by-*ns* [m]
  (filter-by-ns (keyword (str *ns*)) m))

(comment
  (into #{} (map name) #{:space.matterandvoid.malli-gen-eql-pull})
  (namespace ::hello)
  (filter-by-ns #{:space.matterandvoid.malli-gen-eql-pull}
    {::hello 5
     :other  100}))


(defn map->eql-pull-vector
  "Given a malli schema returns a query vector

  Does not handle arbitrary schema, your root schema (the one you pass to this function)
  must be a ':map' schema or an ':and' schema of a ':map'.

  all [:ref ::other-schema] become EQL joins.
  Crux pull https://opencrux.com/reference/queries.html#pull"
  [orig-schema]
  ;(prn ::pull-vector orig-schema)
  ;(println (apply str (repeat 80 "-")))
  (let [schema (m/deref orig-schema)

        {::keys [pull-depth] :or {pull-depth 3}} (m/properties schema)
        _      (assert (supported-schema-types (m/type schema))
                 (str "Invalid schema. Supports: " (pr-str supported-schema-types)))
        ;_      (println "schema type: " (pr-str (m/type schema)))
        entry->pull-item
               (fn ->pull [entry]
                 (let [[prop-name options child-schema] entry]
                   ;(println "child schema: " child-schema)
                   ;(println "options: " options)
                   (if (atomic-prop? prop-name child-schema)
                     prop-name
                     (let [ref-schema (ref-coll->reffed child-schema)
                           ;_          (println "REF schema: " (pr-str ref-schema))
                           ;_          (println "REF schema get map: " (pr-str (get-map-schema ref-schema)))
                           ;_          (println "orig schema: " (pr-str orig-schema))
                           recursive? (or
                                        (map-schemas-equal? ref-schema orig-schema)
                                        (map-schemas-equal? (get-map-schema ref-schema) (get-map-schema orig-schema)))
                           #_#__ (println "orig schema get-map: " (pr-str (get-map-schema orig-schema)))]
                       (if recursive?
                         {prop-name pull-depth}
                         (do
                           ;(println "not recursve: type " (pr-str child-schema) (schema-type child-schema))
                           (cond
                             (= :vector (schema-type child-schema))
                             (do
                               ;(def x ref-schema)
                               {prop-name (map->eql-pull-vector ref-schema)})
                             :else
                             prop-name)))))))]

    (let [map-schema (get-map-schema schema)]
      ;(prn "chidren of schema: " (m/children map-schema))
      (mapv entry->pull-item (m/children map-schema)))))
