(ns plad.parse
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str]))

(defn- dir-node?
  [s]
  (str/ends-with? s "/"))

(defn- path-str [& nodes]
  (str (apply fs/path nodes)))

(defn node->entries
  "Convert a layout `node` into a seq of `{:path :dir?}` entries.
A node is either a path string (file or directory) or a vector
`[dir & children]`. `prefix` is prepended to every produced path."
  [node prefix]
  (cond
    (string? node)
    [{:path (path-str prefix node)
      :dir? (dir-node? node)}]

    (vector? node)
    (let [[dir & children] node
          dir-path (path-str prefix dir)]
      (into [{:path dir-path :dir? true}]
            (mapcat #(node->entries % dir-path) children)))))

(defn layout->entries
  "Convert a `layout` vector of nodes into a flat seq of entries,
each prefixed with `output`."
  [layout output]
  (mapcat #(node->entries % output) layout))
