(ns ubik.server
  (:require [ubik.handlers :refer [controller-handler anim-handler]]
            [ring.middleware.defaults :refer [site-defaults]]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [clojure.core.async :as async :refer [<! go-loop put! chan alt!]]
            [taoensso.timbre :refer [debugf]]
            [taoensso.sente :as sente]
            [org.httpkit.server :as http-kit]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]))

(def tick-ms 10000)

(def last-tick-timestamp (atom (System/currentTimeMillis)))

(def user-queue (atom clojure.lang.PersistentQueue/EMPTY))

(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids]}
      (sente/make-channel-socket! sente-web-server-adapter {:packer :edn
                                                            :user-id-fn (fn [ring-req] (:client-id ring-req))})]
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def connected-uids connected-uids))

(defroutes my-routes
  (GET "/" req (controller-handler req))
  (GET "/anim" req (anim-handler req))
  (GET "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req))
  (route/not-found "404"))

(def my-ring-handler
  (let [ring-defaults-config
        (-> site-defaults
            (assoc-in [:static :resources] "/")
            (assoc-in [:security :anti-forgery] {:read-token (fn [req] (-> req :params :csrf-token))}))]
    (ring.middleware.defaults/wrap-defaults my-routes ring-defaults-config)))

(defn calculate-action-timeout [queue-pos]
  (let [last-tick-elapsed (- (System/currentTimeMillis) @last-tick-timestamp)]
    (+ (* (- queue-pos 2) tick-ms) (- tick-ms last-tick-elapsed))))

(defmulti event-msg-handler :id)

(defmethod event-msg-handler :ubik/enqueue [{:keys [event ring-req]}]
  (let [uid (get-in ring-req [:params :client-id])]
    (debugf "ubik/enqueue: %s %s" event uid)
    (swap! user-queue conj uid)
    (if (= (count @user-queue) 1)
      (chsk-send! uid [:ubik/start-action])
      (chsk-send! uid [:ubik/turn {:time (calculate-action-timeout (count @user-queue))}]))))

(defmethod event-msg-handler :chsk/uidport-close [{:keys [event ring-req]}]
  (let [uid (get-in ring-req [:params :client-id])
        closed-idx (count (take-while #(not= uid %) @user-queue))]
    (debugf "chsk/uidport-close: %s %s" event uid)
    (swap! user-queue #(apply conj clojure.lang.PersistentQueue/EMPTY (remove #{%2} %1)) uid) ;;TODO this is O(n)
    (when-let [next-uid (and (= closed-idx 0) (peek @user-queue))]
      (chsk-send! next-uid [:ubik/start-action]))
    (doseq [[idx uid] (drop (inc closed-idx) (map-indexed vector @user-queue))]
      (chsk-send! uid [:ubik/turn {:time (calculate-action-timeout (inc idx))}]))))

(defmethod event-msg-handler :ubik/change-anim [{:keys [event ?data ring-req]}]
  (debugf "ubik/change-anim: %s %s" event ?data)
  (let [uid (get-in ring-req [:params :client-id])]
    (when (= (peek @user-queue) uid)
      (doseq [uid (:any @connected-uids)]
        (chsk-send! uid event)))))

(defmethod event-msg-handler :default [{:as ev-msg :keys [event ring-req]}]
  (let [uid (get-in ring-req [:params :client-id])]
    (debugf "Unhandled event: %s %s" event uid)))

(defn scheduler []
  (let [poison-ch (chan)]
    (go-loop []
      (when (alt! poison-ch false :default :keep-going) 
        (<! (async/timeout tick-ms))
        (reset! last-tick-timestamp (System/currentTimeMillis))
        (loop [users @user-queue]
          (when-let [uid (and (< 1 (count users)) (peek users))]
            (if (contains? (:any @connected-uids) uid)
              (do
                (chsk-send! uid [:ubik/stop-action {:action-time (calculate-action-timeout (count @user-queue))}])
                (swap! user-queue #(conj (pop %1) %2) uid)
                (chsk-send! (peek @user-queue) [:ubik/start-action]))
              (recur (pop users)))))
        (recur)))
    poison-ch))

(defonce scheduler-poison-ch (atom nil))
(defn stop-scheduler! [] (when-let [ch @scheduler-poison-ch] (put! ch :stop)))
(defn start-scheduler! []
  (stop-scheduler!)
  (reset! scheduler-poison-ch (scheduler)))

(defn start-web-server!* [ring-handler port]
  (let [http-kit-stop-fn (http-kit/run-server ring-handler {:port port})]
    {:server nil
     :port (:local-port (meta http-kit-stop-fn))
     :stop-fn (fn [] (http-kit-stop-fn :timeout 100))}))

(defonce web-server (atom nil))
(defn stop-web-server! [] (when-let [m @web-server] ((:stop-fn m))))
(defn start-web-server! [& [port]]
  (stop-web-server!)
  (let [{:keys [stop-fn port] :as server-map} (start-web-server!* (var my-ring-handler) (or port 0))
        uri (format "http://localhost:%s/" port)]
    (debugf "uri: `%s`" uri)
    (try
      (.browse (java.awt.Desktop/getDesktop) (java.net.URI. uri))
      (catch java.awt.HeadlessException _))
    (reset! web-server server-map)))

(defonce router (atom nil))
(defn stop-router! [] (when-let [stop-f @router] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router (sente/start-chsk-router! ch-chsk event-msg-handler)))

(defn start! []
  (start-router!)
  (start-web-server! 3000)
  (start-scheduler!))

(start!)
