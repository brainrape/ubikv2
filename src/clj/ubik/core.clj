(ns ubik.core
  (:require [ubik.handlers
             [core :refer [ubik-ring-handler]]
             [event :refer [event-msg-handler]]]
            [ubik
             [sente :refer [ch-chsk]]
             [scheduler :refer [start-scheduler! stop-scheduler!]]]
            [clojure.edn :as edn]
            [mount.core :refer [defstate] :as mount]
            [taoensso.timbre :refer [debugf]]
            [taoensso.sente :as sente]
            [org.httpkit.server :as http-kit]))

(defn start-router! []
  (sente/start-chsk-router! ch-chsk event-msg-handler))

(defn start-web-server! [{:keys [port] :or {port 3000}}]
  (let [stop-fn (http-kit/run-server #'ubik-ring-handler {:port port})
        uri (format "http://localhost:%s/" port)]
    (debugf "uri: %s" uri)
    (try
      (.browse (java.awt.Desktop/getDesktop) (java.net.URI. uri))
      (catch java.awt.HeadlessException _))
    stop-fn))

(defstate scheduler
  :start (start-scheduler!)
  :stop (stop-scheduler!))

(defstate router
  :start (start-router!)
  :stop (router))

(defstate web-server
  :start (start-web-server! (mount/args))
  :stop (web-server :timeout 100))

(defn mount-args [[port]]
  {:port (edn/read-string port)})

(defn -main [& args]
  (mount/start-with-args (mount-args args)))
