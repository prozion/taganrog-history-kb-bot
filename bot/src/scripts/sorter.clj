; functions to check consistency of the knowledge base

(ns scripts.sorter
  (:require [clojure.string :as s]
            [org.clojars.prozion.odysseus.debug :refer :all]
            [org.clojars.prozion.odysseus.io :as io]
            [org.clojars.prozion.tabtree.tabtree :as tabtree]
            [org.clojars.prozion.tabtree.utils :as tutils]
            [org.clojars.prozion.tabtree.output :as tio]))

(def ^:dynamic *tabtree* {})

(defn address-sorter [id1 id2]
  (let [index1 (get-in *tabtree* [id1 :__index])
        index2 (get-in *tabtree* [id2 :__index])]
    (->>
        (cond
          (empty? (or index1 [])) [-1]
          (empty? (or index2 [])) [1]
          :else
            (map (fn [c1 c2]
                    (cond
                      (or (string? c1) (string? c2))
                        (compare (str c1) (str c2))
                      :else
                        (compare c1 c2)))
                  index1
                  index2))
        (remove #(= % 0))
        (#(if (empty? %) 0 (first %))))))

(defn sort-tabtree [source-file target-file]
  (let [unsorted-tabtree (tabtree/parse-tab-tree source-file)
        indexed-tabtree (tutils/index-tabtree unsorted-tabtree)]
      (binding [*tabtree* indexed-tabtree]
        (io/write-to-file target-file (tio/tabtree->string indexed-tabtree :sorter address-sorter)))))

(defn sort-houses []
  (sort-tabtree "../factbase/houses/houses.tree" "../factbase/houses/houses_.tree"))
