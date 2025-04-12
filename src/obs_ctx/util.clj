(ns obs-ctx.util
  (:require [obs-ctx.datetime :as datetime]
            [babashka.fs :as fs]
            [clojure.string :as str]))

(def default-lookback "30d")
(def default-limit 100)

(defn validate-lookback [lookback]
  (let [lookback-map {:d :day :m :month :y :year}
        [_ amount unit] (re-matches #"(\d+)([dm])" lookback)]
    (and amount unit)))

(defn parse-lookback [lookback]
  (let [[_ amount unit] (re-matches #"(\d+)([dmy])" lookback)]
    (datetime/calculate-past-datetime (Integer/parseInt amount) unit)))

(defn file-for-wikilink [wikilink]
  (let [wikilink (if (str/starts-with? wikilink "[[")
                   (re-find #"^\[\[(.+)\]\]$" wikilink)
                   wikilink)
        [filename link-title] (str/split wikilink #"\|")
        obsidian-dir (fs/file (System/getenv "OBSIDIAN_DIR"))
        file-paths (fs/glob obsidian-dir (str "**/" filename ".md"))]
    ;; [filename (first file-paths)]
    ;; (prn [filename wikilink link-title (boolean (first file-paths))])
    (some-> file-paths
            (first)
            (fs/file))))

(defn find-recent-files [opts]
  (let [obsidian-dir (System/getenv "OBSIDIAN_DIR")
        lookback (parse-lookback (:lookback opts))]
    (->> (fs/glob obsidian-dir "**/*.md")
         (map fs/file)
         (filter #(.isAfter (.toInstant (fs/last-modified-time %)) lookback))
         (sort-by fs/last-modified-time #(compare %2 %1)) ; Most recent first
         (take (get opts :lookback-limit 1000)))))

(defn cutoff-with-lookback
  "Given a lookback date, return a list of files that were modified after that date"
  [lookback files]
  (let [lookback (parse-lookback lookback)]
    (filter #(.isAfter (.toInstant (fs/last-modified-time %)) lookback) files)))

(comment
  ;; = str - Example 1 = 

  user=> "some string"
  "some string"

  user=> (str)
  ""

  user=> (str nil)
  ""

  user=> (str 1)
  "1"

  user=> (str 1 2 3)
  "123"

  user=> (str 1 'symbol :keyword)
  "1symbol:keyword"

  ;; A very common usage of str is to apply it to an existing collection:
  user=> (apply str [1 2 3])
  "123"

  ;; compare it with:
  user=> (str [1 2 3])
  "[1 2 3]"


  ;; See also:
  clojure.core/pr
  clojure.core/prn
  clojure.core/pr-str
  clojure.core/prn-str
  clojure.core/print-str
  clojure.core/println-str
  :rcf)

(comment
  (file-for-wikilink "[[2025-04-11]]"))