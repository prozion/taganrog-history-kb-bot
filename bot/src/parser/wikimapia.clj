(ns parser.wikimapia
  (:require [clj-http.client :as http]
            [cheshire.core :as cheshire]
            [clojure.string :as s]
            [tgn-history-bot.aux :refer :all :rename {pp pff}]
            [tgn-history-bot.city :as city]
            [tgn-history-bot.globals :as g]
            [org.clojars.prozion.clj-tabtree.tabtree :as tabtree]
            ))

(defn get-house-by-id [id]
  (try
    (let [response (http/get
                        (format "http://api.wikimapia.org/?function=place.getbyid&key=%s&id=%s&format=json" (:wikimapia-api-key g/settings) id))
          status (:status response)
          _ (when (not= status 200)
              (throw (Exception. (format "Response status is: %s" status))))
          res-edn (some-> response :body cheshire/parse-string)
          error (get-in res-edn ["debug" "message"])
          _ (when error
              (throw (Exception. (format "%s" error))))
          description (res-edn "description")
          urls (re-seq #"https?://\S+" description)]
        {:id (res-edn "id")
         :title (res-edn "title")
         :description (remove-urls description)
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
         :photo (get-in res-edn ["photos" 0 "full_url"])
         :author (get-in res-edn ["edit_info" "user_name"])
         :comments-n (count (get-in res-edn ["comments"]))
         })
    (catch Exception e
      {:id id :error (.getMessage e)})))

(defn get-objects-inside-the-area [bbox]
  (let [request-url (format
                      "http://api.wikimapia.org/?function=place.getbyarea&key=%s&lon_min=%s&lat_min=%s&lon_max=%s&lat_max=%s&format=json"
                      (:wikimapia-api-key g/settings)
                      (:lon-min bbox)
                      (:lat-min bbox)
                      (:lon-max bbox)
                      (:lat-max bbox)
                      )
        response (http/get request-url)
        res-edn (some-> response :body cheshire/parse-string)]
    res-edn
    true))

(defn parse-wikimapia []
  (let [CACHED-EDN "/var/cache/projects/taganrog-history-bot/wikimapia-houses.edn"
        processed-houses (read-string (slurp CACHED-EDN))
        processed-houses-ids (filter-map #(and (not (:error %)) (:id %)) processed-houses)
        ; _ (--- processed-houses-ids)
        ; _ (throw (Exception. "Ok"))
        objects-tabtree (tabtree/parse-tab-tree "../factbase/houses/indexes.tree")
        house-wm-ids (filter-map :wm (vals objects-tabtree))
        objects-data (->>
                        house-wm-ids
                        (reduce
                          (fn [acc id]
                            (cond
                              (index-of? processed-houses-ids id)
                              (do
                                (p ".")
                                acc)

                              :else
                              (let [result (get-house-by-id id)]
                                (p (if (:error result) "-" "+"))
                                (conj acc result))))
                          processed-houses)
                        (into []))]
    (write-to-file CACHED-EDN (pr-str objects-data))
    ; objects-data
    true
    ))

(defn sample-request []
  ; (get-objects-inside-the-area {:lat-min "47.211894" :lon-min "38.927201" :lat-max "47.213795" :lon-max "38.928853"}))
  (get-house-by-id "11777338")
  )

(defn process-val [val]
  (cond
    (string? val)
      (->>
        val
        (#(s/replace % "\n" "\\n"))
        (format "\"%s\""))
    (coll? val)
      (s/join "," (map process-val val))
    :else val))

(defn clean-up [edn]
  (->> edn
      (remove :error)
      (filter #(and (:street %) (:housenumber %)))))

(defn edn->shallow-tabtree [edn root-name]
  (reduce
    (fn [acc m]
      (let [address (city/normalize-address (:street m) (:housenumber m))]
        (if (:error m)
          acc
          (format "%s\t%s%s\n"
                  acc
                  address
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
                      (sort-by-order (keys m) [:id :title :street :housenumber :lat :lon :north :west :east :south :comments-n :photo :author :wm-url :url :description]))))))
    (str root-name "\n")
    (sort #(city/compare-addresses
              (city/normalize-address (:street %1) (:housenumber %1))
              (city/normalize-address (:street %2) (:housenumber %2)))
          (clean-up edn))))

(defn build-tabtree []
  (let [CACHED-EDN "/var/cache/projects/taganrog-history-bot/wikimapia-houses.edn"
        GENERATED-TABTREE "../factbase/generated/wikimapia-houses.tree"
        processed-houses (read-string (slurp CACHED-EDN))
        tabtree-houses (edn->shallow-tabtree processed-houses "processed_houses")]
    (write-to-file GENERATED-TABTREE tabtree-houses)
    true))
