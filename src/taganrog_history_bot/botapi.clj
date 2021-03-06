(ns taganrog-history-bot.botapi
  (:require [clojure.string :as s]
            [taganrog-history-bot.globals :refer :all]
            [org.clojars.prozion.odysseus.debug :refer :all]
            [org.clojars.prozion.odysseus.utils :refer :all]
            [clojure.java.io :as io]
            [clj-http.client :as http]))

(defn send-text [text chat-id & [parse-mode server-url & _]]
; server-url - redirect to localhost when debugging
    (let [
          chat-id (str chat-id)
          form-params {:chat_id chat-id ; (str chat-id)
                       :text text}
          form-params (if parse-mode
                          (merge form-params
                                 {:parse_mode (case parse-mode
                                                :markdown "MarkdownV2"
                                                :html "HTML"
                                                "HTML")})
                          form-params)]
      (http/post
        (str (or server-url base-url) "/sendMessage")
        {:form-params form-params})))

(defn send-photo [photo-filepath chat-id]
  (http/post
    (str base-url "/sendPhoto")
    {:multipart
      [
        {:name "chat_id" :content (str chat-id)}
        {:name "photo" :content (io/file photo-filepath)}
      ]}
))

(defn clean-command-string [command-string]
  (s/replace command-string #"[^А-ЯЁа-яA-Za-z0-9 \-_/]" ""))

(defn detect-command-by-first-word [command-string]
  (try
    (let [first-word (first (s/split command-string #" "))]
      (cond
        (empty? command-string) "/error"
        (re-matches #"^\s+$" command-string) "/error"
        (re-matches #"^/init\b.*" first-word) "/init"
        (re-matches #"^/start\b.*" first-word) "/start"
        (re-matches #"^/help\b.*" first-word) "/help"
        (and (re-matches #"^/?((info)|(инфо)|(и)|(i))\b.*" first-word) (re-seq #"\d+" command-string)) "/info"
        (re-matches #"^/?((info)|(инфо)|(и)|(i)|(street)|(проулицу))\b.*" first-word) "/street"
        (re-matches #"^/?((oldest)|(старые)|(с))\b.*" first-word) "/oldest"
        (re-matches #"^/?((photo)|(фото)|(ф))\b.*" first-word) "/photo"
        (re-matches #"^/?((nophoto)|(нетфото))\b.*" first-word) "/nophoto"
        :else "/default"))
    (catch Throwable e "/error")))

(defn parse-command [command-string]
  (let [command-string (clean-command-string command-string)
        command (detect-command-by-first-word command-string)
        words (s/split command-string #" ")
        body (s/join
                " "
                (if (= command "/default")
                  words
                  (rest words)))
        ]
    {:command command
     :body body}))

(defn get-command [text]
  ; (some-> (re-seq #"/(\w+)" text) first second s/lower-case))
  (:command (parse-command text)))

(defn get-command-body [text]
  (:body (parse-command text)))

(defn get-message [body]
  (or (:message body) (:edited_message body)))
