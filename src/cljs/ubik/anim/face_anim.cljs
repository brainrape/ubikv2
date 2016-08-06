(ns ubik.anim.face-anim
  (:require [ubik.anim.cameras :refer [get-perspective-camera]]
            [ubik.anim.video-texture :refer [get-active-video-textures]]
            [taoensso.encore :refer [debugf]]))

(def THREE js/THREE)
(def steps 30)

(defn get-front-face-idx [mesh]
  (mod (.round js/Math (/ (.. mesh -rotation -y) (.-PI js/Math) 0.5)) 4))

(defn rotate-mesh [mesh anim progress]
  (when (some? anim)
    (if (some? @progress)
      (if (= @progress steps)
        (do (reset! progress nil) (debugf "face idx %s" (get-front-face-idx mesh)) nil)
        (do
          (swap! progress inc)
          (let [rotation-op (if (= (:direction anim) :prev) - +)]
            (set! (.. mesh -rotation -y) (rotation-op (.. mesh -rotation -y) (/ (/ (.-PI js/Math) 2) steps))))
          anim))
      (do (reset! progress 0) anim))))

(defn get-multi-material [front-face active-vts]
  (let [face-mapping {4 0, 1 1, 5 2, 0 3}
        get-material (fn [face]
          (let [face-idx (face-mapping face)]
            (cond
              (= face-idx front-face) (THREE.MeshBasicMaterial. #js {:map (get-in active-vts [:curr :texture])})
              (= face-idx (mod (inc front-face) 4))
              (THREE.MeshBasicMaterial. #js {:map (get-in active-vts [:next :texture])})
              (= face-idx (mod (dec front-face) 4))
              (THREE.MeshBasicMaterial. #js {:map (get-in active-vts [:prev :texture])})
              :else (THREE.MeshBasicMaterial. #js {:color 0x000000}))))]
    (THREE.MultiMaterial. (clj->js (map get-material (range 6))))))

(defn get-face-mesh []
  (let [geometry (THREE.CubeGeometry. 600 200 600)]
    (THREE.Mesh. geometry)))

(def face-animation
  (let [anim-types [:top :center :bottom]
        camera (get-perspective-camera)
        scene (THREE.Scene.)
        active-vts (into {} (map (fn [k] [k (get-active-video-textures k 0)]) anim-types))
        materials (into {} (map (fn [k] [k (get-multi-material 0 (active-vts k))]) anim-types))
        meshes (into (sorted-map) (map (fn [k] [k (get-face-mesh)]) anim-types))
        progress (into {} (map (fn [k] [k (atom nil)]) anim-types))]
    (doseq [[i, [type mesh]] (map-indexed vector meshes)]
      (set! (.. mesh -position -y) (- (* i 250) 250))
      (set! (.-material mesh) (materials type))
      (.add scene mesh))
    (doseq [[_, vt] active-vts]
      (.play (get-in vt [:curr :video])))
    (fn [{:keys [anims] :as state} render-fn]
      (doseq [[k vts] active-vts]
        (let [video (get-in vts [:curr :video])]
          (when (= (.-readyState video) (.-HAVE_ENOUGH_DATA video))
            (set! (.-needsUpdate (get-in vts [:curr :texture])) true))))
      (render-fn scene camera)
      (let [updated-anims (into {} (map (fn [[k v]] [k (rotate-mesh (meshes k) v (progress k))]) (dissoc anims :bg)))]
        (assoc-in state [:anims] updated-anims)))))
