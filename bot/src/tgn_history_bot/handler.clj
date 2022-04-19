(ns tgn-history-bot.handler
  (:require
            ; [ring.adapter.jetty :refer :all]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [cheshire.core :as cheshire]
            [tgn-history-bot.botapi :as tb]
            [tgn-history-bot.kb :as kb]
  ))

(defn process-command [text]
  (println 11111 text)
  (case (tb/get-command text)
    "start" "Дорогой друг! 1101110!"
    "help" "Доступны такие команды: /start, /help, /building"
    "building" (tb/get-command-body text)
    "streets" (kb/get-streets)
    :else nil))

; (defn print-streets []
;   (kb/get-streets))

(defroutes app
  (GET "/tgn-history"
        request
        "Please, use POST HTTP request")
  (POST "/tgn-history"
        request
        (let [body (cheshire/parse-string (slurp (:body request)) true)
              _ (println 22222 body)
              message (:message body)
              chat-id (get-in message [:chat :id])
              text (:text message)
              answer (process-command text)]
          ; (println "request = " request)
          ; (println "body = " body)
          ; (println "message = " message)
          (println "text = " text)
          (cond
            (clojure.string/blank? answer)
              (do
                (println "blank answer")
                (tb/send-text "error: blank result" chat-id))
            answer
              (do
                (println "answer = " answer "chat-id = " chat-id)
                (tb/send-text answer chat-id))
            :else
              (println (format "Couldn't process a line: %s" text)))))
  (route/not-found "<h1>Page not found</h1>"))

; (defn handler [request]
;   { :status 200
;     :headers {"Content-Type" "text/html"}
;     :body "<b>Hi there!</b>" })

; (defn -main [& args]
;   (set-webhook)
;   (run-jetty handler {:port 8080}))
