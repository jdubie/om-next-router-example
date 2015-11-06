(ns om-router.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [bidi.bidi :refer [match-route]]
            [om.dom :as dom]))

(enable-console-print!)

;;; routing

(def routes ["/" {"" :index
                  "faq" :faq
                  "contact" :contact
                  "about" :about}])

(defn url->route
  [url]
  (:handler (match-route routes url)))

(defn get-current-route
  []
  (url->route (.-pathname js/location)))

;;; parsing

(defmulti mutate om/dispatch)

(defmethod mutate 'route/update
  [{:keys [state]} _ {:keys [new-route]}]
  {:value [:route]
   :action (fn [] (swap! state assoc :route new-route))})

(defmulti read om/dispatch)

(defmethod read :route
  [{:keys [state]} _ _]
  {:value (or (:route @state) (get-current-route))})

(defmethod read :page
  [{:keys [selector parser] :as env} _ _]
  {:value (parser env selector)})

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

(def home-page (om/factory HomePage))

(defui Contact
  static om/IQuery
  (query [this]
    [:page/contact])
  Object
  (render [this]
    (dom/div nil (:page/contact (om/props this)))))

(def contact (om/factory Contact))

(defui About
  static om/IQuery
  (query [this]
    [:page/about])
  Object
  (render [this]
    (dom/div nil (:page/about (om/props this)))))

(def about (om/factory About))

(defui Faq
  static om/IQuery
  (query [this]
    [:page/faq])
  Object
  (render [this]
    (dom/div nil (:page/faq (om/props this)))))

(def faq (om/factory Faq))

(defn menu-entry [{:keys [route selected?] :as props}]
  (let [{:keys [did-select]} (om/get-computed props)]
    (dom/li nil
      (dom/a (clj->js {:onClick did-select
                       :style (merge {:cursor true}
                                     (if selected? {:color :blue}))})
             (name route)))))

(defui Root
  static om/IQueryParams
  (params [this]
    {:page (om/subquery this :page HomePage)})
  static om/IQuery
  (query [this]
    '[:route {:page ?page}])
  Object
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
                                       (fn [] (om/transact!
                                                this
                                                `[(route/update {:new-route ~cur-route})
                                                  :route]))})))
                      entries)))
        ((case route
           :index home-page
           :faq faq
           :contact contact
           :about about)
         (merge page {:ref :page}))))))

(def parser (om/parser {:read read :mutate mutate}))

(def reconciler
  (om/reconciler
    {:state (atom {})
     :parser parser}))

(om/add-root! reconciler Root (gdom/getElement "app"))
