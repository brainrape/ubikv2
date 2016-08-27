(ns ubik.anim.bg.core
  (:require [ubik.anim.cameras :refer [get-perspective-camera]]
            [ubik.anim.video-texture :refer [preloaded-video-textures update-video-texture]]
            [taoensso.encore :refer [debugf]]))

(def THREE js/THREE)

(def active-vt (atom nil))

(def plane-mesh
  (let [geometry (THREE.PlaneGeometry. (.-innerWidth js/window) (.-innerHeight js/window))]
                  (THREE.Mesh. geometry)))

(defn get-basic-material [texture] )

(defn init-bg-anim! [anim-id]
  (let [{:keys [video texture] :as vt} (get-in preloaded-video-textures [:bg anim-id])
        material (THREE.MeshBasicMaterial. #js {:map texture})]
    (reset! active-vt vt)
    (.play video)
    (set! (.-material plane-mesh) material)))

(defn set-active-anim! [{:keys [id] :as anim}]
  (when (and (not= id (:id @active-vt)) (some? id))
    (let [{:keys [video texture] :as vt} (get-in preloaded-video-textures [:bg id])
          material (THREE.MeshBasicMaterial. #js {:map texture})
          prev-video (:video @active-vt)]
      (.play video)
      (reset! active-vt vt)
      (set! (.-material plane-mesh) material)
      (update-video-texture vt)
      (.pause prev-video)
      nil)))

(def bg-animation
  (let [camera (get-perspective-camera)
        scene (THREE.Scene.)]
    (.add scene plane-mesh)
    (fn [{:keys [anims delta now-msec] :as state} render-fn]
      (update-video-texture @active-vt)
      (render-fn scene camera)
      (assoc-in state [:anims :bg] (set-active-anim! (:bg anims))))))
