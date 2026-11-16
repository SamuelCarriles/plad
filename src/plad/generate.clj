(ns plad.generate
  (:require
   [babashka.fs :as fs]))

(defn layout!
  "Create directories and files from a seq of entries."
  [entries]
  (doseq [{:keys [path] t :type} entries]
    (when-not (fs/exists? path)
      (if (= :dir t)
        (fs/create-dirs path)
        (let [parent (fs/parent path)]
          (when parent
            (fs/create-dirs parent))
          (fs/create-file path))))))

(defn preview!
  "Prints entries that would be created (excludes already existing paths)."
  [entries]
  (let [paths (map :path entries)
        pending-paths (remove fs/exists? paths)]
    (if (seq pending-paths)
      (do
        (println "Would create:")
        (doseq [path pending-paths]
          (println " " path)))
      (println "Nothing to create, everything already exists."))))
