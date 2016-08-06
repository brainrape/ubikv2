(ns ubik.anim.face-anim
  (:require [ubik.anim.cameras :refer [get-perspective-camera]]
            [ubik.anim.video-texture :refer [get-active-video-textures]]
            [ubik.commons.core :refer [anim-ids]]
            [taoensso.encore :refer [debugf]]))

(def THREE js/THREE)
(def steps 30)

(defn get-front-face-idx [mesh]
  (mod (.round js/Math (/ (.. mesh -rotation -y) (.-PI js/Math) 0.5)) 4))

(defn get-face-mesh []
  (let [geometry (THREE.CubeGeometry. 600 200 600)]
    (THREE.Mesh. geometry)))

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

(defn update-video-texture [{:keys [video texture]}]
  (when (= (.-readyState video) (.-HAVE_ENOUGH_DATA video))
    (set! (.-needsUpdate texture) true)))

(defn rotate-mesh [mesh {:keys [direction id type] :as anim} progress vts]
  (when (some? anim)
    (if (some? @progress)
      (if (= @progress steps)
        (do
          (reset! progress nil)
          (.pause (get-in @vts [:curr :video]))
          (let [updated-vts (get-active-video-textures type id)]
            (set! (.-material mesh) (get-multi-material (get-front-face-idx mesh) updated-vts))
            (reset! vts updated-vts))
          nil)
        (do
          (swap! progress inc)
          (let [rotation-op (if (= direction :prev) - +)]
            (set! (.. mesh -rotation -y) (rotation-op (.. mesh -rotation -y) (/ (/ (.-PI js/Math) 2) steps))))
          (update-video-texture (@vts direction))
          anim))
      (do (reset! progress 0) (.play (get-in @vts [direction :video])) anim))))

(def face-animation
  (let [anim-types (keys (dissoc anim-ids :bg))
        camera (get-perspective-camera)
        scene (THREE.Scene.)
        active-vts (into {} (map (fn [k] [k (atom (get-active-video-textures k 0))]) anim-types))
        materials (into {} (map (fn [k] [k (get-multi-material 0 (deref (active-vts k)))]) anim-types))
        meshes (into (sorted-map) (map (fn [k] [k (get-face-mesh)]) anim-types))
        progress (into {} (map (fn [k] [k (atom nil)]) anim-types))]
    (doseq [[i, [type mesh]] (map-indexed vector meshes)]
      (set! (.. mesh -position -y) (- (* i 250) 250))
      (set! (.-material mesh) (materials type))
      (.add scene mesh))
    (doseq [[_, vts] active-vts] (.play (get-in @vts [:curr :video])))
    (fn [{:keys [anims] :as state} render-fn]
      (doseq [[_ vts] active-vts] (update-video-texture (:curr @vts)))
      (render-fn scene camera)
      (let [updated-anims (into {} (map (fn [[k v]] [k (rotate-mesh (meshes k) v (progress k) (active-vts k))])
                                        (dissoc anims :bg)))]
        (assoc-in state [:anims] updated-anims)))))
