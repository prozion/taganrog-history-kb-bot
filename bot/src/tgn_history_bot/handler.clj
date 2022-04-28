(ns tgn-history-bot.handler
  (:require
            ; [ring.adapter.jetty :refer :all]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [cheshire.core :as cheshire]
            [tgn-history-bot.botapi :as tb]
            [tgn-history-bot.kb :as kb]
            [tgn-history-bot.sparql :as sparql]
            [tgn-history-bot.city :as city]
  ))

(defn process-command [message]
  (let [chat-id (get-in message [:chat :id])
        text (:text message)
        command (tb/get-command text)]
    (println "text = '" text "'")
    (case command
      "start" (tb/send-text "Дорогой друг! 1101110!" chat-id)
      "help" (tb/send-text "Доступны такие команды: /start, /help, /list_modern_streets" chat-id)
      ; "building" (let [body (tb/get-command-body text)]
      ;               (tb/send-text body chat-id))
      "list_modern_streets" (tb/send-text (kb/get-modern-streets) chat-id)
      "init_kb" (do
                  (sparql/init-db "../factbase/houses/blocks.tree")
                  (tb/send-text "База знаний инициализирована." chat-id))
      "which_block" (let [ans (or
                                (some-> text tb/get-command-body city/normalize-address sparql/find-blocks first)
                                "Для данного адреса квартал не определен")]
                      ; (println (some-> text tb/get-command-body city/normalize-address))
                      (tb/send-text (format "Квартал: %s" ans) chat-id))
      (do
        (println (format "Couldn't process a line: '%s'" text)))
      )))

; (defn print-streets []
;   (kb/get-streets))

(defroutes app
  (GET "/tgn-history"
        request
        "Please, use POST HTTP request")
  (POST "/tgn-history"
        request
        (let [body (cheshire/parse-string (slurp (:body request)) true)
              message (tb/get-message body)]
          (process-command message)
          "Ok"))
  (route/not-found "<h1>Page not found</h1>"))

; (defn handler [request]
;   { :status 200
;     :headers {"Content-Type" "text/html"}
;     :body "<b>Hi there!</b>" })

; (defn -main [& args]
;   (set-webhook)
;   (run-jetty handler {:port 8080}))
