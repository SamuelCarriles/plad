(ns plad.cli.version)

(defn command [version]
  {:cmds ["version"]
   :doc "Show plad version"
   :fn (fn [_] (println (format "plad version %s" version)))})
