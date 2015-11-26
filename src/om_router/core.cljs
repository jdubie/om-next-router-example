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

(defn url->route [url]
  (:handler (match-route routes url)))

(defn parse-url [url]
  (match-route routes url))

(defn get-current-route
  []
  (url->route (.-pathname js/location)))

;;; parsing

(defmulti mutate om/dispatch)

(defmethod mutate 'route/update
  [{:keys [state]} _ {:keys [new-route]}]
  {:value {:keys [:route]}
   :action (fn [] (swap! state assoc :route new-route))})

(defmulti read om/dispatch)

(defn- get-route
  [state]
  (or (:route @state) (get-current-route)))

(defmethod read :route
  [{:keys [state]} _ _]
  {:value (get-route state)})

(defmethod read :page
  ; NOTE: selector is a union query over all routes. We filter out just the
  ; current route.
  [{:keys [query parser state] :as env} _ _]
  (let [_ (debug ":page query" query)]
    ;{:value (parser env (query (get-route state)))}))
    {:value (parser env query)}))

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

(defn menu-entry [{:keys [route selected?] :as props}]
  (let [{:keys [did-select]} (om/get-computed props)]
    (dom/li nil
      (dom/a (clj->js { ;:onClick did-select
                        :href (route->url route)
                        :style (merge {:cursor true}
                                  (if selected? {:color :blue}))})
        (name route)))))

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

(defui Root
  static om/IQueryParams
  (params [this]
    {:page (:index route->query)})
  static om/IQuery
  (query [this]
    '[:route {:page ?page}])
  Object
  (nav-handler [this match]
    (debug "Pushy caught a nav change" match)

    (debug "Set-Params!")
    (let [params {:page ((:handler match) route->query)}]
      (om/set-query! this {:params params}))

    (debug "om/transact! (route/update)")
    (om/transact! this `[(route/update ~match) :route]))
  (change-route [this handler args]
    (debug "CHANGE URL: " handler args)
    (let [path (:path args)]
      (debug "Pushy/Set-Token!" path)
      (pushy/set-token! history (or path "/unknown-route"))))

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
        (dom/ul nil
          (apply dom/ul nil
                 (map (fn [cur-route]
                        (menu-entry
                         (om/computed {:route cur-route :selected? (= cur-route route)}
                                      {:did-select
                                        (fn [handler args]
                                          (debug "DISPATCH CALLBACK: " handler args)
                                          (.change-route this handler args))})))


                                        ; (fn [] (om/transact!
                                        ;         this
                                        ;         `[(route/update {:new-route ~cur-route})
                                        ;           :route]))})))

                      entries)))
        ;; render the current page here
        ((route->factory route) page)))))

(def parser (om/parser {:read read :mutate mutate}))

(def reconciler
  (om/reconciler
    {:state (atom {:route :index})
     :parser parser}))

(om/add-root! reconciler Root (gdom/getElement "app"))
