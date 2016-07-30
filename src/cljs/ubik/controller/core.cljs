(ns ubik.controller.core
  (:require [ubik.commons.core :refer [anim-ids]]
            [taoensso.encore :refer [debugf]]
            [taoensso.sente :as sente]
            [cljs.core.match :refer-macros [match]]))

(def current-anims (atom (into {} (map (fn [[k v]] [k (first v)]) anim-ids))))

(let [{:keys [ch-recv send-fn]}
      (sente/make-channel-socket! "/chsk" {:type :auto :packer :edn})]
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn))

(defmulti event-msg-handler :id)

(defmethod event-msg-handler :default [{:keys [event]}]
  (debugf "unhandled event: %s" event))

(defmethod event-msg-handler :chsk/state [{:keys [?data]}]
  (chsk-send! [:ubik/enqueue])
  (debugf "chsk/state: %s" ?data))

(defn set-visibility-by-id! [id visibility]
  (let [elem (.getElementById js/document id)]
    (set! (.. elem -style -visibility) visibility)))

(defn start-countdown! [action-time]
  (debugf "wait %s" action-time))

(defmethod event-msg-handler :chsk/recv [{:keys [?data]}]
  (debugf "chsk/recv: %s" ?data)
  (letfn [(show-elem [id] (set-visibility-by-id! id "visible"))
          (hide-elem [id] (set-visibility-by-id! id "hidden"))]
    (match ?data
           [:ubik/current-anims {:ids anims}] (reset! current-anims anims)
           [:ubik/change-anim {:type type :id id}] (swap! current-anims assoc type id)
           [:ubik/start-action] (do (show-elem "main-container") (hide-elem "wait-container"))
           [:ubik/stop-action {:action-time action-time}]
           (do (hide-elem "main-container") (show-elem "wait-container") (start-countdown! action-time))
           :else (debugf "unhandled chsk/recv %s" ?data))))

(defmethod event-msg-handler :chsk/handshake [{:keys [?data]}]
  (debugf "chsk/handshake: %s" ?data))

(defn set-next-anim! [anim-type direction]
  (let [next-fn (if (= direction :prev) dec inc)
        idx (mod (next-fn (@current-anims anim-type)) (count (anim-ids anim-type)))]
    (swap! current-anims assoc anim-type idx)
    (chsk-send! [:ubik/change-anim {:type anim-type :id idx}])))

(defn get-swiper [anim-type]
  (let [swiper (js/Swiper. (str "#" (name anim-type) "-container") #js {:direction "horizontal" :loop true})]
    (.on swiper "onSlideNextEnd" (fn [_] (set-next-anim! anim-type :next)))
    (.on swiper "onSlidePrevEnd" (fn [_] (set-next-anim! anim-type :prev)))
    swiper))

(set! js/top-container (get-swiper :top))
(set! js/center-swiper (get-swiper :center))
(set! js/bottom-swiper (get-swiper :bottom))

(def router (atom nil))
(defn stop-router! [] (when-let [stop-f @router] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router (sente/start-chsk-router! ch-chsk event-msg-handler)))
