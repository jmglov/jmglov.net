;; # Clojure for Functional Programmers 03

;; [<- Lesson 02](https://www.maria.cloud/http-text/https%3A%2F%2Fs3-eu-west-1.amazonaws.com%2Fjmglov.net%2Fteaching%2Fclojure%2Fcffp%2Fcffp-02.clj)

;; Last time on Clojure for Functional Programmers, we learned about the sequence abstractions and hurt our heads with macro madness.

;; This week, we'll get practical!

;; ## How the Clojurians Clojure in real life

;; Now that we've seen the theoretical side of Clojure's take on functional programming, let's dive into a real-world problem. There's no better way to learn than starting with one of the thorniest problems in Computer Science, so let's go!

;; ### Tic-tac-toe

;; Writing a program to play the game of tic-tac-toe may seem daunting at first, but so is climbing a mountain. And like climbing a mountain, if we start by taking one step forward, then just keep recurring, we'll eventually reach the summit.

;; So, what's a good first step? Maybe gathering requirements. Since tic-tac-toe is such a complex game, we reach out to an expert, who fortunately gives us the rules.

;; 1. Draw a 3x3 grid.

;; 2. Find two players who have the intellectual prowess to play the game. Name one of them "X", and the other "O".

;; 3. Flip an official tic-tac-toe starting coin (which has an ornate *X* on one side, and a fancy *Y* on the other). Award the first move to the player whose name matches the arcane sigil on the top of the coin.

;; 4. The first player places one of their tokens (an **X** of hearty oak, or **Y** of sturdy iron) in any open grid square, then yields to their opponent.

;; 5. The opponent places a token in an empty grid square, then yields.

;; 6. Play continues in this manner until one of our stalwart contestants builds an unbroken line of three tokens along the horizontal, vertical, or diagonal.

;; 7. The defeated player bows their head in shame and trudges off a broken person, never to approach the mystic board again.

;; At this point, we suspect that our expert may be prone to fits of intense dramatising, but never mind. We have the requirements!

;; Emboldened by our success, we press on. Being functional programmers, we immediately start thinking about what functions we will need to implement the game. But then we remember the Laws of Clojure, immutable and timeless:

;; 1. Prefer data to functions

;; 2. Prefer functions to macros

;; 3. OMG it's time to write a macro? Abandon all hope!

;; Chastened, we start our design again, this time focusing on data.

;; What data structures to we need? Well, an obvious one is the board. Given that it's a 3x3 grid, a vector of vectors will work nicely. We decide that our top-level vector will represent rows, and that it will be comprised of three vectors for the columns on the board. `nil` seems like a reasonable way to represent an empty space.

(def board [[nil nil nil] [nil nil nil] [nil nil nil]])

;; We also need to represent the players' tokens with data. We can choose the keyword `:x` for player X, and `:o` for player O.

;; Now we need the starting coin. As it has two sides, a vector will suffice.

(def coin [:x :o])

;; Finally, we need a way to represent the state of the game:

;; * The current board

;; * Which player's turn it is

;; * Which player has won the game

;; We'll think like a Clojurian and reach for a trusty map. A game starts with an empty board, nobody's turn (until we flip the coin), and no winner:

(def new-game {:board board
               :turn nil
               :winner nil})

;; As usual, we use `nil` to indicate the absence of a value.

;; At the moment, it seems like we've accomplished all we can with data, so let's move on to functions. Starting from the outside in, we identify that we need functions to:

;; * Create a game

;; * Alternate turns between players until the game is over

;; * Inform the winner of their victory

;; Let's start at the top

;; ### Create a game

;; According to our game data structure, we need three things to represent the game state: a board, a notion of whose turn it is, and a possible winner.

(do
 (declare create-board flip) ; declare tells Clojure "trust us, we'll define these things later".

 (defn create-game []
   {:board (create-board)
    :turn (flip coin)}))

;; Since we've decided that `nil` will represent the lack of a winner, and looking up a key that doesn't exist in a map will return `nil`, we don't need to add an explicit `:winner` key to our new game.

;; In order to write our `create-game` function we need to implement `create-board` and `flip`. We don't need a function to create the coin, because the `coin` data structure we've defined above will do nicely.

;; We could have use the `board` vector of vectors that we defined above directly instead of writing a `create-board` function, but it seems a bit error prone. What if we forget a column in one of the rows when typing it in? So we'll write a function.

(defn create-board []
  (repeat 3 (repeat 3 nil)))

;; Our experience leads us to mistrust magic numbers like `3` and `3`, so we should probably give them names.

(defn create-board []
  (let [num-rows 3
        num-cols 3]
    (repeat num-rows (repeat num-cols nil))))

;; Let's try it out:

(create-board)

;; You may have noticed that our original `board` data structure was a vector of vectors, and this is a sequence of sequences. That seems ok for now, as there's nothing particularly vector-y about our board (it doesn't need to grow, performance doesn't really matter, etc).

;; Now we can turn our attention to the `flip` function. In order to write it, we should first contemplate the following question: "what is flipping a coin, really?" It's nothing more than randomly selecting one of the sides. We can do that with Clojure's standard library:

(def flip (comp first shuffle))

;; Let's unpack this. `shuffle` takes a collection and returns a new collection of all the same elements, but in random order.

(shuffle [1 2 3 4 5])

;; Evaluate that a few times. You should see different results most times.

;; `first`, as we know, returns the first item of a collection. `comp` is function composition. It takes as many functions as you throw at it and returns a new function that applies those functions **from right to left**. Since `comp` returns a function, we can give it a name by using `def` alone.

;; So now we have `flip`. Let's test it:

(flip coin)

;; Having defined all the functions that `create-game` needs, let's see if it works:

(create-game)

;; If you evaluate the function a few times, you should see the value of `:turn` change occasionally.

;; ### Alternate turns between players until the game is done

;; The way this is worded gives us a clue. "Turns" is plural, and "game" is singular. We have a function that takes a collection and returns a single value: `reduce`. If we reduce over a collection of turns, we should win!

(do
 (declare play-turn)

 (def play-game (partial reduce play-turn)))

;; Remember that `partial` returns a function where its first argument is partially applied to the rest of the arguments. `reduce` needs a function, an initial value, and a collection as its arguments. Using `partial`, we give it two of three: the function `play-game` and the initial value `game`. So what are `play-game` and `game`?

;; Let's start with the easiest part. `reduce` takes 3 arguments:

;; 10. A reducing function (in this case, the as of yet to be declared `play-turn`)

;; 11. An initial value of the accumulator

;; 12. A seqable collection

;; Given that our `play-game` function plays a game, we can deduce that the initial value of the accumulator should be a new game, and we have a function for that:

(create-game)

;; The final argument to `play-game` should be a sequence of turns, which we'll defer until the end.

;; It looks like all we need to do now is to implement `play-turn`, and we're done! We know what its arguments will be, since it will be called by `reduce` and must thus adhere to the calling conventions it imposes.

(defn play-turn [game turn])

;; Here, `game` will be the current state of the game, and `turn` will be the current turn. We understand game state well already, but in order to figure out what `turn` should look like, we must pose another universal question.

;; What does it mean to play a turn? Well, according to our possibly overzealous tic-tac-toe expert, it means that the player whose turn it is places their token in an empty space.

;; So our `turn` could simply be the coordinates of a space of the board.

(defn play-turn [game [row col]])

;; And how do we know when the game is over? The rules state that the game is over when either player completes a row of three consecutive tokens in a horizontal, vertical, or diagonal line.

;; Let's write a function that captures those requirements:

(do
 (declare winner row-winner col-winner diagonal-winner)

 (defn winner [board]
   (or (row-winner board)
       (col-winner board)
       (diagonal-winner board))))

;; Each of the winner functions should return either `:x` or `:o` if **x** or **o** won the game, or `nil` if there's no winner according to the rule it's implementing. So `row-winner` should return a player if either of them has completely filled the row, or `nil` otherwise; `col-winner` should return a player if either of them has completely filled the column, or `nil` otherwise; `diagonal-winner` should return a player if either of them has completely filled the left-to-right or right-to-left diagonal, or `nil` otherwise; and `winner` itself should return a player if any of those three returned one.

;; Now it's time to write the helper functions. Let's start with `row-winner`:

(defn row-winner [rows]
  (some (fn [row]
          (let [row-set (set row)]
            (and (= 1 (count row-set))
                 (first row-set))))
        rows))

;; How does this work? `some` is a function that returns the first value in a sequence for which a function `pred` returns a truthy value (remember that all values in Clojure are truthy except for `false` and `nil`).

(some #(> % 1) (range 5))

;; returns `true` because the `>` function will return `true` for the value `2`. If we want to return the first value matching the predicate, we need to wrap the predicate in a function that returns the value itself:

(some #(and (> % 1) %) (range 5))

;; So, to find a row winner, we look for the first truthy value returned by calling a function on each row that does the following:

;; * Convert the row into a set, thus de-duplicating it

;; * If the row contains a single value (which may be either `:x`, `:o`, or `nil`), return the first (and only) member of the set

;; Let's test it:

(row-winner '((:x :x :o)
              (:o :o :o)
              (:x :x :o)))

(row-winner '((:x :x :x)
              (:o :o :x)
              (:x :x :o)))

(row-winner '((:x :x :o)
              (:o :x :o)
              (:x :o :o)))

;; We can actually use `row-winner` to find our column winner if we rotate the board 90 degrees, turning columns into rows.

(defn rotate-board [board]
  (->> (range (count board))
       (map (fn [i] #(nth % i)))
       (map (fn [f] (map f board)))))

(rotate-board '((:x :x :x)
                (:o :o :o)
                (:x :x :x)))

;; `rotate-board` does the following:

;; * Uses `range` to create a sequence of the numbers from `0` to one less than the number of rows in the board (i.e. `[0 1 2]`), which is the indexes of the rows in the board

;; * Maps this sequence of indexes over a function that returns a function that applies `nth` to whatever collection is passed to it (a higher-order anonymous function!)

;; * Maps this sequence of functions over a function that maps over board and applies the function to each row

;; If we walk through applying this to the board above, here's what we get:

(->> (range 3)
     (map (fn [i] #(nth % i)))
     (map (fn [f] (map f board))))

(->> [0 1 2]
     (map (fn [i] #(nth % i)))
     (map (fn [f] (map f board))))

(->> [#(nth % 0) #(nth % 1) #(nth % 2)]
     (map (fn [f] (map f board))))

[[(nth '(:x :o nil) 0) (nth '(:x :o nil) 1) (nth '(:x :o nil) 2)]
 [(nth '(:x :o nil) 0) (nth '(:x :o nil) 1) (nth '(:x :o nil) 2)]
 [(nth '(:x :o nil) 0) (nth '(:x :o nil) 1) (nth '(:x :o nil) 2)]]

;; Using `rotate-board`, the definition of `col-winner` is straight-forward:

(def col-winner (comp row-winner rotate-board))

;; Let's try it:

(col-winner '((:x :x :o)
              (:x :o :x)
              (:x :o :o)))

(col-winner '((:x :o :o)
              (:o :o :x)
              (:x :o :o)))

(col-winner '((:x :x :o)
              (:o :o :x)
              (:x :o :o)))

;; The final way a player can win is to complete a diagonal line. Let's start by writing a function that gets the diagonals:

(defn get-diagonals [board]
  (let [row-indexes (range (count board))
        col-indexes (range (count (first board)))]
    (->> [[row-indexes col-indexes]
          [row-indexes (reverse col-indexes)]]
         (map (fn [[rs cs]]
                (->> (interleave rs cs)
                     (partition 2)
                     (map (partial get-in board))))))))

;; `interleave` takes zero or more sequences and returns a sequence of the elements of its arguments, alternating between them:

(interleave [:a :b :c] [1 2 3])

;; `partition` takes a partition size and a sequence and returns a sequence of sequences, each containing the specified number of elements from the input sequence:

(partition 2 (range 10))

;; Let's see if `get-diagonals` works:

(get-diagonals '((:x :x :o)
                 (:x :x :x)
                 (:o :o :x)))

;; What's going wrong here? The problem is `get-in`, which works perfectly well on a vector:

(get-in [[:x :o :x]
         [:o :x :o]
         [:x :o :x]]
        [1 1])

;; But always returns `nil` when called on a list:

(get-in '((:x :x :o)
          (:x :x :x)
          (:o :o :x))
        [1 1])

;; Luckily, we can fix this by making `create-board` return a vector of vectors, rather than a list of lists, as it currently does:

(create-board)

;; Let's redefine it:

(defn create-board []
  (let [num-rows 3
        num-cols 3]
    (vec (repeat num-rows (vec (repeat num-cols nil))))))

(create-board)

;; The `vec` function takes a sequence and returns a vector containing the same elements, so `create-board` now returns a vector of vectors, and `get-in` works as expected.

;; All that we have left before we can declare a winner is to write the `diagonal-winner` function. Since `get-diagonals` effectively turns a 3x3 (or NxN, for that matter) board into two rows, we can use `row-winner` on the result:

(def diagonal-winner (comp row-winner get-diagonals))

;; Now that we have defined the sub-functions, `winner` should work:

(winner [[:x :x :x]
         [:o :o :x]
         [:o :o :x]])

(winner [[:x :o :x]
         [:o :o :x]
         [:o :o :x]])

(winner [[:x :o :x]
         [:o :x :o]
         [:o :o :x]])

;; Now we can turn our roving eye back to `play-turn`. We choose to keep things simple by ending the game on an illegal move (when a player tries to place their token in a non-empty space).

(defn play-turn [{:keys [board turn] :as game} [row col]]
  (if (get-in board [row col])
    (reduced (assoc game :winner :ILLEGAL-MOVE!))
    (let [game (-> game
                   (assoc-in [:board row col] turn)
                   (assoc :turn (if (= :x turn) :o :x)))]
      (if-let [w (winner (:board game))]
        (reduced (assoc game :winner w))
        game))))

;; ### Sequence of turns

;; We still need a sequence of turns to feed to our `play-game` function. We'll continue the theme of keeping things simple and not so realistic, and generate an endless sequence of random rows and columns that fall within the board. This is sure to generate an illegal move more often than not, but it will be fun to see how many games we have to play to get a winner. :)

(defn turns []
  (let [board (create-board)
        num-rows (count board)
        num-cols (count (first board))]
    (map (fn [_] [(rand-int num-rows) (rand-int num-cols)]) (range))))

;; Let's try generating a few turns:

(take 5 (turns))

;; If we evaluate that a few times, we should see the numbers change.

;; ### Playing a game

;; Now that we have all the ingredients, let's bake a thrilling game of tic-tac-toe!

(play-game (create-game) (turns))

;; If we evaluate that a few times (for some definition of "a few"), we should get a game with a winner!

;; ### What's next

;; Next time on Clojure for Functional Programmers, we'll return to (applied) theory and start implementing our own lisp!

;; [Lesson 04 ->](https://www.maria.cloud/http-text/https%3A%2F%2Fs3-eu-west-1.amazonaws.com%2Fjmglov.net%2Fteaching%2Fclojure%2Fcffp%2Fcffp-04.clj)

;; *Thanks to Cristoph Neuman and Nate Jones for their excellent ["Functional Design in Clojure"](http://clojuredesign.club) podcast, which inspired and informed this example.*
