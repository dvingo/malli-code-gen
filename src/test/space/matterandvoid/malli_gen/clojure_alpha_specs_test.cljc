(ns space.matterandvoid.malli-gen.clojure-alpha-specs-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [malli.util :as mu]
    [malli.core :as m]
    [space.matterandvoid.malli-gen.clojure-alpha-specs :as spec-gen]
    [space.matterandvoid.malli-gen.util :as u]
    [space.matterandvoid.malli-gen.test-schema3 :as ts3]))


(deftest schema->spec-def-test
  (let [task-map-schema (u/get-map-schema ts3/schema:task)]
    (is (= '(s/coll-of string? :kind set?)
           (-> (mu/get task-map-schema ::ts3/tags)
               (spec-gen/schema->spec-def))))

    (is (= 'string?
           (-> (mu/get task-map-schema ::ts3/description)
               (spec-gen/schema->spec-def))))

    (is (= '(s/coll-of :space.matterandvoid.malli-gen.test-schema3/user)
           (-> (mu/get (u/get-map-schema ts3/schema:task) ::ts3/collaborators)
               (spec-gen/schema->spec-def))))))

(comment
  (schema->spec-def-test))


(def expected2
  '[(s/def ::ts3/id uuid?)
    (s/def ::ts3/tags (s/coll-of string? :kind set?))
    (s/def ::ts3/collaborators (s/coll-of ::ts3/user))
    (s/def ::ts3/description string?)
    (s/def ::ts3/global? boolean?)
    (s/def ::ts3/subtasks (s/coll-of ::ts3/task :kind vector?))
    (s/def ::ts3/updated-at inst?)
    (s/def ::ts3/created-at inst?)
    (s/def ::ts3/username string?)
    (s/def ::ts3/task
      (s/keys
        :req [::ts3/id
              ::ts3/user
              ::ts3/tags
              ::ts3/collaborators
              ::ts3/description]
        :opt [::ts3/global?
              ::ts3/subtasks
              ::ts3/updated-at
              ::ts3/created-at]))
    (s/def ::ts3/user (s/keys :req [::ts3/id ::ts3/username]))])

(deftest schemas->all-specs-test
  (is (= expected2 (spec-gen/schemas->all-specs [ts3/schema:task ts3/schema:user]))))

(comment
  (spec-gen/schemas->all-specs [ts3/schema:task ts3/schema:user])
  (t/test-ns 'space.matterandvoid.malli-gen.clojure-alpha-specs-test))
