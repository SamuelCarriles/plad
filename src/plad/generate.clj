(ns plad.generate
  (:require
   [clojure.string :refer [ends-with?]]
   [babashka.fs :as fs]))

(defn- dir? [path]
  (ends-with? path "/"))

(defn layout!
  "Create directories and files from a seq of entries."
  [entries]
  (doseq [{:keys [path]} entries]
    (if (dir? path)
      (fs/create-dirs path)
      (when-not (fs/exists? path)
        (when-let [parent (fs/parent path)]
          (fs/create-dirs parent))
        (fs/create-file path))))
  (println "Layout generated successfully!"))

(defn preview!
  "Prints entries that would be created (excludes already existing paths)."
  [entries]
  (let [paths (map :path entries)
        pending-paths (remove fs/exists? paths)]
    (if (seq pending-paths)
      (do
        (println "Would create:")
        (doseq [path pending-paths]
          (println " " path))
        (println)
        (println "(dry run, nothing was created)"))
      (println "Nothing to create, everything already exists."))))

