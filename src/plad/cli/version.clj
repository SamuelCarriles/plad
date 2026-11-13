(ns plad.cli.version)

(defn command [version]
  {:cmds ["version"]
   :doc "Print plad version"
   :fn (fn [_] (println (format "plad v%s" version)))})
