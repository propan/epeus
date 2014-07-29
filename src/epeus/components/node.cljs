(ns epeus.components.node
  (:require-macros [cljs.core.async.macros :refer [alt! go go-loop]])
  (:require [cljs.core.async :as async :refer [chan close! put! sliding-buffer tap >! <!]]
            [epeus.utils :as u :refer [element-bounds hidden next-uid]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as string]))

(def ESCAPE_KEY 27)
(def ENTER_KEY 13)

(defn update-bounds
  [ch owner]
  (let [n      (om/get-node owner)
        bounds (element-bounds n)]
    (put! ch bounds)))

(defn state-chan
  [in]
  (let [out (chan (sliding-buffer 1))]
    (go
     (loop [state nil
            next  (<! in)]
       (when next
         (when-not (= state next)
           (>! out next))
         (recur next (<! in)))))
    out))

;;
;; Positioning
;;

(defn calculate-button-position
  [owner side root?]
  (if-let [[w h] (om/get-state owner :bounds)]
    (let [offset-x (if root?  7 0)
          offset-y (if root? -3 0)]
      (if (= side :left)
        [(+ offset-x -23) (- (/ h 2) 11 offset-y)]
        [w                (- (/ h 2) 11 offset-y)]))
    [0 0]))

;;
;; Interaction
;;

(defn edit-start
  [node owner events]
  (put! events [:tooltip nil])
  (if-not (om/get-state owner :ignore-click)
    (doto owner
      (om/set-state! :editing true)
      (om/set-state! :needs-focus true)
      (om/set-state! :edit-title (:title @node)))
    (om/set-state! owner :ignore-click nil)))

(defn commit-changes
  [e node owner events]
  (when (om/get-state owner :editing)
    ;; reset editing state
    (doto owner
      (om/set-state! :hover-node nil)
      (om/set-state! :editing nil))
    ;; trigger rename event only if the title has changed 
    (let [new-title (om/get-state owner :edit-title)
          node      @node]
      (when (and new-title
                 (not (= (:title node) new-title)))
        (put! events [:rename [(:uid node) new-title]])))))

(defn change [e node owner]
  (om/set-state! owner :edit-title (.. e -target -value)))

(defn key-down
  [e node owner events]
  (condp == (.-keyCode e)
    ESCAPE_KEY (doto owner
                 (om/set-state! :hover-node nil)
                 (om/set-state! :editing nil))
    ENTER_KEY  (commit-changes e node owner events)
    nil))

(defn drag-start
  [e node owner]
  (when-not (om/get-state owner :editing)
    (let [n     (om/get-node owner)
          rel-x (- (.-pageX e) (.-offsetLeft n))
          rel-y (- (.-pageY e) (.-offsetTop n))]
      (doto owner
        (om/set-state! :rel-x rel-x)
        (om/set-state! :rel-y rel-y)
        (om/set-state! :dragging true)))))

(defn drag-stop
  [e node owner events]
  (when (om/get-state owner :dragging)
    ;; reset dragging state
    (om/set-state! owner :dragging nil)
    ;; trigger drop event only if the node was dragged
    (when (om/get-state owner :ignore-click)
      (let [rel-y (om/get-state owner :rel-y)
            rel-x (om/get-state owner :rel-x)
            off-x (.-clientX e)
            off-y (.-clientY e)]
        (put! events [:drop [@node [(- off-x rel-x) (- off-y rel-y)]]])))))

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
  [e owner events]
  (when-not (or (om/get-state owner :editing)
                (om/get-state owner :dragging))
    (put! events [:tooltip "drag to move or click to edit"])
    (let [label (om/get-node owner "label")]
      (doto owner
        (om/set-state! :bounds (element-bounds label))
        (om/set-state! :hover-node true)))))

(defn mouse-leave
  [e node owner events]
  (when-not (om/get-state owner :editing)
    (put! events [:tooltip nil])
    (om/set-state! owner :hover-node nil)))

(defn execute-action
  [e node position owner events]
  (let [node @node]
    (if (om/get-state owner :alt)
      (doto events
          (put! [:tooltip nil])
          (put! [:remove node]))
      (put! events [:add [node position]]))))

(defn mouse-enter-action
  [e owner root? events]
  (om/set-state! owner :hover-action true)
  (when e
    (.stopPropagation e))
  (if (and (om/get-state owner :alt) (not root?))
    (put! events [:tooltip "click to remove or release <alt> to branch out"])
    (put! events [:tooltip (str "click to branch out" (when-not root? " or hold <alt> to remove"))])))

(defn mouse-leave-action
  [e owner events]
  (om/set-state! owner :hover-action nil)
  (put! events [:tooltip nil]))

;;
;; Component
;;

(defn render-action-button
  [owner events {:keys [color root?] :as node} side actionable alt]
  (let [[left top] (calculate-button-position owner side root?)]
    (dom/div #js {:ref "action-button"
                  :className "action-button"
                  :style #js {:backgroundColor (when-not root? color)
                              :top             top
                              :left            left
                              :display         (if actionable "inline-block" "none")}
                  ;; prevent event propagation to web-node onMouseDown
                  :onMouseDown #(.stopPropagation %)
                  :onMouseOver #(mouse-enter-action % owner root? events)
                  :onMouseOut  #(mouse-leave-action % owner events)
                  :onClick     #(execute-action % node side owner events)}
             (dom/img #js {:src (if (and (true? alt) (not root?))
                                  "resources/images/minus.svg"
                                  "resources/images/plus.svg")}))))

(defn node-component
  [node owner]
  (reify
    om/IDisplayName
    (display-name [_]
      (str "node-" (:uid node)))
    
    om/IInitState
    (init-state [_]
      (let [dim (chan (sliding-buffer 1))]
        {:edit-title (:title node)
         :dim-in     dim
         :dim-out    (state-chan dim)
         :kill-ch    (chan)}))

    om/IDidMount
    (did-mount [_]
      ;; something is wrong here
      (let [comm    (om/get-state owner :comm)
            alt-ch  (chan)
            up-ch   (chan)
            move-ch (chan)
            events  (:events comm)
            kill    (om/get-state owner :kill-ch)
            dim-in  (om/get-state owner :dim-in)
            dim     (om/get-state owner :dim-out)
            up      (tap (:mouse-up comm) up-ch)
            move    (tap (:mouse-move comm) move-ch)
            alt     (tap (:alt-key comm) alt-ch)]
        (update-bounds dim-in owner)
        (go-loop []
                 (let [[v ch] (alts! [kill alt dim move up] :priority true)]
                   (condp = ch
                     alt  (do
                            (om/set-state! owner :alt v)
                            (when (om/get-state owner :hover-action)
                              (mouse-enter-action nil owner (:root? @node) events))
                            (recur))
                     dim  (do
                            (put! events [:dim [(:uid @node) v]])
                            (recur))
                     kill (do
                            (async/untap (:mouse-up comm) up-ch)
                            (async/untap (:mouse-move comm) move-ch)
                            (async/untap (:alt-key comm) alt-ch)
                            (close! dim-in)
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
          (update-bounds (om/get-state owner :dim-in) owner))
        (when (and editing
                   (om/get-state owner :needs-focus))
          (let [node (om/get-node owner "edit-field")
                len  (.. node -value -length)]
            (.focus node)
            (.setSelectionRange node len len)
            (om/set-state! owner :needs-focus nil)))))
    
    om/IRenderState
    (render-state [_ {:keys [alt comm dragging edit-title editing hover-node hover-action]}]
      (let [{:keys [x y color title position has-kids? root?]} node
            events                                             (:events comm)
            actionable                                         (and (or hover-node hover-action)
                                                                    (not (or editing dragging)))
            empty                                              (string/blank? (.trim title))]
        (dom/div #js {:className   (str (if root? "root-node" "web-node") (when dragging " dragging"))
                      :style #js   {:top y :left x :color (when-not root? (u/darken color 0.35))}
                      :onMouseDown #(drag-start % node owner)
                      :onMouseUp   #(drag-stop % node owner events)
                      :onMouseOver #(mouse-enter % owner events)
                      :onMouseOut  #(mouse-leave % node owner events)}
                 (dom/div #js {:ref       "label"
                               :className (if empty "node-empty-label" "node-label")
                               :style     (if (and has-kids? (not root?))
                                            #js {:position "relative" :top -15 :left -10 :display (when editing "none")}
                                            #js {:display (when editing "none")})
                               :onClick   #(edit-start node owner events)}
                          (if empty "[click to edit]" title))
                 (dom/input #js {:ref       "edit-field"
                                 :style     (hidden (not editing))
                                 :value     edit-title
                                 :onKeyDown #(key-down % node owner events)
                                 :onChange  #(change % node owner)
                                 :onBlur    #(commit-changes % node owner events)})
                 (when root?
                   (render-action-button owner events node :left actionable alt))
                 (render-action-button owner events node position actionable alt))))))
