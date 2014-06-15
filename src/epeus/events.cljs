(ns epeus.events
  (:require [cljs.core.async :as async :refer [chan mult put! sliding-buffer <!]]
            [goog.events :as events]))

(defn- sliding-events-chan
  [type]
  (let [ch (chan (sliding-buffer 1))]
    (events/listen js/document type #(put! ch %))
    ch))

(defn mouse-up
  []
  (mult (sliding-events-chan events/EventType.MOUSEUP)))

(defn mouse-down
  []
  (mult (sliding-events-chan events/EventType.MOUSEDOWN)))

(defn mouse-move
  []
  (mult (sliding-events-chan events/EventType.MOUSEMOVE)))

(defn keyboard-alt
  []
  (let [ch (chan (sliding-buffer 1))]
    (events/listen js/window events/EventType.KEYUP #(when (= (.-keyCode %) 18)
                                                         (put! ch false)))
    (events/listen js/window events/EventType.KEYDOWN #(when (= (.-keyCode %) 18)
                                                           (put! ch true)))
    (mult ch)))
