(ns malli-code-gen.gen-eql-test
  (:require [clojure.test :as t :refer [deftest is testing]]
            [malli-code-gen.test-utils :as tu]
            [malli-code-gen.test-schema2 :as ts2]
            [malli-code-gen.gen-eql :as eql]))


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
  (is (= exp-vector (eql/schema->eql-pull ts2/schema:task))))

(comment
  (t/test-ns 'malli-code-gen.gen-eql-test))

