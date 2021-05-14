(ns space.matterandvoid.transform-building-ideas
  "On how to build complete malli generators"
 (:require [malli.core :as m]))


(comment
 "Here malli has this description of its different predicates"
 ; https://github.com/metosin/malli#schema-registry

 "Leaf schemas"
 "From the plethora of malli default schemas,"
 malli.core/predicate-schemas " (so all fns like some? int? etc),"
 malli.core/class-schemas "(read RegExp),"
 "and" malli.core/type-schemas " (like :nil, :int, :double) – can be considered as leaf schemas."
 "So, when you walk a graph – you don't recur into them, only emit prop."
 "E.g. if you generate a pull vector for a user entity"
 [:map [:username string?]] "then :username, being a leaf schema maps into"
 "plain" :username "."

 "Node schemas"
 "Most of" malli.core/base-schemas "will have bodies, so their transformation"
 "will require recursion. Base schemas are"
 #{:enum :schema, :orn :or :and, :ref :maybe :not, :sequential :tuple :vector :set
   :map :map-of :re
   :fn :function :=> :multi}
 "You can see this in malli.clj-kondo ns, with the accept multimethod which"
 "walks into walkable schemas, but for leaf schema returns a simple keyword.")
 ; https://github.com/metosin/malli/blob/master/src/malli/clj_kondo.cljc)

