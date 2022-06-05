(ns tgn-history-bot.botapi
  (:require [clojure.string :as s]
            [tgn-history-bot.globals :refer :all]
            [org.clojars.prozion.odysseus.utils :refer :all]
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

(defn parse-command [command-string]
  (let [words (s/split command-string #" ")
        commands (filter #(re-matches #"/[a-z0-9]+" %) words)
        prime-command (and (= (first words) (first commands)) (first commands))
        subcommand-exprs (filter #(re-matches #"--([a-z]+)=?([a-z0-9]+)?" %) words)
        subcommands (reduce
                      #(apply assoc %1 %2)
                      {}
                      (map rest
                        (remove nil?
                          (map #(re-matches #"--([a-z]+)=?([a-z0-9]+)?" %) words))))
        body (s/join " " (filter #(not (index-of? (concat commands subcommand-exprs) %)) words))
        ]
    {:command prime-command :subcommands subcommands :body body}))

(defn get-command [text]
  ; (some-> (re-seq #"/(\w+)" text) first second s/lower-case))
  (:command (parse-command text)))

(defn get-subcommands [text]
  ; (some-> (re-seq #"/(\w+)" text) first second s/lower-case))
  (:subcommands (parse-command text)))

(defn get-command-body [text]
  (:body (parse-command text)))

(defn get-message [body]
  (or (:message body) (:edited_message body)))
