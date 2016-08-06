(ns ubik.core
  (:require [ubik.handlers
             [core :refer [ubik-ring-handler]]
             [event :refer [event-msg-handler]]]
            [ubik
             [sente :refer [ch-chsk]]
             [scheduler :refer [start-scheduler!]]]
            [taoensso.timbre :refer [debugf]]
            [taoensso.sente :as sente]
            [org.httpkit.server :as http-kit]))

(defn start-web-server!* [ring-handler port]
  (let [http-kit-stop-fn (http-kit/run-server ring-handler {:port port})]
    {:server nil
     :port (:local-port (meta http-kit-stop-fn))
     :stop-fn (fn [] (http-kit-stop-fn :timeout 100))}))

(defonce web-server (atom nil))
(defn stop-web-server! [] (when-let [m @web-server] ((:stop-fn m))))
(defn start-web-server! [& [port]]
  (stop-web-server!)
  (let [{:keys [stop-fn port] :as server-map} (start-web-server!* (var ubik-ring-handler) (or port 0))
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
