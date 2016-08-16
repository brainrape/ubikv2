(ns ubik.controller.core
  (:require [ubik.commons.core :refer [face-anim-types anim-ids]]
            [taoensso.encore :refer [debugf]]
            [taoensso.sente :as sente]
            [cljs.core.match :refer-macros [match]]
            [cljs.core.async :refer [<! timeout put! chan]])
  (:require-macros [cljs.core.async.macros :refer [go-loop alt!]]))

(def current-anims (atom {}))

(let [{:keys [ch-recv send-fn]}
      (sente/make-channel-socket! "/chsk" {:type :auto :packer :edn})]
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn))

(defn set-visibility-by-id! [id visibility]
  (let [elem (.getElementById js/document id)]
    (set! (.. elem -style -visibility) visibility)))

(defn countdown [action-time]
  (let [poison-ch (chan)
        countdown-elem (.getElementById js/document "countdown")]
    (go-loop [current-t (quot action-time 1000)]
      (when (and (< 0 current-t) (alt! poison-ch false :default :keep-going))
        (set! (.-innerHTML countdown-elem) current-t)
        (<! (timeout 1000))
        (recur (dec current-t))))
    poison-ch))

(defonce countdown-poison-ch (atom nil))
(defn stop-countdown! [] (when-let [ch @countdown-poison-ch] (put! ch :stop)))
(defn start-countdown! [action-time]
  (stop-countdown!)
  (reset! countdown-poison-ch (countdown action-time)))

(defn set-next-anim! [swiper type direction]
  (let [next-fn (if (= direction :prev) dec inc)
        idx (mod (dec (.-activeIndex swiper))  (- (.. swiper -slides -length) 2))
        prev-idx (@current-anims type)
        anim-cnt (count (anim-ids type))
        id-range
        (match direction
               :next (map #(-> % inc (mod anim-cnt)) (range prev-idx (if (< idx prev-idx) (+ idx anim-cnt) idx)))
               :prev (reverse (map #(mod % anim-cnt) (range (if (> idx prev-idx) (- idx anim-cnt) idx) prev-idx))))]
    (swap! current-anims assoc type idx)
    (go-loop [[id & rest] id-range]
      (when id
        (chsk-send! [:ubik/change-anim {:type type :id id :direction direction}])
        (<! (timeout 10))
        (recur rest)))))

(defn get-swiper [type]
  (let [swiper (js/Swiper. (str "#" (name type) "-container") #js {:direction "horizontal" :loop true :speed 300})]
    (.on swiper "onSlideNextEnd" (fn [_] (set-next-anim! swiper type :next)))
    (.on swiper "onSlidePrevEnd" (fn [_] (set-next-anim! swiper type :prev)))
    swiper))

(def swipers (into {} (map (fn [type] [type (get-swiper type)]) [:top :center :bottom :bg])))
(set! js/swipers (clj->js swipers))

(defn set-current-slide! [type id]
  (.slideTo (swipers type) (inc id) 0 false))

(defn set-current-anims! [anims]
  (reset! current-anims anims)
  (doseq [[type id] (dissoc anims :bg)]
    (set-current-slide! type id)))

(defmulti event-msg-handler :id)

(defmethod event-msg-handler :default [{:keys [event]}]
  (debugf "unhandled event: %s" event))

(defmethod event-msg-handler :chsk/state [{:keys [?data]}]
  (chsk-send! [:ubik/enqueue])
  (debugf "chsk/state: %s" ?data))

(defmethod event-msg-handler :chsk/recv [{:keys [?data]}]
  (debugf "chsk/recv: %s" ?data)
  (letfn [(show-elem [id] (set-visibility-by-id! id "visible"))
          (hide-elem [id] (set-visibility-by-id! id "hidden"))]
    (match ?data
           [:ubik/current-anims anims] (set-current-anims! anims)
           [:ubik/turn {:action-time action-time}] (start-countdown! action-time)
           [:ubik/start-action anims] (do (set-current-anims! anims)
                                          (show-elem "main-container")
                                          (hide-elem "wait-container"))
           [:ubik/stop-action {:action-time action-time}]
           (do (hide-elem "main-container") (show-elem "wait-container") (start-countdown! action-time))
           :else (debugf "unhandled chsk/recv %s" ?data))))

(def router (atom nil))
(defn stop-router! [] (when-let [stop-f @router] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router (sente/start-chsk-router! ch-chsk event-msg-handler)))
