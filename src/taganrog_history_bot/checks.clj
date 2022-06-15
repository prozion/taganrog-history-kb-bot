; functions to check consistency of the knowledge base

(ns taganrog-history-bot.checks
  (:require [clojure.string :as s]
            [clojure.set :as set]
            [taganrog-history-bot.sparql :as sparql]
            [taganrog-history-bot.city :as city]
            [org.clojars.prozion.odysseus.debug :refer :all]
            [org.clojars.prozion.odysseus.utils :refer :all]
            [org.clojars.prozion.odysseus.io :as io]
            [org.clojars.prozion.tabtree.tabtree :as tabtree]))

(defn- get-addresses [tabtree]
  (let [buildings (filter #(= (tabtree/get-item-parameter :a %) :Building) (vals tabtree))]
    (unique-concat
      (map :__id buildings)
      (flatten (map :eq buildings)))))

(defn minus-addresses [tabtree-file1 tabtree-file2]
  (let [
        tabtree1 (tabtree/parse-tab-tree tabtree-file1)
        houses1 (get-addresses tabtree1)
        tabtree2 (tabtree/parse-tab-tree tabtree-file2)
        houses2 (get-addresses tabtree2)
        ]
    (into [] (set/difference (set houses1) (set houses2)))))

(defn all-dates-in-blocks []
  (minus-addresses "../taganrog-history-kb/facts/houses/dates.tree" "../taganrog-history-kb/facts/houses/blocks.tree"))

(defn all-names-in-blocks []
  (minus-addresses "../taganrog-history-kb/facts/houses/names.tree" "../taganrog-history-kb/facts/houses/blocks.tree"))

(defn duplicated-ids []
  (let [lines (io/read-file-by-lines "../taganrog-history-kb/facts/houses/quarters.tree")
        ids (map (fn [line] (-> line (s/replace "\t" "") (s/split #"\s+") first)) lines)
        duplicates (->> ids
                        frequencies
                        (filter #(> (second %) 1))
                        (sort (fn [a b] (compare (second b) (second a)))))
        _ (---- duplicates)
       ]
    true))
