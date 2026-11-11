(ns plad.validate.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(defn snode?
  [s]
  (and (not (re-find #"//" s))
       (->> (str/split s #"/")
            (every? #(re-matches #"(?![.\s]+$).+" %)))))

(s/def ::snode (s/and string? snode?))

(s/def ::branch
  (s/and vector?
         (s/cat
          :dir ::snode
          :children (s/* ::node))))

(defmulti node-type class)

(defmethod node-type String
  [_]
  ::snode)

(defmethod node-type clojure.lang.IPersistentVector
  [_]
  ::branch)

(s/def ::node
  (s/multi-spec node-type identity))

(s/def ::layout
  (s/coll-of ::node :kind vector?))

(s/def ::doc string?)

(s/def ::config
  (s/keys :req-un [::layout]
          :opt-un [::doc]))
