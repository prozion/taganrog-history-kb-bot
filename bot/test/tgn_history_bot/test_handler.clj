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

(defn make-tests []
    (emulate-command "/init")

    (--- "Информация по дому")
    (emulate-command "/i Итальянский 51")

    (--- "Дома с информацией по улице Пушкинская")
    (emulate-command "/street Пушкинская")

    (--- "7 старейших домов")
    (emulate-command "/oldest 7")
)
