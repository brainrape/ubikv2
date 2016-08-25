(ns ubik.anim.core
  (:require [ubik.anim.renderers :as renderers :refer [update-animation main-renderer get-plane-render-target]]
            [ubik.anim.cameras :as cameras]
            [ubik.anim.audio :as audio]
            [ubik.anim.face.core :refer [face-animation init-face-anim!]]
            [ubik.anim.bg.core :refer [bg-animation init-bg-anim!]]
            [ubik.anim.video-texture :refer [get-video-texture]]
            [ubik.commons.core :refer [anim-ids]]
            [cljs.core.match :refer-macros [match]]
            [taoensso.encore :refer [debugf]]
            [taoensso.sente :as sente]
            [cljs.core.async :refer [<! timeout chan alts!]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def THREE js/THREE)
(def fps 30)

(def init-ch (chan))
(def anim-ch (chan))

(defn get-main-animation [allowed-types]
  (let [camera (cameras/get-perspective-camera)
        scene (THREE.Scene.)
        {bg-rt :rt-texture bg-mesh :mesh} (get-plane-render-target (.-innerWidth js/window) (.-innerHeight js/window))
        {face-rt :rt-texture face-mesh :mesh} (get-plane-render-target 1000 1150)
        active-meshes (vals (select-keys {:bg bg-mesh :face face-mesh} allowed-types))
        active-anims (vals (select-keys {:bg [bg-animation bg-rt] :face [face-animation face-rt]} allowed-types))]
    (set! (.. face-mesh -position -x) 250)
    (set! (.. face-mesh -position -y) -170)
    (doseq [mesh active-meshes]
      (.add scene mesh))
    (fn [{:keys [anims delta now-msec] :as state} render-fn]
      (let [updated-state (reduce (fn [acc [anim rt]] (update-animation main-renderer anim rt acc)) state active-anims)]
        (render-fn scene camera)
        updated-state))))

(def main-animation (atom (get-main-animation [:face])))

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
         [:ubik/current-anims anims] (go (>! init-ch anims))
         :else (debugf "unhandled chsk/recv %s" ?data)))

(defn get-current-anims [previous-anims {:keys [type] :as anim}]
  (if (or (some? (previous-anims type)) (nil? anim))
    previous-anims
    (assoc previous-anims type anim)))

(defn init-anims! [anims]
  (init-face-anim! (dissoc anims :bg))
  (init-bg-anim! (:bg anims)))

(def loop-ch (atom (chan)))

(defn anim-loop [audio-data-fn]
  (go-loop [state {:now-msec (.getTime (js/Date.)) :audio-data (audio-data-fn) :delta 0 :anims {}}]
    (let [prev-anims (:anims state)
          now-msec (<! @loop-ch)
          [next-anim _] (alts! [anim-ch] :default nil)
          updated-state (update-animation main-renderer @main-animation state)
          curr-anims (:anims updated-state)
          delta (- now-msec (:now-msec updated-state))
          anims (get-current-anims curr-anims next-anim)
          processed (apply dissoc prev-anims (map (fn [[k _]] k) (remove (fn [[_ v]] (nil? v)) curr-anims)))]
      (doseq [[_ anim] processed]
        (when anim
          (chsk-send! [:ubik/processed-anim anim])))
      (recur (merge updated-state {:now-msec now-msec :delta delta :audio-data (audio-data-fn) :anims anims})))))

(defn start-loop! []
  (go (while true (<! (timeout (/ 1000 fps))) (>! @loop-ch (.getTime (js/Date.)))))
  (let [analyser (audio/get-audio-analyser)
        audio-data (audio/get-data-array analyser)
        get-audio-data (constantly (do (.getByteFrequencyData analyser audio-data) (array-seq audio-data)))]
    (go
      (init-anims! (<! init-ch))
      (anim-loop get-audio-data))))

(defonce animation-started (atom false))
(when-not @animation-started
  (start-loop!)
  (reset! animation-started true))

(def router (atom nil))
(defn stop-router! [] (when-let [stop-f @router] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router (sente/start-chsk-router! ch-chsk event-msg-handler)))

(defn set-bg-anim! [] (reset! main-animation (get-main-animation [:bg])))
