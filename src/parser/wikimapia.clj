(ns parser.wikimapia
  (:require [clj-http.client :as http]
            [cheshire.core :as cheshire]
            [clojure.string :as s]
            [clojure.set :as set]
            [org.clojars.prozion.odysseus.io :as io :refer :all]
            [org.clojars.prozion.odysseus.debug :refer :all]
            [org.clojars.prozion.odysseus.text :refer :all]
            [org.clojars.prozion.odysseus.utils :refer :all]
            [taganrog-history-bot.city :as city]
            [taganrog-history-bot.globals :as g]
            [org.clojars.prozion.tabtree.tabtree :as tabtree]
            [org.clojars.prozion.tabtree.output :as output]
            [clojure.java.shell :as shell]
            ))

(def ^:dynamic *index-tabtree* {})
(def ^:dynamic *quarters-index-tabtree* {})
(def ^:dynamic *cached-response* {})

(def WM-HOUSES-CACHED-EDN "/var/cache/projects/taganrog-history-bot/wikimapia_houses.edn")
(def RESPONSE-CACHE "/var/cache/projects/taganrog-history-bot/response-cache.edn")
(def WM-HOUSES-TABTREE "../taganrog-history-kb/_generated/wikimapia_houses.tree")
(def WM-HOUSES-CSV "../taganrog-history-kb/_export/wikimapia_houses.csv")
(def WM-QUARTERS-TABTREE "../taganrog-history-kb/_generated/quarters.tree")
; (def PHOTO-REPOSITORY "../../../data/taganrog-history-kb-photo/")
(def PHOTO-REPOSITORY "/home/denis/data/taganrog-history-kb-photo/")

(set! *default-data-reader-fn* tagged-literal)

(defn fruitful-response? [response]
  "HTTP response in edn format either from server or cache, that contains real data"
  (get-in (some-> response :body cheshire/parse-string) ["location" "lat"]))

(defn get-response-by-id [id response-cache-file]
  (let [cached-responses (slurp response-cache-file)
        cached-responses (if (empty? cached-responses) {} cached-responses)
        cached-responses (read-string cached-responses)
        cached-response (cached-responses id)
        truly-read? (fruitful-response? cached-response)]
    ; (--- 111 truly-read? (keys cached-responses))
    (or
      (and truly-read? (do (p ".") cached-response))
      (let [response (http/get
                        (format "http://api.wikimapia.org/?function=place.getbyid&key=%s&id=%s&language=ru&format=json" (:wikimapia-api-key g/settings) id))
            _ (p (if (fruitful-response? response) "+" "-"))
            new-cached-response (merge cached-responses {id response})]
        (spit response-cache-file (pr-str new-cached-response))
        response))))

(defn handle-colon [text]
  "remove blind colons in the text as a preceding word is treated by tabtree reader as a name of parameter"
  (s/replace text #":(?=($|\S|[,.]))" ": "))

(defn id->edn [id]
  (try
      (let [
            response (get-response-by-id id RESPONSE-CACHE)
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
            polygon (get-in res-edn ["polygon"])
            polygon-sequence
              (reduce
                (fn [acc coors]
                  (concat acc [(coors "x") (coors "y")]))
                []
                polygon)
            ]
          {:wm-id (res-edn "id")
           :wm-название (res-edn "title")
           :описание (-> description remove-urls s/trim handle-colon)
           :wm-категория categories
           :url urls
           :wm-url (get-in res-edn ["availableLanguages" "ru" "object_url"])
           ; :address (some-> (res-edn "location") (#(format "%s %s" (% "street") (% "housenumber"))) city/normalize-address)
           :wm-строка-улицы (get-in res-edn ["location" "street"])
           :wm-номер-дома (get-in res-edn ["location" "housenumber"])
           :lat (get-in res-edn ["location" "lat"])
           :lon (get-in res-edn ["location" "lon"])
           :north (get-in res-edn ["location" "north"])
           :east (get-in res-edn ["location" "east"])
           :south (get-in res-edn ["location" "south"])
           :west (get-in res-edn ["location" "west"])
           :polygon polygon-sequence
           :фото photoes
           :wm-редактор (get-in res-edn ["edit_info" "user_name"])
           :wm-число-комментариев (count (get-in res-edn ["comments"]))
           })
      (catch Exception e
        {:wm-id id :error (.getMessage e)})))

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
      (filter #(or (:wm-строка-улицы %) (:wm-номер-дома %) (:wm-название %)))))

(defn get-name-by-index [id]
  (let [item (first (filter #(= id (:wm %)) (vals *index-tabtree*)))]
    (and item (name (:__id item)))))

(defn get-object-name [m]
  (let [address (city/normalize-address (:wm-строка-улицы m) (:wm-номер-дома m))
        title (city/normalize-title (:wm-название m))
        name-by-index (get-name-by-index (:wm-id m))]
    (cond
      (and (:wm-строка-улицы m) (:wm-номер-дома m)) address
      name-by-index name-by-index
      (:wm-название m) title
      :else "_")))

(defn edn->shallow-tabtree [edn root-name]
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
                        (and (or (coll? val) (string? val))
                             (empty? (remove nil? val))) acc2
                        :else
                          (format "%s %s:%s"
                                  acc2
                                  (name k)
                                  (process-val val)))))
                    ""
                    (sort-by-order (keys m) [:id :wm-название :wm-строка-улицы :wm-номер-дома :lat :lon :north :west :east :south :wm-число-комментариев :фото :wm-редактор :wm-категория :wm-url :url :oписание]))))
    (str root-name "\n")
    (sort #(city/compare-addresses
              (get-object-name %1)
              (get-object-name %2))
          (clean-up edn))))

(defn build-houses-tabtree []
  (binding [*index-tabtree* (tabtree/parse-tab-tree "../taganrog-history-kb/facts/houses/indexes.tree")
            *cached-response* (read-string (slurp RESPONSE-CACHE))]
    (let [
          ; processed-houses (read-string (slurp WM-HOUSES-CACHED-EDN))
          objects-tabtree (tabtree/parse-tab-tree "../taganrog-history-kb/facts/houses/indexes.tree")
          house-wm-ids (remove nil? (map :wm (vals objects-tabtree)))
          houses-edn (map id->edn house-wm-ids)
          tabtree-houses (edn->shallow-tabtree houses-edn "processed_houses")]
      (write-to-file WM-HOUSES-TABTREE tabtree-houses)
      true)))

(defn build-quarters-tabtree []
  (binding [*index-tabtree* (tabtree/parse-tab-tree "../taganrog-history-kb/facts/quarters/indexes.tree")]
    (let [
          ; processed-houses (read-string (slurp WM-HOUSES-CACHED-EDN))
          objects-tabtree (tabtree/parse-tab-tree "../taganrog-history-kb/facts/quarters/indexes.tree")
          quarters-wm-ids (remove nil? (map :wm (vals objects-tabtree)))
          quarters-edn (map id->edn quarters-wm-ids)
          tabtree-quarters (edn->shallow-tabtree quarters-edn "processed_quarters")]
      (write-to-file WM-QUARTERS-TABTREE tabtree-quarters)
      true)))

(defn build-csv []
  (write-to-file
    WM-HOUSES-CSV
    (output/make-csv
      (tabtree/parse-tab-tree WM-HOUSES-TABTREE)
      :delimeter "\t"
      :headers [
        [:__id "адрес"]
        [:wm-название "название"]
        [:wm-категория "категория"]
        [:wm-строка-улицы "улица"]
        [:wm-номер-дома "номер дома"]
        [:lat "широта"]
        [:lon "долгота"]
        [:id "wmid"]
        [:wm-url "wikimapia"]
        [:url "ссылки"]
        [:wm-редактор "автор"]
        [:описание "описание"]
      ]))
  true)

;;; PHOTO database

(defn make-filepath [address-id photo-url]
  (let [address-id (name address-id)
        filename (last (s/split photo-url #"/"))]
    (str PHOTO-REPOSITORY address-id "/" filename)))

(defn get-wikimapia-items []
  (-> WM-HOUSES-TABTREE tabtree/parse-tab-tree vals))

(defn download-photo []
  (let [wikimapia-tabtree (tabtree/parse-tab-tree WM-HOUSES-TABTREE)]
    (doall
      (for [item (get-wikimapia-items)]
        (when (:фото item)
          (let [address-id (:__id item)
                photoes (:фото item)
                photoes (if (coll? photoes) photoes (list photoes))
                dirpath (make-filepath address-id "")]
            (when (not (io/file-exists? dirpath))
              (io/create-directory dirpath))
            (doall
              (for [photo-url photoes]
                (let [filepath (make-filepath address-id photo-url)]
                  (when (not (io/file-exists? filepath))
                    (io/copy-from-url photo-url filepath)))))))))
    true))

(defn resize-images []
  (let [width 800]
    (doall
      (doseq [item (get-wikimapia-items)]
        (let [dirpath (make-filepath (:__id item) "")]
          (when (io/file-exists? dirpath)
            (let [filenames (io/list-files dirpath)
                  filepaths (map #(str dirpath %) filenames)]
              (doall
                (doseq [filepath filepaths]
                  (do
                    (--- filepath
                         (:exit (shell/sh "convert" filepath "-resize" "800x600>" filepath)))
                    ; (exit)
                    ))))))))))
