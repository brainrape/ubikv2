(ns ubik.anim.bg.canvas
  (:require [ubik.anim.cameras :refer [get-perspective-camera]]
            [taoensso.encore :refer [debugf]]))

(def THREE js/THREE)

(defn get-bg-plane []
  (let [geometry (THREE.PlaneGeometry. 1920 1080)]
    (THREE.Mesh. geometry)))

(defn get-basic-material [texture] (THREE.MeshBasicMaterial. #js {:map texture}))

(defn get-canvas-with-ctx []
  (let [canvas (.createElement js/document "canvas")
        ctx (.getContext canvas "2d")]
    (set! (.-width canvas) 2048)
    (set! (.-height canvas) 1024)
    [canvas ctx]))

(defn update-canvas-anim! [canvas ctx delta now-msec]
  (set! (.-fillStyle ctx) "white")
  (.fillRect ctx 0 0 (.-width canvas) (.-height canvas))
  (set! (.-font ctx) "100pt Arial")
  (set! (.-fillStyle ctx) "red")
  (set! (.-textAlign ctx) "center")
  (set! (.-textBaseline ctx) "middle")
  (.fillText ctx now-msec (/ (.-width canvas) 2), (/ (.-height canvas) 2)))

(defn get-canvas-anim []
  (let [[canvas ctx] (get-canvas-with-ctx)
        texture (THREE.Texture. canvas)
        update-fn (comp (fn [_] (set! (.-needsUpdate texture) true)) (partial update-canvas-anim! canvas ctx))]
    [texture update-fn]))

(def canvas-animation
  (let [camera (get-perspective-camera)
        scene (THREE.Scene.)
        mesh (get-bg-plane)
        [texture update-fn] (get-canvas-anim)
        material (get-basic-material texture)]
    (.add scene mesh)
    (set! (.-material mesh) material)
    (fn [{:keys [anims delta now-msec] :as state} render-fn]
      (update-fn delta now-msec)
      (render-fn scene camera)
      state)))
