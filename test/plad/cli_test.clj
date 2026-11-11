(ns plad.cli-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [babashka.fs :as fs]
            [plad.cli :as cli]))

(def ^:dynamic *tmp-file* nil)

(defn- with-tmp-file [t]
  (let [f (fs/create-temp-file {:suffix ".edn"})]
    (try
      (binding [*tmp-file* (str f)]
        (t))
      (finally
        (fs/delete-if-exists f)))))

(use-fixtures :each with-tmp-file)

(deftest read-config-test
  (testing "throws when file does not exist"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"not found"
                          (cli/read-config {:file "/tmp/definitely-not-here.edn"}))))

  (testing "throws when EDN is malformed"
    (spit *tmp-file* "{:layout [")
    (is (thrown? Exception
                 (cli/read-config {:file *tmp-file*}))))

  (testing "throws when config fails spec validation"
    (spit *tmp-file* "{:layout \"not-a-vector\"}")
    (is (thrown? clojure.lang.ExceptionInfo
                 (cli/read-config {:file *tmp-file*}))))

  (testing "returns data with :config assoc'd when valid"
    (spit *tmp-file* "{:layout [\"deps.edn\"]}")
    (let [result (cli/read-config {:file *tmp-file*})]
      (is (= *tmp-file* (:file result)))
      (is (= {:layout ["deps.edn"]} (:config result)))))

  (testing "preserves other keys in data"
    (spit *tmp-file* "{:layout [\"deps.edn\"]}")
    (let [result (cli/read-config {:file *tmp-file*
                                   :dry-run true
                                   :output "/tmp/foo"})]
      (is (true? (:dry-run result)))
      (is (= "/tmp/foo" (:output result)))
      (is (= {:layout ["deps.edn"]} (:config result))))))
