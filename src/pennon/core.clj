(ns pennon.core)

(def ^:dynamic *features* [])

(defn feature?
  "Check if a feature is currently switched on. Use this inside a Ring request
  using the wrap-feature-flags middleware."
  [kw]
  (some #{kw} *features*))

(defn enabled-feature?
  "Given a list of toggle functions (keyword -> true|false|nil), find if the
  feature is currently switched on or off. Toggles that return nil have no
  effect. The last toggle in the sequence to return true or false determines the
  state of the feature flags."
  [name toggles]
  (reduce (fn [prev-state toggle]
            (let [next-state (toggle name)]
              (if (nil? next-state)
                prev-state
                next-state)))
          nil
          toggles))

(defn enabled-features
  "Given a list of features and a list of toggles, find all the features that are currently switched on"
  [names toggles]
  (filterv #(enabled-feature? % toggles) names))

(defn wrap-feature-flags
  "Ring middleware wrapper. Give it a sequence of feature names (keywords) and a
  sequence of toggle-factories (request -> name -> true|false|nil). It will bind
  the *features* var so you can check for enabled flags with the `feature?'
  predicate."
  [handler names toggle-factories]
  (fn [req]
    (binding [*features* (enabled-features names (map #(% req) toggle-factories))]
      (handler req))))
