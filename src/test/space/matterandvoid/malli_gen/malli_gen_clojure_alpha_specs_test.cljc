(ns space.matterandvoid.malli-gen-clojure-alpha-specs-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [space.matterandvoid.malli-gen.clojure-alpha-specs :as spec-gen]
    [space.matterandvoid.malli-gen.test-schema2 :as ts2]))

(def expected1
  ['(s/def ::ts2/id uuid?)
   '(s/def ::ts2/tags (s/coll-of string? :kind set?))
   '(s/def ::ts2/description string?)
   '(s/def ::ts2/global? boolean?)
   '(s/def ::ts2/subtasks (s/coll-of ::ts2/task :kind vector?))
   '(s/def ::ts2/updated-at inst?)
   '(s/def ::ts2/created-at inst?)
   '(s/def ::ts2/username string?)
   '(s/def ::ts2/task
      (s/keys
        :req [::ts2/id
              ::ts2/user
              ::ts2/tags
              ::ts2/description]
        :opt [::ts2/global?
              ::ts2/subtasks
              ::ts2/updated-at
              ::ts2/created-at]))
   '(s/def
      ::ts2/user
      (s/keys :req [::ts2/id ::ts2/username]))])


(deftest schemas->all-specs-test
  (is (= expected1 (spec-gen/schemas->all-specs [ts2/schema:task ts2/schema:user]))))
