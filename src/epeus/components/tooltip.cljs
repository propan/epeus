(ns epeus.components.tooltip
  (:require-macros [cljs.core.async.macros :refer [alt! go go-loop]])
  (:require [cljs.core.async :as async :refer [chan close! put!]]
            [epeus.events :refer [window-resize]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn update-window-size
  [owner [w h]]
  (doto owner
    (om/set-state! :window-width w)
    (om/set-state! :window-height h)))

;;
;; Components
;;

(defn tooltip-component
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_]
      "tooltip")
    
    om/IInitState
    (init-state [_]
      {:kill-ch (chan)})

    om/IDidMount
    (did-mount [_]
      (let [resize-ch (window-resize)
            kill-ch   (om/get-state owner :kill-ch)]
        (go-loop []
                 (let [[v ch] (alts! [kill-ch resize-ch] :priority true)]
                   (condp = ch
                     resize-ch (do
                                 (update-window-size owner v)
                                 (recur))
                     kill-ch   (do
                                 (close! resize-ch)
                                 (close! kill-ch))
                     nil)))))

    om/IWillUnmount
    (will-unmount [_]
      (put! (om/get-state owner :kill-ch) true))
    
    om/IRenderState
    (render-state [_ {:keys [window-width window-height]}]
      (let [tooltip (get state :tooltip "")]
        (dom/div #js {:className "tooltip"}
                 tooltip)))))
