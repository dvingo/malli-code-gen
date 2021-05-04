(ns space.matterandvoid.util
  (:refer-clojure :exclude [uuid])
  (:require
    [malli.core :as m]
    [malli.error :as me]
    [malli.registry :as mr]
    [space.matterandvoid.data-model.db :as db]
    [taoensso.timbre :as log])
  ;#?(:cljs (:require-macros [space.matterandvoid.util :refer [make-compare-date-fns-js]]))
  #?(:clj (:import [java.util UUID])))

#?(:cljs (defn uuid
           "Without args gives random UUID.
            With args, builds UUID based on input (useful in tests)."
           ([] (random-uuid))
           ([s] (cljs.core/uuid s)))

   :clj  (defn uuid
           "Without args gives random UUID.
           With args, builds UUID based on input (useful in tests)."
           ([] (UUID/randomUUID))
           ([int-or-str]
            (if (int? int-or-str)
              (UUID/fromString
                (format "ffffffff-ffff-ffff-ffff-%012d" int-or-str))
              (UUID/fromString int-or-str)))))

;(m/=> uuid
;  [:function
;   [:=> [:cat] :uuid]
;   [:=> [:cat [:or [:string {:max 16}] :int]] :uuid]])

(defn make-compare-date-fn
  [cmp fn]
  (let [fn-sym (symbol fn)]
    (println "fn-sym: " fn-sym)
    (println "fn: " fn)
    (println "cmp: " cmp)
    (println "cmp: " (symbol (str cmp "date")))
    `(defn ~(symbol (str cmp "date"))
       [a# b#]
       (println "Comparing: " a# " with " b#)
       (~cmp (~fn-sym a# b#) 0))))

(comment
  (>date #inst "2020" #inst "2021") #_false (>date #inst "2021" #inst "2020") #_true
  (=date #inst "2020" #inst "2021") #_false (=date #inst "2020" #inst "2020") #_true
  (<=date #inst "2020" #inst "2021") #_true (<=date #inst "2020" #inst "2020") #_true
  (>=date #inst "2020" #inst "2021") #_false (>=date #inst "2020" #inst "2020") #_true
  ;(clojure.repl/source >date)
  )


(defmacro make-compare-date-fns-java []
  (let [fns# [~'< ~'> ~'= ~'<= ~'>=]]
    `(do ~@(->> fns# (map #(make-compare-date-fn % ".compareTo"))))))

;(defmacro make-compare-date-fns-js []
;  `(let [fns# [~'< ~'> ~'= ~'<= ~'>=]]
;     (do ~@(->> fns# (map #(make-compare-date-fn % "-"))))))

#_#_:cljs
    (defmacro make-compare-date-fns []
      (let [c #?(:clj ".compareTo" :cljs "-")
            fns       ['< '> '= '<= '>=]]
        `(do ~@(->> fns (map #(make-compare-date-fn % c))))))


;#?(:clj (make-compare-date-fns-java))

(quote .compareTo)
(comment
  (macroexpand-1 '(make-compare-date-fn > '.compareTo))
  (macroexpand '(make-compare-date-java-fns)))

(comment
  (symbol (name '.compareTo))
  (ns-unalias *ns* '>date)
  (>date #inst "2020" #inst "2021")
  (>date #inst "2021" #inst "2020"))

(def vconcat (comp vec concat))

;; copied from Ben Sless - posted in clojurians slack
#?(:clj (defn comparator-relation
   [[sym msg]]
   ;(log/info "sym: " sym)
   (let [f    (try @(resolve sym)
                   (catch #?(:clj NullPointerException :cljs :error) e nil))
         ;_    (log/info "after that f: " f)
         type (keyword "!" (name sym))]
     (when-not f
       (let [msg (str "Comparison function not found: '" (pr-str sym) "'")]
         (throw (ex-info msg {:sym sym}))))
     {type
      (m/-simple-schema
        (fn [_ [a b]]
          (let [fa #(get % a)
                fb #(get % b)]
            {:type type
             :pred (m/-safe-pred #(f (fa %) (fb %))),
             :type-properties
                   {:error/fn
                    {:en (fn [{:keys [schema value]} _]
                           (str "value at key " a ", " (fa value)
                             ", should be " msg
                             " value at key " b ", " (fb value)))}}
             :min  2,
             :max  2})))})))

(defn comparator-relation2
  [[sym msg]]
  (log/info "sym: " sym)
  `(let [f#    (try @(resolve ~sym)
                    (catch #?(:clj NullPointerException :cljs :error) e nil))
         _     (log/info "after that f: " f#)
         type# (keyword "!" (name ~sym))]
     (when-not f#
       (let [msg# (str "Comparison function not found: '" (pr-str ~sym) "'")]
         (throw (ex-info msg# {:sym ~sym}))))
     {type#
      (m/-simple-schema
        (fn [_ [a# b#]]
          (let [fa# #(get % a#)
                fb# #(get % b#)]
            {:type type
             :pred (m/-safe-pred #(f# (fa# %) (fb# %))),
             :type-properties
                   {:error/fn
                    {:en (fn [{:keys [~'_ value#]} ~'_]
                           (str
                             "value at key " a# ", " (fa# value#)
                             ", should be " ~msg
                             " value at key " b# ", " (fb# value#)))}}
             :min  2,
             :max  2})))}))

;; needs more work for cljs support - dynamic resolve isn't supported
;; likely easiest to just set it up statically for each comparison function
;; will need to anyway for more involved custom schema
;; then it will work in cljs without needing more macros as well.
;; see: https://cljs.github.io/api/cljs.core/resolve

;; the hard mode is to convert all of this to macros to be consumed from cljs
;; to get a literal symbol in place for cljs resolve
;; or get rid of resolve

#?(:clj (defn -comparator-relation-schemas
          []
          (into
            {}
            (map comparator-relation)
            (vconcat
              [['> "greater than"]
               ['>= "greater than or equal to"]
               ['= "equal to"]
               ['== "equal to"]
               ['<= "lesser than or equal to"]
               ['< "lesser than"]]
              [['>date "greater than"]
               ['>=date "greater than or equal to"]
               ['=date "equal to"]
               ['<=date "lesser than or equal to"]
               ['<date "lesser than"]]))))

(defmacro -comparator-relation-schemas2
  []
  `(into
     {}
     (map comparator-relation2)
     (vconcat
       [['> "greater than"]
        ['>= "greater than or equal to"]
        ['= "equal to"]
        ['== "equal to"]
        ['<= "lesser than or equal to"]
        ['< "lesser than"]]
       [['>date "greater than"]
        ['>=date "greater than or equal to"]
        ['=date "equal to"]
        ['<=date "lesser than or equal to"]
        ['<date "lesser than"]])))

;(comment
;  (me/humanize
;    (m/explain
;      (m/schema
;        [:and
;         [:map
;          [:x :int]
;          [:y :int]]
;         [:!/> :x :y]]
;        {:registry
;         (mr/composite-registry
;           m/default-registry
;           (-comparator-relation-schemas))})
;      {:x 1 :y 1}))
;
;  (me/humanize
;    (m/explain
;      (m/schema
;        [:and
;         [:map
;          [:x inst?]
;          [:y inst?]]
;         [:!/>date :x :y]]
;        {:registry
;         (mr/composite-registry
;           m/default-registry
;           (-comparator-relation-schemas))})
;      {:x #inst "2020" :y #inst "2021"})))

(defn get-keyword-schemas [registry]
  (->>
    registry
    mr/schemas
    keys
    (filter keyword?)
    sort
    )
  #_(sort (filter keyword? (keys (mr/schemas m/default-registry))))
  )
(comment
  (get-keyword-schemas m/default-registry)
  (get-keyword-schemas
    (mr/composite-registry
      m/default-registry
      (-comparator-relation-schemas))

    )
  (sort (filter keyword? (keys (mr/schemas m/default-registry))))
  (let)
  (mr/composite-registry
    m/default-registry
    (-comparator-relation-schemas))
  )

(comment
  [:and
   [:map
    ::id
    ::description
    ::comments
    [::sub-tasks {:optional true}]
    [::db/updated-at {:optional true}]
    [::db/created-at {:optional true}]]
   [:fn (fn [{::db/keys [created-at updated-at]}]
          #?(:clj  (<= (.compareTo created-at updated-at) 0)
             :cljs (<= created-at updated-at)))]])
