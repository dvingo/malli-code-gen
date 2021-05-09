(ns space.matterandvoid.malli-gen.eql-pull-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [space.matterandvoid.data-model.task :as task]
    ;[space.matterandvoid.data-model.db :as db]
    ;[space.matterandvoid.data-model.comment :as comment]
    [space.matterandvoid.malli-gen.eql-pull :as sut]
    [space.matterandvoid.malli-registry :as reg]))

;; todo think about adding support for [:and :map] schemas - the issue is when determining recursion,
;; but may be able to use the helper map-schemas-equal? and update that helper to handle this case.
;;
(def and-out
  [:space.matterandvoid.data-model.task/id
   :space.matterandvoid.data-model.task/description
   #:space.matterandvoid.data-model.task{:comments [:space.matterandvoid.data-model.comment/id
                                                    :space.matterandvoid.data-model.comment/content
                                                    #:space.matterandvoid.data-model.comment{:replies 3}
                                                    :space.matterandvoid.data-model.db/updated-at
                                                    :space.matterandvoid.data-model.db/created-at]}
   #:space.matterandvoid.data-model.task{:sub-tasks2 3}
   :space.matterandvoid.data-model.db/updated-at
   :space.matterandvoid.data-model.db/created-at])

(def map-out
  [:space.matterandvoid.data-model.task/id
   :space.matterandvoid.data-model.task/description
   #:space.matterandvoid.data-model.task{:comments [:space.matterandvoid.data-model.comment/id
                                                    :space.matterandvoid.data-model.comment/content
                                                    #:space.matterandvoid.data-model.comment{:replies 3}
                                                    :space.matterandvoid.data-model.db/updated-at
                                                    :space.matterandvoid.data-model.db/created-at]}
   #:space.matterandvoid.data-model.task{:sub-tasks 3}
   :space.matterandvoid.data-model.db/updated-at
   :space.matterandvoid.data-model.db/created-at])

(deftest map->eql-pull-vector-test
  (reg/register! task/task-schema)
  (let [#_#_and-out* (sut/map->eql-pull-vector ::task/task2)
        map-out*   (sut/map->eql-pull-vector
                     [:schema {:registry @reg/registry-atom} ::task/task])
        pull-depth 10
        map-out2   (assoc-in map-out [3 ::task/sub-tasks] pull-depth)]

    (is (= map-out map-out*))

    (task/set-pull-depth! 10)
    (is (= map-out2 (sut/map->eql-pull-vector [:schema {:registry @reg/registry-atom} ::task/task])))

    #_(is (= and-out* and-out))))

(comment
  ;; need to figure out how to pass a local registry with a ref-schema,
  ;; this does not work:
  (sut/map->eql-pull-vector
    [:schema {:registry @reg/registry-atom} ::task/task])

  )
