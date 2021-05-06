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
  ;(prn ::comp-schema comp-schema)
  (let [comp-schema (m/deref comp-schema)
        coll-type (m/type comp-schema)
        ;_ (prn ::coll-type coll-type)
        item-type (or (u/ref-coll->reffed comp-schema)
                      (u/coll-schema->pred-symbol comp-schema))
        ;_ (prn ::item-type item-type)
        kind-pred (get coll-type->pred coll-type 'vector?)]
    ; can also have :count :min-count :max-count :distinct
    ; see https://clojure.github.io/spec-alpha2/ (find "every")
    (list *s-coll-of-symbol* item-type :kind kind-pred)))

(assert (= (list 's/coll-of 'string? :kind 'set?)
           (-> (m/schema [:set string?])
               (composite-schema->spec-def))))

(comment
  (-> ts1/schema:task (u/get-map-schema)
      (m/children) (nth 2)
      (nth 2)
      (m/deref)
      (composite-schema->spec-def))

  (-> ts1/schema:task (u/get-map-schema)
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

(assert
  (= (list 's/coll-of 'string? :kind 'set?)
     (-> ts1/schema:task (u/get-map-schema)
         (mu/get ::ts1/tags)
         (composite-schema->spec-def))))


(defn- -map->prop-specs
  "generate a list of specs for props
   opts
   :entity-types - set of et kw"
  [map-schema {::keys [entity-types defined-props] :as opts}]
  (let [entries (m/children map-schema)
        atom:defined (atom defined-props)
        props-defs
        (doall
          (for [[prop-name opts schema] entries
                :when (and (not (contains? defined-props prop-name))
                           (not (contains? entity-types prop-name)))]
            (do
              (swap! atom:defined conj prop-name)
              (list *s-def-symbol* prop-name (schema->spec-def schema)))))]
    [props-defs @atom:defined]))

(comment
  (-map->prop-specs (u/get-map-schema ts1/schema:user) {::defined-props #{}}))

(comment
  ; todo find a way to check equality
  (= [(list (list 's/def :ts1/tags (list 's/coll-of 'string? :kind 'set?))
            (list 's/def :ts1/description 'string?)
            (list 's/def :ts1/global? 'boolean?)
            (list 's/def :ts1/subtasks (list 's/coll-of ::ts1/task :kind 'vector?))
            (list 's/def :ts1/updated-at 'inst?)
            (list 's/def :ts1/created-at 'inst?))
      #{::ts1/subtasks ::ts1/global? ::ts1/tags ::ts1/description
        ::ts1/id ::ts1/created-at ::ts1/updated-at}]
     (-map->prop-specs
       (u/get-map-schema ts1/schema:task)
       {::entity-types #{::ts1/user}
        ::defined-props #{::ts1/id}})))


(defn is-req-prop? [[prop-name options schema]]
  (not (:optional options)))
(def is-opt-prop? (complement is-req-prop?))

(defn map->keys-spec [spec-name map-schema]
  (let [ch-props (m/children map-schema)
        req-props (mapv first (filterv is-req-prop? ch-props))
        opt-props (mapv first (filterv is-opt-prop? ch-props))]
    (list *s-def-symbol* spec-name,
          (seq
            (cond-> [*s-keys-symbol*]
                    (not-empty req-props)
                    (into [:req req-props])
                    (not-empty opt-props)
                    (into [:opt opt-props]))))))


(assert (= (list 's/def ::ts1/task
                 (list 's/keys :req [::ts1/id
                                     ::ts1/user
                                     ::ts1/tags
                                     ::ts1/description]
                       :opt [::ts1/global?
                             ::ts1/subtasks
                             ::ts1/updated-at
                             ::ts1/created-at]))
           (map->keys-spec ::ts1/task (u/get-map-schema ts1/schema:task))))


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
    ::ts1/task
    (u/get-map-schema ts1/schema:task)
    {::entity-types #{::ts1/user}
     ::defined-props #{::ts1/id}}))


(defn gen-clojure-spec-alpha
  "Generate complete set of spec defs for your malli specs"
  ([spec-name->malli-schema] (gen-clojure-spec-alpha spec-name->malli-schema {}))
  ([spec-name->malli-schema
    {::keys [accumulated-specs defined-props entity-types]
     :as opts}]
   (let [entity-types (or entity-types (set (keys spec-name->malli-schema)))
         schemas (vals spec-name->malli-schema)

         def-schema-props
         (fn [[specs-vec defined-props] schema]
           (prn ::schema (m/deref schema))
           (let [schema (u/get-map-schema schema)
                 [new-specs new-def-props]
                 (-map->prop-specs schema {::entity-types  entity-types
                                           ::defined-props defined-props})]
             [(into specs-vec new-specs)
              new-def-props]))

         def-schema-keys
         (fn [specs-vec [spec-name schema]]
           (prn ::schema (m/deref schema))
           (let [schema (u/get-map-schema schema)
                 new-keys-spec (map->keys-spec spec-name schema)]
             (conj specs-vec new-keys-spec)))


         [all-props-specs all-defined-props]
         (reduce def-schema-props [[] #{}] schemas)

         all-props-specs
         (reduce def-schema-keys
                 all-props-specs
                 spec-name->malli-schema)]

     all-props-specs)))

(comment
  (gen-clojure-spec-alpha
    {::ts1/task ts1/schema:task
     ::ts1/user ts1/schema:user}))
