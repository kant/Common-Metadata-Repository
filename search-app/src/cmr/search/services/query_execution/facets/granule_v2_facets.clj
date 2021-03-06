(ns cmr.search.services.query-execution.facets.granule-v2-facets
  "Functions for generating v2 granule facets. Similar structure as v2 collection facets, but
  granule fields. First major use case is supporting OPeNDAP virutal directories capability."
  (:require
   [cmr.common-app.services.search.query-to-elastic :as q2e]
   [cmr.common.util :as util]
   [cmr.search.services.query-execution.facets.facets-v2-helper :as v2h]
   [cmr.search.services.query-execution.facets.facets-v2-results-feature :as v2-facets]
   [cmr.search.services.query-execution.facets.hierarchical-v2-facets :as hv2]
   [cmr.search.services.query-execution.facets.temporal-facets :as temporal-facets]))

(def granule-facet-params->elastic-fields
  "Maps the parameter names for the concept-type to the fields in Elasticsearch."
  {:start-date :start-date-doc-values})

(defmethod v2-facets/facets-v2-params->elastic-fields :granule
  [_]
  granule-facet-params->elastic-fields)

(def granule-facet-params
  "Granule facet params."
  (keys granule-facet-params->elastic-fields))

(def granule-facet-params-with-default-size
  "A map with collection facets parameters and the default term size values."
  (zipmap granule-facet-params
          (repeat (count granule-facet-params) v2-facets/DEFAULT_TERMS_SIZE)))

(defmethod v2-facets/facets-v2-params :granule
  [_]
  granule-facet-params)

(defmethod v2-facets/facets-v2-params-with-default-size :granule
  [_]
  granule-facet-params-with-default-size)

(def granule-fields->aggregation-fields
  "Defines the mapping of granule parameter names to the aggregation parameter names."
  (into {}
        (map (fn [field]
               [field (q2e/query-field->elastic-field field :granule)])
             granule-facet-params)))

(defmethod v2-facets/facet-fields->aggregation-fields :granule
  [_]
  granule-fields->aggregation-fields)

(defmethod v2-facets/v2-facets-root :granule
  [_]
  {:title "Browse Granules"
   :type :group})

(defmethod v2-facets/v2-facets-result-field-in-order :granule
  [_]
  ["Temporal"])

(defn single-collection-validation
  "Validates that the provided query is limited to a single collection. We do this to prevent
  expensive aggregation queries that would have to run against more than one granule index."
  [context]
  (let [collection-ids (:query-collection-ids context)
        collection-count (count collection-ids)]
    (when-not (= 1 collection-count)
      [(format "Granule V2 facets are limited to a single collection, but query matched %s collections."
               (if (= 0 collection-count) "an undetermined number of" collection-count))])))

(def validations
  "Validation functions to run for v2 granule facets."
  (util/compose-validations [single-collection-validation]))

(defmethod v2-facets/facets-validator :granule
  [_]
  (util/build-validator :bad-request validations))

(def group-nodes-in-order
  "The titles of temporal facet group nodes in order."
  ["Year" "Month" "Day"])

(defn add-group-nodes-to-facets
  "Adds group nodes (Year, Month, Day) as applicable to the provided facets."
  [facets remaining-levels]
  (let [has-children (not= remaining-levels ["Day"])
        applied? (some? (some true? (map :applied facets)))
        children-facets (for [facet facets]
                          (if (seq (:children facet))
                            (assoc facet :children [(add-group-nodes-to-facets
                                                     (:children facet) (rest remaining-levels))])
                            (assoc facet :has_children has-children)))]
    (v2h/generate-group-node (first remaining-levels) applied? children-facets)))

(defmethod v2-facets/create-v2-facets-by-concept-type :granule
  [concept-type base-url query-params aggs _]
  (let [subfacets (hv2/hierarchical-bucket-map->facets-v2
                   :temporal-facet
                   (:start-date-doc-values aggs)
                   base-url
                   query-params)]
    (when (seq subfacets)
      (let [field-reg-ex (re-pattern "temporal_facet.*")
            applied? (->> query-params
                          (filter (fn [[k v]] (re-matches field-reg-ex k)))
                          seq
                          some?)
            updated-subfacets (add-group-nodes-to-facets (:children subfacets)
                                                         group-nodes-in-order)]
        [(merge v2h/sorted-facet-map
                (v2h/generate-group-node "Temporal" applied?
                                         [updated-subfacets]))]))))
