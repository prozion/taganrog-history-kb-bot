; functions to check consistency of the knowledge base

(ns tgn-history-bot.checks
  (:require [clojure.string :as s]
            [clojure.set :as set]
            [tgn-history-bot.aux :refer :all]
            [org.clojars.prozion.clj-tabtree.tabtree :as tabtree]))

(defn- get-addresses [tabtree]
  (map :__id (filter #(= (tabtree/get-item-parameter :a %) :Building) (vals tabtree))))

(defn minus-addresses [tabtree-file1 tabtree-file2]
  (let [
        tabtree1 (tabtree/parse-tab-tree tabtree-file1)
        houses1 (get-addresses tabtree1)
        tabtree2 (tabtree/parse-tab-tree tabtree-file2)
        houses2 (get-addresses tabtree2)
        ]
    (into [] (set/difference (set houses1) (set houses2)))))

(defn all-dates-in-blocks []
  (--- (minus-addresses "../factbase/houses/dates.tree" "../factbase/houses/blocks.tree"))
  true)
