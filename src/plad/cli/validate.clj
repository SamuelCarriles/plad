(ns plad.cli.validate)

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
   :fn (wrapper identity)})
