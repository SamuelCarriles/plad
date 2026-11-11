(ns plad.cli.validate)

(defn success!
  "Announce that the config from `:file` is valid.
  Reaching this function implies the reader already validated the config."
  [opts]
  (println "Configuration layout file" (:file opts) "is valid."))

(defn command
  "Build the babashka.cli spec for `validate`. `wrapper` adapts the CLI
callback shape: it takes a function of opts and returns a babashka.cli
`:fn` that first reads the config, then invokes the function."
  [wrapper]
  {:cmds ["validate"]
   :doc "Validate a layout configuration file"
   :spec {:file {:desc "Layout configuration file (.edn)"
                 :default "layout.edn"}}
   :args->opts [:file]
   :fn (wrapper success!)})
