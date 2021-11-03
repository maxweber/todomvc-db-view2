(ns todomvc-db-view.core
  (:require [reagent.core :as r]
            [todomvc-db-view.state.core :as state]
            [todomvc-db-view.db-view.get :as db-view]
            [todomvc-db-view.command.send :as command]
            [cljs.core.async :as a]
            ))

(def todo-list-cursor
  (state/cursor [:db-view/output
                 :todo/list]))

(def todo-list-params-cursor
  (state/cursor [:db-view/input
                 :todo/list]))

(def edit-cursor
  (state/cursor [:db-view/input
                 :todo/edit!]))

(def new-cursor
  (state/cursor [:db-view/input
                 :todo/new!]))

(def error-cursor
  (state/cursor [:db-view/output
                 :error]))

(defn select-filter!
  "Selects the filter (`:all`, `:active` or `:completed`) for the todo
   list items and refreshes the db-view."
  [filter-name]
  (swap! todo-list-params-cursor
         assoc
         :todo/filter filter-name)
  (db-view/refresh!))

(defn todo-stats []
  (let [selected (:todo/filter @todo-list-params-cursor
                               :all)
        props-for (fn [name]
                    {:class (when (= name selected)
                              "selected")
                     :on-click (fn [_e]
                                 (select-filter! name))})
        todo-list @todo-list-cursor
        active-count (:todo/active-count todo-list)]
    [:div
     [:span#todo-count
      [:strong active-count] " " (case active-count
                                   1
                                   "item"
                                   "items") " left"]
     [:ul#filters
      [:li [:a (props-for :all) "All"]]
      [:li [:a (props-for :active) "Active"]]
      [:li [:a (props-for :completed) "Completed"]]]
     (when (pos? active-count)
       [:button#clear-completed
        {:on-click (fn [_]
                     (command/send! (:todo/clear-completed! todo-list)))}
        "Clear completed " (:todo/completed-count todo-list)])]))

(defn edit-todo
  [{:keys [class cursor command-path]}]
  (let [save! (fn []
                (a/go
                  (if (:changed @cursor)
                    (when-not (:error
                               (a/<! (command/send! command-path)))
                      (reset! cursor
                              nil))
                    (reset! cursor
                            nil))))]
    (r/create-class
     {
      :component-did-mount #(.focus (r/dom-node %))
      :reagent-render
      (fn []
        [:input {:class class
                 :type "text"
                 :value (:todo/title @cursor)
                 :placeholder "What needs to be done?"
                 :on-blur (fn [_e]
                            (save!))
                 :on-change (fn [e]
                              (swap! cursor
                                     assoc
                                     :todo/title
                                     (.-value (.-target e))
                                     :changed
                                     true)
                              )
                 :on-key-down (fn [e]
                                (when (= (.-which e)
                                         13)
                                  (save!)))
                 }])})))

(defn todo-item
  [{:keys [db/id todo/done todo/title todo/done! todo/active! todo/delete!]}]
  (let [editing (= (:db/id @edit-cursor)
                   id)]
    [:li {:class (str (when done "completed ")
                      (when editing "editing"))}
     [:div.view
      [:input.toggle {:type "checkbox"
                      :checked done
                      :on-change (fn [_e]
                                   (if done!
                                     (command/send! done!)
                                     (command/send! active!)))}]
      [:label {:on-double-click (fn [_e]
                                  (reset! edit-cursor
                                          {:todo/title title
                                           :db/id id}))}
       title]

      [:button.destroy {:on-click
                        (fn []
                          (command/send! delete!))}]]
     (when editing
       [edit-todo {:class "edit"
                   :cursor edit-cursor
                   :command-path [:todo/edit!]}])]))

(defn todo-app []
  (let [todo-list @todo-list-cursor
        todo-items (:todo/list-items todo-list)]
    [:div
     [:div {:style {:display "flex"
                    :justify-content "center"
                    :color "red"
                    :height "20px"}}
      @error-cursor]
     [:section#todoapp
      [:header#header
       [:h1 "todos"]
       [edit-todo {:class "new-todo"
                   :cursor new-cursor
                   :command-path [:todo/new!]}]]
      (when (seq todo-items)
        [:<>
         [:section#main
          [:input#toggle-all
           (let [active-todo-items? (pos? (:todo/active-count todo-list))]
             {:type "checkbox"
              :checked active-todo-items?
              :on-change (fn [_]
                           (command/send!
                            (if active-todo-items?
                              (:todo/complete-all! todo-list)
                              (:todo/activate-all! todo-list))))})]
          [:label {:for "toggle-all"} "Mark all as complete"]
          [:ul#todo-list
           (for [todo todo-items]
             ^{:key (:db/id todo)} [todo-item todo])]]])
      [:footer#footer
       [todo-stats]]]
     [:footer#info
      [:p "Double-click to edit a todo"]]]))
