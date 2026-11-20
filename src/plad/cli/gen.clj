(ns plad.cli.gen
  (:refer-clojure :exclude [run!])
  (:require
   [plad.parse :as parse]
   [plad.generate :as gen]))

(defn run!
  "Run the gen command: build entries and create them (or preview if `:dry-run`)."
  [{:keys [tree dry-run target-dir]}]
  (let [entries (-> (parse/tree->entries tree target-dir))]
    (if dry-run
      (gen/preview! entries)
      (gen/layout! entries))))

(defn command
  "Build the babashka.cli spec for `gen`.
  `wrapper` adapts the handler shape (see plad.cli/wrap-handler)."
  [wrapper]
  {:cmds ["gen"]
   :doc "Generate a directory layout from an EDN file"
   :spec {:file {:desc "Layout configuration file (.edn)"
                 :default "layout.edn"}

          :dry-run {:desc "Show what would be created without creating anything"
                    :coerce :boolean :alias :d}

          :target-dir {:desc "Target directory where the layout will be created"
                       :default "."
                       :alias :t}}
   :args->opts [:file]
   :fn (wrapper run!)})
