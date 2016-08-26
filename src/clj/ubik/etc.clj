(ns ubikv2.etc
  (:require [clojure.java.io :as io]))

(defn get-file-pair [type orig-i rand-i]
  [(str "public/" type orig-i ".mp4") (str "public/videos/" type rand-i ".mp4")])

(defn shuffle-files []
  (let [path-pairs (mapcat (fn [type] (let [shuffled-indices (shuffle (range 21))]
                                        (map-indexed #(get-file-pair type %1 %2) shuffled-indices)))
                           ["top" "center" "bottom"])]
    (doseq [[source-path dest-path] path-pairs]
      (io/copy (io/file source-path) (io/file dest-path)))))

;for f in *.mp4; do ffmpeg -i $f -ss 00:00:1.000 -vframes 1 "`basename $f .mp4`.jpg"; done
