(ns plad.entry
  (:require
   [clojure.string :as str]
   [babashka.fs :as fs]))

(defn- inspect
  "Runs an entry through a sequence of inspectors using reduce.
 Halts early if an inspector sets :action :throw."
  [entry inspectors]
  (reduce (fn [acc insp]
            (let [res (insp acc)]
              (if (= :throw (:action res))
                (reduced res)
                res)))
          entry
          inspectors))

(defn analyze
  "Analyzes a sequence of `entries` against a list of `inspectors`.
 Returns a map {:actions [...] :errors [...]} where valid entries are transformed
 by :on-success and conflicts by :on-conflict. Halts collecting actions once errors occur."
  ([entries inspectors]
   (analyze entries inspectors {}))
  ([entries inspectors {:keys [on-success on-conflict]
                        :or {on-success identity on-conflict identity}}]
   (reduce (fn [summary entry]
             (let [res (inspect entry inspectors)]
               (cond
                 (:error res)
                 (update summary :errors conj (on-conflict res))

                 (empty? (:errors summary))
                 (update summary :actions conj (on-success res))

                 :else
                 summary)))

           {:actions []
            :errors []}
           entries)))

(defn fs-type
  "Returns the actual filesystem type of an entry's :path (:dir, :file, or nil if missing)."
  [{:keys [path]}]
  (cond
    (fs/directory? path) :dir
    (fs/regular-file? path) :file
    :else nil))

(defn inspect-type
  "Inspector function that compares an entry's declared :type against the filesystem.
 Sets :action :create if missing, :action :skip if matching, or :action :throw if conflicting."
  [entry]
  (let [real-type (fs-type entry)]
    (cond
      (nil? real-type)
      (assoc entry :action :create)

      (= real-type (:type entry))
      (assoc entry :action :skip)

      :else
      (assoc entry
             :action :throw
             :error {:code :type-conflict
                     :spec {:type real-type}}))))

(defn- assoc-type-error-msg
  [{:keys [path error] :as entry}]
  (let [current (name (get-in error [:spec :type]))
        declared (name (:type entry))]
    (assoc-in entry [:error :message]
              (format "Conflict at '%s': exists as %s but declared as %s"
                      path current declared))))

(defn throw-type-errors
  "Throws an ExceptionInfo summarizing all type conflict `errors`,
 attaching {:errors errors} in ex-data."
  [errors]
  (let [messages (map (comp :message :error) errors)
        summary-msg (str "Layout conflicts detected:\n"
                         (str/join "\n" (map #(str "  - " %) messages)))]
    (throw (ex-info summary-msg {:errors errors}))))

(defn analyze-types
  "Analyzes entries against the filesystem for type conflicts.
 Throws ex-info if any conflict is found; otherwise returns {:actions [...] :errors []}."
  [entries]
  (let [{:keys [errors] :as result} (analyze entries
                                             [inspect-type]
                                             {:on-conflict assoc-type-error-msg})]
    (if (seq errors)
      (throw-type-errors errors)
      result)))
