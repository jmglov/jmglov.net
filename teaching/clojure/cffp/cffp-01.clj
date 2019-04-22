;; # Clojure for Functional Programmers 01

;; If you're reading this, I'm assuming you're someone with experience with functional programming, either in a functional language like Haskell, ML, or O'Caml; a Lisp such as Common Lisp, Racket, or Scheme; a hybrid language like Scala; or programming in a functional style in an imperative language like Java Streams, JavaScript with Rambda, Ruby, Python, etc.

;; I'm further assuming that you don't have much experience with Clojure and would like to learn more.

;; If this sounds like you, buckle up and let's go!

;; ### What makes Clojure Clojure?

;; Clojure is several things:

;; * A Lisp

;; * A pragmatic language

;; * A hosted language

;; Let's start by exploring its Lispiness. The biggest difference between Lisp and most other languages is that Lisp has almost no syntax. It is built of *s-expressions*, which are basically tree structures of linked lists. \[[1\]](https://en.wikipedia.org/wiki/S-expression). In Lisp, both code and data are built of s-expressions, leading to the commonly recited mantra:

;; > Code is data and data is code.
;; >
;; > -- Every Lisp programmer ever

;; s-expressions look like this:

(str (+ 1 2 (* 3 4)) (/ 4 2))

;; Drawn as a tree:

;; ```
;;        str
;;     ╱       ╲
;;    +         /
;;  ╱ │ ╲      ╱ ╲
;; 1  2  *    4   2
;;      ╱ ╲
;;     3   4
;; ```

;; To use another fancy word, Lisp is *homoiconic*, since a Lisp program can be manipulated as data using the language itself. \[[2\]](https://en.wikipedia.org/wiki/Homoiconicity)

;; We'll come back to this concept later when we talk about macros, but for now, just feel satisfied with yourself for having learned some fancy new terminology. (If you're a Lisp programmer new to Clojure, I apologise for boring you with a bunch of stuff you already knew.)

;; Another thing to note about s-expressions is that they use *prefix notation* for mathematical operators, meaning the operator comes before the operands. Contrast an expression in the familiar *infix notation* (where the operators come in between the operands):

;; `1 + 2 + 3 + 4`

;; with the same expression in prefix notation:

(+ 1 2 3 4)



























;; This has two benefits over infix notation:

;; * You only type 33% of the operators, and

;; * Precedence is explicit

;; Consider this expression in infix notation:

;; `1 + 2 * 3 - 4 / 17 * 2 + 9`

;; Any clue what that means? We need to remember that `*` and `/` take precedence over `+` and `-`, and that operators with the same precedence are evaluated left to right. Of course, we can add parenthesis so that we don't have to do this grouping exercise in our heads:

;; `1 + (2 * 3) - ((4 / 17) * 2) + 9`

;; With prefix notation, you have no choice:

(+ 1 (- (* 2 3) (* (/ 4 17) 2)) 9)

















;; I won't pretend that writing mathematical expressions in prefix notation is intuitive if you're not used to it, but after a few dozen repetitions, you get the hang of it. ;)

;; You may have noticed that operators are nothing more (or less) than functions, which means they can be passed to higher order functions, stored in data structures, or any other useful thing you can think of to do with a function.

;; This brings us to the rule of evaluation in Clojure:

;; **The first thing in a list is a function, and the rest of the things in the list are arguments to that function.**

;; So when you see this:

(str "foo" 1 "bar" 2)















;; Evaluating it will call the `str` function with the arguments `"foo"`, `1`, `"bar"`, and `2`.

;; ### Literal data structures

;; Speaking of data structures, one of the things that makes Clojure different from other Lisps is its literal data structures. Other Lisps have literal lists, so you can do this:

'(1 2 3 4)

























;; Instead of having to write out

(cons 1 (cons 2 (cons 3 (cons 4 nil))))

























;; Clojure has three more literal data structures:

;; **vectors**

[1 2 3 4]

























;; **sets**

#{1 2 3 4}

























;; **maps**

{1 2
 3 4}























;; Idiomatic Clojure programming is very data-oriented, and the map is the workhorse of Clojure programs. We'll see this over and over as we learn more Clojure.

;; These three data structures do exactly what you'd expect, but they also have a hidden magical power: all three are also functions that look up a value in themselves.

;; A vector, when called with an integer argument, will return the element at that index in the vector.

(def v [1 2 3 4])

(v 2)























;; which is the same as:

(get v 2)























;; A set, when called with any value as an argument, will look up that value in the set, returning it if found, or `nil` otherwise (`nil` is Clojure's special "there's no value here" value; unlike other Lisps, it is not another way to write the empty list).

(def s #{1 2 3 4})

(s 2)























;; which is the same as:

(get s 2)























;; A map, when called with any value as an argument, will return the value associated with that key in the map, or `nil` if no such key exists.

(def m {1 2
        3 4})

(m 1)























;; which is the same as:

(get m 1)























;; A map, when called as a function, can take an extra argument, which is the value to return if the key is not found.

(m 42 "nope")























;; The `get` function can also do that, for all three data structures.

(get v 42 "nope")

(get s 42 "nope")

(get m 42 "nope")





;; Why in the world would you want to use one of these data structures as a function? Well, perhaps you want to select all the balls and dogs from a collection of toys and animals:

(filter #{"ball" "dog"} ["rubber ducky" "dog" "seal" "ball" "ox"])





;; Or maybe you want to select a few values from a map:

(map {:name "Rover"
      :species :dog
      :favourite-thing "A ball! A ball!"}
     [:name :favourite-thing])



;; You might have noticed some weird stuff above, namely: `:name`, `:species`, `:dog`, and `:favourite-thing` . These are **keywords**, which are interned values in Clojure that evaluate to themselves:

:name























;; They are similar to Erlang's atoms and Ruby's symbols, with one tremendously fun difference: they are also functions, that when called with an associative data structure that can be indexed by a keyword, will look themselves up in the data structure!

(def animal {:name "Rover"
             :species :dog 
             :favourite-thing "A ball! A ball!"})

(:name animal)



;; Most maps in Clojure programs are keyed with keywords, as they're a very readable and performant way to represent structured data.





















;; Keywords can also be *namespaced*, which is becoming a more and more common practice.

{:animal/name "Rover"
 :animal/species :dog
 :animal/favourite-thing "A ball! A ball!"}























;; If you want to make sure your keywords are globally unique, you can use a Java package-style namespace:

{:net.jmglov.animal/name "Rover"}
















;; Clojure is a very dynamic language, so it may not surprise you to learn that you can mix datatypes as much as you want within vectors, sets, and maps:

(def fun-stuff [1 "two" ["three"] #{4 "five"} {6 "seven"
                                               "eight" 9
                                               [10] 11
                                               #{12} "thirteen"}])

((fun-stuff 4) [10])























;; ### Persistent data structures

;; Probably unsurprisingly, Clojure uses *persistent data structures* so all the lovely data structures above are immutable. Maybe surprisingly to any Lisp programmers out there, Clojure doesn't give you sneaky ways to mutate data structures anyway.

;; Clojure's persistent data structures implementation (Jean Niklas l'orange has a great [5-part series](https://hypirion.com/musings/understanding-persistent-vector-pt-1) explaining them) is quite performant, though one does have to take care with the many short-lived objects generated. Clojure has a nice answer to that called **transducers**, but that will have to wait for later.
