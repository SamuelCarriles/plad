(ns plad.cli
  (:require

   [babashka.fs :as fs]
   [babashka.cli :as cli]
   [clojure.edn :as edn]
   [plad.cli.check :as check-cli]
   [plad.cli.gen :as gen-cli]
   [plad.cli.validate :as validate-cli]
   [plad.cli.version :as version-cli]
   [plad.validate :as validate]))

(def version "0.1.0-alpha")

(defn read-config
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

(defn wrap-handler
  [handler]
  (fn [{:keys [opts]}]
    (-> opts read-config handler)))

(def table
  [(gen-cli/command wrap-handler)
   (check-cli/command wrap-handler)
   (validate-cli/command wrap-handler)
   (version-cli/command version)])

(defn dispatch
  "Dispatch CLI `args` to the matching subcommand in `table`."
  [args]
  (cli/dispatch table args {:prog "plad" :help true}))
