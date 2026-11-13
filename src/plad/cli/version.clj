(ns plad.cli.version
  (:require [plad.version :as version]))

(defn command []
  {:cmds ["version"]
   :doc "Print plad version"
   :fn (fn [_] (version/show!))})
