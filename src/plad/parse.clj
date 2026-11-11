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
  ([node] (node->entries node ""))
  ([node prefix]
   (cond
     (string? node)
     [{:path (path-str prefix node)
       :dir? (dir-node? node)}]

     (vector? node)
     (let [[dir & children] node
           dir-path (path-str prefix dir)]
       (into [{:path dir-path :dir? true}]
             (mapcat #(node->entries % dir-path) children))))))

(defn layout->entries
  [layout output]
  (mapcat #(node->entries % output) layout))
