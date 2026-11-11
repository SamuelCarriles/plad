(ns plad.core
  (:require
   [plad.cli :as cli])
  (:gen-class))

(defn -main [& args]
  (cli/dispatch args))
