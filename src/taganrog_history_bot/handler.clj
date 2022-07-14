(ns taganrog-history-bot.handler
  (:require
            ; [ring.adapter.jetty :refer :all]
            [clojure.string :as s]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [cheshire.core :as cheshire]
            [taganrog-history-bot.botapi :as tb]
            [org.clojars.prozion.odysseus.utils :refer :all]
            [org.clojars.prozion.odysseus.debug :refer :all]
            [org.clojars.prozion.tabtree.tabtree :as tabtree]
            [taganrog-history-bot.kb :as kb]
            [taganrog-history-bot.security :as security]
            [taganrog-history-bot.sparql :as sparql]
            [taganrog-history-bot.ontologies :as ontologies]
            [taganrog-history-bot.city :as city]
  ))

(def OLD-HOUSES-LIMIT 10)
(def DEBUG-CHAT-ID "242892670")

(def ^:dynamic *testing-mode* false)

(defn process-command [message]
  (let [chat-id (str (get-in message [:chat :id]))
        text (or (:text message) "")
        photo (:photo message)
        command (tb/get-command text)
        body (tb/get-command-body text)]
    (--- (format "text: '%s' -> command: '%s'" text  command))
    (try
      (case command
        "/start" (if *testing-mode*
                    (tb/send-text "Исторический бот Таганрога желает вам доброго времени земных суток!" DEBUG-CHAT-ID)
                    (tb/send-text "Исторический бот Таганрога желает вам доброго времени земных суток!" chat-id))
        "/help" (tb/send-text "Данный бот предназначен для работы с исторической базой знаний Таганрога. Доступно множество команд о зданиях города, таких как 'инфо', 'фото', 'старые' и др. <a href=\"https://github.com/prozion/taganrog-history-kb-bot#readme\">Подробнее</a>" chat-id :html)
        "/init" (do
                    (ontologies/init-db {
                        :taganrog_facts
                             ["../taganrog-history-kb/facts/_namespaces.tree"
                              "../taganrog-history-kb/facts/quarters/quarters_houses.tree"
                              "../taganrog-history-kb/facts/houses/wikimapia_houses.tree"
                              "../taganrog-history-kb/facts/houses/years.tree"
                              "../taganrog-history-kb/facts/houses/houses.tree"]
                        :city_ontology
                             [
                              "../taganrog-history-kb/ontology/city.tree"
                              "../taganrog-history-kb/ontology/city_sources.tree"
                              "../taganrog-history-kb/ontology/city_time.tree"
                              "../taganrog-history-kb/ontology/city_wikimapia.tree"]}
                      :reasoner :owl-micro)
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
                       photo-paths (sparql/get-house-photo-paths address)]
                    (if *testing-mode*
                      (--- photo-paths)
                      (if (empty? photo-paths)
                        (tb/send-text (format "%s: нет фотографий" (city/get-canonical-address address)) chat-id)
                        (doseq [photo-path photo-paths]
                          (tb/send-photo photo-path chat-id)))))
        "/nophoto" (let [ans "нетфото"]
                      (if *testing-mode*
                        (--- ans)
                        (tb/send-text ans chat-id :html)))
        "/error" (let [msg "Неизвестная команда"]
                    (if *testing-mode*
                      (--- msg)
                      (tb/send-text msg chat-id :html)))
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
        (--- (format "ошибка: %s" (.getMessage e)))
        (tb/send-text (format "ошибка: %s" (.getMessage e)) chat-id))))))

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
