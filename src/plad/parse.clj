(ns plad.parse
  (:require
   [clojure.string :as str]
   [plad.node :as node]))

(defn- insert
  "Adds the path given by `parts` to `tree` as a leaf of `leaf-type`,
  creating intermediate dirs as needed. Throws if the path already
  exists with a different type, or if an intermediate part is a file."
  [tree parts leaf-type]
  (letfn [(keyseq->path-str [keyseq]
            (->> keyseq
                 (remove #(= % :children)) (str/join "/")))]
    (loop [tree tree
           keyseq []
           [part & rest-parts] parts]
      (let [new-keyseq (conj keyseq part)
            existing (get-in tree (conj new-keyseq :type))
            leaf? (empty? rest-parts)]
        (cond
          (and leaf? (nil? existing))
          (assoc-in tree new-keyseq {:type leaf-type})

          (and leaf? (= existing leaf-type))
          tree

          leaf?
          (let [path (keyseq->path-str new-keyseq)]
            (throw (ex-info
                    (format "Path '%s' is declared both as %s and as %s"
                            path
                            (name existing)
                            (name leaf-type))
                    {:path path
                     :existing existing
                     :declared leaf-type})))

          (nil? existing)
          (recur (assoc-in tree new-keyseq {:type :dir})
                 (conj new-keyseq :children)
                 rest-parts)

          (= existing :dir)
          (recur tree
                 (conj new-keyseq :children)
                 rest-parts)

          :else
          (let [path (keyseq->path-str new-keyseq)]
            (throw (ex-info
                    (format "Path '%s' is declared as file but used as directory" path)
                    {:path path :existing existing}))))))))

(defn- ->parts
  [s]
  (str/split s #"/"))

(defn- process-node
  "Folds one layout item into `tree`. A string is a single path; a vector
  is `[dir & children]`, with children relative to `dir`."
  [tree node parent-parts]
  (cond
    (string? node)
    (let [parts (into parent-parts (->parts node))
          leaf-type (node/type node)]
      (insert tree parts leaf-type))

    (vector? node)
    (let [[dir & children] node
          dir-parts (into parent-parts (->parts dir))]
      (reduce
       (fn [t child]
         (process-node t child dir-parts))
       tree
       children))
    :else
    (throw (ex-info (format "Invalid node type: %s" (class node)) {:node node}))))

(defn layout->tree
  "Builds a nested tree from `layout`:
  {name {:type :dir|:file, :children {...}}}."
  [layout]
  (reduce #(process-node %1 %2 []) {} layout))

(defn- ->path
  [parent child]
  (if (seq parent)
    (str parent "/" child)
    child))

(defn tree->entries
  "Flattens `tree` into a lazy seq of entries (`:children` removed,
  `:path` added) under `target-dir`. Parents come before their children."
  [tree target-dir]
  (->> (tree-seq

        (fn [[_ info]]
          (:children info))

        (fn [[parent-path info]]
          (map (fn [[child-path child-info]]
                 [(->path parent-path child-path) child-info])
               (sort-by key (:children info))))

        [target-dir {:children tree}])
       rest
       (map (fn [[path info]]
              (-> info
                  (dissoc :children)
                  (assoc :path path))))))

(defn layout->entries
  "Parses `layout` and returns its flattened entries under `target-dir`."
  [layout target-dir]
  (-> layout
      layout->tree
      (tree->entries target-dir)))
