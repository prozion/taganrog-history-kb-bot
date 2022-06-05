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
  (binding [*testing-mode* true]
    ; (emulate-command "/init")

    (--- "\n\nИнформация по дому")
    (emulate-command "/i Итальянский 51")

    (--- "\n\nИнформация по дому с фотографиями")
    (emulate-command "/i Александровская 27 --photo")

    (--- "\n\nДома с информацией по улице Пушкинская")
    (emulate-command "/street Пушкинская")

    (--- "\n\n7 старейших домов")
    (emulate-command "/oldest 7"))
)
