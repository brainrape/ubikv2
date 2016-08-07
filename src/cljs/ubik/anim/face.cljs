(ns ubik.anim.face
  (:require [ubik.anim.cameras :refer [get-perspective-camera]]
            [ubik.anim.video-texture :refer [get-active-video-textures]]
            [ubik.commons.core :refer [face-anim-types]]))

(def THREE js/THREE)
(def steps 30)
(def active-vts (atom {}))
(def progress (atom {}))

(defn get-face-mesh []
  (let [geometry (THREE.CubeGeometry. 600 200 600)]
    (THREE.Mesh. geometry)))

(def meshes (into (sorted-map) (map (fn [k] [k (get-face-mesh)]) face-anim-types)))

(defn set-active-vts! [anims]
  (reset! active-vts (into {} (map (fn [[type id]] [type (get-active-video-textures type id)]) anims))))

(defn get-basic-material [texture] (THREE.MeshBasicMaterial. #js {:map texture}))

(defn get-multi-material [front-face active-vts]
  (let [face-mapping {4 0, 1 1, 5 2, 0 3}
        get-face-material
        (fn [face]
          (let [face-idx (face-mapping face)]
            (cond
              (= face-idx front-face) (get-basic-material (get-in active-vts [:curr :texture]))
              (= face-idx (mod (inc front-face) 4)) (get-basic-material (get-in active-vts [:next :texture]))
              (= face-idx (mod (dec front-face) 4)) (get-basic-material (get-in active-vts [:prev :texture]))
              :else (THREE.MeshBasicMaterial. #js {:color 0x000000}))))]
    (THREE.MultiMaterial. (clj->js (map get-face-material (range 6))))))

(defn set-face-mesh-rotation! [mesh id]
  (set! (.. mesh -rotation -y) (* (mod id 4) (.-PI js/Math) 0.5)))

(defn init-face-anim! [anims]
  (set-active-vts! anims)
  (doseq [[_, vt] @active-vts] (.play (get-in vt [:curr :video])))
  (doseq [[type mesh] meshes]
    (let [material (get-multi-material (anims type) (@active-vts type))]
      (set-face-mesh-rotation! mesh (anims type))
      (set! (.-material mesh) material))))

(defn get-front-face-idx [mesh]
  (mod (.round js/Math (/ (.. mesh -rotation -y) (.-PI js/Math) 0.5)) 4))

(defn update-video-texture [{:keys [video texture]}]
  (when (= (.-readyState video) (.-HAVE_ENOUGH_DATA video))
    (set! (.-needsUpdate texture) true)))

(defn rotate-mesh [{:keys [direction id type] :as anim}]
  (when (some? anim)
    (let [mesh (meshes type)
          vt (@active-vts type)]
      (if (some? (@progress type))
        (if (= (@progress type) steps)
          (do
            (swap! progress assoc type nil)
            (.pause (get-in vt [:curr :video]))
            (let [updated-vts (get-active-video-textures type id)]
              (set! (.-material mesh) (get-multi-material (get-front-face-idx mesh) updated-vts))
              (swap! active-vts assoc type updated-vts))
            nil)
          (do
            (swap! progress update-in [type] inc)
            (let [rotation-op (if (= direction :prev) - +)]
              (set! (.. mesh -rotation -y) (rotation-op (.. mesh -rotation -y) (/ (/ (.-PI js/Math) 2) steps))))
            (update-video-texture (vt direction))
          anim))
      (do (swap! progress assoc type 0)
          (.play (get-in vt [direction :video]))
          (update-video-texture (vt direction))
          anim)))))

(def face-animation
  (let [camera (get-perspective-camera)
        scene (THREE.Scene.)]
    (doseq [[i, [type mesh]] (map-indexed vector meshes)]
      (set! (.. mesh -position -y) (- (* i 250) 250))
      (.add scene mesh))
    (fn [{:keys [anims] :as state} render-fn]
      (doseq [[_ vts] @active-vts] (update-video-texture (:curr vts)))
      (render-fn scene camera)
      (let [updated-anims (into {} (map (fn [[type anim]] [type (rotate-mesh anim)]) (dissoc anims :bg)))]
        (assoc-in state [:anims] updated-anims)))))
