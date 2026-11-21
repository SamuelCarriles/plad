(ns plad.cli
  (:require

   [babashka.fs :as fs]
   [babashka.cli :as cli]
   [clojure.edn :as edn]
   [plad.cli.check :as check]
   [plad.cli.gen :as gen]
   [plad.cli.status :as status]
   [plad.cli.version :as version]
   [plad.validate :as validate]
   [plad.parse :as parse]))

(def version "0.1.0-alpha.1") ;; Needs an update

(defn read-config
  "Read and validate the EDN layout config from `(:file data)`.
Returns `data` with `:tree` assoc'd. Throws ex-info if the file is
missing or the config fails validation."
  [{:keys [file] :as data}]
  (when-not (fs/exists? file)
    (throw (ex-info (format "Layout configuration file not found: %s" file) {:file file})))
  (let [config (-> file slurp edn/read-string validate/layout-config)
        tree (parse/layout->tree (:layout config))]
    (assoc data :tree tree)))

(defn wrap-handler
  [handler]
  (fn [{:keys [opts]}]
    (-> opts read-config handler)))

(def table
  [(check/command wrap-handler)
   (gen/command wrap-handler)
   (status/command wrap-handler)
   (version/command version)])

(defn dispatch
  "Dispatch CLI `args` to the matching subcommand in `table`."
  [args]
  (cli/dispatch table args {:prog "plad" :help true}))
