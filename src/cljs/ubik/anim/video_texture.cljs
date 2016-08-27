(ns ubik.anim.video-texture
  (:require [ubik.commons.core :refer [anim-ids]]
            [taoensso.encore :refer [debugf]]))

(defn get-video-texture [type id]
  (let [url (str "videos/" (name type) id ".mp4")
        video (.createElement js/document "video")
        texture	(js/THREE.Texture. video)]
    (set! (.-height video) 1024)
    (set! (.-width video) 340)
    (set! (.-autoplay video) false)
    (set! (.-loop video) true)
    (set! (.-src video) url)
    (set! (.-minFilter texture) js/THREE.LinearFilter)
    (set! (.-onended video) (fn [] (.load video) (.play video)))
    {:video video :texture texture :type type :id id}))

(def preloaded-video-textures
  (into {} (map (fn [[type ids]] [type (into {} (map (fn [id] [id (get-video-texture type id)]) ids))])
                anim-ids)))

(defn get-active-video-textures
  ([type id]
   (let [prev-id (mod (dec id) (count (preloaded-video-textures type)))]
     (get-active-video-textures type id prev-id)))
  ([type id prev-id]
   (let [vts-by-type (preloaded-video-textures type)
         curr-vt (vts-by-type id)
         prev-vt (vts-by-type prev-id)]
     {:curr curr-vt :prev prev-vt})))

(defn update-video-texture [{:keys [video texture]}]
  (when (= (.-readyState video) (.-HAVE_ENOUGH_DATA video))
    (set! (.-needsUpdate texture) true)))
