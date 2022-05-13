(defproject tgn-history-bot "0.1.0"
  :description "Бот над базой знаний о домах и улицах Таганрога"
  :url "https://github.com/prozion/tgn-history-bot"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "3.12.3"]
                 ; [ring/ring-defaults "0.1.5"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [compojure "1.6.2"]
                 [cheshire "5.10.2"]
                 [org.clojars.prozion/clj-tabtree "0.0.10"]
                 [org.apache.jena/jena-arq "3.2.0"]
                 [org.apache.jena/jena-iri "3.2.0"]
                 [org.apache.jena/jena-tdb "3.2.0"]
                 [org.slf4j/slf4j-api "1.7.5"]
                 [org.slf4j/slf4j-simple "1.6.4"]
                ]

  :plugins [
            [lein-ring "0.12.6"]
            ; [lein-immutant "2.1.0"]
            ]

  :ring {:handler tgn-history-bot.handler/app
         :port 1979
        }

  ; :ring-server-options {:ssl? true
  ;                       :ssl-port 443
  ;                       :keystore "certs/jetty.keystore"
  ;                       :key-password "qwerty"}

  :main tgn-history-bot.handler

  ; :repl-options {:init-ns tgn-history-bot.checks}
  ; :repl-options {:init-ns parser.wikimapia}

  :target-path "target/%s"

  :profiles {:dev {:dependencies
                    [[javax.servlet/servlet-api "2.5"]
                     [ring-mock "0.1.5"]]
                   ; :repl-options {:init-ns tgn-history-bot.checks}}
                   :repl-options {:init-ns parser.wikimapia}}
             :uberjar {:aot :all}})
