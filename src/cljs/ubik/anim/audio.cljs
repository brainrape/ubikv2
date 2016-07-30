(ns ubik.anim.audio)

(enable-console-print!)

(defn setup-audio-stream [audio-ctx analyser distortion biquad-filter convolver gain-node]
  (.webkitGetUserMedia
   js/navigator
   #js {:audio true}
   (fn [stream]
     (let [source (.createMediaStreamSource audio-ctx stream)]
       (.connect source analyser)
       (.connect analyser distortion)
       (.connect distortion biquad-filter)
       (.connect biquad-filter convolver)
       (.connect convolver gain-node)
       (.connect gain-node (.-destination audio-ctx))))
   (fn [err] (.log js/console err))))

(defn create-audio-analyser [audio-ctx]
  (let [analyser (.createAnalyser audio-ctx)]
    (set! (.-minDecibels analyser) -90)
    (set! (.-maxDecibels analyser) -10)
    (set! (.-smoothingTimeConstant analyser) 0.85)
    (set! (.-fftSize analyser) 32)
    analyser))

(defn get-audio-analyser []
  (let [audio-ctx (or (js/window.AudioContext.) (js/window.webkitAudioContext.))
        analyser (create-audio-analyser audio-ctx)
        distortion (.createWaveShaper audio-ctx)
        gain-node (.createGain audio-ctx)
        biquad-filter (.createBiquadFilter audio-ctx)
        convolver (.createConvolver audio-ctx)]
    (setup-audio-stream audio-ctx analyser distortion biquad-filter convolver gain-node)
    analyser))

(defn get-data-array [analyser]
  (let [buffer-length (.-frequencyBinCount analyser)]
    (js/Uint8Array. buffer-length)))
