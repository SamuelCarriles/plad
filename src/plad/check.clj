(ns plad.check
  (:require [babashka.fs :as fs]))

(defn entry-status
  "Return `entry` with an added `:status` of :exists or :missing."
  [entry]
  (assoc entry :status (if (fs/exists? (:path entry)) :exists :missing)))

(defn check
  "Check each entry against the filesystem, returning them with :status."
  [entries]
  (map entry-status entries))

(defn- display-path
  [{:keys [path dir?]}]
  (if dir? (str path "/") path))

(defn- summary-line
  [checked]
  (let [total (count checked)
        dirs (count (filter :dir? checked))
        files (- total dirs)
        present (count (filter #(= :exists (:status %)) checked))
        missing (- total present)]
    (format "Checked %d entries (%d dirs, %d files).\n%d present, %d missing."
            total dirs files present missing)))

(defn report!
  "Print each checked entry with its status and a one-line summary."
  [checked]
  (doseq [{:keys [status] :as entry} checked]
    (println (format "%-9s %s"
                     (format "[%s]" (name status))
                     (display-path entry))))
  (println)
  (println (summary-line checked)))

