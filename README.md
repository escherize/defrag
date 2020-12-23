# defrag

A Clojure library designed to make wrapping defn forms possible for mortals.

## Usage

Instead of having to struggle with wrapping defn forms which deal multi-arities, docstrings, privacy metadata, and more, this library enables us all to wrap defn how we see fit.

You the user are responsible for writing a function that returns source code, which is called on a function's name args and body.

### Print Example (pdefn)

Here's an example of such a function:

``` clojure
(defn wrap-print
  "Example of using a function to wrap various defn forms, which has
  access to argument forms."
  [name args body]
  `((println (pr-str {:name ~name :args ~args :body '~body})) ~@body))
  ```

Calling `defrag!` with a symbol and that function will tie them together

``` clojure
(defrag! pdefn wrap-print)
```

Now you can start defining functions with `pdefn`!

``` clojure
  (pdefn ^:private f
         ([] 1)
         ([x] (+ 2 x))
         ([x & ys] (first ys)))
```

Calling it produces:

``` clojure
  (f)
  ;; prints: {:args [], :name "f", :body [1]}
  ;;=> 1

  (f 2)
  ;; prints: {:args [2], :name "f", :body [(+ 2 x)]}
  ;;=>2

  (f 3 "hi" 5)
  ;; prints: {:args [3 ("hi" 5)], :name "f", :body [(first ys)]}
  ;;=> "hi"

  ;; notice our function is private and has the usual defn metadata:

  (:private (meta #'f))
  ;;=> true

  (:arglists (meta #'f))
  ;;=> ([] [x] [x & ys])

  ```

### Prior Art

This library is based closely off of the work in https://github.com/viebel/defntly, which I would have used except there is no support for wrapping arguments.

I've also included some reader macros, and you can add them `defrag/p` or `defrag/d` to `defn` forms like so:

``` clojure
#defrag/p
(defn ^:private f
  ([] 1)
  ([x] (+ 2 x))
  ([x & ys] (first ys)))
```


## License

Copyright © 2020 Bryan Maass

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
