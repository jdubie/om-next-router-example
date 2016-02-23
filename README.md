# Om Next Router

A simple om next router that demonstrates client-side url-based routing, browser history.


## Getting started
```sh
git clone https://github.com/jdubie/om-next-router
make
# rlwrap lein run -m clojure.main script/figwheel.clj
# figwheel is listening on http://localhost:4952
```

## Routing approach

Uses the **total query** paradigm. This means parsing a subset of the component's 
total query. This subset is a function of what route is selected. One alternative to **total 
query** is to update the root component's query via `om/set-query!`. I haven't figured out a 
clean way to use `om/set-query!`.

Nice properties of om:

1. UI is a pure function of the global state atom
1. Components are dumb and don't have logic - just queries and simple transact calls

Let's flesh out of differences between these two approaches with an example and measure them 
against our "nice properties of om".

Let's say you have a button when a user clicks it you update the route based on some logic. 

### total routing
```clj
;; ui
(defui Component
  static om/IQuery
  (query [this]
    [:foo/bar])
  Object
  (render [this]
    (dom/button #js {:onClick (om/transact `[(button/clicked) :route])} "click me!"))

;; parsing
(defmethod mutate 'button/clicked
  [{:keys [state]} _ _]
  (if (valid? @state)
    {:action #(swap! state merge {:route/current :foo :something :else})}
    {:action #(swap! state merge {:route/current :bar :another :thing})}))
```

This is pretty clean. Not lets try using `om/set-query!`

## via `om/set-query!`
```clj
;; ui
(defui Component
  static om/IQuery
  (query [this]
    [:foo/bar])
  Object
  (render [this]
    (dom/button #js {:onClick #(do #(om/transact! this '[(button/clicked)])
                                   ; we don't have `state` here so need to include relevant 
                                   ; stuff in the component's query
                                   ;
                                   ; also we don't want to set query on `this` we probably want 
                                   ; set the route query on the route component so have to pass 
                                   ; callbacks all the way down to this component which is a pain
                                   (if (valid? state) 
                                     (om/set-query! this {:query {:route (route->query :foo)}}
                                     (om/set-query! this {:query {:route (route->query :bar)}}))}))

;; parsing
(defmethod mutate 'button/clicked
  [{:keys [state]} _ _]
  (if (valid? @state)
    {:action #(swap! state merge {:something :else})}
    {:action #(swap! state merge {:another :thing})}))

```

Using `om/set-query!` is bad in this example because we are moving logic into components. Also now
the UI has to be changed by calling both `om/transact!` and `om/set-query!`  instead of just 
`om/transact!`.

Would be curious if anyone has come up with a clean way to use `set-query!` to do routing.
