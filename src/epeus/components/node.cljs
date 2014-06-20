(ns epeus.components.node
  (:require-macros [cljs.core.async.macros :refer [alt! go go-loop]])
  (:require [cljs.core.async :as async :refer [chan close! put! tap]]
            [epeus.utils :as u :refer [element-bounds hidden next-uid]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as string]))

(def ESCAPE_KEY 27)
(def ENTER_KEY 13)

(defn update-bounds
  [dim uid owner]
  (let [n (om/get-node owner)]
    (swap! dim assoc uid (element-bounds n))))

;;
;; Interaction
;;

(defn edit-start
  [node owner]
  (if-not (om/get-state owner :ignore-click)
    (doto owner
      (om/set-state! :editing true)
      (om/set-state! :needs-focus true)
      (om/set-state! :edit-title (:title @node)))
    (om/set-state! owner :ignore-click nil)))

(defn commit-changes
  [e node owner]
  (when-let [new-title (om/get-state owner :edit-title)]
    (om/update! node :title new-title :create-restore-point)
    (om/set-state! owner :editing false)))

(defn change [e node owner]
  (om/set-state! owner :edit-title (.. e -target -value)))

(defn key-down
  [e node owner]
  (condp == (.-keyCode e)
    ESCAPE_KEY (doto owner
                 (om/set-state! :editing nil)
                 (om/set-state! :edit-title (:title @node)))
    ENTER_KEY  (commit-changes e node owner)
    nil))

(defn drag-start
  [e node owner]
  (when-not (om/get-state owner :editing)
    (let [n     (om/get-node owner)
          rel-x (- (.-pageX e) (.-offsetLeft n))
          rel-y (- (.-pageY e) (.-offsetTop n))]
      (om/set-state! owner :rel-x rel-x)
      (om/set-state! owner :rel-y rel-y)
      (om/set-state! owner :dragging true))))

(defn drag-stop
  [e node owner events]
  (when (om/get-state owner :dragging)
    (om/set-state! owner :dragging nil)

    (let [rel-y (om/get-state owner :rel-y)
          rel-x (om/get-state owner :rel-x)
          off-x (.-clientX e)
          off-y (.-clientY e)]
      (let [dx (- off-x rel-x)
            dy (- off-y rel-y)]
        (put! events [:drag-stop [@node [dx dy]]])))))

(defn drag
  [e node owner events]
  (when (om/get-state owner :dragging)
    (om/set-state! owner :ignore-click true)
    (let [rel-y (om/get-state owner :rel-y)
          rel-x (om/get-state owner :rel-x)
          off-x (.-clientX e)
          off-y (.-clientY e)]
      (let [dx (- off-x rel-x)
            dy (- off-y rel-y)]
        (put! events [:move [@node [dx dy]]])))))

(defn mouse-enter
  [e node owner]
  (when-not (or (om/get-state owner :editing)
                (om/get-state owner :dragging))
    (let [label (om/get-node owner "label")
          [w h] (element-bounds label)]
      (om/set-state! owner :button-x w)
      (om/set-state! owner :hover true))))

(defn mouse-leave
  [e node owner]
  (om/set-state! owner :hover nil))

(defn execute-action
  [e node owner events]
  (let [node @node]
    (if (om/get-state owner :alt)
      (put! events [:remove node])
      (let [[offset] (get @(om/get-shared owner :dim) (:uid node) [0 20])]
        (om/set-state! owner :hover nil)
        (put! events [:add [node offset]])))))

;;
;; Component
;;

(defn node-component
  [node owner]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "node-" (:uid node)))
    
    om/IInitState
    (init-state [_]
      {:edit-title (:title node)
       :kill-ch    (chan)})

    om/IDidMount
    (did-mount [_]
      (update-bounds (om/get-shared owner :dim) (:uid node) owner)
      ;; something is wrong here
      (let [comm    (om/get-state owner :comm)
            alt-ch  (chan)
            up-ch   (chan)
            move-ch (chan)
            events  (:events comm)
            kill    (om/get-state owner :kill-ch)
            up      (tap (:mouse-up comm) up-ch)
            move    (tap (:mouse-move comm) move-ch)
            alt     (tap (:alt-key comm) alt-ch)]
        (go-loop []
                 (let [[v ch] (alts! [kill alt move up] :priority true)]
                   (condp = ch
                     alt  (do
                            (om/set-state! owner :alt v)
                            (recur))
                     kill (do
                            (async/untap (:mouse-up comm) up-ch)
                            (async/untap (:mouse-move comm) move-ch)
                            (async/untap (:alt-key comm) alt-ch)
                            (close! kill))
                     up   (do
                            (drag-stop v node owner events)
                            (recur))
                     move (do
                            (drag v node owner events)
                            (recur))
                     nil)))))

    om/IWillUnmount
    (will-unmount [_]
      (put! (om/get-state owner :kill-ch) true))
    
    om/IDidUpdate
    (did-update [_ _ _]
      (let [editing (om/get-state owner :editing)]
        (when-not editing
          (update-bounds (om/get-shared owner :dim) (:uid node) owner))
        (when (and editing
                   (om/get-state owner :needs-focus))
          (let [node (om/get-node owner "edit-field")
                len  (.. node -value -length)]
            (.focus node)
            (.setSelectionRange node len len)
            (om/set-state! owner :needs-focus nil)))))
    
    om/IRenderState
    (render-state [_ {:keys [alt comm dragging edit-title editing hover]}]
      (let [{:keys [x y color title uid]} node
            events                    (:events comm)
            root                      (= uid -1)
            actionable                (and hover
                                           (not (or editing dragging)))
            empty                     (string/blank? (.trim title))]
        (dom/div #js {:className   (str (if root "root-node" "web-node") (when dragging " dragging"))
                      :style #js   {:top y :left x :color (when-not root color)}
                      :onMouseDown #(drag-start % node owner)
                      :onMouseUp   #(drag-stop % node owner events)
                      :onMouseOver #(mouse-enter % node owner)
                      :onMouseOut  #(mouse-leave % node owner)}
                 (dom/div #js {:ref       "label"
                               :className (if empty "node-empty-label" "node-label")
                               :style     (hidden editing)
                               :onClick   #(edit-start node owner)}
                          (if empty "[click to edit]" title))
                 (dom/input #js {:ref       "edit-field"
                                 :style     (hidden (not editing))
                                 :value     edit-title
                                 :onKeyDown #(key-down % node owner)
                                 :onChange  #(change % node owner)
                                 :onBlur    #(commit-changes % node owner)})
                 (dom/div #js {:ref "action-button"
                               :className "action-button"
                               :style #js {:backgroundColor   (when-not root color)
                                           :top     (if root 3 -4)
                                           :left    (om/get-state owner :button-x)
                                           :display (if actionable "inline-block" "none")}
                               ;; prevent event propagation to web-node onMouseDown
                               :onMouseDown #(.stopPropagation %)
                               :onClick #(execute-action % node owner events)}
                          (dom/img #js {:src (if (true? alt)
                                               "resources/images/minus.svg"
                                               "resources/images/plus.svg")})))))))
