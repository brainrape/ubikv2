(ns ubik.anim.video-texture
  (:require [ubik.commons.core :refer [anim-ids]]
            [taoensso.encore :refer [debugf]]))

(defn get-video-texture [url]
  (let [video (.createElement js/document "video")
        texture	(js/THREE.Texture. video)]
    (set! (.-height video) 1024)
    (set! (.-width video) 340)
    (set! (.-autoplay video) false)
    (set! (.-loop video) true)
    (set! (.-src video) url)
    (set! (.-minFilter texture) js/THREE.LinearFilter)
    {:video video :texture texture}))

(def load-video-texture)

(def preloaded-video-textures
  (into {} (map (fn [[k ids]] [k (into {} (map (fn [id] [id (get-video-texture (str (name k) id ".mkv"))]) ids))])
                anim-ids)))

(defn get-active-video-textures [type id]
  (let [vts-by-type (preloaded-video-textures type)
        curr-vt (vts-by-type id)
        next-vt (vts-by-type (mod (inc id) (count vts-by-type)))
        prev-vt (vts-by-type (mod (dec id) (count vts-by-type)))]
    {:curr curr-vt :next next-vt :prev prev-vt}))
