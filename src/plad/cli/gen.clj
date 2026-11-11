(ns plad.cli.gen
  (:refer-clojure :exclude [run!])
  (:require
   [plad.parse :as parse]
   [plad.generate :as gen]))

(defn run! [{:keys [layout]}]
  (-> layout
      parse/layout->entries
      gen/layout!))


