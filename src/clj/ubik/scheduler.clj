(ns ubik.scheduler
  (:require [ubik
             [commons :refer [current-anims]]
             [sente :refer [connected-uids chsk-send!]]]
            [clojure.core.async :as async :refer [<! go-loop put! chan alt!]]))

(def user-queue (atom clojure.lang.PersistentQueue/EMPTY))

(def tick-ms 20000)

(def last-tick-timestamp (atom (System/currentTimeMillis)))

(defn calculate-action-timeout [queue-pos]
  (let [last-tick-elapsed (- (System/currentTimeMillis) @last-tick-timestamp)]
    (+ (* (- queue-pos 2) tick-ms) (- tick-ms last-tick-elapsed))))

(defn scheduler []
  (let [poison-ch (chan)]
    (go-loop []
      (<! (async/timeout tick-ms))
      (when (alt! poison-ch false :default :keep-going)
        (reset! last-tick-timestamp (System/currentTimeMillis))
        (loop [users @user-queue]
          (when-let [uid (and (< 1 (count users)) (peek users))]
            (if (contains? (:any @connected-uids) uid)
              (do
                (chsk-send! uid [:ubik/stop-action {:action-time (calculate-action-timeout (count @user-queue))}])
                (swap! user-queue #(conj (pop %1) %2) uid)
                (chsk-send! (peek @user-queue) [:ubik/start-action @current-anims]))
              (recur (pop users)))))
        (recur)))
    poison-ch))

(defonce scheduler-poison-ch (atom nil))
(defn stop-scheduler! [] (when-let [ch @scheduler-poison-ch] (put! ch :stop)))
(defn start-scheduler! []
  (stop-scheduler!)
  (reset! last-tick-timestamp (System/currentTimeMillis))
  (reset! scheduler-poison-ch (scheduler)))
