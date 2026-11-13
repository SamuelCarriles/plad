(ns plad.validate.specs
  (:require [clojure.spec.alpha :as s]
            [plad.node :as node]))

(s/def ::str-node node/str-node?)
(s/def ::dir-node node/dir-node?)

(s/def ::branch
  (s/and vector?
         (s/cat
          :dir ::dir-node
          :children (s/* ::node))))

(defmulti node-type class)

(defmethod node-type String
  [_]
  ::str-node)

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
