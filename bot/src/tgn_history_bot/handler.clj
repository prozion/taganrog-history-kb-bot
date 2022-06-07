(ns tgn-history-bot.handler
  (:require
            ; [ring.adapter.jetty :refer :all]
            [clojure.string :as s]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [cheshire.core :as cheshire]
            [tgn-history-bot.botapi :as tb]
            [org.clojars.prozion.odysseus.utils :refer :all]
            [org.clojars.prozion.odysseus.debug :refer :all]
            [tgn-history-bot.kb :as kb]
            [tgn-history-bot.security :as security]
            [tgn-history-bot.sparql :as sparql]
            [tgn-history-bot.city :as city]
  ))

(def OLD-HOUSES-LIMIT 10)

(def ^:dynamic *testing-mode* false)

(defn process-command [message]
  (let [chat-id (get-in message [:chat :id])
        text (or (:text message) "")
        photo (:photo message)
        command (tb/get-command text)
        body (tb/get-command-body text)]
    (--- "text = '" text "'")
    (try
      (case command
        "/start" (tb/send-text "Исторический бот Таганрога желает вам доброго времени земных суток!" chat-id)
        "/help" (tb/send-text "Доступны такие команды: инфо, старые, фото" chat-id)
        "/init" (do
                    (sparql/init-db
                      "../factbase/houses/quarters.tree"
                      "../factbase/houses/wikimapia_houses.tree"
                      "../factbase/houses/years.tree"
                      "../factbase/houses/houses.tree"
                      "../ontology/city-basic.tree"
                      )
                    (if *testing-mode*
                      (--- "База знаний инициализирована")
                      (tb/send-text "База знаний инициализирована." chat-id))
                    )
        "/street" (let [street (some-> body security/clean-text)
                       ; canonical-street-name (city/get-canonical-address street)
                       ans (sparql/list-houses-on-the-street (city/normalize-address street))
                      ]
                    (if *testing-mode*
                      (--- ans)
                      (tb/send-text ans chat-id :html)))
        "/oldest" (let [limit (or (some-> body ->integer) OLD-HOUSES-LIMIT)
                       ans (sparql/list-houses-by-their-age limit)
                      ]
                    (if *testing-mode*
                      (--- ans)
                      (tb/send-text ans chat-id :html)))
        "/photo" (let [address (some-> body city/normalize-address)
                       ans (sparql/get-house-photo address)]
                    (if *testing-mode*
                      (--- ans)
                      (tb/send-text ans chat-id :html)))
        "/nophoto" (let [ans "нетфото"]
                      (if *testing-mode*
                        (--- ans)
                        (tb/send-text ans chat-id :html)))
        ; "/info"
        (let [address (some-> body city/normalize-address)
                  ans (if (re-seq #".+\d+" address)
                        (sparql/get-house-info address)
                        (sparql/list-houses-on-the-street address))
                        ]
                (if *testing-mode*
                  (--- ans)
                  (tb/send-text ans chat-id :html)))
        )
    (catch Throwable e
      (if *testing-mode*
        (--- "ошибка ввода")
        (tb/send-text "ошибка ввода" chat-id))))))

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
