(ns malli-code-gen.test-utils
  "matcher for quick value matching")


(declare has-matching-values?)


(defn m [template-value actual] ; matcher
  (cond

    (map? template-value)
    (and
      (map? actual)
      (every? true?
              (for [k (keys template-value)]
                (and (contains? actual k)
                     (m (get template-value k) (get actual k))))))

    (set? template-value)
    (has-matching-values? template-value actual)

    (vector? template-value)
    (every? true?
            (for [i (range (count template-value))
                  :let [new-template-value (nth template-value i)]]
              (and (contains? actual i)
                   (or (= ::any new-template-value)
                       (m new-template-value (nth actual i))))))

    (= ::empty-or-none template-value) (empty? actual)

    (= ::any template-value) (some? actual)

    (= ::any-or-nil template-value) (or (some? actual) (nil? actual))

    :else (= template-value actual)))


(defn has-matching-values? [template-values-set checked-set]
  (every?
    (fn [template-v]
      (some #(m template-v %) checked-set))
    template-values-set))

(assert (not (m {:a ::any} nil)))
(assert (not (m {:a ::any} "")))

(assert (m {:a ::any-or-nil} {:a nil}))

(assert (not (m {:a ::any} {:a nil})))
(assert (not (m {:a {:b 4 :c {:d 0}}} {:a {:b 4 :c {:d 1}}})))

(assert (m {:a {:b 4 :c {:d ::any}}} {:a {:b 4 :c {:d 1}}}))

(assert (m {:a ::any} {:a "text"}))

; subset matching
(assert (m #{::any :a} #{:a "text"}))
(assert (not (m #{:b} #{:a "text"})))

(assert (m [::any 2] [1 2]))
(assert (not (m [::any 2] [1 3])))
