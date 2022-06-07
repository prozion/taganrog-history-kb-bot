(ns tgn-history-bot.test-handler
  (:require
            [clojure.string :as s]
            [tgn-history-bot.handler :refer :all]
            [tgn-history-bot.sparql :as sparql]
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
