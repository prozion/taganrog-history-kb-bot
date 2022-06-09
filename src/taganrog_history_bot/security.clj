(ns taganrog-history-bot.security
  (:require
    [clojure.string :as s]))

(defn clean-text [text]
  (s/replace
    text
    #"[^ А-ЯЁёа-яA-Za-z0-9_\-]"
    ""))
