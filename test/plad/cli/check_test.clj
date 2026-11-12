(ns plad.cli.check-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [babashka.fs :as fs]
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

(deftest run!-target-dir-validation-test
  (testing "throws when target-dir does not exist"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Target directory not found"
                          (check-cli/run! {:config {:layout []}
                                           :target-dir "/tmp/definitely-not-here-xyz"}))))

  (testing "throws when target-dir is a regular file, not a directory"
    (let [file (str (fs/path *tmp-dir* "a-file"))]
      (spit file "")
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Target directory not found"
                            (check-cli/run! {:config {:layout []}
                                             :target-dir file})))))

  (testing "does not throw when target-dir exists and is a directory"
  (is (= :ok (try
               (with-out-str
                 (check-cli/run! {:config {:layout []}
                                  :target-dir *tmp-dir*}))
               :ok
               (catch Exception _ :threw))))))
