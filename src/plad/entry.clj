(ns plad.entry)

(defn- run-checks
  [entry checks]
  (reduce (fn [acc check]
            (let [res (check acc)]
              (if (= :throw (:action res))
                (reduced res)
                res)))
          entry
          checks))

(defn analyze
  ([entries checks]
   (analyze entries checks {:on-success identity :on-conflict identity}))
  ([entries checks {:keys [on-success on-conflict]}]
   (reduce (fn [summary entry]
             (let [res (run-checks entry checks)]
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
