(ns plad.validate-test
  (:require [clojure.test :refer [deftest testing is are]]
            [plad.validate :as validate]))

(deftest layout-config-valid-test
  (testing "valid configs are returned unchanged"
    (are [config] (= config (validate/layout-config config))
      {:layout []}
      {:layout ["deps.edn"]}
      {:layout ["src/"]}
      {:layout [["src/" "core.clj"]]}
      {:doc "Project layout" :layout [["src/" "core.clj"]]}
      {:doc "Basic example"
       :layout [["src/" ["my_project/" "core.clj"]]
                "deps.edn"
                "test.edn"]})))

(deftest layout-config-invalid-test
  (testing "input is not a map"
    (are [config] (thrown? clojure.lang.ExceptionInfo
                           (validate/layout-config config))
      []
      "foo"
      nil))

  (testing "missing :layout key"
    (are [config] (thrown? clojure.lang.ExceptionInfo
                           (validate/layout-config config))
      {}
      {:doc "x"}))

  (testing ":layout is not a vector"
    (are [config] (thrown? clojure.lang.ExceptionInfo
                           (validate/layout-config config))
      {:layout "src/"}
      {:layout '("a")}))

  (testing ":doc is not a string"
    (is (thrown? clojure.lang.ExceptionInfo
                 (validate/layout-config {:doc 123 :layout []}))))

  (testing "node string with //"
    (is (thrown? clojure.lang.ExceptionInfo
                 (validate/layout-config {:layout ["src//core.clj"]}))))

  (testing "node string with dot-only segment"
    (are [config] (thrown? clojure.lang.ExceptionInfo
                           (validate/layout-config config))
      {:layout ["src/.."]}
      {:layout ["./"]}))

  (testing "node string with whitespace-only segment"
    (is (thrown? clojure.lang.ExceptionInfo
                 (validate/layout-config {:layout ["   /core.clj"]}))))

  (testing "node is neither string nor vector"
    (are [config] (thrown? clojure.lang.ExceptionInfo
                           (validate/layout-config config))
      {:layout [123]}
      {:layout [{:a 1}]}))

  (testing "empty vector as node (missing dir)"
    (is (thrown? clojure.lang.ExceptionInfo
                 (validate/layout-config {:layout [[]]}))))

  (testing "vector node with non-string first element"
    (is (thrown? clojure.lang.ExceptionInfo
                 (validate/layout-config {:layout [[123 "x"]]}))))

  (testing "vector node with invalid child"
    (is (thrown? clojure.lang.ExceptionInfo
                 (validate/layout-config {:layout [["src/" 123]]})))))
