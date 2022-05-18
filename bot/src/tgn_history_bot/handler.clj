(ns tgn-history-bot.handler
  (:require
            ; [ring.adapter.jetty :refer :all]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [cheshire.core :as cheshire]
            [tgn-history-bot.botapi :as tb]
            [odysseus.debug :refer :all]
            [odysseus.files :refer :all]
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
      "start" (tb/send-text "Исторический бот Таганрога желает вам доброго времени земных суток!" chat-id)
      "help" (tb/send-text "Доступны такие команды: /start, /help, /i" chat-id)
      ; "building" (let [body (tb/get-command-body text)]
      ;               (tb/send-text body chat-id))
      ; "streets" (tb/send-text (kb/get-modern-streets) chat-id)
      "init" (do
                  (sparql/init-db "../factbase/houses/quarters.tree" "../factbase/houses/wikimapia-houses.tree" "../factbase/houses/years.tree")
                  (tb/send-text "База знаний инициализирована." chat-id))
      ; "q" (let [ans (or
      ;                 (city/get-historical-quarter (some-> text tb/get-command-body))
      ;                 "Для данного адреса квартал не определен")]
      ;                 ; (println (some-> text tb/get-command-body city/normalize-address))
      ;       (tb/send-text (format "Квартал: %s" ans) chat-id))
      "i" (let [address (some-> text tb/get-command-body city/normalize-address)
                ans (or
                      (sparql/get-house-info address)
                      {:normalized-address address :description "Информация отсутствует"})]
              ; (--- (city/build-house-summary ans)))
              (tb/send-text (city/build-house-summary ans) chat-id :html))
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
