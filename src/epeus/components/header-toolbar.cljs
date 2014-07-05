(ns epeus.components.header-toolbar
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [epeus.history :as history]))

;;
;; Actions
;;

(defn reset-app-state
  [state]
  (om/transact! state [:main :items]
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

(defn undo-button-component
  [state owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className (toolbar-item-class history/can-undo?)
                    :onClick   #(history/undo)}
               (dom/i #js {:className "icon-undo"})))))

(defn redo-button-component
  [state owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className (toolbar-item-class history/can-redo?)
                    :onClick   #(history/redo)}
               (dom/i #js {:className "icon-redo"})))))

(defn reset-button-component
  [state owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "toolbar-item"
                    :onClick   #(reset-app-state state)}
               (dom/i #js {:className "icon-trash"})))))

(defn header-toolbar-component
  [state owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "inner"}
               (om/build undo-button-component state)
               (om/build redo-button-component state)
               (om/build reset-button-component state)))))
