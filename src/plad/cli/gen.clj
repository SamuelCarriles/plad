(ns plad.cli.gen
  (:refer-clojure :exclude [run!])
  (:require
   [plad.parse :as parse]
   [plad.generate :as gen]))

(defn run!
  "Run the gen command: build entries and create them (or preview if `:dry-run`)."
  [{:keys [config dry-run output]}]
  (let [entries (-> config
                    :layout
                    (parse/layout->entries output))]
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

          :output {:desc "Base directory where the layout will be created"
                   :default "."
                   :alias :o}}
   :args->opts [:file]
   :fn (wrapper run!)})
