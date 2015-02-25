(ns cmr.umm.validation.parent-weaver
  "Provides functions to thread together a granule and collection parent objects for validation.
  It weaves together the objects so matching items within the granule and collection are combined"
  (:require [cmr.umm.collection :as c]
            [cmr.umm.granule :as g]
            [cmr.common.util :as u])
  (:import [cmr.umm.granule
            UmmGranule
            PlatformRef
            InstrumentRef
            SensorRef]))

(defprotocol ParentWeaver
  (set-parent [obj parent] "Sets the parent attribute on this object with the given parent"))


(defn- set-parents-by-name
  "Takes a list of child objects and a list of parent objects for the same field.  A parent
  object is matched to a child object by the field passed in as the name-field.  If a match is
  found for the child object in the parent object then the :parent field is set to that match."
  ([objs parent-objs]
   (set-parents-by-name objs parent-objs :name))
  ([objs parent-objs name-field]
   ;; We'll assume there's only a single parent object with a given name
   (let [parent-obj-by-name (u/map-values first (group-by name-field parent-objs))]
     (for [child objs
           :let [parent (parent-obj-by-name (name-field child))]]
       (set-parent child parent)))))

(extend-protocol
  ParentWeaver

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  UmmGranule
  (set-parent
    [granule coll]
    (-> granule
        (assoc :parent coll)
        (update-in [:spatial-coverage] set-parent (:spatial-coverage coll))
        (update-in [:platform-refs] set-parents-by-name (:platforms coll) :short-name)
        (update-in [:product-specific-attributes]
                   set-parents-by-name (:product-specific-attributes coll) :name)))

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  clojure.lang.IRecord
  ;; Default implementation of set-parent for records
  (set-parent
    [obj parent]
    (assoc obj :parent parent))

  PlatformRef
  (set-parent
    [platform-ref platform]
    (-> platform-ref
        (assoc :parent platform)
        (update-in [:instrument-refs] set-parents-by-name (:instruments platform) :short-name)))

  InstrumentRef
  (set-parent
    [instrument-ref instrument]
    (-> instrument-ref
        (assoc :parent instrument)
        (update-in [:sensor-refs] set-parents-by-name (:sensors instrument) :short-name)))

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; The protocol is extended to nil so we can attempt to set the parent on items which do not have
  ;; a parent
  nil
  (set-parent
    [_ _]
    nil)
  )