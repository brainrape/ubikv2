(ns ubik.anim.bg.bg-video-texture
  (:require [ubik.anim.cameras :refer [get-perspective-camera]]
            [ubik.anim.video-texture :refer [get-video-texture update-video-texture]]
            [taoensso.encore :refer [debugf]]))

(defn get-bg-video-anim [id]
  (let [{:keys [video texture] :as vt} (get-video-texture :bg id)
        camera (get-perspective-camera)
        scene (THREE.Scene.)
        geometry (THREE.PlaneGeometry. (.-innerWidth js/window) (.-innerHeight js/window))
        material (THREE.MeshBasicMaterial. #js {:map texture})
        mesh (THREE.Mesh. geometry material)]
    (.add scene mesh)
    {:update-fn (fn [{:keys [anims delta now-msec] :as state} render-fn]
                  (update-video-texture vt)
                  (render-fn scene camera)
                  state)
     :stop-fn (fn [] (.pause video))
     :start-fn (fn [] (.play video))}))
