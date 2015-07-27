(ns cards.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [datascript :as d]
            [rum]
            [keybind :as k]
            [cognitect.transit :as t]
            [ajax.core :refer [POST]]))

(enable-console-print!)

(defonce conn (d/create-conn {
                              :app/card {
                                           :db/cardinality :db.cardinality/many }
                          }))

(defn save-state [] (ajax.core/POST "/save" {:format :edn :params @conn}))

(defn handler [v] (reset! conn (cljs.reader/read-string v)))
(defn load-state [] (ajax.core/GET "/load"
                                   {:handler handler
                                    :error-handler (fn []
                                                     (d/transact! conn [{:db/id 1 :app/overlay false}])
                                                     (d/transact! conn [{:db/id 2 :app/input "!!!"}])
                                                     (d/transact! conn [[:db/add 3 :app/card "First"]])
                                                     (d/transact! conn [[:db/add 3 :app/card "Second"]]))}))

(defn node [x y title]
  [:div
   {:key title :style {:left x :top y :width 100 :height 60 :position "absolute"}}
   [:div
    {:style
     {
      :background-color "#AAA"
      :width 100
      :height 60
      :position "absolute"
      :margin "auto"
      :top 0 :left 0 :bottom 0 :right 0}}
    [:div
     {:style
      {
       :width "100%"
       :margin "auto"
       :position "absolute" :top 0 :left 0 :bottom 0 :right 0
       :text-align "center"
       :display "table"}}
     title]]])

(defn card [title]
  [:li.card {:key title
             :backgound-color "#AAA"}
   [:span title]
   ])

(defn get-nodes [conn]
  (vec (d/q '[:find ?x ?y ?title
                               :where
                               [?n :node/name ?title]
                               [?n :x ?x]
                               [?n :y ?y]
                               ]
            conn)))

(defn get-cards [conn]
  (map first (vec (d/q '[:find ?title
               :where
               [?n :app/card ?title]
               ]
             conn))))

(defn get-overlay [conn]
  (first (flatten (vec (d/q '[:find ?mode :where [_ :app/overlay ?mode]] conn)))))

(defn get-current-input [conn]
  (first (flatten (vec (d/q '[:find ?i :where [_ :app/input ?i]] conn)))))

(defn update-input [v]
  (d/transact! conn [[:db/add 2 :app/input v]]))

(defn add-card []
  (let [v (get-current-input @conn)]
    (d/transact! conn [[:db/add 3 :app/card v]])
    (d/transact! conn [[:db/add 1 :app/overlay false]])
    (update-input "")
    )
  )

(def auto-focus {
  :did-mount (fn [state]
                 (.focus (.getDOMNode (:rum/react-component state))))
  })

(rum/defcs f-input < auto-focus [conn]
  [:input {:type "text"
           :class "dump"
           :style {:top "50%"
                   :left "50%"
                   :-webkit-transform "translate(-50%,-50%)"
                   :position "absolute"}
           :value (get-current-input conn)
           :on-change #(update-input (.. % -target -value))
           }])

(rum/defc card-input < rum/static [conn]
  [:form
   {:on-submit (fn [e] (.preventDefault e)(add-card))}
  (rum/with-props f-input conn :rum/ref "input")
   ])

(defn cards-grid [conn] [:ul.grid (map card (get-cards conn))])

(rum/defc app < rum/static [conn]
  (if (get-overlay conn)
    (card-input conn)
    (cards-grid conn)))

(defn nodes-view [conn] [:div (map #(apply node %) (get-nodes conn))])

(add-watch conn :render
    (fn [_ _ _ conn]
      (rum/mount (app conn) (js/document.getElementById "app"))))


(k/bind! "space" ::my-trigger
         #(d/transact! conn [[:db/add 1 :app/overlay (not (get-overlay @conn))]])
         )

(defn main []
  (load-state))
