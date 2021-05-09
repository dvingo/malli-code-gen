(ns space.matterandvoid.malli-study.dot
  "Clone of malli dot to study transforms in detail"
  (:require [malli.core :as m]
            [malli.util :as mu]
            [malli.registry :as mr]
            [clojure.string :as str]))

(defn -lift
  "huh?
  used once"
  [?schema]
  (let [schema (m/schema ?schema)]
    (if (and (satisfies? m/RefSchema schema)
          (-> schema m/deref m/type (= ::m/schema)))
      ?schema
      [:schema {:registry {::schema ?schema}} ?schema])))

(defn -collect
  "huh?
  used once"
  [schema]
  (let [state (atom {})]
    (m/walk
      schema
      (fn [schema _ _ _]
        (let [properties (m/properties schema)]
          (doseq [[k v] (-> (m/-properties-and-options properties (m/options schema) identity) first :registry)]
            (swap! state assoc-in [:registry k] v))
          (swap! state assoc :schema schema))))
    @state))

(defn -schema-name [base path]
  (->> path (remove #{:malli.core/in}) (map (m/-comp str/capitalize m/-keyword->string)) (into [base]) (str/join "$")))

(defn -normalize [{:keys [registry] :as ctx}]
  (let [registry* (atom registry)]
    (doseq [[k v] registry]
      (swap! registry* assoc k
        (m/walk v (fn [schema path children _]
                    (let [options (update (m/options schema) :registry #(mr/composite-registry @registry* %))
                          schema  (m/into-schema (m/type schema) (m/properties schema) children options)]
                      (if (and (seq path) (= :map (m/type schema)))
                        (let [ref (-schema-name k path)]
                          (swap! registry* assoc ref (mu/update-properties schema assoc ::entity k))
                          ref)
                        schema))))))
    (assoc ctx :registry @registry*)))


(defn -get-links [registry]
  (let [links (atom {})]
    (doseq [[from schema] registry]
      (m/walk
        schema
        (fn [schema path walked-children opts]
          (when-let [to (if (satisfies? m/RefSchema schema) (m/-ref schema))]
            (swap! links update from (fnil conj #{}) to)))))
    @links))


;;
;; public api
;;

(defn transform
  ([?schema] (transform ?schema nil))
  ([?schema options]
   (println "in transform")
   (let [registry (-> ?schema (m/schema options) -lift -collect -normalize :registry)
         _ (println "registry is: " )
         _ (clojure.pprint/pprint registry)
         _        (println "1")
         entity?  #(->> % (get registry) m/properties ::entity)
         _        (println "2")
         props    #(str "[" (str/join ", "
                              (map (fn [[k v]]
                                     (str (name k) "=" (if (fn? v) (v)
                                                                   (pr-str v))))
                                %)) "]")
         esc      #(str/escape (str %) {\> "\\>", \{ "\\{", \} "\\}", \< "\\<", \" "\\\""})
         sorted   #(sort-by (m/-comp str first) %)
         wrap     #(str "\"" % "\"")
         _        (println "4")
         label    (fn [k v] (str "\"{" k "|"
                              (or (some->> (m/entries v) (map (fn [[k s]] (str k " " (esc (m/form (m/deref s)))))) (str/join "\\l"))
                                (esc (m/form v)))
                              "\\l}\""))
         _        (println "5")
         >        #(apply println %&)
         >>       #(apply > " " %&)]
     (println "props: " (props {:shape "record", :style "filled", :color "#000000"}))
     (println "sorted: " (sorted registry))
     (println "get links: " (-get-links registry))
     (println "sorted get links" (sorted (-get-links registry)))
     (doseq
       [[from tos] (sorted (-get-links registry)), to tos]
       (println "from: " from) (println " to: " to)
       (println " wrap from: " (wrap from))
       #_(>> (wrap from) "->" (wrap to) (props {:arrowtail (if (entity? to) "diamond" "odiamond")}))
       )
     #_(with-out-str
       (> "digraph {")
       ;(>> "node" (props {:shape "record", :style "filled", :color "#000000"}))
       ;(>> "edge" (props {:dir "back", :arrowtail "none"}))
       ;(>>)
       ;(doseq [[k v] (sorted registry)]
       ;  (>> (wrap k) (props {:label #(label k v), :fillcolor (if (entity? k) "#e6caab" "#fff0cd")})))
       ;(>>)
       (doseq
         [[from tos] (sorted (-get-links registry)), to tos]
         (>> (wrap from) "->" (wrap to) (props {:arrowtail (if (entity? to) "diamond" "odiamond")})))
       (> "}"))

     #_(with-out-str
       (> "digraph {")
       (>> "node" (props {:shape "record", :style "filled", :color "#000000"}))
       (>> "edge" (props {:dir "back", :arrowtail "none"}))
       (>>)
       (doseq [[k v] (sorted registry)]
         (>> (wrap k) (props {:label #(label k v), :fillcolor (if (entity? k) "#e6caab" "#fff0cd")})))
       (>>)
       (doseq [[from tos] (sorted (-get-links registry)), to tos]
         (>> (wrap from) "->" (wrap to) (props {:arrowtail (if (entity? to) "diamond" "odiamond")})))
       (> "}"))
     )))

