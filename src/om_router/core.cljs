(ns om-router.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [bidi.bidi :as bidi :refer [match-route]]
            [om.dom :as dom]
            [pushy.core :as pushy])
  (:require-macros
            [cljs-log.core :as l :refer [debug info warn severe]]))

(enable-console-print!)

;;; routing

(def routes ["/" {"" :index
                  "faq" :faq
                  "contact" :contact
                  "about" :about}])

(def route->url (partial bidi/path-for routes))

(defn parse-url [url]
  (match-route routes url))

;;; parsing

(defmulti mutate om/dispatch)

(defmethod mutate 'route/update
  [{:keys [state]} _ {:keys [new-route]}]
  {:value {:keys [:route]}
   :action (fn [] (swap! state assoc :route new-route))})

(defmulti read om/dispatch)

(defmethod read :route
  [{:keys [state]} _ _]
  {:value (get @state :route)})

(defmethod read :page
  [{:keys [query parser state] :as env} _ _]
  (debug ":page query" query)
  {:value (parser env query)})

(defmethod read :page/home
  [_ _ _]
  (println "reading: home")
  {:value "home"})

(defmethod read :page/contact
  [_ _ _]
  (println "reading: contact")
  {:value "contact"})

(defmethod read :page/faq
  [_ _ _]
  (println "reading: faq")
  {:value "faq"})

(defmethod read :page/about
  [_ _ _]
  (println "reading: about")
  {:value "about"})

;;; components

(defui HomePage
  static om/IQuery
  (query [this]
    [:page/home])
  Object
  (render [this]
    (dom/div nil (:page/home (om/props this)))))

(defui Contact
  static om/IQuery
  (query [this]
    [:page/contact])
  Object
  (render [this]
    (dom/div nil (:page/contact (om/props this)))))

(defui About
  static om/IQuery
  (query [this]
    [:page/about])
  Object
  (render [this]
    (dom/div nil (:page/about (om/props this)))))

(defui Faq
  static om/IQuery
  (query [this]
    [:page/faq])
  Object
  (render [this]
    (dom/div nil (:page/faq (om/props this)))))

(defn menu-entry
  [{:keys [route selected?] :as props}]
  (dom/li nil
    (dom/a (clj->js {:href (route->url route)})
      (if selected? (str (name route) " (selected)") (name route)))))

;;; Mapping from route to components, queries, and factories

(def route->component
  {:index HomePage
   :faq Faq
   :contact Contact
   :about About})

(def route->factory
  (zipmap (keys route->component)
          (map om/factory (vals route->component))))

(def route->query
  (zipmap (keys route->component)
          (map om/get-query (vals route->component))))

(declare history)

(defn update-query-from-route!
  [this route]
  (debug "Set-Params!")
  (om/set-query! this {:params {:page (get route->query route)}}))

(defui Root
  static om/IQueryParams
  (params [this]
    {:page (:index route->query)})
  static om/IQuery
  (query [this]
    '[:route {:page ?page}])

  Object
  (nav-handler
    [this match]
    "Sync: Browser -> OM"
    (debug "Pushy caught a nav change" match)
    (let [{route :handler} match]
      (update-query-from-route! this route)
      (om/transact! this `[(route/update {:new-route ~route})
                           :route])))

  (componentWillReceiveProps
    [this props]
    "Sync: OM -> Browser"
    (let [[old-route new-route] [(:route (om/props this)) (:route props)]]
      (debug "componentWillReceiveProps" old-route new-route)
      (when (not= new-route old-route)
        (update-query-from-route! this new-route)
        (pushy/set-token! history (or (route->url new-route) "/unknown-route")))))

  (componentDidMount [this]
    (debug "App mounted")
    (let [nav-fn #(.nav-handler this %)
          pushy (pushy/pushy nav-fn parse-url)]
      (debug "HISTORY/get-token" (pushy/get-token pushy))
      (pushy/start! pushy)
      (set! history pushy)))

  (componentWillUnmount [this]
    (debug "App unmounting")
    (pushy/stop! history))

  (render [this]
    (let [{:keys [route page]} (om/props this)
          entries (vals (second routes))]

      (dom/div nil
        ;; routing via pushy
        (dom/div nil
          (dom/p nil "change route via pushy and anchor tags")
          (apply dom/ul nil
                 (map (fn [cur-route]
                        (menu-entry {:route cur-route :selected? (= cur-route route)}))
                      entries)))

        ;; routing in a transaction
        (dom/div nil
          (dom/p nil "change route via a transaction")
          (dom/button #js {:onClick #(om/transact! this '[(route/update {:new-route :about})
                                                         :route])}
                      "goto about"))

        ;; render the current page here
        (dom/div nil
          (dom/p nil "current page:")
          ((route->factory route) page))))))

(def parser (om/parser {:read read :mutate mutate}))

(def reconciler
  (om/reconciler
    {:state (atom {:route :index})
     :parser parser}))

(om/add-root! reconciler Root (gdom/getElement "app"))
