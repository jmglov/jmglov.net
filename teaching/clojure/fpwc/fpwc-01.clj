;; # Functional Programming with Clojure - L01

;; Welcome to Functional Programming with Clojure, a course that teaches the basics of functional programming using the Clojure programming language. We'll start our explanation of Clojure by asking the all-important question:

;; ## Why does Clojure have so many parens?

;; Most programming languages use infix notation for operators:

;; `1 + 2 + 3 + 4`

;; And put the name of the function before the parens grouping its arguments:

;; `String.join(",", 1, 2, 3, 4)`

;; Clojure uses prefix notation, so the operator always comes first:

(+ 1 2 3 4)

;; Note that we didn't need to repeat the `+` between the numbers. Such efficiency of keystrokes!

;; As a brief aside, since we're conducting this lesson on **maria.cloud**, we can check our work by evaluating any Clojure code we want. Whenever you see a white box like this:

(+ 1 2 3 4)

;; You can position your cursor at the end of the line and hit Ctrl + Enter on Linux / Windows or Apple + Enter on MacOS (fine, "Command" + Enter, even though that feels like betraying my 22 years of Apple fandom).

;; Go ahead, try it!

;; See what Maria does for you? It prints out the result of evaluating the expression to the right of the box.

;; Maria can do a lot of other stuff too. If you hold down the Ctrl or Apple key for a few seconds, Maria will show you a quick reference at the bottom of the screen. We'll use some of that stuff later.

;; But for now, back to prefix notation!

;; For functions, we move the opening paren to the left of the function name. So this:

;; `String.join(",", 1, 2, 3, 4)`

;; Becomes this:

(clojure.string/join "," [1 2 3 4])

;; Note that operators and functions look the same. Surprise! In Clojure, an operator is just a normal function!

;; Prefix notation, in addition to letting you save a keystroke here and there when adding more than two numbers, has another nice property: it makes order of operation absolutely explicit. Consider this mathematical expression:

;; `1 + 2 * 3 - 4 / 17 * 2 + 9`

;; Any clue what that means? We need to remember that `*` and `/` take precedence over `+` and `-`, and that operators with the same precedence are evaluated left to right. Of course, we can add parenthesis so that we don't have to do this grouping exercise in our heads:

;; `1 + (2 * 3) - ((4 / 17) * 2) + 9`

;; With prefix notation, you have no choice, so precedence is always explicit:

(+ 1 (- (* 2 3) (* (/ 4 17) 2)) 9)

;; I won't pretend that writing mathematical expressions in prefix notation is intuitive if you're not used to it, but after a few dozen repetitions, you get the hang of it. ;)

;; Translating function calls is rather easier. Imagine we have code that turns a `Foo` into a `Bar` and then does a `baz` on it:

;; `baz(toBar(foo))`

;; In Clojure, parens rule, so we put them first!

;; `(baz(toBar foo))`

;; This brings us to the rule of evaluation in Clojure:

;; **When you see an opening paren, the first thing after it is the function, and the rest of the things that come before the closing paren are arguments to that function.**

;; So when you see this:

(str "foo" 1 "bar" 2)

;; Evaluating it will call the `str` function on the arguments `"foo"`, `1`, `"bar"`, and `2`.

;; ## What is functional programming, really?

;; Functional programming is basically programming with functions. To be less flippant, it is programming with functions that are *referentially transparent*, meaning they always return the same value when called with the same arguments.

;; An example of a referentially transparent function is our friend `str`, which returns the concatenation of the string values of its arguments:

(str 1.27 " is a fun number")

;; In object-oriented programming, we often work with functions called *instance methods*, which are often not referentially transparent:

;; ```
;; foo.getBar() => 3
;; foo.setBar(12)
;; foo.getBar() => 12
;; ```

;; Since the `getBar` function returns different values when called with the same arguments (in this case, no arguments), it is **not** referentially transparent.

;; The reason these functions are called referentially transparent is that whenever you see a call to such a function, you can replace it with the function body itself. For example, image a function called `add-and-double`, which adds its arguments together, then doubles the result.

(defn add-and-double [x y]
  (* (+ x y) 2))

;; In Clojure, we define a function by calling the special function `defn` with three arguments:

;; 1. The name of the function, `add-and-double`

;; 2. A vector of *formal parameters* (i.e. arguments) to the function (a *vector* is something like an array, and we write it as a list of stuff inside square brackets): `[x y]`

;; 3. The body of the function: `(* (+ x y) 2))`

;; Imagine a program that contains a call to `add-and-double` like this:

(str (add-and-double 2 6))

;; Since `add-and-double` is referentially transparent, you can replace it with its definition:

(str (* (+ 2 6) 2))

;; A referentially transparent function has one other important property: it must have no *side effects*. A side effect is anything that affects the world outside of the function body. Here's an obvious side effect:

(defn add-and-double! [x y]
  (database/put "x" x)
  (* (+ x y) 2))

;; Putting a value in a database definitely affects the outside world!

;; By the way, if you tried to evaluate that box, Maria would have given you an error. That's because I used my teaching licence to make up a function, `database/put`, that doesn't actually exist. So don't worry, that error is on me. Put it out of your mind and feel no guilt.

;; Back to side effects, here's a more subtle one:

(defn add-and-double-debug [x y]
  (println "add-and-double called with" x y)
  (* (+ x y) 2))

;; The `println` function concatenates the string version of its arguments, then prints it to the console, along with a newline. Even though this side effect doesn't matter in most cases, it still must be considered a side effect, for reasons we'll see later.

;; Since referentially transparent functions have no side effects, they are often also called *pure functions*, which has a nice ring to it, doesn't it?

;; ## OK, but how is functional programming different?

;; If you compare object-oriented programming to functional programming, the names of the two paradigms give you a clue in how they differ:

;; * Object-oriented programming is oriented around objects, meaning objects guide the design of programs

;; * Functional programming puts functions front and centre, meaning functions guide the design of programs

;; Object-oriented programming is one paradigm in a broader grouping of styles of programming: *imperative programming*. Imperative programming is all about telling computers exactly what to do. At the lowest level, we can program in the imperative style using assembly language, in which we tell the CPU which instructions to execute in which order.

;; Functional programming, on the other hand, is more concerned with the **what** than the **how**. Imagine a functional programmer and an imperative programmer going to the bakery. The functional programmer might order a cake like this:

;; > I'd like a birthday cake, please. It should be white cake with chocolate frosting, and say "Happy Birthday Jimmy" on top.

;; The imperative programmer's order would look somewhat different:

;; > Take two eggs, 50g of sugar, 200g of flour, and 5ml of baking soda. Preheat your oven to 175, then get out a bowl...

;; For a more technical example, imagine we have a list of numbers that we want to turn into a list of strings. No problem, we have a function that turns a number into a string, so we just make a `for` loop:

;; ```
;; numbers = [1, 2, 3, 4, 5]
;; strings = []
;; for (i = 0; i < numbers.length; i++) {
;;   strings += numbers[i].toString()
;; }
;; ```

;; We've probably written code like this a million times, right? It is very clear to the computer what steps we want it to take, and it blindly follows our orders. What it doesn't know is what we intended to do: turn a list of numbers into a list of strings. Since the computer doesn't know, it can't save us from subtle bugs like this:

;; `for (i = 0; i <= numbers.length; i++)`

;; Oops! We're off by one, and our glorious list of strings is missing the final `"5"`. This is such an easy mistake to make that we have a joke about it:

;; What are the two hardest problems in computer science?

;; 1. Off-by-one errors

;; 2. Cache invalidation

;; 3. Off-by-one errors

;; Functional programming takes a different tack: we tell the computer what we want to accomplish and let it work out how. Orienting our design around functions (because that's what functional programming is all about), we can say two things:

;; * We want to turn a list of numbers into a list of strings

;; * We have a function that can turn a single number into a string

;; Once we're more experienced, we'll know that there's actually a function that turns a list of one thing into a list of another thing by applying a function that turns one thing into another thing to each element in the list.

;; ## map

;; That function is called `map`, and it is one of the cornerstones of functional programming. Here's how we use it:

(def numbers [1 2 3 4 5])

(map str numbers)

;; `map` takes two arguments:

;; * The function that turns one thing into another; in this case, `str`, which turns anything into a string

;; * The list of things; in this case, `numbers`

;; We don't have to care about **how** `map` works, we just care about **what** it does.

;; Time for another bit of jargon: *higher order function*. A higher order function is a function that takes another function as an argument (or returns a function, but we'll come back to that later). `map` is a higher order function because it takes a transform function (`str`, in our example) and uses it on a list.

;; ## filter

;; Another higher order function that is an important part of any functional programmer's toolkit is `filter`. We use `filter` whenever we want to keep only certain elements of a list, depending on a condition that we specify.

;; Let's return to our numbers from the previous example, and ignite in our hearts the desire to keep only the even ones. Imperatively, we could say:

;; ```
;; numbers = [1, 2, 3, 4, 5]
;; strings = []
;; for (i = 0; i < numbers.length; i++) {
;;   if (numbers[i] % 2 == 0) {
;;     strings += numbers[i]
;;   }
;; }
;; ```

;; Understanding what this code is doing at a glance is even harder than when were just converting numbers to strings.

;; Using `filter`, we can again focus on the **what** instead of the **how**:

(filter even? numbers)

;; `filter` is a function taking two arguments:

;; * A *predicate*, which is a function that takes one argument and returns true or false (in this case, the `even?` function, which returns true if a number is even and false otherwise)

;; * A list of things

;; What `filter` does is apply the predicate to each member of the list, and keeps only the ones for which the predicate returns true.

;; ## reduce

;; `map` and `filter` both take a list and return a list. `map` takes a list and returns a list of the same size, whereas `filter` takes a list and returns a list containing between 0 and the same number of elements as the original list.

;; But what about when you want to squish a list into a single value? Enter `reduce`, the meanest of the higher order functions.

;; Casting our thoughts back to our eternal list of numbers, we are overcome with a sudden desire to sum them all up. We know exactly how to write this with a `for` loop:

;; ```
;; numbers = [1, 2, 3, 4, 5]
;; result = 0
;; for (i = 0; i < numbers.length; i++) {
;;   result += numbers[i]
;; }
;; ```

;; What are we actually doing here? We're starting with a sum of zero, then walking through the list, adding each element to the current sum. And hoping we're not off by one. ;)

;; This is exactly what `reduce` does:

(reduce + 0 numbers)

;; `reduce` takes three arguments:

;; * A function which takes two arguments (in this case, the `+` function, which adds numbers together)

;; * An initial value (in this case, `0`, which is the identity for addition)

;; * A list of things (in this case, our numbers)

;; What reduce does is walks through the list, calling the function with the current value (which starts out as the initial value argument) and the current element of the list. What that function returns becomes the new current value for the next list element, and when the list has no more elements, `reduce` returns the current value.

;; So this is what `reduce` is doing behind the scenes to sum up our numbers:

(+ 0 1)

(+ 1 2)

(+ 3 3)

(+ 6 4)

(+ 10 5)

;; ## Tying it all together

;; To prove to you that functional programming is ready for production, let's solve the following problem: given a new list of numbers (our old one was getting, well, old), let's sum up the number of digits in all the even ones!

;; In the imperative style, the `for` loop once again leads to salvation:

;; ```
;; numbers = [123, 456, 789, 101112, 131415]
;; result = 0
;; for (i = 0; i < numbers.length; i++) {
;;   if (numbers[i] % 2 == 0) {
;;     result += numbers[i].toString().length()
;;   }
;; }
;; ```

;; Thinking functionally leads us to the realisation that we have higher order functions that do each of the three steps:

;; * Filter out the odd numbers: `filter` with `even?`

;; * Convert the remaining numbers to the number of digits in each number: `map` with, um, something?

;; * Squish that list of numbers of digits into a single sum: `reduce` with `+`

;; All we're missing is an elusive function to count the number of digits in a number. That function is trivial to write, though. In fact, our imperative instincts showed us the way:

(defn num-digits [number]
  (count (str number)))

;; `count` is a Clojure function that returns the length of a string, or a list, or anything else that can reasonably be counted (for Clojure's definition of reasonable, which may differ from yours).

;; Given this last piece, we can solve the problem in a functional way:

(def numbers [123 456 789 101112 131415])

(reduce + 0 (map num-digits (filter even? numbers)))

;; One thing you might notice and even complain about is that this code reads right to left; we're saying "`reduce` the results of `map`ping over a `filter`ed list of numbers". It may be preferable to a human reader to list the things in the order in which they happen, in which case Clojure has a treat for you!

(->> numbers
     (filter even?)
     (map num-digits)
     (reduce + 0))

;; `->>` is the so-called **thread-last** macro. What it does is rewrites your code (behind the scenes, don't worry) so that the result of the first thing is fed as the last argument to the next thing, which is in turn fed as the last argument to the next thing, and so on. In this case, the result of evaluating `numbers`, which is itself, is fed as the last argument to the `filter` function call, thus becoming `(filter even? numbers)`. The result of this becomes the last argument of the `map` function call: `(map num-digits (filter even? numbers))`. The result of that is fed to our final function call, `reduce`: `(reduce + 0 (map num-digits (filter even? numbers)))`, which is exactly what we wrote in the beginning.

;; With the thread-last macro, you can finally overcome the last argument against Clojure: it has too many parenthesis at the end of some lines. We reduced those line-ending parens by 33%, which I think is a very compelling improvement!

;; ## You seem to avoid loops like the plague

;; It's not that loops are intrinsically evil, it's just that we have other ways of accomplishing what imperative programmers typically use loops for. As a nice added bonus, our intent becomes more clear when shed of the `for (i = 0; i < ...)` baggage.

;; Here's what I mean. See if you can tell me what this bit of code is doing:

;; ```
;; result = 5
;; for (i = 4; i > 0; i--)
;;   result \*= i
;; ```

;; If we squint really hard, we can just about make out the hazy outline of `5!` (5 factorial). To compute the factorial of any number, we compute the product of all positive integers less than or equal to it. That's what we did above: `5 * 4 * 3 * 2 * 1`.

;; Let's try this with a function-first approach. We know that the factorial of any number is that number multiplied with one number less than it, multiplied by one number less than it, and so on all the way down to 1, which is the smallest positive number.

;; How can we express this with a function?

(defn factorial [n]
  (* n :???))

;; What should we multiply `n` with in this case? Why `n - 1`, of course! Let's try that:

(defn factorial [n]
  (* n (dec n)))

;; `dec` is a function that decrements a number, and is the idiomatic way to write `(- n 1)` in Clojure. (`dec` has an evil twin called `inc`, which increments a number.)

(factorial 2)

;; Seems to work, right?

(factorial 5)

;; Weeell, not really. It worked for 2, but not any other number. We forgot the "and so on all the way down to 1" part of the algorithm. If we think about this for a second, we can figure something nifty out using only algebra. `5!` is defined like so:

;; `5 * 4 * 3 * 2 * 1`

;; And `4!` like so:

;; `4 * 3 * 2 * 1`

;; And `3!`:

;; `3 * 2 * 1`

;; Wait, this is starting to look pretty similar. Let's try adding some grouping parens (since we're doing infix anyway):

;; `5! => 5 * (4 * 3 * 2 * 1) => 5 * 4!`

;; If we keep going, we can see that:

;; ```
;; 4! => 4 * (3 * 2 * 1) => 4 * 3!
;; 3! => 3 * (2 * 1) => 3 * 2!
;; 2! => 2 * 1!
;; 1! => 1
;; ```

;; In plain English, this means that the factorial of `n` is equal to `n` multiplied by the factorial of `n - 1`. And **that** we know how to write!

(defn factorial [n]
  (* n (factorial (dec n))))

;; This style of defining a function in terms of itself is called *recursion*, and it's the oldest trick in the book of functional programming.

;; Let's out our shiny new recursive function!

(factorial 5)

;; Oops! We kept calling `factorial` until we ran out of stack frames! We committed the cardinal sin of recursion, which is forgetting to stop. D'oh!

;; When writing a recursive function, the best way to start is by thinking of the *base case*, which is the condition that should halt recursion. In the case of our factorial algorithm, a convenient base case is `1!`, which we can define as simply returning 1. For all other cases, we proceed as before.

;; Let's see how that would look:

(defn factorial [n]
  (if (= 1 n)
    1
    (* n (factorial (dec n)))))

;; We've introduced two new Clojure functions here, so let's break them down:

;; The `=` function tests for equality. It works for numbers, strings, and lists as you'd probably expect:

(= 1 1)

(= "one" "one")

(= [1 2] [1 2])

;; Just beware of using `=` on floating point numbers (`1.27`, for example). Due to the way floating point numbers are represented by computers, things like `(= 1.27 1.27)` may not always be true!

;; The second function that we introduced is `if`, which can be thought of as an `if / else` statement in typical imperative languages. It takes three arguments:

;; * The condition to test; `(= 1 n)` here

;; * The expression to evaluate when the test returns true

;; * The expression to evaluate when the test returns false

;; Now that we understand the code we wrote, let's see if it works:

(factorial 5)

;; That's more like it!

;; ## Next time, on Functional Programming with Clojure

;; Next time, we'll do some serious practice with recursion and plumb the depths of the forbidden knowledge of functional programming.

;; [Lesson 2 ->](https://www.maria.cloud/http-text/https%3A%2F%2Fs3-eu-west-1.amazonaws.com%2Fjmglov.net%2Fteaching%2Fclojure%2Ffpwc%2Ffpwc-02.clj)
