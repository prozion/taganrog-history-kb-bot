(ns tgn-history-bot.sparql
  (:require
            [clojure.string :as s]
            [jena.triplestore :as ts :refer [with-transaction query-sparql]]
            [org.clojars.prozion.tabtree.tabtree :as tabtree]
            [org.clojars.prozion.odysseus.time :as time]
            [org.clojars.prozion.odysseus.debug :refer :all]
            [org.clojars.prozion.odysseus.io :as io]
            [org.clojars.prozion.tabtree.rdf :as rdf]))

(def RDF_FILE "../export/kb.ttl")
(def DATABASE_PATH "/var/db/jena/tgn-history")

(defn get-db []
  (ts/init-database DATABASE_PATH))

(defn init-db [& tree-files]
  (let [
        tabtrees (map tabtree/parse-tab-tree tree-files)
        joined-tabtree (apply merge-with merge tabtrees)
        joined-rdf (rdf/to-rdf
                      joined-tabtree
                      :namespaces {"" "https://purl.org/taganrog#"
                                  "rdf" "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                                  "rdfs" "http://www.w3.org/2000/01/rdf-schema#"
                                  "owl" "http://www.w3.org/2002/07/owl#"})
        _ (io/write-to-file RDF_FILE joined-rdf)
        db (ts/init-db DATABASE_PATH RDF_FILE)]
      db))

(defn init-db-from-rdf []
  (ts/init-db DATABASE_PATH "../export/kb-mini.ttl"))

(defn get-all-node-info [id]
  (let [result (some->>
                  (query-sparql (get-db)
                    (s/replace
                      "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                       prefix : <https://purl.org/taganrog#>
                       prefix owl: <http://www.w3.org/2002/07/owl#>
                       SELECT ?predicate ?object
                       WHERE {
                         :<id> ?predicate ?object .
                       }"
                       #"<id>"
                       id))
                  ts/result->hash-no-ns)]
      result))

(defn get-house-info [address]
  (let [result (->>
                  (query-sparql (get-db)
                    (s/replace
                      "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                       prefix : <https://purl.org/taganrog#>
                       prefix owl: <http://www.w3.org/2002/07/owl#>
                       SELECT ?description ?quarter ?year ?url ?title
                       WHERE {
                         OPTIONAL { :<address-id> :description ?description }
                         OPTIONAL { ?quarter :has_building :<address-id> }
                         OPTIONAL { ?quarter :has_building ?t.
                                    ?t :eq :<address-id> }
                         OPTIONAL { :<address-id> :built ?year }
                         OPTIONAL { :<address-id> :url ?url }
                         OPTIONAL { :<address-id> :title ?title }
                       }"
                       #"<address-id>"
                       address))
                  ts/result->hash-no-ns
                  first)]
    (and result (merge result {:normalized-address address}))))

(defn list-houses-on-the-street [street]
  (let [result (some->>
                  (query-sparql (get-db)
                    (s/replace
                      "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                       prefix : <https://purl.org/taganrog#>
                       prefix owl: <http://www.w3.org/2002/07/owl#>
                       SELECT ?house
                       WHERE {
                         ?house rdf:type :House .
                         ?house :street :<street> .
                         ?house :description ?description .
                       }"
                       #"<street>"
                       street))
                  ts/result->hash-no-ns)]
      result))

(defn list-houses-by-their-age [limit]
  (let [all-dates (some->>
                    (query-sparql (get-db)
                      (format
                        "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                         prefix : <https://purl.org/taganrog#>
                         SELECT ?house ?date
                         WHERE {
                           ?house rdf:type :House .
                           ?house :built ?date .
                         }"))
                    ts/result->hash-no-ns)
        sorted-dates (sort
                        (fn [a b]
                          (let [date1 (:date a)
                                date2 (:date b)]
                            (cond
                              (time/d> date1 date2) 1
                              (time/d< date1 date2) -1
                              :else 0)))
                        all-dates)
        top-dates (take limit sorted-dates)
        ]
      top-dates))
