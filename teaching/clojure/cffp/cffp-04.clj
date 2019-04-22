;; ## Clojure for Functional Programmers - 04

;; [<- Lesson 03](https://www.maria.cloud/http-text/https%3A%2F%2Fs3-eu-west-1.amazonaws.com%2Fjmglov.net%2Fteaching%2Fclojure%2Fcffp%2Fcffp-03.clj)

;; Last time, we built a game engine for tic-tac-toe.

;; This time, we'll implement our own lisp, using the approach from chapter 4 of the venerable [Structure and Interpretation of Computer Programs](https://mitpress.mit.edu/sites/default/files/sicp/full-text/book/book-Z-H-26.html).

;; The core of any lisp is comprised of two functions, `eval` and `apply`. These two simple functions form a mighty foundation upon which we can build an elegant, towering structure.

;; ### eval

;; `eval`, as its name implies is concerned with the evaluation of **s-expressions**. Luckily for us, evaluating an s-expression is trivial! Remember, an s-expression is usually a list in which the first element is an operator, and the rest of the elements are operands. It may also be:

;; * A **value**, which evaluates to itself

;; * A **symbol**, which evaluates to whatever the symbol is bound to in the current **environment**

;; * A **quoted** s-expression, which evaluates to the text of the s-expression (i.e. the s-expression is not evaluated)

;; An operator is either a function or one of the following **special forms**:

;; * A **definition**, in which case we need to create a new **binding** in the current environment

;; * A **conditional**, in which case we need to evaluate the test and then evaluate either the truthy or falsy branch of the expression, depending on the result of the test

;; * A **lamdba**, in which case we create a new **anonymous function**

;; * A **do block**, in which case we evaluate the sub-expressions in order

;; We'll call our `eval` function `sl-eval` so that we don't shadow Clojure's `eval`, and can define it in Clojure as follows:

(defn sl-eval [exp env])

;; To use `sl-eval`, we'll call it with a quoted s-expression and a map for the environment. We're immediately introduced to the difference between the **implementation language** and the **hosted language**. Since we're implementing our lisp (which we shall now dub SmooothLisp, for no particular reason) in another lisp (Clojure), our implementation can be defined either using itself or Clojure, depending on how much we've bootstrapped. In this case, we'll use our language from a Clojure REPL, so we need to pass in the arguments to `sl-eval` using Clojure's quoting facility and map data structure.

(sl-eval '(some-fn 1 "two" (3 4)) {})

;; Of course, this currently returns `nil`, since we haven't actually implemented anything. Let's fix that.

;; ### Self-evaluating s-expressions

;; In SmooothLisp, we'll have a few things that evaluate to themselves:

;; * Numbers

;; * Strings

;; * The booleans `true` and `false`

;; * `nil`

;; There is no compelling reason why we shouldn't use Clojure's numbers, strings, booleans, and `nil` for our SmooothLisp equivalents, which makes our definition of `self-evaluating?` quite straightforward:

(defn self-evaluating? [exp]
  (or (number? exp)
      (string? exp)
      (boolean? exp)
      (nil? exp)))

;; Let's try it!

(map self-evaluating? [1 2.3 "four" true nil '(1 2 3)])

;; Now that we've defined `self-evaluating?`, we're ready to handle self-evaluating expressions in `sl-eval`:

(defn sl-eval [exp env]
  (cond
   (self-evaluating? exp) exp))

;; Let's try evaluating a few of these!

(sl-eval '1 {})

(sl-eval '2.3 {})

(sl-eval '"foo" {})

(sl-eval 'true {})

(sl-eval 'nil {})

;; ### The global environment

;; It's getting a bit annoying to always have to pass `sl-eval` an environment, so let's introduce another arity that reads the global environment.

(def global-env (atom {}))

(defn sl-eval
  ([exp]
   (sl-eval exp @global-env))
  ([exp env]
   (cond
    (self-evaluating? exp) exp)))

(sl-eval '42)

;; Ah... that's smooother!

;; ### Symbols

;; A **symbol** is the name of a binding. We'll just use Clojure symbols to represent SmooothLisp symbols `symbol?` function directly:

(symbol? 'some-function)

(symbol? "foo")

;; When we encounter a symbol, we need to look it up in the environment. That sounds like a job for a Clojure map!

(defn lookup-symbol [sym env]
  (if-let [v (get env sym)]
    v
    (error "Cannot resolve symbol" sym)))

;; Let's add it to the evaluator:

(defn sl-eval
  ([exp]
   (sl-eval exp @global-env))
  ([exp env]
   (cond
    (self-evaluating? exp) exp
    (symbol? exp) (lookup-symbol exp env))))

;; Since we currently don't have any way to define a binding, we'll get nothing but errors when trying to resolve a symbol:

(sl-eval 'foo)

;; This error message is a bit misleading, as what Maria is telling us is that we haven't defined the `error` function. Let's do that now.

;; ### Reporting errors

;; We'll take advantage of the fact that Maria is implemented in ClojureScript and throw a JavaScript error, which Maria will conveniently catch and display to us in a nice way.

(do
 (require '[clojure.string :as string])

 (defn error [& msgs]
   (throw (js/Error. (string/join " " msgs)))))

;; Now we'll try evaluating a symbol again and see what happens.

(sl-eval 'foo)

;; Not bad!

;; ### Quoted s-expressions

;; A quoted s-expression is a list of code that shouldn't be evaluated (yet). We've been happily making these in Clojure every time we say something like this:

'(foo "bar" 42)

;; Now it's time to define our first SmooothLisp special form! Clojure has its own `quote` special form, but we can name ours the same and not shadow Clojure's since SmooothLisp code is passed to `sl-eval` as a Clojure-quoted s-expression! Got that? ;)

(defn quoted? [exp]
  (= 'quote (first exp)))

(quoted? '(quote (some-function 1 2)))

(quoted? '(some-function 1 2 3))

;; We do have one minor problem, though:

(quoted? '1)

;; `sl-eval` will guarantee that `quoted?` is only called on an s-expression that is a list, but it feels a bit dirty to have a function that will blow up with a very vague error message, so let's fix it.

(defn quoted? [exp]
  (and (sequential? exp) (= 'quote (first exp))))

(quoted? '1)

;; When `sl-eval` encounters a quoted s-expression, it should return the quoted text--i.e. the s-expression itself:

(defn quoted-text [exp]
  (if (sequential? exp)
    (second exp)
    (error "Quote must be sequential")))

(quoted-text '(quote (some-function 1 2)))

;; Now to add it to the evaluator:

(defn sl-eval
  ([exp]
   (sl-eval exp @global-env))
  ([exp env]
   (cond
    (self-evaluating? exp) exp
    (symbol? exp) (lookup-symbol exp env)
    (quoted? exp) (quoted-text exp))))

;; Let's try evaluating a quoted s-expression:

(sl-eval '(quote (some-function 1 2)))

;; ### Definitions

;; Let's give SmooothLisp a decidedly old-school flavour and name the special form for creating a binding `define`, which takes two arguments: a symbol, and the value to bind to the symbol. A SmooothLisp definition looks like this:

'(define foo 42)

;; Determining if an expression is a definition is very similar to how we implemented `quoted?`:

(defn definition? [exp]
  (and (sequential? exp) (= 'define (first exp))))

(definition? '(define foo 42))

(definition? '(quote (foo 42)))

;; All a binding is is a map entry where the key is a symbol and the value is, well, a value, so we can create one like this:

(defn eval-definition [exp env]
  (if (sequential? exp)
    (let [[_ sym rhs] exp]
      (assoc env sym (sl-eval rhs env)))
    (error "Definition must be sequential")))

(eval-definition '(define foo 42) {})

;; Let's add it to the evaluator:

(defn sl-eval
  ([exp]
   (sl-eval exp @global-env))
  ([exp env]
   (cond
    (self-evaluating? exp) exp
    (symbol? exp) (lookup-symbol exp env)
    (quoted? exp) (quoted-text exp)
    (definition? exp) (eval-definition exp env))))

(sl-eval '(define foo 42))

;; Great! Now we've bound the symbol `foo` to the value `42`! Right?

(sl-eval 'foo)

;; Well yes, we have created the binding, but just in the environment that we passed to `eval-definition`. If we pass the environment returned by `eval-definition` to `sl-eval` when evaluating the symbol, then we can see it:

(->> (sl-eval '(define foo 42) {})
     (sl-eval 'foo))

;; Nice!

;; But clunky. What we'd really like to do is be able to create a global binding like this:

(sl-eval '(define foo 42))

;; And then look up its value like this:

(sl-eval 'foo)

;; This is not as tricky as it looks as long as we're willing to do some mutation. And since Clojure supplies us with nice tools to manage state, we are willing, right?

;; Let's modify the 1-arity version of `sl-eval` to pass not the value of the global environment atom, but the atom itself:

(defn sl-eval
  ([exp]
   (sl-eval exp global-env))
  ([exp env]
   (cond
    (self-evaluating? exp) exp
    (symbol? exp) (lookup-symbol exp env)
    (quoted? exp) (quoted-text exp)
    (definition? exp) (eval-definition exp env))))

;; Now, we need to modify `lookup-symbol` and `eval-definition` to deal with this new contract:

(defn lookup-symbol [sym env]
  (if-let [v (get @env sym)]
    v
    (error "Cannot resolve symbol" sym)))

(defn eval-definition [exp env]
  (if (sequential? exp)
    (let [[_ sym rhs] exp]
      (swap! env assoc sym (sl-eval rhs env)))
    (error "Definition must be sequential")))

(sl-eval '(define foo 42))

(sl-eval 'foo)

;; Perfect!

;; Except for one matter of cleanliness: evaluating a definition returns the entire environment resulting from the definition! Let's take a page from Clojure's book and return the symbol itself.

(defn eval-definition [exp env]
  (if (sequential? exp)
    (let [[_ sym rhs] exp]
      (swap! env assoc sym (sl-eval rhs env))
      sym)
    (error "Definition must be sequential")))

(sl-eval '(define bar "some string"))

(sl-eval 'bar)

;; Also note the recursive call to `sl-eval` when we create the binding. That means that we can use any valid SmooothLisp code on the right-hand side of a definition!

(sl-eval '(define baz bar))

(sl-eval 'baz)

;; Amazing!

;; ### Conditionals

;; We have two choices for building a special form for conditionals:

;; 10. Define `if`, which we can use later to build a `cond`, or

;; 11. Define `cond`, which we can use later to build an `if`

;; `cond` is more fun, because it lets us have as many if / else if / else if / ... / else branches as we want. So let's do that!

(defn conditional? [exp]
  (and (sequential? exp) (= 'define (first exp))))

;; `conditional?` looks pretty familiar. In fact, it looks just like `definition?`, and `quoted?`, with the exception of what we expect `op` to be. The Rule of Three now forces us to generalise:

(do
 (defn tagged-list? [tag exp]
   (and (sequential? exp) (= tag (first exp))))

 (def definition? (partial tagged-list? 'define))
 (def quoted? (partial tagged-list? 'quote))
 (def conditional? (partial tagged-list? 'cond)))

;; Now we can get back to evaluating conditionals:

(defn eval-conditional [exp env]
  (if (sequential? exp)
    :do-stuff
    (error "Conditional must be sequential")))

;; Rule of three time again!

(do
 (defn assert-sequential [exp-type exp]
   (when-not (sequential? exp)
     (error exp-type "must be sequential")))

 (defn quoted-text [exp]
   (assert-sequential "Quote" exp)
   (second exp))

 (defn eval-definition [exp env]
   (assert-sequential "Definition" exp)
   (let [[_ sym rhs] exp]
     (swap! env assoc sym (sl-eval rhs env))
     sym))

 (defn eval-conditional [exp env]
   (assert-sequential "Conditional" exp)
   (let [[_ & pairs] exp]
     (if (even? (count pairs))
       (->> pairs
            (partition 2)
            (reduce (fn [result [test exp]]
                      (if (sl-eval test env)
                        (reduced (sl-eval exp env))
                        result))
                    nil))
       (error "Conditional requires an even-numbered list")))))

;; Let's add conditionals to `sl-eval`:

(defn sl-eval
  ([exp]
   (sl-eval exp global-env))
  ([exp env]
   (cond
    (self-evaluating? exp) exp
    (symbol? exp) (lookup-symbol exp env)
    (quoted? exp) (quoted-text exp)
    (definition? exp) (eval-definition exp env)
    (conditional? exp) (eval-conditional exp env))))

(sl-eval '(cond false "no"
                true "yes"))

(sl-eval '(cond false "no"
                nil "still no"))

;; ### Lambdas

;; Lambdas are where lisp gets interesting. Let's start with the straightforward bit, defining the syntax of one. Let's say that it starts with the literal `lambda`, then a list of parameters, then zero or more s-expressions which form the body of the function. So we'd create a lambda that determines who wins a game like this:

'(lambda (x y)
         (cond x "x wins!"
               y "y wins!"
               true "it's a draw!"))

;; Given this definition, determining whether an s-expression is a lambda is easy enough:

(def lambda? (partial tagged-list? 'lambda))

(lambda? '(lambda (x y)
                  (cond x "x wins!"
                        y "y wins!"
                        true "it's a draw!")))

;; We can also write helper functions to pull the parameters and body out of a lambda:

(defn function-parameters [exp]
  (assert-sequential "Lambda" exp)
  (second exp))

(defn function-body [exp]
  (assert-sequential "Lambda" exp)
  (drop 2 exp))

(function-parameters '(lambda (x y)
                              (cond x "x wins!"
                                    y "y wins!"
                                    true "it's a draw!")))

(function-body '(lambda (x y)
                        (cond x "x wins!"
                              y "y wins!"
                              true "it's a draw!")))

;; Now, what does it mean to make a lambda? Well, we know that a lambda is an anonymous function, and if we think about what a function is, it's nothing more than a way to extract a few expressions from some code so that we can cut down on some copy and paste action.

(def x 3)

(def y 4)

(+ (inc (* x 2)) (inc (* y 2)))

(defn sqinc [n]
  (inc (* n 2)))

(+ (sqinc x) (sqinc y))

;; A function also does something else, though:

(def delimiter "-")

(defn join-strings [& strings]
  (string/join delimiter strings))

(join-strings "one" "two" "three")

;; Note that we're using not only the `strings` parameter but also the global binding `delimiter` in the function body. A function actually forms a **closure** over the global environment, effectively adding all the global bindings to its parameter list. A lambda also closes over its environment, but that environment need not be the global one.

;; So when we create a lambda, we need to preserve not only the code and parameter list, but also the current environment. We'll use a Clojure map to represent functions:

(defn make-function [parameters body env]
  {:type :function
   :parameters parameters
   :body body
   :env env})

;; All that's left is to add lambdas to the evaluator:

(defn sl-eval
  ([exp]
   (sl-eval exp global-env))
  ([exp env]
   (cond
    (self-evaluating? exp) exp
    (symbol? exp) (lookup-symbol exp env)
    (quoted? exp) (quoted-text exp)
    (definition? exp) (eval-definition exp env)
    (conditional? exp) (eval-conditional exp env)
    (lambda? exp) (make-function (function-parameters exp) (function-body exp) env))))

;; And let's make a lambda and bind it to a symbol so we can use it (once we've implemented function application, that is):

(sl-eval '(define winner (lambda (x y)
                                 (cond x "x wins!"
                                       y "y wins!"
                                       true "it's a draw!"))))

;; ### Do expressions

;; The `do` special form gives us a way to evaluate a sequence of s-expressions instead of just a single one. This is only really useful for side effects, but SmooothLisp ain't afraid of no side effects!

(def do? (partial tagged-list? 'do))

(defn do-expressions [exp]
  (assert-sequential "Do expression" exp)
  (drop 1 exp))

;; Some manual testing never hurts:

(do? '(do
       (quote (1 2))
       (cond true "yes")))

(do-expressions '(do
                  (quote (1 2))
                  (cond true "yes")))

;; Evaluation consists of evaluating each expression in sequence and returning the result of the last evaluation:

(defn eval-sequence [exps env]
  (reduce (fn [res exp] (sl-eval exp env)) nil exps))

;; Adding it to the evaluator looks familiar:

(defn sl-eval
  ([exp]
   (sl-eval exp global-env))
  ([exp env]
   (cond
    (self-evaluating? exp) exp
    (symbol? exp) (lookup-symbol exp env)
    (quoted? exp) (quoted-text exp)
    (definition? exp) (eval-definition exp env)
    (conditional? exp) (eval-conditional exp env)
    (lambda? exp) (make-function (function-parameters exp) (function-body exp) env)
    (do? exp) (eval-sequence (do-expressions exp) env))))

(sl-eval '(do
           (quote (1 2))
           (cond true "yes")))

;; ### Function applications

;; A function application is a list containing one or more elements. The first element is either:

;; * a symbol, in which case it is evaluated

;; * a lambda, in which case it is evaluated

;; * or a primitive function (more on this next time)

(def primitive-functions
  #{'+ '- '/ '*})

(def primitive-function? (comp boolean primitive-functions))

(defn function-application? [exp]
  (when (sequential? exp)
    (let [op (first exp)]
      (or (symbol? op)
          (lambda? op)
          (primitive-function? op)))))

;; Some testing is in order:

(primitive-function? '+)

(function-application? '(foo 42))

(function-application? '((lambda (x y) (+ x y)) 1 2))

(function-application? '(+ 1 2))

;; Once we know we have a function application, we need to be able to pull out the function and its arguments:

(defn function [exp env]
  (assert-sequential "Function application" exp)
  (let [op (first exp)]
    (if (primitive-function? op)
      op
      (sl-eval op env))))

(defn arguments [exp]
  (assert-sequential "Function application" exp)
  (rest exp))

;; Let's test this:

(function '(+ 1 2) global-env)

(function '((lambda (x y) (+ x y)) 1 2) global-env)

(function '(winner false true) global-env)

(arguments '(+ 1 2))

(arguments '((lambda (x y) (+ x y)) 1 2))

(arguments '(winner false true))

;; Finally, before applying the function to its arguments, we need to evaluate the list of arguments:

(defn list-of-values [exps env]
  (map #(sl-eval % env) exps))

(list-of-values ['foo 'winner 1] global-env)

;; We'll leave the actual implementation of `sl-apply` until next time, but for now, we can promise Clojure we'll define it later and add it to `sl-eval`. We'll also throw an error if we get an expression we don't know how to handle.

(declare sl-apply)

(defn sl-eval
  ([exp]
   (sl-eval exp global-env))
  ([exp env]
   (cond
    (self-evaluating? exp) exp
    (symbol? exp) (lookup-symbol exp env)
    (quoted? exp) (quoted-text exp)
    (definition? exp) (eval-definition exp env)
    (conditional? exp) (eval-conditional exp env)
    (lambda? exp) (make-function (function-parameters exp) (function-body exp) env)
    (do? exp) (eval-sequence (do-expressions exp) env)
    (function-application? exp) (sl-apply (function exp env)
                                          (list-of-values (arguments exp) env)))))

;; ### Where next?

;; Next time, we'll implement the elusive `sl-apply`, and the kernel of SmooothLisp will be complete!
