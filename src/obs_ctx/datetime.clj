(ns obs-ctx.datetime
  (:require [clojure.string :as str])
  (:import (java.time Instant Period ZonedDateTime ZoneId)
           (java.time.format DateTimeFormatter FormatStyle)))

(defn format-instant
  "Concisely formats an Instant to a human-friendly string using the
   system default time zone and MEDIUM localized format style."
  [^Instant inst]
  (.format (.withZone (DateTimeFormatter/ofLocalizedDateTime FormatStyle/MEDIUM) (ZoneId/systemDefault))
           inst))

(defn format-now []
  (format-instant (Instant/now)))

(def zone (ZoneId/systemDefault))

(defn calculate-past-datetime
  "Calculates a ZonedDateTime in the past by subtracting a period defined
   by an amount and a unit string from the current time in a given time zone.

   Arguments:
     amount   - The numeric amount (long or integer).
     unit-str - The time unit as a string (\"d\", \"w\", \"m\", \"y\"). Case-insensitive.
     zone-id  - The java.time.ZoneId to use for the 'now' reference.

   Returns:
     A java.time.ZonedDateTime representing the calculated past date/time.

   Throws:
     IllegalArgumentException if the unit-str is not supported."
  [amount unit-str] ; Type hint ^ZoneId for clarity/performance
  (let [unit   (clojure.string/lower-case unit-str)
        period (case unit
                 "d" (Period/ofDays (int amount)) ; Period methods require int
                 "w" (Period/ofWeeks (int amount))
                 "m" (Period/ofMonths (int amount)) ; Assumes 'm' is months
                 "y" (Period/ofYears (int amount))
                 ; Example if adding hours using Duration (Duration methods take long)
                 ; "h" (Duration/ofHours amount)
                 ; Default case for unsupported units
                 (throw (IllegalArgumentException. (str "Unsupported time unit: '" unit-str "'"))))
        now    (Instant/now)]
    ;; The core logic: subtract the calculated TemporalAmount (Period or Duration)
    (.minus now period)))

(defn calculate-past-datetime-default-zone
  "Calculates a ZonedDateTime in the past using the system default time zone.
   See calculate-past-datetime for details."
  [amount unit-str]
  (calculate-past-datetime amount unit-str))

;; --- Example Usage (within a comment block or REPL) ---
(comment
  (calculate-past-datetime 2 "d")

  (let [zone (ZoneId/systemDefault)]
    (println "Current Time (" zone "):" (ZonedDateTime/now zone))
    (println "2 days ago:  " (calculate-past-datetime 2 "d"))
    (println "6 months ago:" (calculate-past-datetime 6 "m"))
    (println "3 weeks ago: " (calculate-past-datetime 3 "W")) ; Case-insensitive test
    (println "1 year ago:  " (calculate-past-datetime-default-zone 1 "y")) ; Using default zone fn

    ; Test error handling
    (try
      (calculate-past-datetime 10 "x" zone)
      (catch IllegalArgumentException e
        (println "Caught expected error:" (.getMessage e))))))