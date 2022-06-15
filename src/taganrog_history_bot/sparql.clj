(ns taganrog-history-bot.sparql
  (:require
            [clojure.string :as s]
            [jena.triplestore :as ts :refer [query-sparql]]
            [org.clojars.prozion.tabtree.tabtree :as tabtree]
            [org.clojars.prozion.odysseus.utils :refer :all]
            [org.clojars.prozion.odysseus.time :as time]
            [org.clojars.prozion.odysseus.debug :refer :all]
            [org.clojars.prozion.odysseus.io :as io]
            [org.clojars.prozion.odysseus.text :as text]
            [taganrog-history-bot.city :as city]
            [taganrog-history-bot.globals :refer :all]
            [org.clojars.prozion.tabtree.rdf :as rdf]))

; (def RDF_FILE "../taganrog-history-kb/_export/kb.ttl")

(def PHOTO_DATABASE_PATH "../../data/taganrog-history-kb-photo")

(defn get-db []
  (ts/init-database DATABASE_PATH))

(defn close-db []
  (.close (get-db)))

(defn get-all-node-info [id]
  (let [result (some->>
                  (query-sparql (get-db)
                    (s/replace
                      "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                       prefix : <https://purl.org/taganrog/city#>
                       prefix owl: <http://www.w3.org/2002/07/owl#>
                       SELECT ?predicate ?object
                       WHERE {
                         :<id> ?predicate ?object .
                       }"
                       #"<id>"
                       id))
                  ts/result->hash-no-ns)]
      result))

(defn print-all-node-info [id]
  (---- (get-all-node-info id)))

(defn merge-query-results [results]
  (reduce (fn [acc result]
            (merge-with
              (fn [a b] (cond
                          (not a) b
                          (= a b) a
                          (coll? a) (conj a b)
                          :else (list a b)))
              acc result))
      {}
      results))

(defn get-single-year [years]
  (if (coll? years)
    (str (apply min (map ->integer years)))
    years))

(defn get-house-info [address]
  (let [query-result (->>
                        (query-sparql (get-db)
                          (s/replace
                            "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                             prefix owl: <http://www.w3.org/2002/07/owl#>
                             prefix : <https://purl.org/taganrog/city#>
                             SELECT ?description ?quarter ?year ?photo ?url ?title ?alias_address
                             WHERE {
                               OPTIONAL { :<address-id> :описание ?description }
                               OPTIONAL { :<address-id> owl:sameAs ?alias_address }
                               OPTIONAL { :<address-id> :квартал ?quarter }
                               OPTIONAL { :<address-id> :начало-здания ?year }
                               OPTIONAL { :<address-id> :фото ?photo }
                               OPTIONAL { :<address-id> :url ?url }
                               OPTIONAL { :<address-id> :название ?title }
                             }"
                             #"<address-id>"
                             address))
                        ts/result->hash-no-ns
                        merge-query-results)
        query-result (and query-result (merge query-result {:normalized-address address}))
        year-result (get-single-year (query-result :year))
        output-parameter (fn [value & args]
                            (let [header (first args)]
                              (if value
                                (format "\n\n%s%s%s"
                                  (if header (-> header text/boldify text/->str) "")
                                  (if header " " "")
                                  value)
                                "")))
        link-html (when-let
                     [urls (query-result :url)]
                     (text/make-html-link "Подробнее" (if (coll? urls) (first urls) urls)))
        photo-html (let [photos (query-result :photo)
                         photos (and photos (if (coll? photos) (first photos) photos))]
                      (and photos (format "<a href=\"%s\">%s</a>" photos photos)))
        str-result
          (str
            (or
              (some-> query-result :normalized-address city/get-canonical-address)
              "")
            ;; Заголовок
            (output-parameter (some-> query-result :title text/boldify))
            ;; Главный адрес
            (when (not (:description query-result)) (output-parameter (:alias_address query-result) "См."))
            ;; Квартал
            (output-parameter (:quarter query-result) "Квартал:")
            ;; Год постройки
            (output-parameter year-result "Построен:")
            ;; Описание
            (output-parameter (:description query-result))
            ;; Картинка
            (output-parameter (or link-html photo-html)))]
    (if (not query-result)
      "<i>Информация отсутствует</i>"
      str-result)))

(defn get-house-photo [address]
  (let [query-result (->>
                  (query-sparql (get-db)
                    (s/replace
                      "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                       prefix owl: <http://www.w3.org/2002/07/owl#>
                       prefix : <https://purl.org/taganrog/city#>
                       SELECT ?photo
                       WHERE {
                         :<address-id> :фото ?photo
                       }"
                       #"<address-id>"
                       address))
                  ts/result->hash-no-ns
                  merge-query-results)
        photo-html (let [photo-urls (query-result :photo)
                         photo-urls (and photo-urls (if (coll? photo-urls) photo-urls (list photo-urls)))]
                      (if photo-urls
                        (reduce
                          (fn [acc photo-url]
                            (format "%s<a href=\"%s\">%s</a>\n" acc photo-url photo-url))
                          ""
                          photo-urls)
                        (format "%s: фотографий этого здания в базе пока нет" (city/get-canonical-address address))))]
    photo-html))

(defn get-house-photo-paths [address]
  (let [photo-dirpath (str PHOTO_DATABASE_PATH "/" address)
        files (io/list-files photo-dirpath)]
    (map #(str photo-dirpath "/" %) files)))

(defn list-houses-on-the-street [street]
  (let [sparql-result
          (some->>
            (query-sparql (get-db)
              (s/replace
                "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 prefix owl: <http://www.w3.org/2002/07/owl#>
                 prefix : <https://purl.org/taganrog/city#>
                 SELECT ?house
                 WHERE {
                   ?house rdf:type :Здание .
                   ?house :улица :<street> .
                   ?house :описание ?description .
                 }"
                 #"<street>"
                 street))
            ts/result->hash-no-ns)
            result (cond
                      ; (not canonical-street-name) (format "%s: не удалось распознать как улицу в Таганроге" street)
                      (not sparql-result) (format "%s: дома с этой улицы в базе отсутствуют" street)
                      :else
                         (format "<i>Есть информация про дома:</i>\n%s"
                           (->> sparql-result (map :house) (map name) (sort city/compare-address) (map city/get-canonical-address) (s/join "\n"))))]
    result))

(defn list-houses-by-their-age [limit]
  (let [
        all-dates (some->>
                    (query-sparql (get-db)
                      (format
                        "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                         prefix : <https://purl.org/taganrog/city#>
                         SELECT ?house ?date
                         WHERE {
                           ?house rdf:type :Здание .
                           ?house :построено ?date .
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
        result (cond
                 (empty? top-dates) (format "ошибка ввода, возвращен пустой результат")
                 :else
                     (format "<i>%s самых старых домов:</i>\n%s"
                      limit
                      (->>
                        top-dates
                        (map (fn [res] (format "<b>%s</b> – %s" (:date res) (city/get-canonical-address (:house res)))))
                        (s/join "\n"))))]
    result))
