(ns ubik.controller.core
  (:require [taoensso.encore :refer [debugf]]
            [taoensso.sente :as sente]))

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

(defmethod event-msg-handler :chsk/recv [{:keys [?data]}]
  (debugf "chsk/recv: %s" ?data))

(defmethod event-msg-handler :chsk/handshake [{:keys [?data]}]
  (debugf "chsk/handshake: %s" ?data))

(when-let [target-el (.getElementById js/document "btn1")]
  (.addEventListener target-el "click"
                     (fn [ev]
                       (chsk-send! [:ubik/change-anim {:type 0 :id 0}]))))

(defn get-swiper [swiper-id]
  (let [swiper (js/Swiper. (str "#" swiper-id) #js {:direction "horizontal" :loop true})]
    (.on swiper "slideChangeEnd"
         (fn [_] (let [idx (mod (dec (.-activeIndex swiper)) (- (.. swiper -slides -length) 2))]
                   (chsk-send! [:ubik/change-anim {:type swiper-id :id idx}]))))
    swiper))

(set! js/top-container (get-swiper 'top-container))
(set! js/center-swiper (get-swiper 'center-container))
(set! js/bottom-swiper (get-swiper 'bottom-container))

(def router (atom nil))
(defn stop-router! [] (when-let [stop-f @router] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router (sente/start-chsk-router! ch-chsk event-msg-handler)))

(start-router!)
