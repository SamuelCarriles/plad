(ns plad.generate
  (:require
   [babashka.fs :as fs]
   [plad.entry :as entry]))

(defn write-entry!
  "Create the entry's file or directory and any missing parents.
 Assumes the caller has already selected the entry for creation."
  [{:keys [path] t :type}]
  (if (= :dir t)
    (fs/create-dirs path)
    (let [parent (fs/parent path)]
      (when parent
        (fs/create-dirs parent))
      (fs/create-file path))))

(defn layout!
  "Create missing files and directories, skipping entries whose types already match.
 If analysis detects type conflicts, throw before creating anything."
  [entries]
  (let [{:keys [actions]} (entry/analyze-types entries)]
    (doseq [entry actions
            :when (= :create (:action entry))]
      (write-entry! entry))))

(defn preview!
  "Prints entries that would be created (excludes already existing paths)."
  [entries]
  (let [{:keys [actions]} (entry/analyze-types entries)
        to-create (filter #(= :create (:action %)) actions)]
    (if (seq to-create)
      (do
        (println "Would create:")
        (doseq [entry actions
                :when (= :create (:action entry))
                :let [msg (format " - %-7s %s"
                                  (str "[" (-> entry :type name) "]")
                                  (:path entry))]]
          (println msg)))
      (println "Nothing to create, everything already exists."))))
