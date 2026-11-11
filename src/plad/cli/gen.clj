(ns plad.cli.gen
  (:refer-clojure :exclude [run!])
  (:require
   [plad.parse :as parse]
   [plad.generate :as gen]))

(defn run! [{:keys [config dry-run]}]
  (let [entries (-> config
                    :layout
                    parse/layout->entries)]
    (if dry-run
      (gen/preview! entries)
      (gen/layout! entries))))

(defn- handler
  [reader {:keys [opts]}]
  (-> opts reader run!))

(defn command
  [reader]
  {:cmds ["gen"]
   :doc "Generate a directory layout from an EDN file"
   :spec {:file {:desc "Layout configuration file (.edn)"
                 :default "layout.edn"}
          :dry-run {:desc "Show what would be created without creating anything"
                    :coerce :boolean :alias :d}}
   :args->opts [:file]
   :fn (partial handler reader)})
