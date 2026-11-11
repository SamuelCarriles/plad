(ns plad.generate-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [clojure.string :as str]
            [babashka.fs :as fs]
            [plad.generate :as gen]))

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

(deftest layout!-test
  (testing "creates a directory"
    (let [path (p "src")]
      (gen/layout! [{:path path :dir? true}])
      (is (fs/directory? path))))

  (testing "creates an empty file"
    (let [path (p "deps.edn")]
      (gen/layout! [{:path path :dir? false}])
      (is (fs/regular-file? path))
      (is (zero? (fs/size path)))))

  (testing "creates missing parent directories for files"
    (gen/layout! [{:path (p "src/my/core.clj") :dir? false}])
    (is (fs/directory? (p "src/my")))
    (is (fs/regular-file? (p "src/my/core.clj"))))

  (testing "creates a nested mixed layout"
    (gen/layout! [{:path (p "src") :dir? true}
                  {:path (p "src/my") :dir? true}
                  {:path (p "src/my/core.clj") :dir? false}
                  {:path (p "deps.edn") :dir? false}])
    (is (fs/directory? (p "src/my")))
    (is (fs/regular-file? (p "src/my/core.clj")))
    (is (fs/regular-file? (p "deps.edn"))))

  (testing "is idempotent: running twice does not fail"
    (let [entries [{:path (p "src") :dir? true}
                   {:path (p "src/core.clj") :dir? false}]]
      (gen/layout! entries)
      (gen/layout! entries)
      (is (fs/regular-file? (p "src/core.clj")))))

  (testing "does not overwrite existing files"
    (let [file (p "existing.txt")]
      (spit file "original content")
      (gen/layout! [{:path file :dir? false}])
      (is (= "original content" (slurp file))))))

(deftest preview!-test
  (testing "does not create anything on disk"
    (let [entries [{:path (p "src") :dir? true}
                   {:path (p "deps.edn") :dir? false}]]
      (with-out-str (gen/preview! entries))
      (is (not (fs/exists? (p "src"))))
      (is (not (fs/exists? (p "deps.edn"))))))

  (testing "mentions missing paths"
    (let [missing (p "src/core.clj")
          output (with-out-str
                   (gen/preview! [{:path missing :dir? false}]))]
      (is (str/includes? output missing))))

  (testing "all existing → announces nothing to create"
    (fs/create-dirs (p "src"))
    (let [output (with-out-str
                   (gen/preview! [{:path (p "src") :dir? true}]))]
      (is (re-find #"(?i)nothing to create" output))))

  (testing "mixed: lists only missing paths"
    (fs/create-dirs (p "existing-dir"))
    (let [existing (p "existing-dir")
          missing (p "new-file.txt")
          output (with-out-str
                   (gen/preview! [{:path existing :dir? true}
                                  {:path missing :dir? false}]))]
      (is (str/includes? output missing))
      (let [bullets (filter #(str/starts-with? % "  ")
                            (str/split-lines output))]
        (is (= 1 (count bullets)))))))
