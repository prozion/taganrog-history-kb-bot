(ns tgn-history-bot.handler
  (:require
            ; [ring.adapter.jetty :refer :all]
            [clojure.string :as s]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [cheshire.core :as cheshire]
            [tgn-history-bot.botapi :as tb]
            [odysseus.debug :refer :all]
            [odysseus.files :refer :all]
            [tgn-history-bot.kb :as kb]
            [tgn-history-bot.security :as security]
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
      "init" (do
                  (sparql/init-db
                    "../factbase/houses/quarters.tree"
                    "../factbase/houses/wikimapia-houses.tree"
                    "../factbase/houses/years.tree"
                    "../factbase/houses/houses.tree"
                    "../ontology/city-basic.tree"
                    )
                  )
                  ; (tb/send-text "База знаний инициализирована." chat-id))
      "i" (let [address (some-> text tb/get-command-body city/normalize-address)
                ans (or
                      (sparql/get-house-info address)
                      {:normalized-address address :description "Информация отсутствует"})]
              ; (--- (city/build-house-summary ans)))
              (tb/send-text (city/build-house-summary ans) chat-id :html))
      "lsh" (let [street (some-> text tb/get-command-body security/clean-text)
                  ; canonical-street-name (city/get-canonical-address street)
                  sparql-result (sparql/list-houses-on-the-street (city/normalize-address street))
                  text-result (cond
                                ; (not canonical-street-name) (format "%s: не удалось распознать как улицу в Таганроге" street)
                                (not sparql-result) (format "%s: дома с этой улицы в базе отсутствуют" street)
                                :else
                                    (format "Есть данные про дома:\n%s"
                                      (->> sparql-result (map :house) (map name) (sort city/compare-address) (map city/get-canonical-address) (s/join "\n"))))
                  ]
              (--- text-result))
              ; (tb/send-text text-result chat-id :html))
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
