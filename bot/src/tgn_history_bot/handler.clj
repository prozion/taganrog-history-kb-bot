(ns tgn-history-bot.handler
  (:require
            ; [ring.adapter.jetty :refer :all]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [cheshire.core :as cheshire]
            [tgn-history-bot.botapi :as tb]
  ))

(defn process-command [text]
  (case (tb/get-command text)
    "start" "Дорогой друг! 1101110!"
    "help" "Доступны такие команды: /start, /help, /building"
    "building" (tb/get-command-body text)
    :else nil))

(defroutes app
  (GET "/tgn-history"
        request
        "Please, use POST HTTP request")
  (POST "/tgn-history"
        request
        (let [body (cheshire/parse-string (slurp (:body request)) true)
              message (:message body)
              chat-id (get-in message [:chat :id])
              text (:text message)
              answer (process-command text)]
          ; (println "request = " request)
          ; (println "body = " body)
          ; (println "message = " message)
          (println "text = " text)
          (if answer
            (tb/send-text answer chat-id)
            (println (format "Couldn't process a line: %s" text)))))
  (route/not-found "<h1>Page not found</h1>"))

; (defn handler [request]
;   { :status 200
;     :headers {"Content-Type" "text/html"}
;     :body "<b>Hi there!</b>" })

; (defn -main [& args]
;   (set-webhook)
;   (run-jetty handler {:port 8080}))
