(ns plad.parse)

(defn node->entries
  ([node] (node->entries node ""))
  ([node prefix]
   (cond
     (string? node)
     [{:path (str prefix node)}]

     (vector? node)
     (let [[dir & children] node
           dir-path (str prefix dir)]
       (into [{:path dir-path}]
             (mapcat #(node->entries % dir-path) children))))))

(defn layout->entries
  [layout]
  (mapcat node->entries layout))

(comment
  (layout->entries [["docs/" "guide.md" "readme.md"]
                    ["src/" "core.clj" ["http/" "server.clj"]]])
  :.)
