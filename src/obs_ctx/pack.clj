(ns obs-ctx.pack
  (:require [babashka.fs :as fs]
            [obs-ctx.util :as util]
            [obs-ctx.datetime :as datetime]
            [clojure.string :as str]))

(defn excluded-note? [filename]
  (contains? #{"Clippings" "Memos" "Scraps"} filename))

(defn daily-note? [filename]
  (re-matches #"\d{4}-\d{2}-\d{2}" filename))

(defn clipping? [file]
  (str/includes? file "/clippings/"))

(defn find-files [{:keys [exclude-dailies lookback]}]
  (->> (util/find-recent-files {:lookback lookback})
       (filter (fn [f]
                 (let [filename (fs/file-name f)]
                   (and (not (excluded-note? filename))
                        (not (clipping? (fs/file f)))
                        (if exclude-dailies
                          (not (daily-note? filename))
                          true)))))
       (sort-by fs/last-modified-time #(compare %2 %1))))

(defn format-file-content [file]
  (let [content (slurp file)
        relative-path (str/replace (str file) (System/getenv "OBSIDIAN_DIR") "")]
    (str "<file path=\"" relative-path "\">\n"
         content
         "\n</file>")))

(defn create-directory-structure [files]
  (let [dirs (->> files
                  (map #(str/replace (str %) (System/getenv "OBSIDIAN_DIR") ""))
                  (map #(str/split % #"/"))
                  (map butlast)
                  (map #(str/join "/" %))
                  (distinct)
                  (sort))]
    (str "<directory_structure>\n"
         (str/join "\n" (map #(str "  " %) dirs))
         "\n</directory_structure>")))

(defn pack-cmd
  "Handler for the pack command - creates a bundle of multiple files"
  [{:keys [opts]}]
  (let [opts (merge {:lookback util/default-lookback
                     :limit util/default-limit}
                    opts)
        files (find-files opts)
        limit (:limit opts)
        selected-files (take limit files)]

    (println "This file is a merged representation of files in my obsidian vault.\n")

    (println "<file_summary>")
    (println "  Total files included:" (count selected-files))
    (println "  Last modified:" (datetime/format-instant (.toInstant (fs/last-modified-time (first selected-files)))))
    (println "  Opts:" (pr-str opts))
    (println "</file_summary>\n")

    (println (create-directory-structure selected-files))
    (println "\n<files>")
    (doseq [file selected-files]
      (println (format-file-content file)))
    (println "</files>")

    (when-let [instruction-file (:instruction-file opts)]
      (println "\n<instruction>")
      (when (fs/exists? instruction-file)
        (println (slurp instruction-file)))
      (println "</instruction>"))))