(ns plad.node
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
