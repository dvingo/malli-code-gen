(ns space.matterandvoid.malli-gen.eql-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [clojure.data]
    [space.matterandvoid.malli-gen.eql :as geql]
    [space.matterandvoid.malli-gen.test-schema2 :as ts2]))


(def exp-vector
  [::ts2/id
   {::ts2/user [::ts2/id ::ts2/username]}
   ::ts2/tags
   ::ts2/description
   ::ts2/global?
   {::ts2/subtasks
    [::ts2/id
     {::ts2/user [::ts2/id ::ts2/username]}
     ::ts2/tags
     ::ts2/description
     ::ts2/global?
     {::ts2/subtasks
      [::ts2/id
       {::ts2/user [::ts2/id ::ts2/username]}
       ::ts2/tags
       ::ts2/description
       ::ts2/global?
       {::ts2/subtasks [::ts2/id ::ts2/user ::ts2/tags ::ts2/description ::ts2/global? ::ts2/subtasks ::ts2/updated-at ::ts2/created-at]}
       ::ts2/updated-at
       ::ts2/created-at]}
     ::ts2/updated-at
     ::ts2/created-at]}
   ::ts2/updated-at
   ::ts2/created-at])

(deftest schema->eql-pull-test
  (is (= exp-vector (geql/schema->eql-pull ts2/schema:task nil))))

(comment
  (clojure.data/diff exp-vector (geql/schema->eql-pull ts2/schema:task nil))
  (t/test-ns 'space.matterandvoid.malli-gen.eql-test))

