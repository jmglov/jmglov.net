;; # Clojure for Functional Programmers 02

;; [<- Lesson 01](https://www.maria.cloud/http-text/https%3A%2F%2Fs3-eu-west-1.amazonaws.com%2Fjmglov.net%2Fteaching%2Fclojure%2Fcffp%2Fcffp-01.clj)

;; Last time on Clojure for Functional Programmers, we learned about s-expressions, literal data structures, and persistent data structures.

;; This time, we'll start with:

;; ## The sequence abstraction

;; One of the core ideas in Clojure is the [sequence abstraction](https://clojure.org/reference/sequences), which is a simple interface containing these operations:

;; * `(first xs)` - return the first item in the seq `xs`

;; * `(rest xs)` - return all but the first item in the seq `xs`

;; * `(cons x xs)` - return a new seq where `x` is the first item and `xs` is the rest

;; The `seq` function takes a collection as its argument, and returns an implementation of the `ISeq` interface that is appropriate to the collection.

(seq [1 2 3 4])

(seq #{1 2 3 4})

(seq {1 2 3 4})

(seq "foo")

;; Seqs are persistent and immutable, so nothing you do to a seq can possibly affect the underlying collection.

;; Calling `seq` on an empty collection returns `nil`:

(seq [])

(seq #{})

(seq {})

;; And calling `seq` on `nil` helpfully returns `nil` as well:

(seq nil)

;; There are a huge number of functions in [Clojure's seq library](https://clojure.org/reference/sequences#_the_seq_library), ranging from old friends like `map`, `filter`, and `reduce` to bizarre species such as `nthnext` and `to-array-2d`.

;; What most of them have in common are the following:

;; * If they take a collection as an argument, they first call `seq` on that collection and then operate on the resulting seq instead of the collection directly

;; * If they return a collection, it is a seq

;; * They are lazy, meaning they consume and produce sequences incrementally

;; For example, calling `map` with a vector returns a seq:

(map identity [1 2 3])

;; The fact that seqs are lazy gives us the power to express infinity.

(take 5 (range))

(take 10 (iterate (partial * 2) 1))

;; Both `range` and `iterate` return seqs, out of which you can take as many as you like.

;; I also sneaked another Clojure concept in there: *partial evaluation*. Partial evaluation is very similar to currying in Haskell, except it doesn't happen automatically. The `partial` function takes as its arguments a function `f` and fewer than the normal number of arguments to `f`, and returns a function in which those arguments are fixed to the values you supplied. So the `(partial * 2)` that we wrote above returns a function that multiplies its argument(s) by 2:

(def times-2 (partial * 2))

(times-2 3)

(times-2 3 4 5)

;; Given lazy sequences, we can do cool things like sum up the first 10 powers of 2:

(reduce + (take 10 (iterate times-2 1)))

;; Now that we've done something cool enough to require us to use a few functions, we can address another common complaint about functional programming:

;; > I have to read your stupid program right-to-left!

;; That's true. What we're actually doing here is:

;; * Iterate from 1 to infinity, doubling each time

;; * Take the first 10 of those numbers

;; * Reduce them with the `+` function into a lovely number

;; So why can't we say it to Clojure in that order? Well, it turns out we can:

(->> (iterate times-2 1)
     (take 10)
     (reduce +))

;; `->>` is the so-called **thread-last** macro. What it does is rewrites your code (behind the scenes, don't worry) so that the result of the first thing is fed as the last argument to the next thing, which is in turn fed as the last argument to the next thing, and so on. In this case, the result of evaluating the `iterate` function call is fed as the last argument to the `take` function call, thus becoming `(take 10 (iterate times-2 1))`. The result of that is fed to our final function call, `reduce`: `(reduce + (take 10 (iterate times-2 1)))`, which is exactly what we wrote in the beginning.

;; With the thread-last macro, you can finally overcome the last argument against Clojure: it has too many parenthesis at the end of some lines. We reduced those line-ending parens by 33%, which I think is a very compelling improvement!

;; ## So macros, eh?

;; Yes, macros. A macro is one of the Lispiest parts of Lisp, and oddly enough, Clojure programmers tend to subscribe to the following rules of macros:

;; 1. The first rule of Macro Club is that we don't write macros.

;; 2. Seriously, don't write a macro! A function will do just fine.

;; 3. OK, fine, write a macro.

;; Idiomatic Clojure prefers data to functions, functions to macros, and macros to dying of the plague (but only just).

;; So, what is a macro, and why are Clojure programmers so afraid of them?

;; Remember how code is data and data is code? Well, a macro is code that runs data that is actually your code. A macro runs at compile time, and receives s-expressions corresponding to your code as its inputs.

;; Consider a trivial logging system which allows us to set the log level at compile time, and also keeps track of how many times something has been logged at each level.

(def log-level :debug)

(def log-times (atom {:trace 0
                      :debug 0
                      :info 0
                      :error 0}))

;; As usual, we need to take a detour to learn a new thing:

;; ### The almighty atom

;; Clojure takes a rather more laissez-faire attitude towards side effects than a pure language like Haskell, but it still takes a hard line on uncontrolled mutation. It gives you a few tools for responsible mutation, and one is the atom. An atom is like a box into which you put a value, and the `atom` function creates just such a box, placing into it the value you pass as its argument.

;; In our logging system, we've created an atom which holds a map of how many times we've logged at each level, and bound it to the symbol `log-times`. We can look inside the box by *dereferencing* the atom with the `deref` function:

(deref log-times)

;; Since we're Lisp programmers now, and thus hate typing, we can eschew the `deref` function in favour of the concise `@` reader literal:

@log-times

;; Using `@` to dereference atoms (i.e. peer inside the box) is the idiomatic thing to do in Clojure.

;; OK, but there's no mutation happening here, and we started this detour with dreams of controlled mutation in our hearts.

;; If an atom is like a box which holds a value, it might not surprise us that we can actually put a new value in the box:

(reset! log-times {:trace 0
                   :debug 1
                   :info 0
                   :error 0})

;; `reset!` is a function that, given an atom as its first argument, updates it to contain the second argument (ending the name of a function with `!` is a convention in Clojure that indicates the function is doing some mutation).

;; Now looking into the box, we see the new value:

@log-times

;; If someone had a reference to the old value, our shenanigans haven't caused any trouble for them. We can prove this with a little more mischief.

(def my-log-times @log-times)

(reset! log-times :something-truly-naughty-not-a-map!)

@log-times

my-log-times

;; Let's put `log-times` back the way we found it so that we can continue to build a logging system.

(reset! log-times my-log-times)

;; Now, let's write a function that increments the number of times we've logged at a given level.

(defn inc-logged! [level]
  (let [lt @log-times]
    (reset! log-times (assoc lt level (inc (lt level))))))

(inc-logged! :info)

;; If we evaluate the call above a few times, we will see the number of info lines increase. However, to say that the `inc-logged!` function is an unreadable mess is a huge understatement. Let's look first at the `assoc` call. `assoc` takes a map, a key, and a value, and returns a new map containing the key and the value.

(assoc @log-times :info (inc (:info @log-times)))

;; Here, we want the `:info` value to be the result of incrementing the old value. So can't we just say so? Luckily, we can!

(update @log-times :info inc)

;; `update`, like `assoc`, takes a map and a key as arguments, but instead of taking a value as its third argument, it takes a function. What is returned is a new map where the value of the specified key is the result of  calling the function with the current value of the key. Using `update`, we can make our `inc-logged!` function a little nicer:

(defn inc-logged! [level]
  (reset! log-times (update @log-times level inc)))

;; Something is still bugging us, though. It seems a little yucky somehow to dereference the `log-times` atom in order to modify it. What if there were a function that took an atom and a function, called the function on the current value of the atom, and stored the return value in the atom? Luckily for us, there is just such a function:

(defn inc-logged! [level]
  (swap! log-times #(update % level inc)))

(inc-logged! :info)

;; Amazing!

;; ### Back to logging

;; Restating the requirements of our logging system, we want to be able to log at one of four levels:

;; 10. Trace

;; 11. Debug

;; 12. Info

;; 13. Error

;; We'll set the log level at compile time:

(def log-level :debug)

;; This should result in debug, info, and error messages being logged, but not trace ones.

;; We should also keep track of the number of messages we've logged at each level. Since we've been messing around with our atom, we'll reset it now:

(reset! log-times {:trace 0
                   :debug 0
                   :info 0
                   :error 0})

;; A user will call us like this:

(log :debug "Something something" 42 "something")

;; How might we implement the `log` function? Well, we'll clearly need to assign the log levels a numeric value so we know whether to log or not. We'll do this with a regular map.

(def log-levels {:trace 0
                 :debug 1
                 :info 2
                 :error 3})

;; The `log` function is relatively straight-forward, then:

(defn log [level & msgs]
  (if (>= (log-levels level) (log-levels log-level))
    (inc-logged! level)
    (apply println msgs)))

;; We can test it by logging a few things then checking out our `log-times` atom.

(log :trace "This should not be logged")

(log :debug "But a debug message should")

(log :info "An info message should also be")

(log :error "And an error message? Mos def!")

;; This is a bulletproof implementation to be sure. Or is it?

;; Imagine a user has a function for deep tracing of their system:

(defn thread-dump []
  (reduce + (range 9999999)))

;; Now imagine that they use it in tracing statements:

(log :trace "Current state of mah thread:" (thread-dump))

;; This should be fine, since our log level is debug. However, if we evaluate the above expression, we see that it takes an unfortunately long time to execute. Why is this?

;; Well, Clojure is eagerly evaluated, which means that the call to `thread-dump` will be evaluated right after it's read, before the actual call to `log`.

;; Clearly, since Clojure has logging libraries that are performant, there must be a way around this.

;; ### Macros, that's what we were talking about, right?

;; Remember how Lisp programmers repeat "code is data; data is code" 42 times before leaving the house? Well, macros are the reason why.

;; A macro is a Clojure function that runs at compile time, receiving code as its arguments and returning code as its result. This might be a little hard to wrap our heads around, so let's just use a macro to solve our logging problem and then see if that explains it.

(defmacro log [level & msgs]
  `(when (>= (log-levels ~level) (log-levels log-level))
     (inc-logged! ~level)
     (println ~@msgs)))

;; The macro looks very similar to the function, with the exception of three new characters: backtick, `~`, and `~@`.

;; Backtick is a **syntax quote**. It tells the reader not to evaluate the following form, but leave it as a literal list. `~` is **unquote**. It tells the reader to stop being so literal and evaluate the next form after all. `~@` is **splicing unquote**. It is just like `~`, but treats the next form as a collection, then expands it. This is sometimes called "splatting" in other languages (e.g. Ruby).

;; So, when the `log` macro encounters code like this:

(log :trace "Current state of mah thread:" (thread-dump))

;; It will be invoked with three arguments: `:trace`, `"Current state of mah thread"`, and, as an unevaluated literal list, `(thread-dump`). `:trace` will be bound to its `level` argument, and the other two arguments will be rolled up into the `msgs` argument.

;; The body of the `log` macro starts with a syntax quote, so it will proceed building a literal list: `(when (>= (log-levels `... but then it hits an unquote, so it has to insert the value of the next form, `level`, with is `:trace`. As the unquote only applies to a single form, the reader goes back to syntax quoting: `) (log-levels log-level)) (inc-logged! `, and then hits another unquote. It inserts the value of `level` again, `:trace`, then continues with syntax quoting: `) (println `. Now it comes to `~@`, the splicing unquote. First, it takes the value of `msgs`, which is `("Current state of mah thread:" (thread-dump))`, then splices that into the current s-expression, the `println`. This yields `"Current state of mah thread:" (thread-dump)`, then it returns to syntax quoting for the rest of the macro: `)))`.

;; What the macro returns is the following code:

(when (>= (log-levels :trace) (log-levels log-level))
  (inc-logged! :trace)
  (println "Mah threads:" (thread-dump)))

;; This is the code that will be evaluated at runtime. We can think of a macro as effectively inlining a function, but with the power to transform the function body in any way we see fit, using the same programming language we're writing our program in!

;; Other languages have macros, but they are either simple text processing macros like the C preprocessor, or require you to use special library functions to manipulate the abstract syntax tree of the program, like Elixir. Only Lisp is homoiconic, so only Lisp programmers claim that code is data (and data is code).

;; Unfortunately, due to limitations of Maria, we can't evaluate this inline to test it. Instead, we can use an online REPL like [Repl.it](https://repl.it/languages/clojure). Here's our code for easy copy-pasting:

;; ```
;; (def log-level :debug)

;; (def log-times (atom {:trace 0
;;                       :debug 0
;;                       :info 0
;;                       :error 0}))

;; (defn inc-logged! [level]
;;   (swap! log-times #(update % level inc)))

;; (def log-levels {:trace 0
;;                  :debug 1
;;                  :info 2
;;                  :error 3})

;; (defmacro log [level & msgs]
;;   `(when (>= (log-levels ~level) (log-levels log-level))
;;      (inc-logged! ~level)
;;      (println ~@msgs)))

;; (defn thread-dump []
;;   (Thread/sleep 1000))

;; (log :debug "The truth:" 42)
;; (log :trace "Mah threads:" (thread-dump))

;; @log-times
;; ```

;; We can use the `macroexpand-1` function to expand the `log` macro one level to see what happens at compile time:

(macroexpand-1 '(log :trace "Mah threads:" (thread-dump)))

;; Again, this won't work in Maria, but Repl.it will show us what's up.

;; ### Macros aren't really that special, are they?

;; Well, it's true that our `log` macro wasn't doing anything other than splicing some values into some code, and that hardly demonstrates how we use Clojure to manipulate Clojure. How about something more fun?

;; We've seen a few cool ways to do things conditionally in Clojure so far: `if`, `when`, and `cond`. Let's look a little more closely at each of them:

(what-is if)

(what-is when)

(what-is cond)

;; Interesting. None of these three are functions, for the same reason as `log` couldn't be: we don't want Clojure to evaluate code unless the condition is true. `when` and `cond` are macros, which we know, but `if` is something called a *special form*. Evaluating a special form is hard-coded into Clojure itself. Everything else is either a function or a macro in Clojure's standard library!

;; Clojure has [13 special forms](https://clojure.org/reference/special_forms), plus two for vector and map destructuring. That's a lot for a Lisp, by the way; one only truly needs 6 (`def`, `fn`, `quote`, `if`, `do`, and `let`)to build everything else. Clojure's extra ones are mainly focused on fitting in nicely with the host platform (the JVM, a JavaScript engine, the [CLR](https://clojure.org/about/clojureclr), or even the [BEAM](http://clojerl.org/).

;; It turns out that `when` and `cond` are implemented in terms of `if`. `when` is very straight-forward:

(defmacro our-when [test body]
  `(if ~test ~body))

(our-when (= 1 1) :yes!)

;; (Again, you'll need to use Repl.it to test the above. Sorry about that.)

;; This works fine, until we try to use the killer feature of `when`: the ability to do more than one thing when the condition is true:

(when (= 1 1)
  (println "Math works!")
  :yes!)

;; Now we get a complaint that we've tried to pass 3 arguments to something expecting 2. We can get around this by treating everything after the `test` argument as a list:

(defmacro our-when [test & body]
  `(if ~test (do ~@body)))

;; Nice!

;; Now, let's think about how to implement `cond`. We know that it should take a list of condition / expression pairs, and return the result of the first expression for which the corresponding condition is true.

(defmacro our-cond [& cond-expr]
  (when (seq cond-expr)
    (let [[test# expr# & exprs#] cond-expr]
      `(if ~test#
         ~expr#
         (our-cond ~@exprs#)))))

;; The funky `#` characters on the end of our binding symbols (e.g `test#`, `expr#`, `exprs#`, etc.) are *auto gensyms*. A gensym (short for generate symbol) is a way to avoid unintended capture in macros.

;; Imagine a macro like this (example from the "Clojure for the Brave and True" [chapter on writing macros](https://www.braveclojure.com/writing-macros/)):

(defmacro with-mischief [& stuff-to-do]
  `(let [message "Under no circumstances should you "]
     (println (str message "forget to do the following:"))
     ~@stuff-to-do))

;; (Clojure will actually stop you from doing this mischief by exploding when you try to expand it, but imagine a world in which Clojure cared less about naughtiness.)

;; Now imagine that the user calls it like this:

(let [message "Don't forget to "]
  (with-mischief
   (map (partial str message) ["get the groceries" "walk the dog"])))

;; The poor user will get back a list like this:

["Under no circumstances should you get the groceries"
 "Under no circumstances should you walk the dog"]

;; We could instead generate a symbol to which to bind the `"Under no circumstances should you "` message, which would not collide with any user-generated symbol.

(gensym "message")

;; This is what the `#` character does:

(defmacro with-mischief [& stuff-to-do]
  `(let [message# "Under no circumstances should you "]
     (println (str message# "forget to do the following:"))
     ~@stuff-to-do))

;; Variable capture actually has some legitimate uses, which you can [read more about](http://thinkrelevance.com/blog/2008/12/17/on-lisp-clojure-chapter-9).

;; So, the `our-cond` macro:

;; 1. Sticks the entire expression into a list

;; 2. Makes sure the list is not empty (remember, `seq` will return `nil` for an empty collection, which is falsy)

;; 3. Binds the first two elements of the list to `test#` and `expr#` gensyms and the rest of the list to the `exprs#` gensym

;; 4. If Generates an `if` expression where the test condition is `test#`, the true expression is `expr#`, and the false expression is a recursive call to the `our-cond` macro with the rest of `exprs#` as the new value

;; Such beauty!

(defmacro our-cond [& cond-expr]
  (when (seq cond-expr)
    (let [[test# expr# & exprs#] cond-expr]
      `(if ~test#
         ~expr#
         (our-cond ~@exprs#)))))
