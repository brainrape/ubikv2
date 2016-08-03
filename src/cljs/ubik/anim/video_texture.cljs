(ns ubik.anim.video-texture)

(defn get-video-texture [url]
  (let [video (.createElement js/document "video")
        texture	(js/THREE.Texture. video)]
    (set! (.-height video) 1024)
    (set! (.-width video) 340)
    (set! (.-autoplay video) false)
    (set! (.-loop video) true)
    (set! (.-src video) url)
    (set! (.-minFilter texture) js/THREE.LinearFilter)
    [video texture]))
