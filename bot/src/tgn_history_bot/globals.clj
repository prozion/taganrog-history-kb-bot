(ns tgn-history-bot.globals)

; (def settings
;   (read-string (slurp "src/tgn_history_bot/settings.clj")))

(def settings {
    :token "5223345298:AAGZ8JaU-kY5atE97EAVuAlX7ScuBXGSAEs" ; history
    :name "История Таганрога"
    :username "tgn_history_bot"
    :bot-url "https://t.me/tgn_history_bot"
    :webhook-url "https://bots.denis-shirshov.ru/tgn-history"
    ; :wikimapia-api-key "34FA424A-BD2C7000-C26DADD4-E4E6B789-33EB6DB7-D6B4ED09-07BA07FA-CAC4C15B"
    ; :wikimapia-api-key "34FA424A-A88E454B-8A03C6DA-91B76601-6DDA60A2-D99A9BB9-BA46AA66-8315E1D"
    ; :wikimapia-api-key "34FA424A-42DD6E49-793FE107-A02DDAB9-4126E5EF-FBD9FE9A-E0AFC92B-E72BE864"
    ; :wikimapia-api-key "34FA424A-F26E0D06-90530930-3E454EEE-17DE81B0-80BD3A22-32A32CFB-CB8E52A3"

    ; :wikimapia-api-key "34FA424A-386C2057-EEE39F66-EEA21F9B-B0473636-C15748B5-C99DBC55-4BBECC8C"
    ; :wikimapia-api-key "34FA424A-921E0BE1-B617C3E2-A2EE12D9-7786BDD3-66B80613-DFC47BD4-BDE2D7A3"
    :wikimapia-api-key "34FA424A-883A8D23-24D547CF-4822C66D-EA0FE669-19CA81E0-95708C8D-46DA8326"
})

(def base-url (format "https://api.telegram.org/bot%s" (:token settings)))
