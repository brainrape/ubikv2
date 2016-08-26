(ns ubik.anim.bg.core
  (:require [ubik.anim.cameras :refer [get-perspective-camera]]
            [ubik.anim.renderers :as renderers :refer [update-animation main-renderer]]
            [ubik.anim.bg.cube :refer [cube-animation]]
            [ubik.anim.bg.canvas :refer [canvas-animation]]
            [ubik.anim.bg.bg-video-texture :refer [get-bg-video-anim]]
            [ubik.commons.core :refer [anim-ids]]
            [taoensso.encore :refer [debugf]]))

(def THREE js/THREE)

(def active-anim (atom nil))

(def all-bg-anims {0 (get-bg-video-anim 0) 1 (get-bg-video-anim 1) 2 (get-bg-video-anim 2)})

(defn init-bg-anim! [anim-id]
  (let [{:keys [start-fn] :as anim} (all-bg-anims anim-id)]
    (start-fn)
    (reset! active-anim [anim-id anim])))

(def plane-render-target (renderers/get-plane-render-target))

(defn set-active-anim! [{:keys [id] :as anim}]
  (let [[active-anim-id {:keys [stop-fn]}] @active-anim]
    (if (and (not= id active-anim-id) (some? id))
      (let [{:keys [start-fn] :as next-anim} (all-bg-anims id)]
        (stop-fn)
        (start-fn)
        (reset! active-anim [id next-anim])
        nil)
      anim)))

(def bg-animation
  (let [camera (get-perspective-camera)
        scene (THREE.Scene.)
        {:keys [rt-texture mesh]} plane-render-target]
    (.add scene mesh)
    (fn [{:keys [anims delta now-msec] :as state} render-fn]
      (let [[_ active-anim] @active-anim
            updated-state (update-animation main-renderer (:update-fn active-anim) rt-texture state)
            final-state (assoc-in updated-state [:anims :bg] (set-active-anim! (:bg anims)))]
        (render-fn scene camera)
        final-state))))
