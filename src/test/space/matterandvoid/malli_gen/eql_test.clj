(ns space.matterandvoid.malli-gen.eql-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [space.matterandvoid.malli-gen.eql :as geql]
    [space.matterandvoid.malli-gen.test-schema2 :as ts2]
    [space.matterandvoid.malli-gen.test-utils :as tu]))


(def exp-vector
  [::ts2/id
   {::ts2/user [::ts2/id ::ts2/username #:e{:address [:zip :street]}]}
   ::ts2/description
   ::ts2/global?
   {::ts2/subtasks
    [::ts2/id
     {::ts2/user [::ts2/id ::ts2/username #:e{:address [:zip :street]}]}
     ::ts2/description
     ::ts2/global?
     {::ts2/subtasks
      [::ts2/id
       {::ts2/user [::ts2/id ::ts2/username :e/address]}
       ::ts2/description
       ::ts2/global?
       {::ts2/subtasks [::ts2/id ::ts2/user ::ts2/description ::ts2/global? ::ts2/subtasks ::ts2/updated-at ::ts2/created-at]}
       ::ts2/updated-at
       ::ts2/created-at]}
     ::ts2/updated-at
     ::ts2/created-at]}
   ::ts2/updated-at
   ::ts2/created-at])

(deftest schema->eql-pull-test
  (is (= exp-vector (geql/schema->eql-pull ts2/schema:task nil))))

(comment
  (t/test-ns 'malli-code-gen.gen-eql-test))

