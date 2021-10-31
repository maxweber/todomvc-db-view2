(ns todomvc-db-view.core
  (:require [reagent.core :as r]
            [todomvc-db-view.state.core :as state]
            [todomvc-db-view.db-view.get :as db-view]
            [todomvc-db-view.command.send :as command]
            [clojure.string :as str]
            [cljs.core.async :as a]
            )
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn add-todo
  [title]
  (swap! state/state
         assoc-in
         [:db-view/input
          :todo/new]
         {:todo/title title})
  (go
    (a/<! (db-view/refresh!))
    (a/<! (command/send! (get-in @state/state
                                 [:db-view/output
                                  :todo/new
                                  :todo/new!])))))

(defn todo-input [{:keys [title on-save on-stop]}]
  (let [val (r/atom title)
        stop #(do (reset! val "")
                  (when on-stop
                    (on-stop)))
        save #(let [v (-> @val str str/trim)]
                (if-not (empty? v) (on-save v))
                (stop))]
    (fn [{:keys [id class placeholder]}]
      [:input {:type "text" :value @val
               :id id :class class :placeholder placeholder
               :on-blur save
               :on-change #(reset! val (-> % .-target .-value))
               :on-key-down #(case (.-which %)
                               13 (save)
                               27 (stop)
                               nil)}])))

(def todo-list-cursor
  (state/cursor [:db-view/output
                 :todo/list]))

(def todo-list-params-cursor
  (state/cursor [:db-view/input
                 :todo/list]))

(def todo-edit (with-meta todo-input
                 {:component-did-mount #(.focus (r/dom-node %))}))

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

(defn todo-item []
  (let [editing (r/atom false)]
    (fn [{:keys [db/id todo/done todo/title todo/done! todo/active! todo/delete!]}]
      [:li {:class (str (when done "completed ")
                        (when @editing "editing"))}
       [:div.view
        [:input.toggle {:type "checkbox"
                        :checked done
                        :on-change (fn [_e]
                                     (if done!
                                       (command/send! done!)
                                       (command/send! active!)))}]
        [:label {:on-double-click #(reset! editing true)} title]

        [:button.destroy {:on-click
                          (fn []
                            (command/send! delete!))}]]
       (when @editing
         [todo-edit {:class "edit" :title title
                     :on-save (fn [new-title]
                                (swap! state/state
                                       assoc-in
                                       [:db-view/input
                                        :todo/edit]
                                       {:todo/title new-title
                                        :db/id id})
                                (go
                                  (a/<! (db-view/refresh!))
                                  (if-let [error (get-in @state/state [:db-view/output
                                                                       :todo/edit
                                                                       :error])]
                                    (js/alert error)
                                    (a/<! (command/send! (get-in @state/state
                                                                 [:db-view/output
                                                                  :todo/edit
                                                                  :todo/edit!]))))))
                     :on-stop #(reset! editing false)}])])))

(defn todo-app []
  (let [todo-list @todo-list-cursor
        todo-items (:todo/list-items todo-list)]
    [:div
     [:section#todoapp
      [:header#header
       [:h1 "todos"]
       [todo-input {:id "new-todo"
                    :placeholder "What needs to be done?"
                    :on-save add-todo}]]
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
