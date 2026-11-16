(ns plad.parse-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as str]
            [plad.parse :as parse]))

(deftest layout->tree-basic
  (testing "empty layout"
    (is (= {} (parse/layout->tree []))))

  (testing "single file at root"
    (is (= {"README" {:type :file}}
           (parse/layout->tree ["README"]))))

  (testing "single dir at root"
    (is (= {"src" {:type :dir}}
           (parse/layout->tree ["src/"]))))

  (testing "multiple siblings at root"
    (is (= {"README" {:type :file}
            "src" {:type :dir}}
           (parse/layout->tree ["README" "src/"])))))

(deftest layout->tree-nested
  (testing "string with slashes creates implicit dirs"
    (is (= {"src" {:type :dir
                   :children {"core.clj" {:type :file}}}}
           (parse/layout->tree ["src/core.clj"]))))

  (testing "deep nesting"
    (is (= {"src" {:type :dir
                   :children {"plad" {:type :dir
                                      :children {"core.clj" {:type :file}}}}}}
           (parse/layout->tree ["src/plad/core.clj"]))))

  (testing "branch with children"
    (is (= {"src" {:type :dir
                   :children {"core.clj" {:type :file}
                              "util.clj" {:type :file}}}}
           (parse/layout->tree [["src/" "core.clj" "util.clj"]]))))

  (testing "nested branches"
    (is (= {"src" {:type :dir
                   :children {"plad" {:type :dir
                                      :children {"core.clj" {:type :file}}}}}}
           (parse/layout->tree [["src/" ["plad/" "core.clj"]]])))))

(deftest layout->tree-idempotent
  (testing "same file declared twice is no-op"
    (is (= {"a" {:type :file}}
           (parse/layout->tree ["a" "a"]))))

  (testing "dir declared explicitly and implicitly is no-op"
    (is (= (parse/layout->tree ["src/core.clj"])
           (parse/layout->tree ["src/" "src/core.clj"]))))

  (testing "redundant branches produce same tree"
    (is (= (parse/layout->tree [["src/" "a.clj" "b.clj"]])
           (parse/layout->tree ["src/a.clj" ["src/" "b.clj"]])))))

(deftest layout->tree-conflicts
  (testing "file then dir same name"
    (is (thrown? clojure.lang.ExceptionInfo
                 (parse/layout->tree ["a" "a/"]))))

  (testing "file as ancestor"
    (is (thrown? clojure.lang.ExceptionInfo
                 (parse/layout->tree ["a" "a/x.txt"]))))

  (testing "dir then file same name"
    (is (thrown? clojure.lang.ExceptionInfo
                 (parse/layout->tree ["a/" "a"]))))

  (testing "conflict across branches"
    (is (thrown? clojure.lang.ExceptionInfo
                 (parse/layout->tree [["src/" "a"] ["src/" ["a/" "x.txt"]]])))))

(deftest layout->tree-error-data
  (testing "ex-info contains useful data"
    (try
      (parse/layout->tree ["a" "a/"])
      (is false "should have thrown")
      (catch clojure.lang.ExceptionInfo e
        (let [data (ex-data e)]
          (is (= "a" (:path data)))
          (is (= :file (:existing data)))
          (is (= :dir (:declared data))))))))

(deftest tree->entries-basic
  (testing "empty tree"
    (is (= [] (parse/tree->entries {} "out"))))

  (testing "single file"
    (is (= [{:type :file :path "out/README"}]
           (parse/tree->entries {"README" {:type :file}} "out"))))

  (testing "children are flattened and :children is removed"
    (is (= [{:type :dir :path "out/src"}
            {:type :file :path "out/src/core.clj"}]
           (parse/tree->entries
            {"src" {:type :dir
                    :children {"core.clj" {:type :file}}}}
            "out"))))

  (testing "empty dir has no children entries"
    (is (= [{:type :dir :path "out/src"}]
           (parse/tree->entries
            {"src" {:type :dir :children {}}}
            "out")))))

(deftest tree->entries-order
  (testing "parents come before their children"
    (let [entries (parse/tree->entries
                   {"a" {:type :dir
                         :children {"b" {:type :dir
                                         :children {"c" {:type :file}}}}}}
                   "out")
          paths (mapv :path entries)]
      (is (= ["out/a" "out/a/b" "out/a/b/c"] paths)))))

(deftest layout->entries-integration
  (testing "layout goes all the way to entries"
    (is (= [{:type :dir :path "out/src"}
            {:type :dir :path "out/src/plad"}
            {:type :file :path "out/src/plad/core.clj"}]
           (parse/layout->entries ["src/plad/core.clj"] "out"))))

  (testing "conflicts still throw through layout->entries"
    (is (thrown? clojure.lang.ExceptionInfo
                 (parse/layout->entries ["a" "a/"] "out"))))

  (testing "error message shows layout path, not target-prefixed path"
    (try
      (parse/layout->entries ["a" "a/"] "out")
      (catch clojure.lang.ExceptionInfo e
        (is (= "a" (:path (ex-data e))))
        (is (not (str/includes? (ex-message e) "out/")))))))
