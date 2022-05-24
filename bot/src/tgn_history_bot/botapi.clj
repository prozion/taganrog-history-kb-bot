(ns tgn-history-bot.botapi
  (:require [clojure.string :as s]
            [tgn-history-bot.globals :refer :all]
            [clj-http.client :as http]))

(defn send-text [text chat-id & [parse-mode & _]]
    ; (println (str base-url "/sendMessage") chat-id text)
    (let [form-params {:chat_id chat-id ; (str chat-id)
                       :text text}
          form-params (if parse-mode
                          (merge form-params
                                 {:parse_mode (case parse-mode
                                                :markdown "MarkdownV2"
                                                :html "HTML"
                                                "HTML")})
                          form-params)]
      (http/post
        (str base-url "/sendMessage")
        {:form-params form-params})))

(defn get-command [text]
  (some-> (re-seq #"/(\w+)" text) first second s/lower-case))

(defn get-message [body]
  (or (:message body) (:edited_message body)))

(defn get-command-body [text]
  (and
    (get-command text)
    (s/join " " (rest (s/split text #" ")))))
