(ns ubik.anim.bg.cube
  (:require [ubik.anim.cameras :as cameras]
            [taoensso.encore :refer [debugf]]))

(def THREE js/THREE)

(def cube-animation
  (let [camera (cameras/get-perspective-camera)
        scene (THREE.Scene.)
        geometry (THREE.CubeGeometry. 600 400 600)
        material (THREE.MeshBasicMaterial. #js {:color 0xffffff, :wireframe true})
        mesh (THREE.Mesh. geometry material)]
    (.add scene mesh)
    (fn [{:keys [anims delta now-msec] :as state} render-fn]
      (set! (.-x (.-rotation mesh)) (+ (.-x (.-rotation mesh)) 0.02))
      (set! (.-y (.-rotation mesh)) (+ (.-y (.-rotation mesh)) 0.04))
      (render-fn scene camera)
      state)))
