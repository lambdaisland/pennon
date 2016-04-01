(ns pennon.toggles
  "Feature flags are controlled by toggles. A toggle is a function that takes
  the name of a feature flag (a keyword) and either returns true (toggle the
  feature on), false (toggle the feature off), or nil (leave it as it is, don't
  care).

  When setting up Pennon you give it a list of toggles to check. These could
  turn features on or off based on values in the database, request parameters,
  the current user, etc. Toggles are tried in the order they are given, and the
  last one to return true or false 'wins'.
  "
  (:require [clojure.string :as s]
            [ring.util
             [codec :as codec]
             [request :as req]]))

(defn- query-params
  "Gets the query parameters out of Ring request as a map with String keys and
  values."
  [req]
  (let [query-string (or (:query-string req) "")
        encoding (req/character-encoding req)
        params (codec/form-decode query-string (or encoding "UTF-8"))]
    (if (map? params) params {})))

(defn query-params-toggle-factory
  "Given a Ring request map, returns a toggle that switches feature flags on or
  off based on the 'flag_on' and 'flag_off' query paramters. Flag names are
  comma separated."
  [req]
  (fn [feature]
    (if-let [params (query-params req)]
      (let [on (s/split (get params "flag_on" "") #",")
            off (s/split (get params "flag_off" "") #",")]
        (cond
          (some #{(name feature)} on) true
          (some #{(name feature)} off) false
          :else nil)))))
