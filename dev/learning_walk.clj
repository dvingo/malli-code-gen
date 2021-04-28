(ns learning-walk)


; shallow walk over data entries with inner,
; then apply outer to the resulting data structure
(clojure.walk/walk
  (fn inner [x]
    (prn :inner x)
    x)
  (fn outer [x]
    (prn :outer x)
    x)
  [1 2 3 4])


; walk doesn't walk in
(clojure.walk/walk
  (fn inner [x]
    (prn :inner x)
    x)
  (fn outer [x]
    (prn :outer x)
    x)
  [[1] 2 {3 :f} 4])

; walk over map
(clojure.walk/walk
  (fn inner [[k v :as map-entry]]
    (prn :inner map-entry (type map-entry))
    [k (* 2 v)])
  (fn outer [x]
    (prn :outer x)
    x)
  {:a 1, :b 2, :c 3})



; Depth Walking

; Pre-Order traversal goes depth first, from the left-most node.
; left, right, root...
; then sibling's  left, right, root...
; etc... visiting left child first, then right child, then the root

; Post-Order traversal goes from the top.
; root, left, right


(clojure.walk/postwalk
  (fn outer [x]
    (prn :x x)
    x)
  [[1] 2 {3 :f} 4])

(clojure.walk/postwalk
  (fn outer [x]
    (prn :x x)
    x)
  {:a 1, :b 2, :c 3})

(clojure.walk/prewalk
  (fn outer [x]
    (prn :x x)
    x)
  {:a 1, :b 2, :c 3})












