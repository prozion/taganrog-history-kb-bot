(ns tgn-history-bot.botapi
  (:require [clojure.string :as s]
            [tgn-history-bot.globals :refer :all]
            [clj-http.client :as http]))

(defn send-text [text chat-id]
  (println (str base-url "/sendMessage") chat-id text)
  (http/post
    (str base-url "/sendMessage")
    {:form-params {
                    :chat_id chat-id ; (str chat-id)
                    ; :parse_mode "MarkdownV2"
                    :text text}}))

(defn get-command [text]
  (some-> (re-seq #"/(\w+)" text) first second))

(defn get-message [body]
  (or (:message body) (:edited_message body)))

(defn get-command-body [text]
  (and
    (get-command text)
    (s/join " " (rest (s/split text #" ")))))
