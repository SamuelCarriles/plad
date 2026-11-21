(ns plad.cli.check
  (:refer-clojure :exclude [run!])
  (:require [babashka.fs :as fs]
            [plad.parse :as parse]
            [plad.entry :as entry]))

(defn run!
  "Run the check command: compare the layout against the target directory.
  Exits with status 1 if any entry is missing."
  [{:keys [tree no-fs target-dir]}]
  (when-not no-fs
    (when-not (fs/directory? target-dir)
      (throw (ex-info (format "Target directory not found: %s" target-dir)
                      {:target-dir target-dir})))
    (-> tree
        (parse/tree->entries target-dir)
        entry/analyze-types)))

(defn command
  "Build the babashka.cli spec for `check`.
  `wrapper` adapts the handler shape (see plad.cli/wrap-handler)."
  [wrapper]
  {:cmds ["check"]
   :doc "Check layout syntax, semantics, and filesystem compatibility"
   :spec {:file {:desc "Layout configuration file (.edn)"
                 :default "layout.edn"}
          :no-fs {:desc "Check layout configuration syntax and semantics only"
                  :coerce :boolean}
          :target-dir {:desc "Directory to check against"
                       :default (fs/cwd)
                       :alias :t}}
   :args->opts [:file]
   :fn (wrapper run!)})
