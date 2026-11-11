(ns plad.cli
  (:require
   [clojure.edn :as edn]
   [babashka.fs :as fs]
   [plad.validate :as validate]
   [plad.cli.gen :as gen]
   [babashka.cli :as cli]))

(defn- read-config
  "Read and validate the EDN layout config from `(:file data)`.
Returns `data` with `:config` assoc'd. Throws ex-info if the file is
missing or the config fails validation."
  [{:keys [file] :as data}]
  (when-not (fs/exists? file)
    (throw (ex-info (format "Layout configuration file not found: %s" file) {:file file})))
  (assoc data :config (-> file
                          slurp
                          edn/read-string
                          validate/layout-config)))

(def table
  [(gen/command read-config)])

(defn dispatch
  "Dispatch CLI `args` to the matching subcommand in `table`."
  [args]
  (cli/dispatch table args {:prog "plad" :help true}))
