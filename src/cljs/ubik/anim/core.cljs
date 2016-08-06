(ns ubik.anim.core
  (:require [ubik.anim.renderers :as renderers]
            [ubik.anim.cameras :as cameras]
            [ubik.anim.audio :as audio]
            [ubik.anim.face-anim :refer [face-animation]]
            [ubik.anim.video-texture :refer [get-video-texture]]
            [ubik.commons.core :refer [anim-ids]]
            [cljs.core.match :refer-macros [match]]
            [taoensso.encore :refer [debugf]]
            [taoensso.sente :as sente]
            [cljs.core.async :refer [<! timeout chan alts!]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def THREE js/THREE)
(def fps 60)

(def anim-ch (chan))

(let [{:keys [ch-recv send-fn]}
      (sente/make-channel-socket! "/chsk" {:type :auto :packer :edn})]
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn))

(defmulti event-msg-handler :id)

(defmethod event-msg-handler :default [{:keys [event]}]
  (debugf "unhandled event: %s" event))

(defmethod event-msg-handler :chsk/recv [{:keys [?data]}]
  (debugf "chsk/recv: %s" ?data)
  (match ?data
         [:ubik/change-anim anim] (go (>! anim-ch anim))
         :else (debugf "unhandled chsk/recv %s" ?data)))

(defn update-animation
  ([renderer animation rt-texture anim-state]
   (let [render-fn (fn [scene camera] (.render renderer scene camera rt-texture true))]
     (animation anim-state render-fn)))
  ([renderer animation anim-state]
   (let [render-fn (fn [scene camera] (.render renderer scene camera))]
     (animation anim-state render-fn))))

(defonce main-renderer (renderers/get-main-renderer))

(def loop-ch (atom (chan)))

(defn get-current-anims [previous-anims {:keys [type] :as anim}]
  (if (or (some? (previous-anims type)) (nil? anim))
    previous-anims
    (assoc-in previous-anims [type] anim)))

(defn start-loop! []
  (go (while true (<! (timeout (/ 1000 fps))) (>! @loop-ch (.getTime (js/Date.)))))
  (let [analyser (audio/get-audio-analyser)
        audio-data (audio/get-data-array analyser)
        get-audio-data (constantly (do (.getByteFrequencyData analyser audio-data) (array-seq audio-data)))]
    (go-loop [state {:now-msec (.getTime (js/Date.)) :audio-data (get-audio-data) :delta 0 :anims {}}]
      (let [now-msec (<! @loop-ch)
            [next-anim _] (alts! [anim-ch] :default nil)
            updated-state (update-animation main-renderer face-animation state)
            delta (- now-msec (:now-msec updated-state))
            anims (get-current-anims (:anims updated-state) next-anim)]
        (recur (merge updated-state {:now-msec now-msec :delta delta :audio-data (get-audio-data) :anims anims}))))))

(defonce animation-started (atom false))
(when-not @animation-started
  (start-loop!)
  (reset! animation-started true))

(def router (atom nil))
(defn stop-router! [] (when-let [stop-f @router] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router (sente/start-chsk-router! ch-chsk event-msg-handler)))
