(ns ubik.anim.face.core
  (:require [ubik.anim.cameras :refer [get-perspective-camera]]
            [ubik.anim.video-texture :refer [get-active-video-textures update-video-texture]]
            [ubik.commons.core :refer [face-anim-types]]
            [taoensso.encore :refer [debugf]]))

(def THREE js/THREE)
(def PI (.-PI js/Math))
(def steps 100)
(def active-vts (atom {}))
(def progress (atom {}))

(def camera-settings {:fov 14 :near 1 :far 6000 :pos {:x 0 :y 0 :z 5000} :aspect (/ 1000 1150)})

(def face-settings {:top {:size {:x 1000 :y 330 :z 20} :pos {:x 0 :y 400}}
                    :center {:size {:x 1000 :y 330 :z 20} :pos {:x 0 :y -10}}
                    :bottom {:size {:x 1000 :y 330 :z 20} :pos {:x 0 :y -410}}})

(defn get-face-mesh [type]
  (let [{:keys [x y z]} (get-in face-settings [type :size])
        geometry (THREE.CubeGeometry. x y z)]
    (THREE.Mesh. geometry)))

(def meshes (into (sorted-map) (map (fn [k] [k (get-face-mesh k)]) face-anim-types)))

(defn set-active-vts! [anims]
  (reset! active-vts
          (into {} (map (fn [[type id]] [type (get-active-video-textures type id)]) anims))))

(defn get-basic-material [texture] (THREE.MeshBasicMaterial. #js {:map texture}))

(defn get-multi-material [front-face active-vts swap?]
  (let [face-mapping {4 0, 1 1, 5 2, 0 3}
        get-face-material
        (fn [face]
          (let [face-idx (face-mapping face)
                curr-k (if swap? :prev :curr)
                prev-k (if swap? :curr :prev)]
            (cond
              (= face-idx (mod front-face 4)) (get-basic-material (get-in active-vts [curr-k :texture]))
              (= face-idx (mod (- front-face 2) 4)) (get-basic-material (get-in active-vts [prev-k :texture]))
              :else (THREE.MeshBasicMaterial. #js {:color 0x000000}))))]
    (THREE.MultiMaterial. (clj->js (map get-face-material (range 6))))))

(defn get-front-face-idx [mesh]
  (mod (.round js/Math (/ (.. mesh -rotation -y) PI 0.5)) 4))

(defn init-face-anim! [anims]
  (set-active-vts! anims)
  (doseq [[_, vt] @active-vts] (.play (get-in vt [:curr :video])))
  (doseq [[type mesh] meshes]
    (let [material (get-multi-material (get-front-face-idx mesh) (@active-vts type) false)]
      (set! (.-material mesh) material))))

(def velocities (atom {}))

(defn get-velocity [direction v delta prog]
  (if (= direction :next)
    (let [accel 65
          max-speed 60]
      (cond
        (< prog 25) (+ v (* accel (/ delta 1000)))
        (<= 25 prog 74) max-speed
        (< 74 prog) (- v (* accel (/ delta 1000)))))
    (let [accel 120
          max-speed 65]
      (cond
        (< prog 25) (+ v (* accel (/ delta 1000)))
        (<= 25 prog 83) max-speed
        (< 83 prog) (- v (* accel (/ delta 1000)))))))

(defn rotate-mesh [{:keys [direction id type] :as anim} delta]
  (when (some? anim)
    (let [mesh (meshes type)
          vt (@active-vts type)
          prog (@progress type)
          velocity (@velocities type)]
      (if (some? prog)
        (if (> prog steps)
          (do
            (swap! progress assoc type nil)
            (swap! velocities assoc type 0)
            (.pause (get-in vt [:prev :video]))
            nil)
          (do
            (let [rotation-op (if (= direction :prev) + -)
                  v (or velocity 0)
                  s (* v (/ delta 1000))
                  v' (get-velocity direction v delta prog)]
              (debugf "%s %s" prog (.. mesh -rotation -y))
              (swap! velocities assoc type v')
              (swap! progress update type + (Math/abs s))
              (set! (.. mesh -rotation -y) (rotation-op (.. mesh -rotation -y) (* (/ s steps) PI))))
            (update-video-texture (:prev vt))
          anim))
      (do (swap! progress assoc type 0)
          (let [prev-id (get-in vt [:curr :id])
                updated-vts (get-active-video-textures type id prev-id)]
            (set! (.-material mesh) (get-multi-material (get-front-face-idx mesh) updated-vts true))
            (swap! active-vts assoc type updated-vts)
            (.play (get-in updated-vts [:curr :video]))
            (update-video-texture (:prev updated-vts)))
          anim)))))

(def face-animation
  (let [{:keys [fov near far pos aspect]} camera-settings
        camera (get-perspective-camera fov near far pos aspect)
        scene (THREE.Scene.)]
    (doseq [[i, [type mesh]] (map-indexed vector meshes)]
      (set! (.. mesh -position -x) (get-in face-settings [type :pos :x]))
      (set! (.. mesh -position -y) (get-in face-settings [type :pos :y]))
      (.add scene mesh))
    (fn [{:keys [anims delta] :as state} render-fn]
      (doseq [[_ vts] @active-vts] (update-video-texture (:curr vts)))
      (render-fn scene camera)
      (let [updated-anims (into {} (map (fn [[type anim]] [type (rotate-mesh anim delta)]) (dissoc anims :bg)))]
        (assoc-in state [:anims] updated-anims)))))
