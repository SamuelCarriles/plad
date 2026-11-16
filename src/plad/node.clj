(ns plad.node
  (:refer-clojure :exclude [type])
  (:require [clojure.string :as str]))

(defn str-node?
  "True if `s` is a valid string node for plad: rejects `//` and segments
  made only of dots/spaces."
  [s]
  (and
   (string? s)
   (not (re-find #"//" s))
   (->> (str/split s #"/")
        (every? #(re-matches #"(?![.\s]+$).+" %)))))

(defn dir-node?
  "True if `s` is a valid directory node (string node ending with `/`)."
  [s]
  (and
   (str-node? s)
   (str/ends-with? s "/")))

(defn type
  [node]
  (cond
    (dir-node? node) :dir
    (str-node? node) :file
    :else 
    (throw (ex-info "Unknown node type" {:node node}))))
