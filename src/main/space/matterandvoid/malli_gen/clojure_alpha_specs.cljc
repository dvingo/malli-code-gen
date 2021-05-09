(ns space.matterandvoid.malli-gen.clojure-alpha-specs
  "Generate clojure/spec-alpha2 specs from malli specs"
  (:require [malli.core :as m]
            [space.matterandvoid.malli-gen.util2 :as u]
            [space.matterandvoid.malli-gen.test-schema2 :as ts2]
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
  (-> ts2/schema:task (u/get-map-schema)
      (m/children) (nth 2)
      (nth 2)
      (m/deref)
      (composite-schema->spec-def))

  (-> ts2/schema:task (u/get-map-schema)
      (m/children) (nth 5) (nth 2)
      (composite-schema->spec-def)))


(defn schema->spec-def
  "schema may be a malli schema ref or a predicate fn"
  [schema]
  (if (m/schema? schema)
    (if (u/schema-atomic? schema)
      (m/form (m/deref schema))
      (composite-schema->spec-def schema))
    schema))

(comment
  (assert
    (= '(s/coll-of string? :kind set?)
      (-> ts2/schema:task (u/get-map-schema)
        (mu/get ::ts2/tags)
        (schema->spec-def)))))

(comment
  (assert
    (= 'string?
      (-> ts2/schema:task (u/get-map-schema)
        (mu/get ::ts2/description)
        (schema->spec-def)))))

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
  (-map->prop-specs (u/get-map-schema ts2/schema:user) {::defined-props #{}}))

(comment
  (assert
    (= ['((s/def ::ts2/tags (s/coll-of string? :kind set?))
          (s/def ::ts2/description string?)
          (s/def ::ts2/global? boolean?)
          (s/def ::ts2/subtasks (s/coll-of ::ts2/task :kind vector?))
          (s/def ::ts2/updated-at inst?)
          (s/def ::ts2/created-at inst?))
        #{::ts2/subtasks ::ts2/global? ::ts2/tags ::ts2/description
          ::ts2/id ::ts2/created-at ::ts2/updated-at}]
      (-map->prop-specs
        (u/get-map-schema ts2/schema:task)
        {::entity-types  #{::ts2/user}
         ::defined-props #{::ts2/id}}))))


(defn is-req-prop? [[prop-name options schema]]
  (not (:optional options)))
(def is-opt-prop? (complement is-req-prop?))

(defn map->keys-spec [spec-name map-schema]
  (let [ch-props (m/children map-schema)
        ; todo add support for :req-un
        req-props (mapv first (filterv is-req-prop? ch-props))
        opt-props (mapv first (filterv is-opt-prop? ch-props))]
    (list *s-def-symbol* spec-name,
          (seq
            (cond-> [*s-keys-symbol*]
                    (not-empty req-props)
                    (into [:req req-props])
                    (not-empty opt-props)
                    (into [:opt opt-props]))))))


(comment
  (assert (= '(s/def ::ts2/task
                (s/keys :req [::ts2/id
                              ::ts2/user
                              ::ts2/tags
                              ::ts2/description]
                  :opt [::ts2/global?
                        ::ts2/subtasks
                        ::ts2/updated-at
                        ::ts2/created-at]))
            (map->keys-spec ::ts2/task (u/get-map-schema ts2/schema:task)))))

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
    ::ts2/task
    (u/get-map-schema ts2/schema:task)
    {::entity-types #{::ts2/user}
     ::defined-props #{::ts2/id}}))


(defn map->all-specs
  "Generate complete set of spec defs for your malli specs
  {:spec-name [:schema]}"
  ([spec-name->malli-schema] (map->all-specs spec-name->malli-schema {}))
  ([spec-name->malli-schema
    {::keys [accumulated-specs defined-props entity-types]
     :as opts}]
   (let [entity-types (or entity-types (set (keys spec-name->malli-schema)))
         schemas (vals spec-name->malli-schema)

         def-schema-props
         (fn [[specs-vec defined-props] schema]
           ;(prn ::schema (m/deref schema))
           (let [schema (u/get-map-schema schema)
                 [new-specs new-def-props]
                 (-map->prop-specs schema {::entity-types  entity-types
                                           ::defined-props defined-props})]
             [(into specs-vec new-specs)
              new-def-props]))

         def-schema-keys
         (fn [specs-vec [spec-name schema]]
           ;(prn ::schema (m/deref schema))
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
  (map->all-specs
    {::ts2/task ts2/schema:task
     ::ts2/user ts2/schema:user}))


(defn schemas->all-specs
  "Takes a sequence of malli RefSchemas or their source vectors
   and returns a list clojure/specs-alpha2 defs
   [[:schema ...]]"
  [schemas]
  (let [schema->spec-name-entry
        (fn [schema]
          (cond
            (vector? schema) [(last schema) (m/schema schema)]
            (u/ref-schema? schema) [(last (m/form schema)) schema]))
        spec-name->schema
        (into {} (map schema->spec-name-entry schemas))]
    (map->all-specs spec-name->schema)))


(comment
  (schemas->all-specs [ts2/schema:task ts2/schema:user]))
