(ns plad.version)

(def cli "0.1.0-alpha")
(def edn-format 1)

(defn show!
  []
  (println (format "plad v%s" cli)))
