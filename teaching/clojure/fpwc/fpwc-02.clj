;; # Functional Programming with Clojure - L02

;; [<- Lesson 1](https://www.maria.cloud/http-text/https%3A%2F%2Fs3-eu-west-1.amazonaws.com%2Fjmglov.net%2Fteaching%2Fclojure%2Ffpwc%2Ffpwc-01.clj)

;; In the last lesson, we learned:
;; - What Clojure looks like (prefix notation)
;; - What functional programming is and how it differs from imperative programming
;; - The big three functions: `map`, `filter`, and `reduce`
;; - The meta-algorithm for recursive algorithms

;; This time, we'll do some serious practice with recursion and plumb the depths of the forbidden knowledge of functional programming.

;; ## Making Fibonacci proud

;; Now that we have a meta-algorithm for constructing recursive algorithms, let's look at another of the grand old examples of functional programming: the Fibonacci sequence.

;; The sequence goes like this:

;; `1, 1, 2, 3, 5, 8, 13, 21, 34, ...`

;; The rules for constructing the sequence are:

;; * The first two numbers in the sequence are both 1

;; * Subsequent numbers in the sequence are computed by adding the previous two numbers in the sequence

;; So the sequence can also be represented like this:

;; `1, 1, (1 + 1), 1 + (1 + 1), (1 + 1) + (1 + (1 + 1)), ...`

;; What we'd like to do is construct a recursive algorithm that returns the `n`th number of the Fibonacci sequence:

(defn fibonacci [n]
  :???)

;; The meta-algorithm for constructing recursive algorithms goes like this:

;; 1. Identify the base case

;; 2. Define the function in terms of itself

;; What's the base case here? Surprise! There are actually two of them!

;; * `(fib 1) => 1`

;; * `(fib 2) => 1`

;; Let's write them down:

(defn fibonacci [n]
  (if (= 1 n)
    1
    (if (= 2 n)
      1
      :???)))

;; Now that we've got the base cases covered, we can look at the definition of the sequence again: 

;; * Subsequent numbers in the sequence are computed by adding the previous two numbers in the sequence

;; Let's give that a crack!

(defn fibonacci [n]
  (if (= 1 n)
    1
    (if (= 2 n)
      1
      (+ (fibonacci (- n 2)) (fibonacci (dec n))))))

;; For Fibonacci numbers where `n` is 3 or more, we're adding the Fibonacci number `n - 2` to the Fibonacci number `n - 1`. A beautiful recursive definition to be sure!

;; Before we see if this actually works, let's deal with one bit of yuckiness: the nested `if`s.

;; Most imperative languages give us some syntactic sugar for the common `if`, `else if`, `else` construct:

;; `if (something) { ... } else if (otherthing) { ... } else { ... }`

;; This is exactly equivalent to writing:

;; `if (something) { ... } else { if (otherthing) { ... } else { ... } }`

;; Computers don't care, but humans seem to prefer reading the first. Since functional programming is all about love, Clojure offers up the `cond` function, which can take as many arguments as you like, in pairs of tests and expressions.

;; To see this in action, let's write a little function that tells us what its argument is:

(defn what-is-it? [x]
  (cond
   (number? x) "It's a number!"
   (string? x) "It's a string!"
   :else "I dunno!"))

;; The first test / expression pair is `(number? x)` / `"It's a number!"`. If the test evaluates to true, the `"It's a number!"` expression will be evaluated and returned as the result of the `cond` function.

(what-is-it? 27)

;; The next pair is `(string? x)` / `"It's a string!"`!

(what-is-it? "Blue")

;; The final pair is `:else` / `"I dunno!"`. Writing `:else` here is a Clojure convention (some people write `:default` instead), but it can actually be anything that Clojure considers "truthy", which is anything but `false` or `nil` (a special "value" in Clojure that actually indicates the absence of a value--more on which later).

;; Since the final test evaluates to true, `cond` (and indeed our function itself) returns:

(what-is-it? [1])

;; Using our new best friend `cond`, we can clean up our factorial function a bit:

(defn fibonacci [n]
  (cond
    (= 1 n) 1
    (= 2 n) 1
    :else (+ (fibonacci (- n 2)) (fibonacci (dec n)))))

;; So, does it work?

(fibonacci 9)

;; Seems like it!

;; To quote L. Peter Deutsch:

;; > To iterate is human; to recurse divine.

;; Now that we've learned the basics, the only thing standing between us and the divine is practice, practice, practice.

;; ## Practice, practice, practice

;; Now that we understand how to recurse, and we've been introduced to the Big Three functions of functional programming (`map`, `filter`, and `reduce`), let's combine our need to practise recursion with our burning desire to demystify functional programming. Time to implement `map`, `filter`, and `reduce` for ourselves!

;; Let's start with `map`. In order to take the first step in the meta-algorithm of recursion, finding the base case(s), we must first recall what it is that `map` purports to do.

;; `map` transforms a sequence of one thing into a sequence of another thing. It does this by taking as its arguments two things:

;; * A function `f`, which transforms a thing `x` into a thing `y`

;; * A sequence of `xs`

;; It returns the sequence of `ys` that are produced by calling the function `f` with each `x` as its argument.

;; Before we can write this function, we need to learn how to build a sequence in Clojure.

;; ### Sequences and the construction thereof

;; Clojure gives us an easy way to create an empty sequence: a *data literal*. A data literal is a thing that, when evaluated, becomes a data structure. Many languages have at least one data literal:

;; `[1, 2, 3, 4]`

;; In quite a few languages, this will give you an array containing the numbers 1, 2, 3, and 4. Clojure is no exception, except it uses the fancy word data literal to describe the syntax and the fancy word `vector` to describe the result.

;; A vector is like an array in that it is an ordered collection of things that offers constant-time access to any of the things regardless of how large it gets. A vector is unlike an array in that it can grow dynamically, and that the things it contained are not necessarily adjacent to each other in memory.

;; Clojure has a family of data structures called `sequences`, which are things than can be iterated over in a stable order. Vector is one such data structure, and is the one that Clojure programmers tend to reach for when they need a sequence unless they have a good reason to use something else.

;; Having said all that, let's construct an empty sequence!

[]

;; Empty sequences are well and good, but in order to do interesting stuff, we should be able to fill them with wonderful things.

;; This is where `cons` comes in. `cons` (probably short for "construct"? who knows--it's an old Lisp thing, and Lisp programmers hate typing so much that they shorten all function and variable names to the point of being undecipherable) is a function that adds an element to the front of a sequence.

;; So, if we take the empty sequence that we created above and `cons` a 1 onto it, we get a sequence containing the element `1`.

(cons 1 [])

;; If we `cons` something onto that, we get a sequence with two elements.

(cons 2 (cons 1 []))

;; And so on.

(cons 3 (cons 2 (cons 1 [])))

(cons 4 (cons 3 (cons 2 (cons 1 []))))

;; Now we have enough knowledge to implement `map`!

;; ### The making of a map function

;; Now it's time to apply our old friend the meta-algorithm for recursion:

;; 1. Identify the base case: **applying `map` to an empty sequence should return that empty sequence**

;; 2. Define the function in terms of itself: **call the transform function on the first element in the sequence, then stick the resulting value onto the front of the result of mapping over the rest of the sequence**

;; Let's say that in code! (Note that we'll call the function `my-map` so that we don't accidentally cheat and use Clojure's standard `map` function.)

(defn our-map [f xs]
  (if (empty? xs)
    xs
    (cons (f (first xs)) (our-map f (rest xs)))))

;; We're using a new function here called `empty?`, which (as you may have guessed) returns true for an empty sequence and false for anything else.

(empty? [])

(empty? [1 2 3 4])

;; Note that the `empty?` function ends with a question mark. This says two things about Clojure:

;; 1. `?` has no special meaning in Clojure's syntax

;; 2. Clojure functions that ask a question (known as predicates, like our friend `even?` from the previous lesson) often end with a `?`

;; There are two other new functions that we've made use of:

;; * `first`, which returns the first element of a sequence

;; * `rest`, which returns the rest of a sequence (i.e. all elements but the first one)

;; Our (as of yet untested) implementation of `our-map` has actually discovered one of Clojure's core ideas: the [sequence abstraction](https://clojure.org/reference/sequences). The sequence abstraction is nothing more than a contract requiring a data structure that wishes to be abstracted over as a sequence to implement the following three functions:

;; * `first`

;; * `rest`

;; * `cons`

;; Any collection or datatype that implements the sequence abstraction can be used by all of the functions in [Clojure's seq library](https://clojure.org/reference/sequences#_the_seq_library), ranging from old friends like `map`, `filter`, and `reduce` to bizarre species such as `nthnext` and `to-array-2d`.

;; But let's get back to `our-map`. Does it actually work?

(our-map str [1 2 3 4])

;; Sweet, sweet, victory!

;; We're 33% of the way to becoming Functional Programmers with A Capital F and P. Let's grab the next 33% by implementing `filter`.

;; ### Filtering all the things to get some of the things

;; According to the meta-algorithm for recursion:

;; 1. Identify the base case: **applying `filter` to an empty sequence should return that empty sequence**

;; 2. Define the function in terms of itself: **call the predicate function on the first element in the sequence; if it returns true, then stick the resulting element onto the front of the result of filtering over the rest of the sequence**

(defn our-filter [pred xs]
  (if (empty? xs)
    xs
    (if (pred (first xs))
      (cons (first xs) (our-filter pred (rest xs)))
      (our-filter pred (rest xs)))))

;; We've called our predicate argument `pred` because that's a Clojure convention. It could be worse--in most Lisp dialects it would be conventional to call it `p`.

;; Let's try filtering the even numbers:

(our-filter even? [1 2 3 4])

;; It worked! But we can pretty this up a bit, can't we? Remember `cond` from the halcyon days of `fibonacci`? Let's try it here:

(defn our-filter [pred xs]
  (cond
   (empty? xs) xs
   (pred (first xs)) (cons (first xs) (our-filter pred (rest xs))) 
   :else (our-filter pred (rest xs))))

(our-filter even? [1 2 3 4])

;; Still works! But we can refactor further. It's a bit annoying that we have to keep saying `(first xs)` and `(rest xs)` all over the place, right?

(defn our-filter [pred xs]
  (let [x (first xs)
        rxs (rest xs)]
    (cond
     (empty? xs) xs
     (pred x) (cons x (our-filter pred rxs))
     :else (our-filter pred rxs))))

;; `let` is a function that introduces local *bindings*. A binding is an association of a value (such as `1`) with a *symbol* (such as `x`). A symbol is a fancy name for a name, and thus we can state that a binding is basically a variable that doesn't vary. We've actually done a lot of binding already, I just didn't tell you about it!

;; One can make a binding of the vector `[1 2 3 4]` to the symbol `numbers` like so:

(def numbers [1 2 3 4])

;; That looks familiar, right? `def`, by the way, is probably short for "define".

;; And how about how we define functions? `defn`... `defn`... `def fn`? That's right! `defn` means "define function", and it's doing nothing more than binding a value, which in this case is a function, to a symbol of our choosing!

;; `def` and `defn` create what we can currently think of as global bindings. Any thing we `def` or `defn` in this page will be visible everywhere else.

;; `let` is a different beast. It creates bindings that are only visible inside the body of the `let`:

(let [top-secret "terces pot"]
  top-secret)

;; Inside the secure confines of the `let`, we can read the secret. But if we try to access it outside the `let`, we are rebuked:

top-secret

;; `let` takes pairs of s-expressions, where the first member of the pair is a so-called *binding form* and the second member of the pair is an expression which, when evaluated, will be bound as instructed by the binding form. The binding form we've used above is the symbol `top-secret`, to which we bound the value `"terces pot"`.

;; So our `our-filter` function now makes sense. We're binding `x` to the first element in the sequence `xs` and `rxs` to the rest of `xs`, but only inside our `let`.

(defn our-filter [pred xs]
  (let [x (first xs)
        rxs (rest xs)]
    (cond
     (empty? xs) xs
     (pred x) (cons x (our-filter pred rxs))
     :else (our-filter pred rxs))))

(our-filter even? [1 2 3 4])

;; Still works! But now we've made our function longer (or at least taller). Surely we can do better!

(defn our-filter [pred xs]
  (let [[x & rxs] xs]
    (cond
     (empty? xs) xs
     (pred x) (cons x (our-filter pred rxs))
     :else (our-filter pred rxs))))

;; What is this weird `[x & rxs]` nonsense? Well, we can take a hint from the fact that it is the first member of a binding, which makes it a binding form. And since it is inside square brackets, we might guess that it has something to do with a vector?

;; Yes! What we're doing here is called *destructuring*, which is a way for Clojure to pull apart a value, binding pieces of it to different symbols.

;; In this case, we are doing [sequential destructuring](https://clojure.org/guides/destructuring#_sequential_destructuring). If we have a sequence, such as this one:

(def numbers [1 2 3 4])

;; We can bind the first element in the sequence to the symbol `e1` like this:

(let [[e1] numbers]
  e1)

;; We can bind the first two like this:

(let [[e1 e2] numbers]
  (+ e1 e2))

;; Note that in our binding forms above, anything we don't bind is just throw away. This hardly seems polite! We can do the right thing by `numbers` and bind the rest of the sequence to the symbol `saving-the-best-for-last` like this:

(let [[e1 e2 & saving-the-best-for-last] numbers]
  (str "The first thing is " e1
       ", and the second thing is " e2
       ", but adding up the best stuff equals: "
       (reduce + saving-the-best-for-last)))

;; In a sequential binding form, `&` means "take the rest of the sequence and bind it using the binding form that I'm about to throw atcha!" In this case, the binding form is the symbol `saving-the-best-for-last`, but we can use any binding form we want here, including another sequential one!

(let [[e1 e2 & [e3 e4]] numbers]
  (str "e1: " e1 ", e2: " e2 ", e3: " e3 ", e4: " e4))

;; Let's look at the latest version of `our-filter` again.

(defn our-filter [pred xs]
  (let [[x & rxs] xs]
    (cond
     (empty? xs) xs
     (pred x) (cons x (our-filter pred rxs))
     :else (our-filter pred rxs))))

;; We're binding the first element of `xs` to the symbol `x`, then the rest of it to the symbol `rxs`. Does it still work?

(our-filter even? [1 2 3 4])

;; Of course! And now, we can do one more nifty trick:

(defn our-filter [pred [x & rxs :as xs]]
  (cond
   (empty? xs) xs
   (pred x) (cons x (our-filter pred rxs))
   :else (our-filter pred rxs)))

;; We can actually move the binding form to the vector of arguments of the function. In fact, the vector of arguments to `defn` is not really a vector, it's a binding form! This makes sense if we look at a function call:

(our-filter even? [1 2 3 4])

;; We're giving `our-filter` a sequence of two things: the function `even?` and the vector `[1 2 3 4]`. So when we write `(defn our-filter [pred [x & rxs]] ...)`, we're destructuring `[even? [1 2 3 4]]` and binding `even?` to `pred`, the first element of `[1 2 3 4]` to `x`, and the rest of `[1 2 3 4]` to `rxs` inside the body of `our-filter`.

;; Let's catch our breath for a second.

;; OK now?

;; There's one more wrinkle here. If we simply destructure `[x & rxs]`, we don't have a binding for `xs` in the body of the function, which is a problem given that we have an `(empty? xs)` expression in the body.

;; That's what the `:as xs` part that we snuck in is for. In a destructuring binding form, we can actually bind the entire expression being destructured to a symbol. So `[x & rxs :as xs]` is binding three things:

;; * The first element of the sequence to `x`

;; * The rest of the elements of the sequence to `rxs`

;; * The entire sequence to `xs`

;; Given this knowledge, we can understand the glory of `our-filter`:

(defn our-filter [pred [x & rxs :as xs]]
  (cond
   (empty? xs) xs
   (pred x) (cons x (our-filter pred rxs))
   :else (our-filter pred rxs)))

;; Now we're 66% of the way to enlightenment. All that remains is...

;; ### Reducing all of the things to one other thing

;; What does `reduce` do again? Takes a function, an initial value, and a sequence, and for each element of the sequence, calls the function with the accumulated value and the current element, the result of which becomes the accumulated value for the next call of `reduce`.

;; Or, stated in terms of the meta algorithm:

;; 1. Identify the base case: **applying `reduce` to an empty sequence should return the accumulated value**

;; 2. Define the function in terms of itself: **call the function with the accumulated value and the first element in the sequence, then call `reduce` with the result of the function call as the new accumulated value and the rest of the sequence as the new sequence**

;; My goodness! It's easier to just say it in Clojure, isn't it?

(defn our-reduce [f acc [x & rxs :as xs]]
  (if (empty? xs)
    acc
    (our-reduce f (f acc x) rxs)))

;; Can we sum a sequence of numbers?

(our-reduce + 0 [1 2 3 4])

;; Why yes we can!

;; Now we can claim another 33% of all the knowledge required to be honest-to-goodness Functional Programmers and earn that all-important LinkedIn endorsement for Functional Programming!

;; 33% from `our-map`, 33% from `our-filter`, and another 33% from `our-reduce` makes...

(+ 0.33 0.33 0.33)

;; Uh-oh, we're still 1% short. How can we earn that?

;; ### Implementing map and filter using reduce

;; What if we read in an ancient tome of forbidden functional programming lore (let's call it the NecronomSICPicon) that we can write `map` and `filter` by using `reduce`? Since forbidden books of lore seem to know what they're talking about, let's just take this one at its word, roll up our sleeves, and get to coding!

(defn forbidden-map [f xs]
  (reverse (our-reduce (fn [acc x] (cons (f x) acc)) [] xs)))

(forbidden-map str [1 2 3 4])

;; As usual, we have to learn a few new things to understand what we've written. Such is the nature of forbidden knowledge. In this case, the new things are, in order of decreasing obviousness:

;; * The `reverse` function, which takes a sequence as its argument and returns a sequence of the same elements, but in the opposite order

;; * The `fn` function, which takes as its arguments a binding form and a function body and returns a function

;; Wait, that second one sounds familiar, doesn't it?

(defn double-trouble [x]
  (* x 2))

;; `defn` takes a symbol, a binding form, and a function body, then binds that function to the symbol. We can accomplish the same thing by combining `def` and `fn`:

(def doubler-troubler (fn [x] (* x 2)))

(double-trouble 2)

(doubler-troubler 2)

;; Ha! So `defn` is nothing more than `def` + `fn`, or `deffn` if you will! You won't? OK, how about `defn`? Oh... so that's where the name comes from. Who'd have thought?

;; Anyway, going back to `forbidden-map`, the function we're feeding to `our-reduce` is one that `cons`es the result of calling the function `f` onto the accumulated value. The accumulated value starts as an empty sequence, so evaluating the expression `(our-reduce (fn [acc x] (cons (f x) acc)) [] [1 2 3 4])` goes like this:

;; ```
;; (cons (f 1) [])
;; (cons (f 2) [1])
;; (cons (f 3) [2 1])
;; (cons (f 4) [3 2 1])
;; => [4 3 2 1]
;; ```

;; The sequence is unfortunately in reverse order, but fortunately we have the `reverse` function, but unfortunately we feel guilty using it because we haven't written it, but fortunately we have `our-reduce`, and we can write any sequence function in terms of `our-reduce`! Ha!

(defn forbidden-reverse [xs]
  (our-reduce #(cons %2 %1) [] xs))

;; By the way, that `#(cons %2 %1)` stuff is another way to create an anonymous function. Inside the `#()`, `%1` is the first argument to the function, and `%2` is the second argument. `%3`, `%4`, `%5`, and so on all the way up to `%9` exist, but using those is in poor taste, so we'll refrain.

;; Anyway, back to reversing a sequence. Have we done it?

(forbidden-reverse [1 2 3 4])

;; Yes indeed. Now we can fix `forbidden-map` by using only stuff we've derived from first principles:

(defn forbiddener-map [f xs]
  (forbidden-reverse (our-reduce #(cons (f %2) %1)[] xs)))

(forbiddener-map str [1 2 3 4])

;; Now, flush with confidence from our earlier successes, we whip out our trusty parens and derive `forbidden-filter`:

(defn forbidden-filter [pred xs]
  (forbidden-reverse (our-reduce #(if (pred %2) (cons %2 %1) %1) [] xs)))

(forbidden-filter even? [1 2 3 4])

;; We've done it! We've demystified functional programming by discovering that `map`, `filter`, and `reduce` aren't so complicated after all, and that once we've implemented reduce, we can use it to implement `map` and `filter` (and `reverse`, and in fact any function that iterates over a sequence).
