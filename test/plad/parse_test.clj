(ns plad.parse-test
  (:require [clojure.test :refer [deftest testing is]]
            [plad.parse :as parse]))

(deftest node->entries-test
  (testing "string node (file)"
    (is (= [{:path "core.clj" :dir? false}]
           (parse/node->entries "core.clj" ""))))

  (testing "string node (directory, trailing slash)"
    (is (= [{:path "src" :dir? true}]
           (parse/node->entries "src/" ""))))

  (testing "string node with prefix"
    (is (= [{:path "src/core.clj" :dir? false}]
           (parse/node->entries "core.clj" "src"))))

  (testing "vector node with single file child"
    (is (= [{:path "src" :dir? true}
            {:path "src/core.clj" :dir? false}]
           (parse/node->entries ["src/" "core.clj"] ""))))

  (testing "nested vector: directories propagate prefix"
    (is (= [{:path "src" :dir? true}
            {:path "src/my" :dir? true}
            {:path "src/my/core.clj" :dir? false}]
           (parse/node->entries ["src/" ["my/" "core.clj"]] ""))))

  (testing "vector with multiple mixed children"
    (is (= [{:path "src" :dir? true}
            {:path "src/core.clj" :dir? false}
            {:path "src/util" :dir? true}
            {:path "src/util/str.clj" :dir? false}
            {:path "src/README.md" :dir? false}]
           (parse/node->entries ["src/" "core.clj" ["util/" "str.clj"] "README.md"] ""))))

  (testing "prefix with and without trailing slash yield the same result"
    (is (= (parse/node->entries ["src/" "core.clj"] "root")
           (parse/node->entries ["src/" "core.clj"] "root/"))))

  (testing "directory entry always precedes its children"
    (let [entries (parse/node->entries ["a/" ["b/" ["c/" "file.txt"]]] "")
          paths (map :path entries)]
      (is (= ["a" "a/b" "a/b/c" "a/b/c/file.txt"] paths)))))

(deftest layout->entries-test
  (testing "empty layout"
    (is (= () (parse/layout->entries [] ""))))

  (testing "flat layout with files and directories"
    (is (= [{:path "deps.edn" :dir? false}
            {:path "src" :dir? true}
            {:path "test" :dir? true}]
           (parse/layout->entries ["deps.edn" "src/" "test/"] ""))))

  (testing "layout with relative output prefix"
    (is (= [{:path "out/src" :dir? true}
            {:path "out/src/core.clj" :dir? false}]
           (parse/layout->entries [["src/" "core.clj"]] "out"))))

  (testing "layout with absolute output prefix"
    (is (= [{:path "/tmp/foo/src" :dir? true}
            {:path "/tmp/foo/src/core.clj" :dir? false}]
           (parse/layout->entries [["src/" "core.clj"]] "/tmp/foo"))))

  (testing "layout with '.' output keeps paths usable"
    (let [entries (parse/layout->entries [["src/" "core.clj"]] ".")
          paths (map :path entries)]
      (is (every? #(or (= % "./src") (= % "./src/core.clj")) paths))
      (is (= 2 (count paths)))))

  (testing "complete realistic layout"
    (is (= [{:path "root/src" :dir? true}
            {:path "root/src/my_project" :dir? true}
            {:path "root/src/my_project/core.clj" :dir? false}
            {:path "root/deps.edn" :dir? false}
            {:path "root/test.edn" :dir? false}]
           (parse/layout->entries [["src/" ["my_project/" "core.clj"]]
                                   "deps.edn"
                                   "test.edn"]
                                  "root")))))
