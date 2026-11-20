(ns plad.entry-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.string :as str]
            [babashka.fs :as fs]
            [plad.entry :as entry]))

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

(defn- dummy-inspect-type
  [entry]
  (if (= (:type entry) :invalid)
    (assoc entry :action :throw :error {:reason "invalid type"})
    entry))

(defn- dummy-inspect-path
  [entry]
  (if (= (:path entry) "bad")
    (assoc entry :action :throw :error {:reason "bad path"})
    (assoc entry :action :create)))

(deftest analyze-success-test
  (testing "returns all entries as actions when there are no errors"
    (let [entries [{:path "src" :type :dir}
                   {:path "src/core.clj" :type :file}]
          result (entry/analyze entries [dummy-inspect-type dummy-inspect-path])]
      (is (empty? (:errors result)))
      (is (= 2 (count (:actions result))))
      (is (= :create (:action (first (:actions result))))))))

(deftest analyze-conflict-test
  (testing "collects errors and stops adding subsequent actions when an error occurs"
    (let [entries [{:path "ok" :type :dir}
                   {:path "bad" :type :file}
                   {:path "ignored" :type :dir}]
          result (entry/analyze entries [dummy-inspect-type dummy-inspect-path])]
      (is (= 1 (count (:errors result))))
      (is (= "bad path" (get-in (first (:errors result)) [:error :reason])))
      (is (= 1 (count (:actions result)))))))

(deftest analyze-transformations-test
  (testing "applies on-success and on-conflict transformers"
    (let [entries [{:path "bad" :type :file}]
          result (entry/analyze entries
                                [dummy-inspect-path]
                                {:on-success #(assoc % :transformed true)
                                 :on-conflict #(assoc % :reported true)})]
      (is (true? (:reported (first (:errors result))))))))

(deftest fs-type-test
  (testing "returns :dir, :file, or nil depending on filesystem state"
    (let [dir (p "src")
          file (p "deps.edn")
          missing (p "nope")]
      (fs/create-dirs dir)
      (spit file "{}")
      (is (= :dir (entry/fs-type {:path dir})))
      (is (= :file (entry/fs-type {:path file})))
      (is (nil? (entry/fs-type {:path missing}))))))

(deftest inspect-type-test
  (testing "sets :create when path does not exist"
    (let [res (entry/inspect-type {:path (p "new-file.txt") :type :file})]
      (is (= :create (:action res)))
      (is (nil? (:error res)))))

  (testing "sets :skip when path exists with matching type"
    (let [dir (p "src")
          file (p "deps.edn")]
      (fs/create-dirs dir)
      (spit file "{}")
      (is (= :skip (:action (entry/inspect-type {:path dir :type :dir}))))
      (is (= :skip (:action (entry/inspect-type {:path file :type :file}))))))

  (testing "sets :throw and attaches conflict details when type mismatches"
    (let [file-as-dir (p "a")
          dir-as-file (p "b")]
      (spit file-as-dir "text")
      (fs/create-dirs dir-as-file)
      (let [res1 (entry/inspect-type {:path file-as-dir :type :dir})
            res2 (entry/inspect-type {:path dir-as-file :type :file})]
        (is (= :throw (:action res1)))
        (is (= {:code :type-conflict :spec {:type :file}} (:error res1)))
        (is (= :throw (:action res2)))
        (is (= {:code :type-conflict :spec {:type :dir}} (:error res2)))))))

(deftest analyze-types-test
  (testing "returns actions when there are no type conflicts"
    (let [existing (p "src")
          missing (p "deps.edn")]
      (fs/create-dirs existing)
      (let [{:keys [actions errors]} (entry/analyze-types
                                      [{:path existing :type :dir}
                                       {:path missing :type :file}])]
        (is (empty? errors))
        (is (= [:skip :create] (mapv :action actions))))))

  (testing "throws ex-info summarizing all conflicts when mismatches exist"
    (let [bad1 (p "a")
          bad2 (p "b")]
      (spit bad1 "file")
      (fs/create-dirs bad2)
      (try
        (entry/analyze-types [{:path bad1 :type :dir}
                              {:path bad2 :type :file}])
        (is false "Expected analyze-types to throw")
        (catch clojure.lang.ExceptionInfo e
          (let [msg (ex-message e)
                errors (:errors (ex-data e))]
            (is (= 2 (count errors)))
            (is (str/includes? msg bad1))
            (is (str/includes? msg bad2))
            (is (str/includes? msg "exists as file but declared as dir"))
            (is (str/includes? msg "exists as dir but declared as file"))))))))
