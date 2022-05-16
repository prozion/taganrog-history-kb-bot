(ns parser.wikimapia
  (:require [clj-http.client :as http]
            [cheshire.core :as cheshire]
            [clojure.string :as s]
            [clojure.set :as set]
            [tgn-history-bot.aux :refer :all :rename {pp pff}]
            [tgn-history-bot.city :as city]
            [tgn-history-bot.globals :as g]
            [org.clojars.prozion.clj-tabtree.tabtree :as tabtree]
            [org.clojars.prozion.clj-tabtree.output :as output]
            ))

(def ^:dynamic *index-tabtree* {})
(def ^:dynamic *cached-response* {})

(def WM-HOUSES-CACHED-EDN "/var/cache/projects/taganrog-history-bot/wikimapia-houses.edn")
(def RESPONSE-CACHE "/var/cache/projects/taganrog-history-bot/response-cache.edn")
(def WM-HOUSES-TABTREE "../factbase/generated/wikimapia-houses.tree")
(def WM-HOUSES-CSV "output/wikimapia-houses.csv")

(set! *default-data-reader-fn* tagged-literal)

(defn fruitful-response? [response]
  (get-in (some-> response :body cheshire/parse-string) ["location" "lat"]))

(defn get-response-by-id [id]
  (let [response (or (*cached-response* id) {})
        truly-read? (fruitful-response? response)]
    (or
      (and truly-read? (do (p ".") response))
      (let [response (http/get
                        (format "http://api.wikimapia.org/?function=place.getbyid&key=%s&id=%s&language=ru&format=json" (:wikimapia-api-key g/settings) id))
            _ (p (if (fruitful-response? response) "+" "-"))
            new-cached-response (conj *cached-response* {id response})]
        (write-to-file RESPONSE-CACHE (pr-str new-cached-response))
        response))))

(defn id->edn [id]
  (try
    (binding [*cached-response* (read-string (slurp RESPONSE-CACHE))]
      (let [response (get-response-by-id id)
            status (:status response)
            _ (when (not= status 200)
                (throw (Exception. (format "Response status is: %s" status))))
            res-edn (some-> response :body cheshire/parse-string)
            error (get-in res-edn ["debug" "message"])
            _ (when error
                (throw (Exception. (format "%s" error))))
            description (res-edn "description")
            urls (re-seq #"https?://\S+" description)
            tags (res-edn "tags")
            categories (map #(% "title") tags)
            photoes (map #(% "full_url") (get-in res-edn ["photos"]))
            ]
          {:id (res-edn "id")
           :title (res-edn "title")
           :description (remove-urls description)
           :category categories
           :url urls
           :wm-url (get-in res-edn ["availableLanguages" "ru" "object_url"])
           ; :address (some-> (res-edn "location") (#(format "%s %s" (% "street") (% "housenumber"))) city/normalize-address)
           :street (get-in res-edn ["location" "street"])
           :housenumber (get-in res-edn ["location" "housenumber"])
           :lat (get-in res-edn ["location" "lat"])
           :lon (get-in res-edn ["location" "lon"])
           :north (get-in res-edn ["location" "north"])
           :east (get-in res-edn ["location" "east"])
           :south (get-in res-edn ["location" "south"])
           :west (get-in res-edn ["location" "west"])
           :photo photoes
           :author (get-in res-edn ["edit_info" "user_name"])
           :comments-n (count (get-in res-edn ["comments"]))
           })
      (catch Exception e
        {:id id :error (.getMessage e)}))))

; (defn get-objects-inside-the-area [bbox]
;   (let [request-url (format
;                       "http://api.wikimapia.org/?function=place.getbyarea&key=%s&lon_min=%s&lat_min=%s&lon_max=%s&lat_max=%s&format=json"
;                       (:wikimapia-api-key g/settings)
;                       (:lon-min bbox)
;                       (:lat-min bbox)
;                       (:lon-max bbox)
;                       (:lat-max bbox)
;                       )
;         response (http/get request-url)
;         res-edn (some-> response :body cheshire/parse-string)]
;     res-edn
;     true))

; (defn parse-wikimapia []
;   (let [WM-HOUSES-CACHED-EDN "/var/cache/projects/taganrog-history-bot/wikimapia-houses.edn"
;         processed-houses (read-string (slurp WM-HOUSES-CACHED-EDN))
;         truly-processed-houses (filter #(and (not (:error %)) (:id %)) processed-houses)
;         truly-processed-houses-ids (map :id truly-processed-houses)
;         ; not-really-processed-houses-ids (into [] (set/difference (set (filter-map :id processed-houses)) (set truly-processed-houses-ids)))
;         ; _ (--- (filter-map #(and (:error %) (:id %)) processed-houses))
;         ; _ (--- not-really-processed-houses-ids)
;         ; _ (throw (Exception. "Ok"))
;
;         objects-tabtree (tabtree/parse-tab-tree "../factbase/houses/indexes.tree")
;         house-wm-ids (filter-map :wm (vals objects-tabtree))
;         objects-data (->>
;                         house-wm-ids
;                         (reduce
;                           (fn [acc id]
;                             (cond
;                               (index-of? truly-processed-houses-ids id)
;                               (do
;                                 (p ".")
;                                 acc)
;
;                               :else
;                               (let [result (id->edn id)]
;                                 (p (if (:error result) "-" "+"))
;                                 (conj acc result))))
;                           truly-processed-houses)
;                         (into []))]
;
;     (write-to-file WM-HOUSES-CACHED-EDN (pr-str objects-data))
;     true
;     ))

(defn process-val [val]
  (cond
    (string? val)
      (->>
        val
        (#(s/replace % "\"" "'"))
        (#(s/replace % "\n" "\\n"))
        (format "\"%s\""))
    (coll? val)
      (s/join "," (remove nil? (map process-val val)))
    :else val))

(defn clean-up [edn]
  (->> edn
      (remove :error)
      (filter #(or (:street %) (:housenumber %) (:title %)))))

(defn get-name-by-index [id]
  (let [item (first (filter #(= id (:wm %)) (vals *index-tabtree*)))]
    (and item (name (:__id item)))))

(defn get-object-name [m]
  (let [address (city/normalize-address (:street m) (:housenumber m))
        title (city/normalize-title (:title m))
        name-by-index (get-name-by-index (:id m))]
    (cond
      (or (:street m) (:housenumber m)) address
      name-by-index name-by-index
      (:title m) title
      :else "_")))

(defn edn->shallow-tabtree [edn root-name]
  (--- (count edn))
  (reduce
    (fn [acc m]
        (format "%s\t%s%s\n"
                acc
                (get-object-name m)
                (reduce
                  (fn [acc2 k]
                    (let [val (m k)]
                      (cond
                        (not val) acc2
                        (and (number? val) (= 0 val)) acc2
                        (and (string? val) (empty? val)) acc2
                        :else
                          (format "%s %s:%s"
                                  acc2
                                  (name k)
                                  (process-val val)))))
                    ""
                    (sort-by-order (keys m) [:id :title :street :housenumber :lat :lon :north :west :east :south :comments-n :photo :author :category :wm-url :url :description]))))
    (str root-name "\n")
    (sort #(city/compare-addresses
              (get-object-name %1)
              (get-object-name %2))
          (clean-up edn))))

(defn build-tabtree []
  (binding [*index-tabtree* (tabtree/parse-tab-tree "../factbase/houses/indexes.tree")]
    (let [
          ; processed-houses (read-string (slurp WM-HOUSES-CACHED-EDN))
          objects-tabtree (tabtree/parse-tab-tree "../factbase/houses/indexes.tree")
          house-wm-ids (filter-map :wm (vals objects-tabtree))
          houses-edn (map id->edn house-wm-ids)
          tabtree-houses (edn->shallow-tabtree houses-edn "processed_houses")]
      (write-to-file WM-HOUSES-TABTREE tabtree-houses)
      true)))

(defn build-csv []
  (write-to-file
    WM-HOUSES-CSV
    (output/make-csv
      (tabtree/parse-tab-tree WM-HOUSES-TABTREE)
      :delimeter "\t"
      :headers [
        [:__id "адрес"]
        [:title "название"]
        [:category "категория"]
        [:street "улица"]
        [:housenumber "номер дома"]
        [:lat "широта"]
        [:lon "долгота"]
        [:id "wmid"]
        [:wm-url "wikimapia"]
        [:url "ссылки"]
        [:author "автор"]
        [:description "описание"]
      ]))
  true)
