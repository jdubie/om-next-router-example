(ns om-router.core
  (:require
    [goog.dom :as gdom]
    [om.next :as om :refer-macros [defui]]
    [bidi.bidi :as bidi :refer [match-route]]
    [goog.string :as string]
    [goog.string.format]
    [om.dom :as dom]
    [pushy.core :as pushy])
  (:require-macros
    [cljs-log.core :refer [debug info warn severe]]))

(enable-console-print!)

;; ============================================================================
;; Routing

(def routes ["/" {"" :index
                  "faq" :faq
                  "about" :about}])

(defn route->url
  [route]
  (bidi/path-for routes route))

(defn url->route [url]
  (:handler (match-route routes url)))

;; ============================================================================
;; Parsing

(defmulti mutate om/dispatch)

(defmethod mutate 'route/update
  [{:keys [state]} _ {:keys [value]}]
  {:value {:keys [:current-route]}
   :action #(swap! state assoc :current-route value)})

(defmulti read om/dispatch)

(defmethod read :route
  [{:keys [state query parser] :as env} _ _]
  (let [{:keys [current-route]} @state]
    {:value (parser (dissoc env :query)
                    (get query current-route))}))

(defmethod read :current-route
  [{:keys [state]} _ _]
  {:value (get @state :current-route)})

(defmethod read :default
  [_ k _]
  {:value (string/format "Reading: %s" (name k))})

;; ============================================================================
;; Pages

(defn render-page
  [this]
  (dom/div nil (pr-str (om/props this))))

(defui HomePage
  static om/IQuery
  (query [this]
    [:page/home])
  Object
  (render [this] (render-page this)))

(defui About
  static om/IQuery
  (query [this]
    [:page/about])
  Object
  (render [this] (render-page this)))

(defui Faq
  static om/IQuery
  (query [this]
    [:page/faq])
  Object
  (render [this] (render-page this)))

;; ============================================================================
;; Menu

(defn menu-entry
  [{:keys [route selected?]}]
  (dom/li nil
    (dom/a (clj->js {:href (route->url route)})
      (if selected? (str (name route) " (selected)") (name route)))))

;; ============================================================================
;; Mapping from route to components, queries, and factories

(def route->component
  {:index HomePage
   :faq Faq
   :about About})

(def route->factory
  (zipmap (keys route->component)
          (map om/factory (vals route->component))))

(def route->query
  (zipmap (keys route->component)
          (map om/get-query (vals route->component))))

(declare history)

(defn update-route
  [root route]
  (debug "update-route" (name route))
  (om/transact! root `[(route/update {:value route}) :route]))

(defui Root
  static om/IQuery
  (query [_]
    "This is called the \"Total Query\" approach because `route->query` is the complete
    query of the app."
    [:current-route {:route route->query}])
  Object
  (componentDidUpdate [this _ _]
    "Sync state -> url"
    (let [{:keys [current-route]} (om/props this)]
      (when (not= current-route (url->route js/location.pathname))
        (pushy/set-token! history (route->url current-route)))))
  (componentDidMount [this]
    "Sync url -> state"
    (let [listener (pushy/pushy #(om/transact! this `[(route/update {:value ~%}) :route])
                                url->route)]
      (pushy/start! listener)
      (set! history listener)))
  (componentWillUnmount [_]
    (pushy/stop! history))
  (render [this]
    (let [{:keys [current-route route]} (om/props this)
          factory (route->factory current-route)]
      (dom/div nil
        (dom/div nil "navigate via links:")
        (apply dom/div nil (map #(menu-entry {:route % :selected? (= % current-route)})
                                (keys route->component)))
        (dom/div nil "navigate via transactions:")
        (dom/button #js {:onClick #(om/transact! this '[(route/update {:value :about}) :route])}
                    "Goto about page")
        (dom/div nil "current page:")
        (factory route)))))

(def reconciler
  (om/reconciler
    {:state (atom {:current-route (url->route js/location.pathname)})
     :parser (om/parser {:read read :mutate mutate})}))

(om/add-root! reconciler Root (gdom/getElement "app"))