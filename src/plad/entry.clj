(ns plad.entry
  (:require
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


