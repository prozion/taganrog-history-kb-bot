(ns tgn-history-bot.kb
  (:require
    [clojure.string :as s]
    [org.clojars.prozion.clj-tabtree.tabtree :as tabtree]))

(defn id->name [item]
  (or (:name item)
      (some-> item
              :__id
              name
              (s/replace "_" " "))
      (str item)))

(defn get-streets []
  (let [streets (tabtree/parse-tab-tree "../factbase/streets.tree")
        _ (println 1111)
        street-names (->>
                        streets
                        vals
                        (filter #(or (:start %) (:end %)))
                        (map id->name))
        _ (println 2222 street-names)
        street-names-printing-view (s/join "/n" street-names)]
    street-names-printing-view))
