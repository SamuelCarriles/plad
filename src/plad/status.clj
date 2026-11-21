(ns plad.status
  (:require
   [plad.parse :as parse]
   [plad.entry :as entry]))

(defn inspect-status
  "Inspector for status reporting. Sets :status to :exists, :missing, or :conflict."
  [entry]
  (let [real-type (entry/fs-type entry)]
    (cond
      (nil? real-type)
      (assoc entry :status :missing)

      (= (:type entry) real-type)
      (assoc entry :status :exists)

      :else
      (assoc entry :status :conflict))))

(defn analyze-status
  "Inspects tree entries against target-dir for status reporting."
  [tree target-dir]
  (let [entries (parse/tree->entries tree target-dir)]
    (map inspect-status entries)))

(defn report!
  "Prints a human-readable status report comparing `tree` against `target-dir`."
  [tree target-dir]
  (let [analyzed (analyze-status tree target-dir)
        type-counts (frequencies (map :type analyzed))
        status-counts (frequencies (map :status analyzed))
        dirs (get type-counts :dir 0)
        files (get type-counts :file 0)
        total (+ dirs files)
        present (get status-counts :exists 0)
        missing (get status-counts :missing 0)
        conflicts (get status-counts :conflict 0)]
    (println)
    (println (format "Checking layout against %s" target-dir))
    (println)
    (doseq [{:keys [path status]} analyzed]
      (println (format "%-9s %s" (str "[" (name status) "]") path)))
    (println)
    (println (format "Checked %d entries (%d dirs, %d files)." total dirs files))
    (if (pos? conflicts)
      (println (format "%d present, %d missing, %d conflict(s)." present missing conflicts))
      (println (format "%d present, %d missing." present missing)))))

