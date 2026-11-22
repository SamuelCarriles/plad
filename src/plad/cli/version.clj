(ns plad.cli.version)

(def command
  {:cmds ["version"]
   :doc "Show plad version"
   :fn (fn [_] (println "plad version 0.1.0-alpha.3"))})
