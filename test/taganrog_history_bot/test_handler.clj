(ns taganrog-history-bot.test-handler
  (:require
            [clojure.string :as s]
            [taganrog-history-bot.handler :refer :all]
            [taganrog-history-bot.botapi :as tb]
            [taganrog-history-bot.sparql :as sparql]
            [org.clojars.prozion.odysseus.debug :refer :all]
  ))

(defn emulate-command [command-str]
  (binding [*testing-mode* true]
    (process-command {:text command-str})))

(defn run-tests []
  (binding [*testing-mode* true]
    ; (emulate-command "/init")

    (--- "\n\nИнформация по дому")
    (emulate-command "Итальянский 51")
    (emulate-command "и Чехова 42")
    (emulate-command "инфо Розы Люксембург 43")

    (--- "\n\nИнформация по дому с фотографиями")
    (emulate-command "фото Александровская 27")

    (--- "\n\nДома с информацией по улице Пушкинская")
    (emulate-command "Пушкинская")
    (emulate-command "и Пушкинская")

    (--- "\n\n12 старейших домов")
    (emulate-command "старые 12"))
)

(defn test-send-photo []
  (let [chat-id "242892670"]
    (tb/send-photo
      "/home/denis/data/taganrog-history-kb-photo/Глушко_26/58_full.jpg"
      ; "https://raw.githubusercontent.com/prozion/taganrog-history-kb-photo/main/%D0%93%D0%BE%D0%B3%D0%BE%D0%BB%D0%B5%D0%B2%D1%81%D0%BA%D0%B8%D0%B9_15/76_full.jpg"
      chat-id)))
