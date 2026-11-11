(ns plad.cli
  (:require
   [clojure.edn :as edn]
   [babashka.fs :as fs]
   [plad.validate :as validate]
   [plad.cli.gen :as gen]
   [babashka.cli :as cli]))

(defn- read-config
  [{:keys [file] :as data}]
  (when-not (fs/exists? file)
    (throw (ex-info (format "Layout configuration file not found: %s" file) {:file file})))
  (assoc data :config (-> file
                          slurp
                          edn/read-string
                          validate/layout-config)))

(def table
  [(gen/command read-config)])

(defn dispatch [args]
  (cli/dispatch table args {:prog "plad" :help true}))
