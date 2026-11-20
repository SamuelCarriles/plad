(ns plad.generate
  (:require
   [clojure.string :as str]
   [babashka.fs :as fs]
   [plad.entry :as entry]))

(defn- assoc-error-msg
  [{:keys [path error] :as entry}]
  (let [current (name (get-in error [:spec :type]))
        declared (name (:type entry))]
    (assoc-in entry [:error :message]
              (format "Cannot create '%s': exists as %s but declared as %s"
                      path current declared))))

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

(defn analyze-entries
  "Check entries against the filesystem without writing.
 Return {:actions [...] :errors [...]}. Use :actions only when :errors is empty."
  [entries]
  (entry/analyze
   entries
   [entry/inspect-type]
   {:on-conflict assoc-error-msg}))

(defn throw-errors
  "Throw a single ExceptionInfo summarizing all conflicts, with the conflicting
 entries under :errors in ex-data."
  [errors]
  (let [messages (map (comp :message :error) errors)
        summary-msg (str "Layout conflicts detected:\n"
                         (str/join "\n" (map #(str "  - " %) messages)))]
    (throw (ex-info summary-msg {:errors errors}))))

(defn layout!
  "Create missing files and directories, skipping entries whose types already match.
 If analysis detects type conflicts, throw before creating anything."
  [entries]
  (let [{:keys [actions errors]} (analyze-entries entries)]
    (if (seq errors)
      (throw-errors errors)
      (doseq [entry actions
              :when (= :create (:action entry))]
        (write-entry! entry)))))

(defn preview!
  "Prints entries that would be created (excludes already existing paths)."
  [entries]
  (let [{:keys [actions errors]} (analyze-entries entries)
        to-create (filter #(= :create (:action %)) actions)]
    (cond
      (seq errors) (throw-errors errors)

      (seq to-create)
      (do
        (println "Would create:")
        (doseq [entry actions
                :when (= :create (:action entry))
                :let [msg (format " - %-9s %s"
                                  (str "[" (-> entry :type name) "]")
                                  (:path entry))]]
          (println msg)))

      :else
      (println "Nothing to create, everything already exists."))))
