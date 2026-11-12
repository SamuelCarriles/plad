(ns plad.cli.check
  (:refer-clojure :exclude [run!])
  (:require [babashka.fs :as fs]
            [plad.parse :as parse]
            [plad.check :as check]))

(defn run!
  "Run the check command: compare the layout against the target directory.
  Exits with status 1 if any entry is missing."
  [{:keys [config target-dir]}]
  (when-not (fs/directory? target-dir)
    (throw (ex-info (format "Target directory not found: %s" target-dir)
                    {:target-dir target-dir})))
  (let [checked (-> config
                    :layout
                    (parse/layout->entries target-dir)
                    check/check)]
    (println)
    (println (format "Checking layout against %s" target-dir))
    (println)
    (check/report! checked)
    (when (some #(= :missing (:status %)) checked)
      (System/exit 1))))

(defn command
  "Build the babashka.cli spec for `check`.
  `wrapper` adapts the handler shape (see plad.cli/wrap-handler)."
  [wrapper]
  {:cmds ["check"]
   :doc "Check that a layout exists on the filesystem"
   :spec {:file {:desc "Layout configuration file (.edn)"
                 :default "layout.edn"}
          :target-dir {:desc "Directory to check against"
                       :default "."
                       :alias :t}}
   :args->opts [:file]
   :fn (wrapper run!)})
