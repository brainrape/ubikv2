(ns ubik.anim.bg.core
  (:require [ubik.anim.cameras :refer [get-perspective-camera]]
            [ubik.anim.renderers :as renderers]
            [ubik.anim.bg.cube :refer [cube-animation]]
            [ubik.anim.bg.canvas :refer [canvas-animation]]
            [ubik.commons.core :refer [anim-ids]]
            [taoensso.encore :refer [debugf]]))

(def THREE js/THREE)

(def active-anim (atom nil))

(def all-bg-anims {0 cube-animation 1 canvas-animation})

(defn init-bg-anim! [anim-id]
  (reset! active-anim [anim-id (all-bg-anims anim-id)]))

(def plane-render-target (renderers/get-plane-render-target))

(defn set-active-anim! [{:keys [id] :as anim}]
  (let [[active-anim-id _] @active-anim]
    (if (and (not= id active-anim-id) (some? id))
      (do
        (reset! active-anim [id (all-bg-anims id)])
        nil)
      anim)))

(def bg-animation
  (let [camera (get-perspective-camera)
        scene (THREE.Scene.)
        {:keys [rt-texture mesh]} plane-render-target]
    (.add scene mesh)
    (fn [{:keys [anims delta now-msec] :as state} render-fn]
      (let [[_ active-anim] @active-anim
            updated-state (renderers/update-animation renderers/main-renderer active-anim rt-texture state)
            final-state (assoc-in updated-state [:anims :bg] (set-active-anim! (:bg anims)))]
        (render-fn scene camera)
        final-state))))
