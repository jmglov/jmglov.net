Title: Hacking the blog: REPLing to victory
Date: 2022-07-14
Tags: clojure,blog,babashka

One of the [things I learned on Tuesday](2022-07-12-stuff-i-learned.html) was
that I had a bug in the caching code on my blog (of course there's a bug in
caching code; that is one of the few things you can count on in life). As any
Clojure programmer would, I meditated upon the words of Rich Hickey and became
enlightened. I must decomplect!

Let's look at what files are produced by my blog and what could cause them to
become stale.

- Static assets (for example, images): the file itself is modified
- Stylesheet: the file itself is modified
- Posts: one of the following is modified:
  - The entry in
     [`posts.edn`](https://github.com/jmglov/jmglov.net/blob/575a12cf2a87a4fd2a46dc131ed3a51f864ba57f/blog/posts.edn),
    corresponding to a given post, since that means that some post metadata such
    as title, date, tags, or filename may have changed
  - The Markdown file for a given post
  - A template file
  - Any Clojure file that is responsible for rendering:
    - [`render_blog.clj`](https://github.com/jmglov/jmglov.net/blob/main/render_blog.clj)
    - [`lib.clj`](https://github.com/jmglov/jmglov.net/blob/main/lib.clj)
    - The version of [Hiccup](https://github.com/weavejester/hiccup),
      [markdown-clj](https://github.com/yogthos/markdown-clj), or
      [Selmer](https://github.com/yogthos/Selmer). I had not thought of this
      before. Since this is a bit tricky, let's just use
      [`deps.edn`](https://github.com/jmglov/jmglov.net/blob/main/deps.edn) as a
      proxy for this, since all of these dependencies are specified there.
- Archive page:
  - A template file
  - The Markdown file for any post, since the title or filename may have changed
  - Any Clojure file that is responsible for rendering
- Tags index and tag-specific pages:
  - A template file
  - The Markdown file for any post, since the title, filename, or tags may have
    changed
  - Any Clojure file that is responsible for rendering
- Archive page:
  - A template file
  - The Markdown file for any post, since the title or filename may have changed
  - Any Clojure file that is responsible for rendering

This simplifies matters quite a bit, because we only have three categories here:
1. Files that only depend on themselves
2. Files that depend on themselves, the templates, and the rendering system
3. Files that depend on posts, templates, and the rendering system

Category 1 is already simple (in the Hickian sense of the word), and in fact
already works with my initial approach. Category 2 only applies to posts, which
in fact category 3 depends on, so let's focus on getting category 2 working
before we turn our attention to category 3.

Category 2 is definitely complex, since there are four separate things that
should trigger a re-render for a given post:
1. Its entry in `posts.edn`
2. Its Markdown file
3. Any template (to be on the safe side)
4. The rendering system

One obvious irritation is that the post's metadata and content come from
different files. It would be simpler if the metadata was contained in the same
file as the post, so that all we have to do to determine if the post needs to be
re-rendered is to check if the Markdown file has been modified.

Luckily, there is a solution for this!
[MultiMarkdown](https://github.com/fletcher/MultiMarkdown/wiki/MultiMarkdown-Syntax-Guide)—which
is the flavour of Markdown implemented by markdown-clj—has an affordance for
including
[metadata](https://github.com/fletcher/MultiMarkdown/wiki/MultiMarkdown-Syntax-Guide#metadata)
in a Markdown file. If you start your Markdown file like this:

```
Title: Hacking the blog: REPLing to victory
Date: 2022-07-14
Tags: clojure,blog,babashka

Content starts here.
```

you are defining `Title`, `Date`, and `Tags` metadata.

markdown-clj supports this through the gloriously named
[`md-to-html-string-with-meta`](https://github.com/yogthos/markdown-clj#metadata)
function. Whereas the normal `md-to-html-string` function returns an HTML
string, `md-to-html-string-with-meta` returns a map with keys `:metadata` and
`:html`. I'll go into more details on this in a future post, but for now, I want
to focus on the problem of moving the metadata from `posts.edn` to each Markdown
file.

There is the obvious approach of doing it manually, which I would have chosen if
I had just a few posts, but since I have 31 now, that would both take more time
than I care to spend and also be rife with opportunities for manual errors.

So automation it is! Since [Babashka](https://github.com/babashka/babashka) was
initially developed to allow us to write shell scripts in Clojure instead of
Bash (or some other icky language like Python), it's an obvious choice for
automating this. And since I'm using Clojure, I can skip the whole trial and
error thing by using a technique we call REPL-driven development.

REPL-driven development basically boils down to constantly evaluating code in
the context of our running program to try things out, rather than writing some
code, running the program, watching it fail, scratching our heads, adding some
debug print statements, re-running the program, watching it fail, reading the
debug output, realising that we need a debug print in a place we didn't think
of, adding it, re-running the program, reading the debug output, scratching our
heads, changing the code, re-running the program, watching it fail in a
different way...

If that last paragraph was tedious to read, you can imagine how tedious it is to
actually do things this way! Or more likely, you don't have to imagine, because
you've done things this way many many times.

So let me walk you through the actual REPL session I used to transform my files.

Since I'm using Emacs and [CIDER](https://docs.cider.mx/cider/index.html), I'll
be REPLing right in my editor, which is the superpower of the Clojure ecosystem.
Many languages have a primitive REPL where you can execute code, but most don't
integrate with your editor in any meaningful way (Elixir is an absolutely
delightful exception to this rule 💜).

In order to enable this goodness, I need to start a Babashka REPL:

```
bb nrepl-server 1667
```

Now, I open `lib.clj` in Emacs and run the `cider-connect` function, entering
`localhost` and `1667` when prompted for host and port. CIDER will then open a
REPL buffer and print something like this:

```
;; Connected to nREPL server - nrepl://localhost:1667
;; CIDER 1.3.0 (Ukraine), babashka.nrepl 0.0.6-SNAPSHOT
;; Babashka 0.8.156
;;     Docs: (doc function-name)
;;           (find-doc part-of-name)
;;   Source: (source function-name)
;;  Javadoc: (javadoc java-object-or-class)
;;     Exit: <C-c C-q>
;;  Results: Stored in vars *1, *2, *3, an exception in *e;
WARNING: Can't determine Clojure version.  The refactor-nrepl middleware requires clojure 1.8.0 (or newer)WARNING: clj-refactor and refactor-nrepl are out of sync.
Their versions are 3.5.2 and n/a, respectively.
You can mute this warning by changing cljr-suppress-middleware-warnings.
user> 
```

Now I can evaluate Clojure forms in the REPL!

```
user> (+ 1 2)
;; => 3
```

This is cool, but no cooler than the REPLs that I called "primitive" a minute
ago. To move from merely cool to totally awesome, I'll switch back to my
`lib.clj` file and hit `C-c C-k` (that's Emacs-speak for Control + c followed by
Control + k), which runs the `cider-load-buffer` Emacs function, which evaluates
the entire file in your running REPL process. Now I can write code in the file
and evaluate it straight away!

I'll start by writing a so-called [Rich
comment](https://betweentwoparens.com/blog/rich-comment-blocks/#rich-comment):

``` clojure
(comment

  )
```

The `comment` macro is a nice way to comment out some code in a way that allows
for [structural editing](https://practical.li/spacemacs/structural-editing/),
but it has a hidden superpower when combined with a REPL: you can evaluate code
inside the comment block, safe in the knowledge that it won't be evaluated when
the file is loaded in a real program (or when you press `C-c C-k` to re-evaluate
the entire file).

So now I'm ready to rapidly iterate. Let me just remember what I'm trying to do
again... oh yeah, move the metadata in the `posts.edn` file to the file for each
post.

I can start by loading `posts.edn` and seeing what it looks like. I have a
function called `load-posts` that loads the `posts.edn` file, so let's call it
and see what it returns. What I can do is write some code inside my comment
block:

``` clojure
(comment
  (->> (load-posts (fs/file "blog" "posts.edn"))
       first)

  )
```

and then put my cursor at the end of the line ending with `first)` and press `C-c
C-v f c e`, which runs the Emacs function
`cider-pprint-eval-last-sexp-to-comment`, which does this:

``` clojure
(comment
  (->> (load-posts (fs/file "blog" "posts.edn"))
       first)
  ;; => {:title "Some stuff I learned today",
  ;;     :file "2022-07-12-stuff-i-learned.md",
  ;;     :tags #{"waffle"},
  ;;     :date "2022-07-12"}

  )
```

What I've done here is evaluated code in a file in my editor and had the result
written right back to the file. No need to change windows, no need to copy and
paste, no need to move my eyes or engage my brain; it's all just muscle memory!

OK, so now I know what a post metadata entry looks like. In order to write that
metadata to the top of a file, I'm going to need to read in the file. Let me try
that out in my comment block and evaluate it:

``` clojure
(->> (load-posts (fs/file "blog" "posts.edn"))
     (map (fn [{:keys [file] :as post}]
            (let [contents (->> file
                                (fs/file "blog" "posts")
                                slurp)]
              (assoc post :contents contents))))
     first)
;; => {:title "Some stuff I learned today",
;;     :file "2022-07-12-stuff-i-learned.md",
;;     :tags #{"waffle"},
;;     :date "2022-07-12",
;;     :contents
;;     "Title: Some stuff I learned today\nTags: waffle\nDate: 2022-07-12\n\nToday..."}
```

Cool, that worked! Now I need to figure out which metadata I want to write to
the top of the file. From my example above, I want the title, the date, and the
tags, so let me grab them and put them into the post data structure under a
`:metadata` key:

``` clojure
(-> (->> (load-posts (fs/file "blog" "posts.edn"))
         (map (fn [{:keys [file] :as post}]
                (let [contents (->> file
                                    (fs/file "blog" "posts")
                                    slurp)
                      metadata (dissoc post :file)]
                  (assoc post
                         :contents contents
                         :metadata metadata))))
         first
         :metadata))
;; => {:title "Some stuff I learned today", :tags #{"waffle"}, :date "2022-07-12"}
```

Looking good! Now, according to the MultiMarkdown spec, metadata keys should
look like `Title: Some title here` instead of `:title "Some title here"`. I'll
try transforming the metadata to this format:

``` clojure
(-> (->> (load-posts (fs/file "blog" "posts.edn"))
         (map (fn [{:keys [file] :as post}]
                (let [contents (->> file
                                    (fs/file "blog" "posts")
                                    slurp)
                      metadata (dissoc post :file)]
                  (assoc post
                         :contents contents
                         :metadata metadata))))
         first
         :metadata
         (map (fn [[k v]] (format "%s: %s" (str/capitalize (name k)) v)))))
;; => ("Title: Some stuff I learned today" "Tags: #{\"waffle\"}" "Date: 2022-07-12")
```

Looks pretty good except for that `"Tags: #{\"waffle\"}"` bit, which is the
result of Clojure stringifying the set of tags that were in `posts.edn`. I
decide that if I encounter a metadata value that is a list or set like
`#{"thing1" "thing2" "thing3"}`, I'll transform it into a comma-delimited string
like `"thing1,thing2,thing3"`. Of course, I can never remember which function in
Clojure to use for seeing if a thing is a list or a set, so I'll try a few
things in the REPL until I find the right one:

``` clojure
(sequential? #{})
;; => false

(coll? #{})
;; => true
```

Oh right, it's `coll?` that I'm after. Armed with this knowledge, I can try my
metadata transformation once more:

``` clojure
(-> (->> (load-posts (fs/file "blog" "posts.edn"))
         (map (fn [{:keys [file] :as post}]
                (let [contents (->> file
                                    (fs/file "blog" "posts")
                                    slurp)
                      metadata (dissoc post :file)]
                  (assoc post
                         :contents contents
                         :metadata metadata))))
         first
         :metadata
         (map (fn [[k v]]
                (let [v (if (coll? v) (str/join "," v) v)]
                  (format "%s: %s" (str/capitalize (name k)) v))))))
;; => ("Title: Some stuff I learned today" "Tags: waffle" "Date: 2022-07-12")
```

Looks better than before, but I'd really like to see what it does to a post with
more than one tag. Let me see if I have any such posts:

``` clojure
(->> (load-posts (fs/file "blog" "posts.edn"))
     (map #(count (:tags %))))
;; => (1 3 2 2 3 2 3 3 6 5 1 1 2 1 1 1 5 1 2 2 1 3 3 2 1 4 2 1)
```

Sure do! Now I'll grab the first such post:

``` clojure
(->> (load-posts (fs/file "blog" "posts.edn"))
     (some #(and (> (count (:tags %)) 1) %)))
;; => {:title "Hacking the blog: caching",
;;     :file "2022-07-11-hacking-blog-caching.md",
;;     :tags #{"clojure" "blog" "babashka"},
;;     :date "2022-07-11"}
```

This `(some #(and (> (count (:tags %)) 1) %))` is a trick to get back the first
item in a collection that matches a predicate. The [`some`
function](https://clojuredocs.org/clojure.core/some) is somewhat odd in that it:

> Returns the first logical true value of (pred x) for any x in coll, else nil.

If I just use the predicate, I get this:

``` clojure
(->> (load-posts (fs/file "blog" "posts.edn"))
     (some #(> (count (:tags %)) 1)))
;; => true
```

This is not very helpful, since it just tells me what I already know, that I
have a post with more than one tag. Using `and` gives me a way to return the
thing that I found, since I know that the thing I found (a post, in this case)
is logically true in Clojure since it is not `nil` or `false`.

OK, now that I have a post with more than one tag, let me try my transformation
logic on it and make sure it works:

``` clojure
(->> (load-posts (fs/file "blog" "posts.edn"))
     (map (fn [{:keys [file] :as post}]
            (let [contents (->> file
                                (fs/file "blog" "posts")
                                slurp)
                  metadata (dissoc post :file)]
              (assoc post
                     :contents contents
                     :metadata metadata))))
     (some #(and (> (count (:tags %)) 1) %))
     :metadata
     (map (fn [[k v]]
            (let [v (if (coll? v) (str/join "," v) v)]
              (format "%s: %s" (str/capitalize (name k)) v)))))
;; => ("Title: Hacking the blog: caching"
;;     "Tags: clojure,blog,babashka"
;;     "Date: 2022-07-11")
```

Nice stuff! The next wrinkle is that some of my posts already contain metadata
(because I started adding it to my last couple of posts in preparation for this
switch), so I shouldn't overwrite it if it's already there. Let's see how I can
detect such posts:

``` clojure
(->> (load-posts (fs/file "blog" "posts.edn"))
     (map (fn [{:keys [file] :as post}]
            (let [contents (->> file
                                (fs/file "blog" "posts")
                                slurp)
                  metadata (dissoc post :file)]
              (assoc post
                     :contents contents
                     :metadata metadata))))
     (some #(and (not (re-find #"^[A-z]+: " (:contents %))) %)))
;; => {:title "Hacking the blog: caching",
;;     :file "2022-07-11-hacking-blog-caching.md",
;;     :tags #{"clojure" "blog" "babashka"},
;;     :date "2022-07-11",
;;     :contents
;;     "Well, it had to come to this, didn't it? At some point in the life...",
;;     :metadata
;;     {:title "Hacking the blog: caching",
;;      :tags #{"clojure" "blog" "babashka"},
;;      :date "2022-07-11"}}
```

OK, I know how to find posts that already have metadata, so now I'm ready to
prepend metadata to the file content only if it's not already there. I'll go
ahead and do it, and then pick a random post and have a look at it to make sure
it looks good:

``` clojure
(->> (load-posts (fs/file "blog" "posts.edn"))
     (map (fn [{:keys [file] :as post}]
            (let [contents (->> file
                                (fs/file "blog" "posts")
                                slurp)
                  metadata (dissoc post :file)
                  metadata-str
                  (->> metadata
                       (map (fn [[k v]]
                              (let [v (if (coll? v) (str/join "," v) v)]
                                (format "%s: %s" (str/capitalize (name k)) v))))
                       (str/join "\n"))
                  contents (if (re-find #"^[A-z]+: " contents)
                             contents
                             (format "%s\n\n%s" metadata-str contents))]
              (assoc post
                     :contents contents
                     :metadata metadata))))
     shuffle
     first)
;; => {:title "Story of a mediocre fan",
;;     :file "2022-06-16-story-of-a-mediocre-fan.md",
;;     :tags #{"arsenal" "stories"},
;;     :date "2022-06-16",
;;     :contents
;;     "Title: Story of a mediocre fan\nTags: arsenal,stories\nDate: 2022-06-16\n\nThe winter...",
;;     :metadata
;;     {:title "Story of a mediocre fan",
;;      :tags #{"arsenal" "stories"},
;;      :date "2022-06-16"}}
```

Sure enough, `:contents` begins with my metadata! Out of a surfeit of caution,
I'll make sure that no posts would remain that don't have metadata at the
beginning of their content:

``` clojure
(->> (load-posts (fs/file "blog" "posts.edn"))
     (map (fn [{:keys [file] :as post}]
            (let [contents (->> file
                                (fs/file "blog" "posts")
                                slurp)
                  metadata (dissoc post :file)
                  metadata-str
                  (->> metadata
                       (map (fn [[k v]]
                              (let [v (if (coll? v) (str/join "," v) v)]
                                (format "%s: %s" (str/capitalize (name k)) v))))
                       (str/join "\n"))
                  contents (if (re-find #"^[A-z]+: " contents)
                             contents
                             (format "%s\n\n%s" metadata-str contents))]
              (assoc post
                     :contents contents
                     :metadata metadata))))
     (some #(and (not (re-find #"^[A-z]+: " (:contents %))) %)))
;; => nil
```

All the pieces are now in place. The only thing left to do is actually write the
updated file contents back to the file:

``` clojure
(->> (load-posts (fs/file "blog" "posts.edn"))
     (map (fn [{:keys [file] :as post}]
            (let [contents (->> file
                                (fs/file "blog" "posts")
                                slurp)
                  metadata (dissoc post :file)
                  metadata-str
                  (->> metadata
                       (map (fn [[k v]]
                              (let [v (if (coll? v) (str/join "," v) v)]
                                (format "%s: %s" (str/capitalize (name k)) v))))
                       (str/join "\n"))
                  contents (if (re-find #"^[A-z]+: " contents)
                             contents
                             (format "%s\n\n%s" metadata-str contents))]
              (assoc post
                     :contents contents
                     :metadata metadata))))
     (map (fn [{:keys [file contents]}]
            (spit (fs/file "blog" "posts" file) contents)))
     doall)
;; => (nil
;;     nil
;;     ...
;;     nil)
```

OK, something happened, but it's a little hard to tell exactly what, given that
the [`spit`](https://clojuredocs.org/clojure.core/spit) function returns `nil`.
By the way, the reason that I added the `doall` to the end of my pipeline is
that CIDER will abbreviate the result of evaluating the expression in order to
avoid possibly writing millions of lines of text to your file if you're
processing a lot of data, and [`map`](https://clojuredocs.org/clojure.core/map)
is a lazy function, meaning that it will only execute the mapping function when
the result needs to be used. In my case, it will be used when the REPL tries to
print it out, but if CIDER truncates the result of my evaluation such that not
all results will be printed, some of my files won't be processed. That's where
[`doall`](https://clojuredocs.org/clojure.core/doall) comes in: it walks the
lazy sequence returned by `map` and forces evaluation of each value in the
sequence.

This is why you shouldn't have side effects in mapping functions in real
production code. You should use something like
[`doseq`](https://clojuredocs.org/clojure.core/doseq) instead, which exists
specifically to execute expressions with side effects. But hey, I'm
experimenting here, so I'll take the convenience of being able to shove `map` in
a pipeline and have my effects on the side, thank you very much! If I wanted to
be told what to do by my programming language, I'd be writing Haskell. 😜

OK, now that I've updated my files (in theory, anyway), let me check a few to
make sure they actually start with metadata like they should:

``` clojure
(->> (load-posts (fs/file "blog" "posts.edn"))
     (map (fn [{:keys [file] :as post}]
            (let [contents (->> file
                                (fs/file "blog" "posts")
                                slurp)]
              (subs contents 0 80))))
     (take 3))
;; => ("Title: Some stuff I learned today\nTags: waffle\nDate: 2022-07-12\n\nToday was a hig"
;;     "Title: Hacking the blog: caching\nTags: clojure,blog,babashka\nDate: 2022-07-11\n\nW"
;;     "Title: Hands off women's football\nTags: football,euro2022\nDate: 2022-07-10\n\nThe ")
```

Yes they do, and now I can declare victory, thanks to my trusty REPL and my
wonderful CIDER! 🏆
