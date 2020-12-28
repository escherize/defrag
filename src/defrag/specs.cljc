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

(s/fdef clojure.core/defn
  :args ::defn-args
  :ret any?)

(s/fdef clojure.core/defn-
  :args ::defn-args
  :ret any?)

(s/fdef clojure.core/fn
  :args (s/cat :fn-name (s/? simple-symbol?)
               :fn-tail (s/alt :arity-1 ::params+body
                               :arity-n (s/+ (s/spec ::params+body))))
  :ret any?)

;;;; ns

(s/def ::exclude (s/coll-of simple-symbol?))
(s/def ::only (s/coll-of simple-symbol?))
(s/def ::rename (s/map-of simple-symbol? simple-symbol?))
(s/def ::filters (s/keys* :opt-un [::exclude ::only ::rename]))

(s/def ::ns-refer-clojure
  (s/spec (s/cat :clause #{:refer-clojure}
                 :refer-filters ::filters)))

(s/def ::refer (s/or :all #{:all}
                     :syms (s/coll-of simple-symbol?)))

(s/def ::prefix-list
  (s/spec
   (s/cat :prefix simple-symbol?
          :libspecs (s/+ ::libspec))))

(s/def ::libspec
  (s/alt :lib simple-symbol?
         :lib+opts (s/spec (s/cat :lib simple-symbol?
                                  :options (s/keys* :opt-un [::as ::refer])))))

(s/def ::ns-require
  (s/spec (s/cat :clause #{:require}
                 :body (s/+ (s/alt :libspec ::libspec
                                   :prefix-list ::prefix-list
                                   :flag #{:reload :reload-all :verbose})))))

(s/def ::package-list
  (s/spec
   (s/cat :package simple-symbol?
          :classes (s/+ simple-symbol?))))

(s/def ::import-list
  (s/* (s/alt :class simple-symbol?
              :package-list ::package-list)))

(s/def ::ns-import
  (s/spec
   (s/cat :clause #{:import}
          :classes ::import-list)))

(s/def ::ns-refer
  (s/spec (s/cat :clause #{:refer}
                 :lib simple-symbol?
                 :refer-filters ::filters)))

;; same as ::prefix-list, but with ::use-libspec instead
(s/def ::use-prefix-list
  (s/spec
   (s/cat :prefix simple-symbol?
          :libspecs (s/+ ::use-libspec))))

;; same as ::libspec, but also supports the ::filters options in the libspec
(s/def ::use-libspec
  (s/alt :lib simple-symbol?
         :lib+opts (s/spec (s/cat :lib simple-symbol?
                                  :options (s/keys* :opt-un [::as ::refer ::exclude ::only ::rename])))))

(s/def ::ns-use
  (s/spec (s/cat :clause #{:use}
                 :libs (s/+ (s/alt :libspec ::use-libspec
                                   :prefix-list ::use-prefix-list
                                   :flag #{:reload :reload-all :verbose})))))

(s/def ::ns-load
  (s/spec (s/cat :clause #{:load}
                 :libs (s/* string?))))

(s/def ::name simple-symbol?)
(s/def ::extends simple-symbol?)
(s/def ::implements (s/coll-of simple-symbol? :kind vector?))
(s/def ::init symbol?)
(s/def ::class-ident (s/or :class simple-symbol? :class-name string?))
(s/def ::signature (s/coll-of ::class-ident :kind vector?))
(s/def ::constructors (s/map-of ::signature ::signature))
(s/def ::post-init symbol?)
(s/def ::method (s/and vector?
                       (s/cat :method-name simple-symbol?
                              :param-types ::signature
                              :return-type ::class-ident)))
(s/def ::methods (s/coll-of ::method :kind vector?))
(s/def ::main boolean?)
(s/def ::factory simple-symbol?)
(s/def ::state simple-symbol?)
(s/def ::get simple-symbol?)
(s/def ::set simple-symbol?)
(s/def ::expose (s/keys :opt-un [::get ::set]))
(s/def ::exposes (s/map-of simple-symbol? ::expose))
(s/def ::prefix string?)
(s/def ::impl-ns simple-symbol?)
(s/def ::load-impl-ns boolean?)

(s/def ::ns-gen-class
  (s/spec (s/cat :clause #{:gen-class}
                 :options (s/keys* :opt-un [::name ::extends ::implements
                                            ::init ::constructors ::post-init
                                            ::methods ::main ::factory ::state
                                            ::exposes ::prefix ::impl-ns ::load-impl-ns]))))

(s/def ::ns-clauses
  (s/* (s/alt :refer-clojure ::ns-refer-clojure
              :require ::ns-require
              :import ::ns-import
              :use ::ns-use
              :refer ::ns-refer
              :load ::ns-load
              :gen-class ::ns-gen-class)))

(s/def ::ns-form
  (s/cat :ns-name simple-symbol?
         :docstring (s/? string?)
         :attr-map (s/? map?)
         :ns-clauses ::ns-clauses))

(s/fdef clojure.core/ns
  :args ::ns-form)

(defmacro ^:private quotable
  "Returns a spec that accepts both the spec and a (quote ...) form of the spec"
  [spec]
  `(s/or :spec ~spec :quoted-spec (s/cat :quote #{'quote} :spec ~spec)))

(s/def ::quotable-import-list
  (s/* (s/alt :class (quotable simple-symbol?)
              :package-list (quotable ::package-list))))

(s/fdef clojure.core/import
  :args ::quotable-import-list)





