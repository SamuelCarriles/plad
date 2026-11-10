(ns plad.validate
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [plad.validate.specs :as spec]))

(defn- path->str [path]
  (if (empty? path)
    "(root)"
    (apply str (map #(str "[" % "]") path))))

(defn- best-error
  [problems]
  (let [{:keys [in val]} (first problems)
        preds (set (map :pred problems))
        ->error-str #(format "Location: %s\n Value: %s\n Message: %s\n" (path->str in) (pr-str val) %)]

    (cond
      (preds `spec/snode?)
      (->error-str "is not a valid path")

      (preds `string?)
      (->error-str "is not a string")

      :else
      (->error-str "is not a valid node"))))

(defn layout
  [data]
  (if-let [problems (-> (s/explain-data ::spec/layout data)
                        ::s/problems)]
    (let [grouped  (group-by :in problems)
          messages (mapv best-error (vals grouped))
          n        (count messages)
          errors (map #(format "%s\n" %)
                      messages)
          report (str (format "\n Error%s count: %d\n\n" (if (= n 1) "" "s") n)
                      (str/join "\n" errors)
                      "\n")]
      (throw (ex-info report {:count n
                              :errors errors
                              :layout data})))
    data))
