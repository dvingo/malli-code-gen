(ns space.matterandvoid.data-model.comment
  (:require
    [malli.core :as m]
    [malli.generator :as mg]
    [malli.registry :as mr]
    [malli.util :as mu]
    [space.matterandvoid.data-model.db :as db]
    [space.matterandvoid.malli-registry :as reg]))

(def comment-schema
  {::id      :uuid
   ::content :string
   ::replies [:vector [:ref ::comment]]
   ::comment [:map {:doc-string "A comment is a textual content that can be attached to another entity."}
              ::id
              ::content
              [::replies {:optional true}]
              ::db/updated-at
              ::db/created-at]
   #_#_::comment-db
             [:and
              ::comment
              [:fn (fn [{::db/keys [created-at updated-at]}]
                     #?(:clj  (<= (.compareTo created-at updated-at) 0)
                        :cljs (<= created-at updated-at)))]]})

(defn set-comment-depth! [d]
  (reg/register!
    {::comment
     (mu/update-properties (m/deref ::comment) assoc :pull-depth d)}))

(comment
  (.compareTo #inst "2022" #inst "2021")
  )
(reg/register! comment-schema)

(comment
  (require '[malli.core :as m])
  (as-> (mr/schemas mr/custom-registry) x
    (keys x)
    (filter keyword? x)
    (sort x)
    ;(filter #(keyword (first %)) x)
    ;(sort x)
    )

  (m/schema (m/deref ::db/created-at))
  (m/schema ::db/created-at)
  (m/schema ::db/updated-at)

  (m/schema ::comment)
  (mg/generate ::comment {:size 3})
  (m/validate (m/schema ::db/created-at) #inst "2020")
  (m/schema ::comment {:registry mr/custom-registry}))
