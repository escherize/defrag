(ns defrag.specs
  (:require [clojure.spec.alpha :as s]))

;;; This ns coppied from clojure.core.specs.alpha 0.2.44, with slight modifications

;;;; destructure

(s/def ::local-name (s/and simple-symbol? #(not= '& %)))

(s/def ::binding-form
  (s/or :local-symbol ::local-name
        :seq-destructure ::seq-binding-form
        :map-destructure ::map-binding-form))

;; sequential destructuring

(s/def ::seq-binding-form
  (s/and vector?
         (s/conformer identity vec)
         (s/cat :elems (s/* ::binding-form)
                :rest (s/? (s/cat :amp #{'&} :form ::binding-form))
                :as (s/? (s/cat :as #{:as} :sym ::local-name)))))

;; map destructuring

(s/def ::keys (s/coll-of ident? :kind vector?))
(s/def ::syms (s/coll-of symbol? :kind vector?))
(s/def ::strs (s/coll-of simple-symbol? :kind vector?))
(s/def ::or (s/map-of simple-symbol? any?))
(s/def ::as ::local-name)

(s/def ::map-special-binding
  (s/keys :opt-un [::as ::or ::keys ::syms ::strs]))

(s/def ::map-binding (s/tuple ::binding-form any?))

(s/def ::ns-keys
  (s/tuple
   (s/and qualified-keyword? #(-> % name #{"keys" "syms"}))
   (s/coll-of simple-symbol? :kind vector?)))

(s/def ::map-bindings
  (s/every (s/or :map-binding ::map-binding
                 :qualified-keys-or-syms ::ns-keys
                 :special-binding (s/tuple #{:as :or :keys :syms :strs} any?)) :kind map?))

(s/def ::map-binding-form (s/merge ::map-bindings ::map-special-binding))

;; bindings

(defn even-number-of-forms?
  "Returns true if there are an even number of forms in a binding vector"
  [forms]
  (even? (count forms)))

(s/def ::binding (s/cat :form ::binding-form :init-expr any?))
(s/def ::bindings (s/and vector? even-number-of-forms? (s/* ::binding)))

;; defn, defn-, fn

(defn arg-list-unformer [a]
  (vec
   (if (and (coll? (last a)) (= '& (first (last a))))
     (concat (drop-last a) (last a))
     a)))

(s/def ::param-list
  (s/and
   vector?
   (s/conformer identity arg-list-unformer)
   (s/cat :args (s/* ::binding-form)
          :varargs (s/? (s/cat :amp #{'&} :form ::binding-form)))))

(s/def ::params+body
  (s/cat :params ::param-list
         :body (s/alt :prepost+body (s/cat :prepost map?
                                           :body (s/+ any?))
                      :body (s/* any?))))

(s/def ::defn-args
  (s/cat :fn-name simple-symbol?
         :docstring (s/? string?)
         :meta (s/? map?)
         :fn-tail (s/alt :arity-1 ::params+body
                         :arity-n (s/cat :bodies (s/+ (s/spec ::params+body))
                                         :attr-map (s/? map?)))))

;; (s/fdef clojure.core/defn
;;   :args ::defn-args
;;   :ret any?)

;; (s/fdef clojure.core/defn-
;;   :args ::defn-args
;;   :ret any?)

;; (s/fdef clojure.core/fn
;;   :args (s/cat :fn-name (s/? simple-symbol?)
;;                :fn-tail (s/alt :arity-1 ::params+body
;;                                :arity-n (s/+ (s/spec ::params+body))))
;;   :ret any?)

