(ns plad.entry-test
  (:require [clojure.test :refer [deftest is testing]]
            [plad.entry :as entry]))

(defn- dummy-check-type
  [entry]
  (if (= (:type entry) :invalid)
    (assoc entry :action :throw :error {:reason "invalid type"})
    entry))

(defn- dummy-check-path
  [entry]
  (if (= (:path entry) "bad")
    (assoc entry :action :throw :error {:reason "bad path"})
    (assoc entry :action :create)))

(deftest analyze-success-test
  (testing "returns all entries as actions when there are no errors"
    (let [entries [{:path "src" :type :dir}
                   {:path "src/core.clj" :type :file}]
          result (entry/analyze entries [dummy-check-type dummy-check-path])]
      (is (empty? (:errors result)))
      (is (= 2 (count (:actions result))))
      (is (= :create (:action (first (:actions result))))))))

(deftest analyze-conflict-test
  (testing "collects errors and stops adding subsequent actions when an error occurs"
    (let [entries [{:path "ok" :type :dir}
                   {:path "bad" :type :file}
                   {:path "ignored" :type :dir}]
          result (entry/analyze entries [dummy-check-type dummy-check-path])]
      (is (= 1 (count (:errors result))))
      (is (= "bad path" (get-in (first (:errors result)) [:error :reason])))
      (is (= 1 (count (:actions result)))))))

(deftest analyze-transformations-test
  (testing "applies on-success and on-conflict transformers"
    (let [entries [{:path "bad" :type :file}]
          result (entry/analyze entries
                                [dummy-check-path]
                                {:on-success #(assoc % :transformed true)
                                 :on-conflict #(assoc % :reported true)})]
      (is (true? (:reported (first (:errors result))))))))
