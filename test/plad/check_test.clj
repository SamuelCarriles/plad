(ns plad.check-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [clojure.string :as str]
            [babashka.fs :as fs]
            [plad.check :as check]
            [plad.cli.check :as check-cli]))

(def ^:dynamic *tmp-dir* nil)

(defn- with-tmp-dir [t]
  (let [dir (fs/create-temp-dir)]
    (try
      (binding [*tmp-dir* (str dir)]
        (t))
      (finally
        (fs/delete-tree dir)))))

(use-fixtures :each with-tmp-dir)

(defn- p [& segments]
  (str (apply fs/path *tmp-dir* segments)))

(deftest check-test
  (testing "marks existing entries as :exists"
    (fs/create-dirs (p "src"))
    (spit (p "deps.edn") "")
    (let [result (check/check [{:path (p "src") :dir? true}
                               {:path (p "deps.edn") :dir? false}])]
      (is (every? #(= :exists (:status %)) result))))

  (testing "marks non-existing entries as :missing"
    (let [result (check/check [{:path (p "nope") :dir? true}
                               {:path (p "ghost.txt") :dir? false}])]
      (is (every? #(= :missing (:status %)) result))))

  (testing "mixed existence is reported per entry"
    (fs/create-dirs (p "src"))
    (let [result (check/check [{:path (p "src") :dir? true}
                               {:path (p "missing.txt") :dir? false}])]
      (is (= [:exists :missing] (map :status result)))))

  (testing "preserves :path and :dir? on each entry"
    (let [entry {:path (p "foo") :dir? true}
          [result] (check/check [entry])]
      (is (= (p "foo") (:path result)))
      (is (true? (:dir? result)))))

  (testing "returns empty seq for empty input"
    (is (empty? (check/check [])))))

(deftest missing?-test
  (testing "true when at least one entry is missing"
    (is (check-cli/missing? [{:status :exists} {:status :missing}]))
    (is (check-cli/missing? [{:status :missing}])))

  (testing "false when all entries exist"
    (is (not (check-cli/missing? [{:status :exists} {:status :exists}]))))

  (testing "false for empty input"
    (is (not (check-cli/missing? [])))))

(deftest report!-test
  (testing "mentions every path in the output"
    (let [entries [{:path "src" :dir? true :status :exists}
                   {:path "src/core.clj" :dir? false :status :missing}
                   {:path "deps.edn" :dir? false :status :exists}]
          output (with-out-str (check/report! entries))]
      (is (str/includes? output "src/"))
      (is (str/includes? output "src/core.clj"))
      (is (str/includes? output "deps.edn"))))

  (testing "appends '/' to directory paths"
    (let [output (with-out-str
                   (check/report! [{:path "src" :dir? true :status :exists}]))]
      (is (str/includes? output "src/"))))

  (testing "marks each entry with its status"
    (let [output (with-out-str
                   (check/report! [{:path "a" :dir? true :status :exists}
                                   {:path "b" :dir? false :status :missing}]))]
      (is (str/includes? output "[exists]"))
      (is (str/includes? output "[missing]"))))

  (testing "summary shows totals, dirs/files breakdown and present/missing"
    (let [entries [{:path "a" :dir? true :status :exists}
                   {:path "b" :dir? true :status :missing}
                   {:path "c" :dir? false :status :exists}
                   {:path "d" :dir? false :status :exists}
                   {:path "e" :dir? false :status :missing}]
          output (with-out-str (check/report! entries))]
      (is (str/includes? output "5"))
      (is (str/includes? output "2 dirs"))
      (is (str/includes? output "3 files"))
      (is (str/includes? output "3 present"))
      (is (str/includes? output "2 missing")))))
