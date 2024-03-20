(ns taganrog-history-bot.kb
  (:require
    [clojure.string :as s]
    [org.clojars.prozion.tabtree.tabtree :as tabtree]))

(defn id->name [id]
  (-> id
      name
      (s/replace "_" " ")))

(defn item->name [item]
  (or (:name item)
      (some-> item
              :__id
              id->name)
      (str item)))

(defn get-streets [select-fn]
  (let [streets (tabtree/parse-tab-tree "~/data/taganrog_houses_kgr/facts/streets.tree")
        filtered-street-ids (select-fn streets)
        street-names (map id->name filtered-street-ids)]
        ; _ (println 2222 street-names)
    (s/join "\n" street-names)))

(def get-modern-streets #(get-streets (fn [streets] (get-in streets [:улицы :__children]))))
(def get-old-streets get-modern-streets)
