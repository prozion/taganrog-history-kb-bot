(ns tgn-history-bot.city
  (:require
            [clojure.string :as s]))

(defn normalize-address [s]
  (-> s s/lower-case (s/replace #"ул\S*|пер\S*" "") (s/replace #"(?<=[а-я])\s+(?=\d)" "_") s/trim s/capitalize))
