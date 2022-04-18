(ns tgn-history-bot.globals)

; (def settings
;   (read-string (slurp "src/tgn_history_bot/settings.clj")))

(def settings {
    :token "5223345298:AAGZ8JaU-kY5atE97EAVuAlX7ScuBXGSAEs" ; history
    :name "История Таганрога"
    :username "tgn_history_bot"
    :bot-url "https://t.me/tgn_history_bot"
    :webhook-url "https://bots.denis-shirshov.ru/tgn-history"
})

(def base-url (format "https://api.telegram.org/bot%s" (:token settings)))
