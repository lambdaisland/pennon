# Pennon

A feature flag library for Clojure.

Pennon is primarily intended for use with Ring, but can also be used elsewhere.
It is based on a few simple concepts, making it highly composable and adaptable.

## Usage

Feature flags in Pennon are identified with keywords.

```clojure
(def features #{:link-sharing :new-layout :reporting})
```

In your code you can branch based on whether features are enabled or disabled:

```clojure
(if (feature? :new-layout)
  (new-layout)
  (old-layout))
```

This assumes you are using the Ring middleware and are inside a request handler.

Feature flags are enabled or disabled by "toggles". A toggle is a function that
takes the name of a feature flag, and returns true, false, or nil.

Multiple toggles can be active at once. When a toggle returns true or false it
switches the feature flag on or off. If it returns nil it leaves the feature
flag in whatever state it was left in by previous toggles.

For example, we could have three different toggles configured. One checks if a
feature has been turned on or off globally, one checks if the current user has
the feature enabled, and finally the last one checks for a query parameter that
temporarily enables the features.

The `:link-sharing` feature is still experimental, but the QA people are trying
it out by adding a query parameter to the URI.

The `:reporting` feature is mostly ready, and so we have given a few customers
early access in exchange for feedback.

The `:new-layout` has been rolled out, but it has made a high profile customer
unhappy, so it's been disabled for this customer while we make some fixes.

<table>
  <tr>
    <th></th>
    <th>:link-sharing</th>
    <th>:reporting</th>
    <th>:new-layout</th>
  </tr>
  <tr>
    <th>global</th>
    <td>nil</td>
    <td>nil</td>
    <td>true</td>
  </tr>
  <tr>
    <th>user</th>
    <td>nil</td>
    <td>true</td>
    <td>false</td>
  </tr>
  <tr>
    <th>query-param</th>
    <td>true</td>
    <td>nil</td>
    <td>nil</td>
  </tr>
  <tr>
    <th>(feature? x)</th>
    <td><strong>true</strong></td>
    <td><strong>true</strong></td>
    <td><strong>false</strong></td>
  </tr>
</table>

To use Pennon with Ring, use `pennon.core/wrap-feature-flags`, passing the Ring
handler, the available feature flags, and the toggle factories to use.

A toggle factory is a function that takes a Ring request map, and returns a
toggle, for example:

```clojure
(defn my-toggle-factory [req]
  (fn [flag]
    (= flag :link-sharing)))
```

This factory is called once per request, the inner function is then called once
for each feature flag. This allows you to toggle based on the current request or
session, and it gives you an opportunity to do one time setup like accessing a
database.

A complete example:

```clojure
(ns my-app.core
  (:require [pennon.core :refer [wrap-feature-flags]]
            [pennon.toggles :refer [query-params-toggle-factory])
            [compojure.core :refer [defroutes GET]]))

(def features #{:link-sharing :reporting :new-layout})
(def default-enabled-flags #{:new-layout})

(defn db-toggle-factory [_]
  (let [enabled-features (db-fetch-enabled-features)]
    (fn [flag]
      (some #{flag} enabled-features))))

(defn user-toggle-factory [req]
  (let [enabled-features (fetch-user-features (-> req :session ::identity)]
    (fn [flag]
      (some #{flag} enabled-features))))

(def feature-toggles
  [(constantly default-enabled-flags) ;; use of a set as a hard-coded toggle function
   db-toggle-factory
   user-toggle-factory
   query-params-toggle-factory ;; provided by pennon.toggles, checks for flag_on=.. and flag_off=... request params
   ])

(defroutes routes
  (GET "/" [] (if (feature? :link-sharing) ,,, ) ))

(def http-handler
  (-> routes
      (wrap-feature-flags features feature-toggles)))
```

## Use outside Ring

The `feature?` function checks `pennon.core/*features*`. The Ring middleware
takes care of binding this. If you are using Pennon elsewhere you have to make
sure that somewhere up the stack you wrap your code in a `binding` form.


```clojure
(binding [pennon.core/*features* (pennon.core/enabled-features feature-names toggle-fns)]
  ,,,
  )
```

## License

All code and content Copyright © 2016 Arne Brasseur

Distributed under the [Mozilla Public License 2.0](https://www.mozilla.org/en-US/MPL/2.0/), see LICENSE.

[tl;dr](https://tldrlegal.com/license/mozilla-public-license-2.0-%28mpl-2%29)

**This is only a short summary of the Full Text, this information is not legal advice.**

MPL is a copyleft license that works on individual files. Any changes you make to files covered by MPL must be made available under MPL, but you may combine these with non-MPL (proprietary) source files in the same project. Version 2.0 is compatible with GPL version 3. You can distribute binaries under a proprietary license, as long as you make the source available under MPL.
