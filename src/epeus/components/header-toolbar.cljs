(ns epeus.components.header-toolbar
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [epeus.history :as history]))

;;
;; Actions
;;

(defn reset-app-state
  [state]
  (om/transact! state :items
                #(-> %
                     (assoc :title "")
                     (assoc :children {}))
                :create-restore-point))

;;
;; Helpers
;;

(defn toolbar-item-class
  [predicate]
  (str "toolbar-item"
       (when-not (predicate) " disabled")))

;;
;; Components
;;

(defn create-button-component
  [class predicate action]
  (fn  [state owner]
    (reify
      om/IRender
      (render [_]
        (dom/div #js {:className (toolbar-item-class predicate)
                      :onClick   action}
         (dom/i #js {:className class}))))))

(defn header-toolbar-component
  [state owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "inner"}
               (om/build (create-button-component
                          "icon-undo" history/can-undo? #(history/undo))
                         state)
               (om/build (create-button-component
                          "icon-redo" history/can-redo? #(history/redo))
                         state)
               (om/build (create-button-component
                          "icon-trash" #(constantly true) #(reset-app-state state))
                         state)))))
