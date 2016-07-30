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

(def router (atom nil))
(defn stop-router! [] (when-let [stop-f @router] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router (sente/start-chsk-router! ch-chsk event-msg-handler)))

(start-router!)
