(ns obs-ctx.core
  (:require [babashka.fs :as fs]
            [babashka.cli :as cli]
            [obs-ctx.pack :as pack]
            [obs-ctx.util :as util]
            [clojure.string :as str]))

(defn excluded-note? [^java.io.File file]
  (contains? #{"Clippings.md" "Memos.md" "Scraps.md"} (fs/file-name file)))

(defn daily-note? [^java.io.File file]
  (re-matches #"\d{4}-\d{2}-\d{2}.md" (fs/file-name file)))

(defn extract-wikilinks [content]
  (map second (re-seq #"\[\[([^\]]+)\]\]" content)))

(defn strip-md-extension [filename]
  (cond
    (str/ends-with? filename ".md") (subs filename 0 (- (count filename) 3))
    (str/ends-with? filename ".markdown") (subs filename 0 (- (count filename) 9))
    :else filename))

(defn recent-links
  "Find most frequently used wikilinks in recent files"
  [{:keys [opts]}]
  (util/preflight!)
  (let [opts (merge {:lookback util/default-lookback
                     :limit util/default-limit}
                    opts)
        _ (println "Finding recent markdown files..." opts)
        {:keys [exclude-daily-notes include-count limit]} opts
        recent-files (util/find-recent-files opts)
        all-files (->> recent-files
                       (mapcat (fn [f]
                                 (let [content (slurp f)]
                                   (->> (extract-wikilinks content)
                                        (map util/file-for-wikilink)
                                        (filter identity)))))
                       (remove excluded-note?)
                       (remove (if exclude-daily-notes daily-note? (constantly false)))
                       frequencies
                       (sort-by val >))]

    (println "\nFound wiki links (sorted by frequency):")
    (doseq [[file freq] (take limit all-files)
            :let [link (strip-md-extension (fs/file-name file))]]
      (println (str "- [[" link "]]"
                    (when include-count
                      (str " (" freq " references)")))))))

#_(defn error-fn
    [{:keys [spec type cause msg option] :as data}]
    (when (= :org.babashka/cli type)
      (case cause
        :require
        (println
         (format "Missing required argument: %s\n" option))
        :validate
        (println
         (format "%s does not exist!\n" msg)))))

;; Define the spec for options applicable to subcommands or globally
(def recent-links-cli-spec
  {:spec {:help {:coerce :boolean}
          :limit {:coerce :int
                  :default 100
                  :desc "Limit the number of results"}
          :lookback {:default "30d"
                     :validate util/validate-lookback
                     :desc "Ignore files with modified-times older than this (30d, 6m, 1y)"}
          :include-count {:coerce :boolean}
          :exclude-daily-notes {:coerce :boolean
                                :desc "Exclude links from daily notes"}}})

(def pack-cli-spec
  {:spec {:help {:coerce :boolean}
          :limit {:coerce :int
                  :default 100
                  :desc "Limit the number of results"}
          :lookback {:default "30d"
                     :validate util/validate-lookback
                     :desc "Ignore files with modified-times older than this (30d, 6m, 1y)"}
          :instruction-file {:desc "Path to file containing custom instructions (included at the end of the output)"}}})

(defn help-cmd
  "Handler for the help command or default case"
  [_]
  (println "Usage: obsidian-context <subcommand> [options]\n")
  (println "  $OBSIDIAN_DIR must point to an Obsidian vault\n\n")
  (println "Subcommands:")
  (println "  recent-links   Find most frequently used wikilinks in recent files")
  (println "  pack            Combine recent files into a big blob with xml tags (a la repopack)")
  (println "\n\nrecent-link options:\n")
  (println (cli/format-opts recent-links-cli-spec))

  (println "\n\npack options:\n")
  (println (cli/format-opts pack-cli-spec)))

(defn help-wrapper [cmd-fn]
  (fn [{:keys [opts] :as m}]
    (if (:help opts)
      (help-cmd nil)
      (cmd-fn m))))

(def dispatch-table
  [{:cmds ["recent-links"] :fn (help-wrapper recent-links) :spec recent-links-cli-spec}
   {:cmds ["pack"]          :fn (help-wrapper pack/pack-cmd) :spec pack-cli-spec}
   {:cmds []               :fn (help-wrapper help-cmd)}])

(defn -main [& args]
  (cli/dispatch dispatch-table args))

#_(-main *command-line-args*)
