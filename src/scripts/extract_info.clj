; functions to check consistency of the knowledge base

(ns scripts.extract-info
  (:require [clojure.string :as s]
            [org.clojars.prozion.odysseus.debug :refer :all]
            [org.clojars.prozion.odysseus.utils :refer :all]
            [org.clojars.prozion.odysseus.io :as io]
            [org.clojars.prozion.tabtree.tabtree :as tabtree]))

(defn has-category? [v category]
  (if (fn? category)
    (or
      (and (not coll?) (category v))
      (and (coll? v) (not (empty? (filter category v)))))
    (or
      (= v category)
      (and (coll? v) (index-of? v category)))))

(defn extract-categories []
  (let [content (tabtree/parse-tab-tree "../taganrog-history-kb/factbase/houses/wikimapia_houses.tree")
        categories (reduce
                      (fn [acc [k v]]
                        (if (:category v)
                          (conj acc {k (:category v)})
                          acc))
                      {}
                      content)
        years-addresses (keys (tabtree/parse-tab-tree "../taganrog-history-kb/factbase/houses/years.tree"))
        result (reduce
                  (fn [acc k]
                    (let [v (categories k)]
                      (cond
                        (has-category? v "строение 1800-х годов") (format "%s\n%s built:180x" acc (name k))
                        (has-category? v "строение 1810-х годов") (format "%s\n%s built:181x" acc (name k))
                        (has-category? v "строение 1820-х годов") (format "%s\n%s built:182x" acc (name k))
                        (has-category? v "строение 1830-х годов") (format "%s\n%s built:183x" acc (name k))
                        (has-category? v "строение 1840-х годов") (format "%s\n%s built:184x" acc (name k))
                        (has-category? v "строение 1850-х годов") (format "%s\n%s built:185x" acc (name k))
                        (has-category? v "строение 1860-х годов") (format "%s\n%s built:186x" acc (name k))
                        (has-category? v "строение 1870-х годов") (format "%s\n%s built:187x" acc (name k))
                        (has-category? v "строение 1880-х годов") (format "%s\n%s built:188x" acc (name k))
                        (has-category? v "строение 1890-х годов") (format "%s\n%s built:189x" acc (name k))
                        (has-category? v "строение 1900-х годов") (format "%s\n%s built:190x" acc (name k))
                        (has-category? v "строение 1910-х годов") (format "%s\n%s built:191x" acc (name k))
                        (has-category? v #(re-matches #"строение (\S+) года" %)) (format "%s\n%s built:?" acc (name k))
                        :else acc)))
                  ""
                  (sort (minus (keys categories) years-addresses)))]
    (io/write-to-file "../taganrog-history-kb/factbase/generated/years_from_categories.tree" result)
    true))
