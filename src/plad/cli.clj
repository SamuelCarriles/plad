(ns plad.cli
  (:require
   [clojure.edn :as edn]
   [babashka.fs :as fs]
   [plad.validate :as validate]
   [plad.cli.gen :as gen]
   [babashka.cli :as cli]))

(defn- read-layout-config [file]
  (when-not (fs/exists? file)
    (throw (ex-info (format "Layout configuration file not found: %s" file) {:file file})))
  (-> file
      slurp
      edn/read-string
      validate/layout-config))

(defn- gen-handler
  [{:keys [opts]}]
  (-> (:file opts)
      read-layout-config
      gen/run!))

(def table
  [{:cmds ["gen"]
    :doc "Generate a directory layout from an EDN file"
    :spec {:file {:desc "Layout configuration file (.edn)"
                  :default "layout.edn"}}
    :args->opts [:file]
    :fn gen-handler}])

(defn dispatch [args]
  (cli/dispatch table args {:prog "plad" :help true}))
