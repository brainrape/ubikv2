(ns ubik.anim.core
  (:require [ubik.anim.renderers :as renderers]
            [ubik.anim.cameras :as cameras]
            [ubik.anim.audio :as audio]
            [ubik.commons.core :refer [anim-ids]]
            [cljs.core.match :refer-macros [match]]
            [taoensso.encore :refer [debugf]]
            [taoensso.sente :as sente]
            [cljs.core.async :refer [<! timeout chan]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def THREE js/THREE)
(def fps 60)

(def event-queue (atom cljs.core.PersistentQueue.EMPTY))

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
         [:ubik/change-anim event] (swap! event-queue conj event)
         :else (debugf "unhandled chsk/recv %s" ?data)))

(defn get-render-target []
  (THREE.WebGLRenderTarget.
   (.-innerWidth js/window)
   (.-innerHeight js/window)
   #js {:minFilter THREE.LinearFilter, :magFilter THREE.NearestFilter, :format THREE.RGBFormat}))

(def sphere-render-target
  (let [rt-texture (get-render-target)
        geometry (THREE.SphereGeometry. 500 500 64)
        material (THREE.MeshBasicMaterial. #js {:color 0xffffff, :map rt-texture})
        mesh (THREE.Mesh. geometry material)]
    {:rt-texture rt-texture :mesh mesh}))

(def cube-animation
  (let [camera (cameras/get-perspective-camera)
        scene (THREE.Scene.)
        geometry (THREE.CubeGeometry. 300 200 1000)
        material (THREE.MeshBasicMaterial. #js {:color 0xffffff, :wireframe true})
        mesh (THREE.Mesh. geometry material)]
    (.add scene mesh)
    (fn [state]
      (set! (.-x (.-rotation mesh)) (+ (.-x (.-rotation mesh)) 0.01))
      (set! (.-y (.-rotation mesh)) (+ (.-y (.-rotation mesh)) 0.02))
      {:scene scene :camera camera})))

(defn update-animation
  ([renderer animation rt-texture anim-state]
   (let [{:keys [scene camera]} (animation anim-state)]
     (.render renderer scene camera rt-texture true)))
  ([renderer animation anim-state]
   (let [{:keys [scene camera]} (animation anim-state)]
     (.render renderer scene camera))))2

(defonce main-renderer (renderers/get-main-renderer))

(def main-animation
  (let [renderer (renderers/get-renderer)
        camera (cameras/get-perspective-camera)
        scene (THREE.Scene.)
        {:keys [rt-texture mesh]} sphere-render-target]
    (.add scene mesh)
    (fn [state]
      (update-animation main-renderer cube-animation rt-texture state)
      {:scene scene :camera camera})))

(defn get-anims
  ([] (get-anims {}))
  ([current-events]
   (loop [events current-events]
     (if-let [event (peek @event-queue)]
       (do
         (swap! event-queue pop)
         (let [event-data (dissoc event :type)]
           (recur (update-in events [(:type event)] #(conj (or %1 cljs.core.PersistentQueue.EMPTY) %2) event-data))))
       events))))

(def loop-ch (atom (chan)))

(defn start-loop! []
  (go (while true (<! (timeout (/ 1000 fps))) (>! @loop-ch (.getTime (js/Date.)))))
  (let [analyser (audio/get-audio-analyser)
        audio-data (audio/get-data-array analyser)
        get-audio-data (constantly (do (.getByteFrequencyData analyser audio-data) (array-seq audio-data)))]
    (go-loop [state {:now-msec (.getTime (js/Date.)) :audio-data (get-audio-data) :delta 0 :anims (get-anims)}]
      (update-animation main-renderer main-animation state)
      (let [now-msec (<! @loop-ch)
            delta (- now-msec (:now-msec state))
            anims (get-anims (:anims state))]
        (recur (merge state {:now-msec now-msec :delta delta :audio-data (get-audio-data) :anims anims}))))))

(defonce animation-started (atom false))
(when-not @animation-started
  (start-loop!)
  (reset! animation-started true))

(def router (atom nil))
(defn stop-router! [] (when-let [stop-f @router] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router (sente/start-chsk-router! ch-chsk event-msg-handler)))
