(ns space.matterandvoid.malli-gen.eql
  "Generate EQL pull vectors from schemas
  EQL reference https://github.com/edn-query-language/eql#eql-for-selections"
  (:require [malli.util :as mu]
            [malli.core :as m]
            [space.matterandvoid.malli-gen.test-schema :as ts1]
            [space.matterandvoid.malli-gen.util :as u]))

(comment
  "Main members are:"
    malli-code-gen.gen-eql/schema->eql-pull
    malli-code-gen.gen-eql/map->eql-pull-vector
  "EQL reference")
  ; [1] https://github.com/edn-query-language/eql#eql-for-selections
  ; [2] Crux pull https://opencrux.com/reference/queries.html#pull


(def schema:task ts1/schema:task)


(def composite-schema-types
  #{:vector :map :list :set ::m/schema})

(def list-like-types
  #{:vector :list :set})


(def schema-map:task
  (m/deref (m/deref schema:task)))

(defn is-prop-atomic? [prop-name et-schema root-schema]
  ;(prn ::is-atomic prop-name et-schema root-schema)
  #_(assert (= :map (m/type et-schema))
            (str et-schema " isn't a :map, but a " (m/type et-schema)))
  (if-not (composite-schema-types (m/type et-schema))
    true
    (let [prop-schema (mu/get et-schema prop-name)
          schema? (= ::m/schema (m/type prop-schema))
          prop-schema (cond-> prop-schema schema? m/deref)]
      (not (composite-schema-types (m/type prop-schema))))))

(assert (is-prop-atomic? ::ts1/id schema-map:task schema:task))
(assert (not (is-prop-atomic? ::ts1/user schema-map:task schema:task)))
(assert (not (is-prop-atomic? ::ts1/subtasks schema-map:task schema:task)))


(defn is-ref-coll?
  ; fixme add maps support
  [prop-name et-schema root-schema]
  (let [prop-schema (m/deref (mu/get et-schema prop-name))
        is-coll? (list-like-types (m/type prop-schema))]
    (boolean
      (when is-coll?
        (let [children (m/children prop-schema)
              first-child (first children)
              single-child-and-ref? (and (= 1 (count children))
                                         (= :ref (m/type first-child)))]
          single-child-and-ref?)))))

(assert (not (is-ref-coll? ::ts1/id schema-map:task schema:task)))
(assert (not (is-ref-coll? ::ts1/user schema-map:task schema:task)))
(assert (is-ref-coll? ::ts1/subtasks schema-map:task schema:task))


(defn ref-coll->reffed [ref-coll-schema]
  (def s1 ref-coll-schema)
  (-> ref-coll-schema
      (m/deref)
      (m/children) (first)
      (m/children) (first)))

(comment
  (m/schema? s1))

(defn map->eql-pull-vector
  "Given a map spec – returns an EQL query vector
  See [1] for EQL vector reference.

  Walk over schema entries,
  For each entry:
  - return a prop name if it's an atomic prop (1),
  - otherwise recur depending on the property type.

  et-schema – map schema or a m/MapSchema"
  ; todo support EQL nest expressions '... (
  ([et-schema] (map->eql-pull-vector et-schema {:mcg/max-nest 3}))
  ([et-schema
    {:mcg/keys [root-schema max-nest cur-nest]
     :as       options
     :or       {max-nest 3
                cur-nest 0}}]
   ;(prn ::pull-vector et-schema)
   ;(def e1 et-schema)
   (assert (composite-schema-types (m/type et-schema)) "Expecting a composite schema type")
   (let [root-schema (or root-schema et-schema)

         entry->pull-item
         (fn [[^keyword spec-item-id
               ^map spec-item-options
               ^malli.core/schema spec-item-schema
               :as entry]]

           (if (is-prop-atomic? spec-item-id et-schema root-schema)
             spec-item-id

             ; nesting operation
             (if-let [can-nest? (< cur-nest max-nest)]
               (let [ref-coll? (is-ref-coll? spec-item-id et-schema root-schema)
                     map? (= :map (m/type (m/deref spec-item-schema)))
                     reffed-name (if ref-coll? (ref-coll->reffed spec-item-schema))
                     reffed-schema (if reffed-name (u/get-from-registry root-schema reffed-name))]

                 (cond

                   map?
                   {spec-item-id
                    (map->eql-pull-vector
                      (m/deref spec-item-schema)
                      {:mcg/root-schema root-schema
                       :mcg/cur-nest    (inc cur-nest)
                       :mcg/max-nest    max-nest})}

                   ref-coll?
                   {spec-item-id
                    (map->eql-pull-vector
                      (m/deref reffed-schema)
                      {:mcg/root-schema root-schema
                       :mcg/cur-nest    (inc cur-nest)
                       :mcg/max-nest    max-nest})}

                   :else
                   spec-item-id))

               spec-item-id)))]

     (mapv entry->pull-item (m/children et-schema)))))

(assert (= [:goog/id :id :gist] (map->eql-pull-vector ts1/spec:task)))

(comment
  (m/type e1)
  (m/entries schema-map:task)
  (u/prn-walk (m/deref-all schema:task)))


(defn schema->eql-pull
  "Generates an EQL pull vector from a schema
  Expects a RefSchema or a compatible data."
  [schema opts]
  (comment "maybe use" (satisfies? m/RefSchema (m/schema schema)))
  (assert (#{:schema ::m/schema} (m/type schema)) "Expecting a schema")
  (let [schema* (m/schema schema)
        map-schema (m/deref (m/deref schema*))]
    (map->eql-pull-vector
      map-schema
      (merge
        {:mcg/root-schema schema*
         :mcg/max-nest    3}
        opts))))


(comment

  (schema->eql-pull schema:task)

  (m/deref
    (mu/get
      (m/deref (m/deref schema:task))
      ::ts1/id))

  (-> (m/type (m/deref (m/deref schema:task))))

  (-> (m/deref (m/deref schema:task))
      (m/entries)
      (nth 4)
      (second))

  (type (first (m/children schema:task)))

  (m/children schema:task))
