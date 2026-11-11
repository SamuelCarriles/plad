(ns plad.validate
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :as expound]
            [plad.validate.specs :as spec]))

(expound/defmsg `spec/snode?
  "must be a valid path (no // and no segments with only dots or spaces)")

(expound/defmsg ::spec/snode
  "must be a non-empty string representing a valid path")

(expound/defmsg ::spec/branch
  "must be a vector where the first element is a directory path and the rest are nodes")

(expound/defmsg ::spec/node
  "must be a path string or a vector (directory with children)")

(expound/defmsg ::spec/layout
  "must be a vector of nodes")

(expound/defmsg ::spec/doc
  "must be a string")

(expound/defmsg ::spec/config
  "must be a map with a required :layout key and an optional :doc key")

(def ^:private printer
  (expound/custom-printer {:print-specs? false
                           :theme :none}))

(defn- explain-str [spec data]
  (with-out-str
    (printer (s/explain-data spec data))))

(defn layout-config
  "Validate `config` against the layout config spec.
Returns `config` unchanged if valid, otherwise throws ex-info with a
human-readable Expound explanation as the message."
  [config]
  (if (s/valid? ::spec/config config)
    config
    (throw (ex-info (explain-str ::spec/config config)
                    {:config config}))))

