(ns space.matterandvoid.malli-gen-clojure-alpha-specs
  "Generate clojure/spec-alpha2 specs from malli specs"
  (:require [malli.core :as m]
            [space.matterandvoid.util2 :as u]
            [space.matterandvoid.test-schema2 :as ts1]
            [clojure.spec.alpha :as s]
            [malli.util :as mu]))


(def ^:dynamic *s-def-symbol* 's/def)
(def ^:dynamic *s-keys-symbol* 's/keys)
(def ^:dynamic *s-coll-of-symbol* 's/coll-of)


(def coll-type->pred
  {:vector 'vector?
   :list   'list?
   :set    'set?})


(defn composite-schema->spec-def [comp-schema]
  (let [coll-type (m/type (m/deref comp-schema))
        ;_ (prn ::coll-type coll-type)
        item-type (or (u/ref-coll->reffed comp-schema)
                      (u/coll-schema->pred-symbol comp-schema))
        ;_ (prn ::item-type item-type)
        kind-pred (get coll-type->pred coll-type 'vector?)]
    (list *s-coll-of-symbol* item-type :kind kind-pred)))

(assert (= (list 's/coll-of 'string? :kind 'set?)
           (-> (m/schema [:set string?])
               (composite-schema->spec-def))))

(comment
  (-> ts1/schema:task (m/deref) (m/deref)
      (m/children) (nth 2)
      (nth 2)
      (m/deref)
      (composite-schema->spec-def))

  (-> ts1/schema:task (m/deref) (m/deref)
      (m/children) (nth 5) (nth 2)
      (composite-schema->spec-def)))


(defn schema->spec-def
  "schema may be a malli schema ref or a predicate fn"
  [schema]
  (if (m/schema? schema)
    (if (u/schema-atomic? schema)
      (m/deref schema)
      (composite-schema->spec-def schema))
    schema))


(defn- -map->prop-specs
  "generate a list of specs for props"
  [map-schema {::keys [defined-props] :as opts}]
  (let [entries (m/children map-schema)
        atom:defined (atom defined-props)
        props-defs
        (doall
          (for [[prop-name opts schema] entries
                :when (not (contains? defined-props prop-name))]
            (do
              (swap! atom:defined conj prop-name)
              (list *s-def-symbol* prop-name (schema->spec-def schema)))))]
    [props-defs @atom:defined]))

(comment
  ; todo find a way to check equality
  (= [(list (list 's/def :ts1/user ':ts1/user)
            (list 's/def :ts1/description 'string?)
            (list 's/def :ts1/global? 'boolean?)
            (list 's/def :ts1/subtasks ':ts1/subtasks)
            (list 's/def :ts1/updated-at 'inst?)
            (list 's/def :ts1/created-at 'inst?))]
     (-map->prop-specs
       (u/get-map-schema ts1/schema:task)
       {::defined-props #{::ts1/id}})))


(defn is-req-prop? [[prop-name options schema]]
  (not (:optional options)))
(def is-opt-prop? (complement is-req-prop?))

(defn map->keys-spec [spec-name map-schema]
  (let [ch-props (m/children map-schema)
        req-props (mapv first (filterv is-req-prop? ch-props))
        opt-props (mapv first (filterv is-opt-prop? ch-props))]
    (list *s-def-symbol* spec-name,
          (list *s-keys-symbol* :req req-props, :opt opt-props))))


(defn map->spec-set
  [spec-name
   map-schema
   {::keys [defined-props] :as opts}]
  (let [[props-specs defined-props] (-map->prop-specs map-schema opts)]
    (if (contains? defined-props spec-name)
      [props-specs defined-props]
      [(concat props-specs (list (map->keys-spec spec-name map-schema)))
       (conj defined-props spec-name)])))

(comment
  (map->spec-set
    ::task
    (u/get-map-schema ts1/schema:task)
    {::defined-props #{::ts1/id}}))


(defn gen-clojure-spec-alpha
  "Generate complete set of spec defs for your malli specs"
  [all-malli-schemas opts]
  1)

(comment
  (gen-clojure-spec-alpha
    [[:schema {:registry ts1/registry:main} ::task]]))
