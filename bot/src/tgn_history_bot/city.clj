(ns tgn-history-bot.city
  (:require
    [clojure.string :as s]
    [org.clojars.prozion.clj-tabtree.tabtree :as tabtree]
    [tgn-history-bot.aux :refer :all]
    [tgn-history-bot.sparql :as sparql]))

(def ADDRESSES (tabtree/parse-tab-tree "../factbase/streets/ids.tree"))

(defn normalize-address
  ([address-string]
    (-> address-string
        s/lower-case
        (s/replace "null" "")
        (s/replace "," "")
        (s/replace "/" "-")
        (s/replace #"(\bул\.?\s+)|(\bпер\.?\s+)|(\bд\.?\s+)|(\bтуп\.?\s+)" "")
        (s/replace #"(\bулица\.?\s+)|(\bпереулок\.?\s+)|(\bдом\.?\s+)|(\bтупик\.?\s+)" "")
        (s/replace #"(\bulitsa\.?\s+)|(\bul\.?\s+)|(\bper\.?\s+)|(\bpereulok\.?\s+)|(\bdom\.?\s+)" "")
        (s/replace "." "")
        (s/replace #"(\s+корп\.?\s+)|(\s+корпус\s+)" "-")
        (s/replace #"антона\s*" "") ; Антона Глушко -> Глушко
        (s/replace "grecheskaya" "греческая")
        (s/replace "petrovskaya" "петровская")
        (s/replace "italyansky" "итальянский")
        (s/replace "frunze" "фрунзе")
        (s/replace "chekhova" "чехова")
        (s/replace #"(-й)|(-я)" "")
        (s/replace #"\s+" " ")
        s/trim
        ((fn [s] (s/join "_" (map s/capitalize (s/split s #" ")))))))
  ([street housenumber]
    (let [housenumber (-> housenumber
                          (or "?")
                          (s/split #"/|\s")
                          first)
          street (or street "?")]
      (some->
        (format
          "%s %s"
          street
          housenumber)
        normalize-address))))

(defn get-canonical-address [normalized-address]
  (let [parts (s/split normalized-address #"_")
        street-id (keyword (s/join "_" (butlast parts)))
        street-canonical (get-in ADDRESSES [street-id :canonical])
        number (last parts)
        number-parts (s/split number #"-")
        first-number (first number-parts)
        block-number (second number-parts)
        canonical-address (format
                            "%s, %s%s"
                            street-canonical
                            first-number
                            (if block-number
                                (format ", корпус %s" block-number)
                                ""))]
    canonical-address))


(defn normalize-title [title]
  (-> title
    (s/replace "«" "")
    (s/replace "»" "")
    (s/replace ")" "")
    (s/replace "(" "")
    normalize-address))

; (defn get-historical-quarter [address-text]
;   (some-> address-text normalize-address sparql/find-historical-quarters first))

(defn break-up-address [address]
  (cond
    (empty? address) []
    (nil? address) []
    :else
    (let [parts (s/split address #"_")
          streetname (s/join "_" (butlast parts))
          number-part (last parts)
          blocks (s/split number-part #"-")
          block-parts (map #(rest (re-matches #"(\d+)(.*)" %)) blocks)]
      (remove
        #(or
          (not %)
          (and (string? %) (empty? %)))
        (flatten
          `(~streetname
            ~(map
                #(list (some-> % first ->int) (rest %))
                block-parts)))))))

(defn compare-addresses [address1 address2]
  (loop [address-parts1 (break-up-address address1)
         address-parts2 (break-up-address address2)]
    (cond
      (empty? address-parts1) -1
      (empty? address-parts2) 1
      :else
      (let [comparison-result (compare (first address-parts1) (first address-parts2))]
        (if (= 0 comparison-result)
          (recur (rest address-parts1) (rest address-parts2))
          comparison-result)))))

(defn build-house-summary [data-m]
  (let [headers {:quarter "Квартал" :year "Построен" :description "Описание" :url "Ссылки"}]
    (reduce
      (fn [acc key]
        (format
          "%s%s"
          acc
          (cond
            (or
              (not (key data-m))
              (empty? (key data-m))) ""
            (= :description key) (format "\n\n%s" (data-m key))
            (= :url key) (let [urls (data-m key)]
                            (format "\n\n%s"
                              (make-html-link "Подробнее" (if (coll? urls) (first urls) urls))))
            :else (format "\n\n<b>%s</b>: %s" (headers key) (key data-m)))))
      (or (get-canonical-address (:normalized-address data-m)) "")
      (keys headers))))
