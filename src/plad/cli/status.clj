(ns plad.cli.status
  (:refer-clojure :exclude [run!])
  (:require
   [babashka.fs :as fs]
   [plad.status :as status]))

(defn run!
  "Run the status command: compare the layout against the target directory
   and print a human-readable report."
  [{:keys [tree target-dir]}]
  (when-not (fs/directory? target-dir)
    (throw (ex-info (format "Target directory not found: %s" target-dir)
                    {:target-dir target-dir})))
  (status/report! tree target-dir))

(defn command
  [wrapper]
  {:cmds ["status"]
   :doc "Show status report comparing layout against the filesystem"
   :spec {:file {:desc "Layout configuration file (.edn)"
                 :default "layout.edn"}
          :target-dir {:desc "Directory to inspect"
                       :default (fs/cwd)
                       :alias :t}}
   :args->opts [:file]
   :fn (wrapper run!)})
