(ns plad.status-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [clojure.string :as str]
            [babashka.fs :as fs]
            [plad.status :as status]))

(def ^:dynamic *tmp-dir* nil)

(defn- with-tmp-dir [t]
  (let [dir (fs/create-temp-dir)]
    (try
      (binding [*tmp-dir* (str dir)]
        (t))
      (finally
        (fs/delete-tree dir)))))

(use-fixtures :each with-tmp-dir)

(defn- p
  "Build a path inside the current tmp dir."
  [& segments]
  (str (apply fs/path *tmp-dir* segments)))

(deftest analyze-status-test
  (testing "correctly classifies entries into :exists, :missing, and :conflict"
    (let [dir-ok (p "src")
          file-ok (p "deps.edn")
          missing (p "test.edn")
          conflict (p "bad")]
      (fs/create-dirs dir-ok)
      (spit file-ok "{}")
      (spit conflict "i am a file")
      (let [tree {"src" {:type :dir}
                  "deps.edn" {:type :file}
                  "test.edn" {:type :file}
                  "bad" {:type :dir}}
            analyzed (status/analyze-status tree *tmp-dir*)]
        (is (= :exists (some #(when (= (:path %) dir-ok) (:status %)) analyzed)))
        (is (= :exists (some #(when (= (:path %) file-ok) (:status %)) analyzed)))
        (is (= :missing (some #(when (= (:path %) missing) (:status %)) analyzed)))
        (is (= :conflict (some #(when (= (:path %) conflict) (:status %)) analyzed)))))))

(deftest report!-test
  (testing "prints output for all missing entries"
    (let [tree {"src" {:type :dir}
                "deps.edn" {:type :file}}
          output (with-out-str (status/report! tree *tmp-dir*))]
      (is (str/includes? output "Checking layout against"))
      (is (str/includes? output "[missing]"))
      (is (str/includes? output "Checked 2 entries (1 dirs, 1 files)."))
      (is (str/includes? output "0 present, 2 missing."))))

  (testing "prints output for mixed status (present and missing)"
    (fs/create-dirs (p "src"))
    (let [tree {"src" {:type :dir}
                "deps.edn" {:type :file}}
          output (with-out-str (status/report! tree *tmp-dir*))]
      (is (str/includes? output "[exists]"))
      (is (str/includes? output "[missing]"))
      (is (str/includes? output "1 present, 1 missing."))))

  (testing "prints output when conflicts are present"
    (let [conflict-path (p "conflict-item")]
      (spit conflict-path "i am a file")
      (let [tree {"conflict-item" {:type :dir}}
            output (with-out-str (status/report! tree *tmp-dir*))]
        (is (str/includes? output "[conflict]"))
        (is (str/includes? output "0 present, 0 missing, 1 conflict(s)."))))))
