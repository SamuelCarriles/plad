(ns plad.core
  (:require
   [plad.cli :as cli])
  (:gen-class))

(defn- die!
  "Print `msgs` to stderr and exit the process with status 1."
  [& msgs]
  (binding [*out* *err*]
    (apply println msgs))
  (System/exit 1))

(defn -main [& args]
  (try
    (cli/dispatch args)
    (catch clojure.lang.ExceptionInfo e
      (die! (ex-message e)))
    (catch Exception e
      (die! "Unexpected error:" (ex-message e)))))
