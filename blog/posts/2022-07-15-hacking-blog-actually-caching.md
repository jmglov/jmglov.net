Title: Hacking the blog: actually caching
Tags: clojure,blog,babashka
Date: 2022-07-15

When last we left our intrepid blogger (me), he (I) had just [admitted that
there was a bug](2022-07-14-hacking-blog-repl.html) in his (my) caching code. 😢

But never fear! He (I) grabbed some [hammock
time](https://melreams.com/2017/05/rich-hickey-hammock-driven-development/),
then grabbed his (my) trusty REPL, then set about decomplecting the caching code
to make it Rich compliant. This post will detail the outcome of that
decomplecting.

OK, I'm getting tired of switching from third to first person, so let me settle
on one from here on out: second person! No, that would be horribly confusing to
you (me), so perhaps first person would be the right person.

To explain the caching strategy I landed on, let me recap the three categories
of staleness that I identified:
1. Files that only depend on themselves: assets and stylesheet
2. Files that depend on themselves, the templates, and the rendering system:
   post pages
3. Files that depend on posts, templates, and the rendering system: archive
   page, index page, tag pages, and RSS feeds

Let's start at the top of [`render_blog.clj`](https://github.com/jmglov/jmglov.net/blob/6cc42e6927b1c0c2bd8621a01a774d5185608fa1/render_blog.clj)
and walk through how we handle each of these.

## Assets and stylesheet

This has not changed from my [original caching
strategy](2022-07-11-hacking-blog-caching.html):

``` clojure
(lib/copy-tree-modified (fs/file blog-dir "assets")
                        asset-dir
                        (.getParent out-dir))

(let [style-src (fs/file templates-dir "style.css")
      style-target (fs/file out-dir "style.css")]
  (lib/copy-modified style-src style-target))
```

## Posts

Now that we don't use `posts.edn` anymore, posts are loaded like this:

``` clojure
(def posts (->> (lib/load-posts posts-dir default-metadata)
                (lib/add-modified-metadata posts-dir out-dir)))
```

[`lib/load-posts`](https://github.com/jmglov/jmglov.net/blob/6cc42e6927b1c0c2bd8621a01a774d5185608fa1/lib.clj#L114)
is similar to how it worked [back in the `posts.edn`
days](https://github.com/jmglov/jmglov.net/blob/54f030bbb04e4f07f9e6fb512bdf99ae28753fd7/lib.clj#L114),
except instead of returning a list of post metadata, it returns a list of maps
containing both the metadata and the actual rendered markdown for each post.
Let's have a look:

``` clojure
(defn load-posts
  "Returns all posts from `post-dir` in descending date order"
  [posts-dir default-metadata]
  (->> (fs/glob posts-dir "*.md")
       (map #(load-post (.toFile %) default-metadata))
       (remove
        (fn [{:keys [metadata]}]
          (when-let [missing-keys
                     (seq (set/difference required-metadata
                                          (set (keys metadata))))]
            (println "Skipping" (:file metadata)
                     "due to missing required metadata:"
                     (str/join ", " (map name missing-keys)))
            :skipping)))
       (sort-by (comp :date :metadata) (comp - compare))))
```

So instead of reading `posts.edn`, we now do the following:
* Call [`fs/glob`](https://babashka.org/fs/codox/babashka.fs.html#var-glob) to
list all Markdown files in the posts directory
* Map over these with `load-post`, which we'll look at in a minute (note that
`fs/glob` returns a list of `sun.nio.fs.UnixPath` objects, and `load-file` wants
a `java.io.File`, so we need to call `.toFile` here)
* Remove the posts that don't have the required metadata keys (`:date` and
  `:title`), since we won't be able to render them
* Sort by date, reversing the order so that we get the most recent posts first

Now let's look into
[`lib/load-post`](https://github.com/jmglov/jmglov.net/blob/6cc42e6927b1c0c2bd8621a01a774d5185608fa1/lib.clj#L102)
to see what this new post data structure looks like:

``` clojure
(defn load-post
  [file default-metadata]
  {:html (delay (markdown->html file))
   :metadata (do
               (println "Reading metadata for file:" (str file))
               (-> (slurp file)
                   md/md-to-meta
                   (transform-metadata default-metadata)
                   (assoc :file (.getName file))))})
```

The first thing that stands out here is that we're returning a map with keys
`:html` and `:metadata`, rather than returning the metadata map directly like
`posts.edn` did. Let's look first at the value of the `:metadata` key:

``` clojure
(do
  (println "Reading metadata for file:" (str file))
  (-> (slurp file)
      md/md-to-meta
      (transform-metadata default-metadata)
      (assoc :file (.getName file))))
```

We're reading in the file with
[`slurp`](https://clojuredocs.org/clojure.core/slurp), then feeding it to
markdown-clj's [`md-to-meta`](https://github.com/yogthos/markdown-clj#metadata)
function. If you recall from [Hacking the blog: REPLing to
victory](2022-07-14-hacking-blog-repl.html), we are now [adding
metadata](https://github.com/fletcher/MultiMarkdown/wiki/MultiMarkdown-Syntax-Guide#metadata)
to our Markdown files by starting the files like this:

``` markdown
Title: Hacking the blog: REPLing to victory
Date: 2022-07-14
Tags: clojure,blog,babashka

One of the [things I learned on Tuesday](2022-07-12-stuff-i-learned.html) was...
```

The `md-to-meta` function just reads in and parses the metadata, without
rendering the Markdown itself. The reason why we're decomplecting parsing metadata from
rendering Markdown here is that parsing metadata is ⚡fast⚡, whereas rendering
Markdown is (relatively) 🐢slow🐢. Also cuz Rich Hickey sez so, of course. 😉

After reading in the metadata, we transform it with
[`transform-metadata`](https://github.com/jmglov/jmglov.net/blob/6cc42e6927b1c0c2bd8621a01a774d5185608fa1/lib.clj#L63):

``` clojure
(def metadata-transformers
  {:default first
   :tags #(-> % first (str/split #",\s*") set)})

(defn transform-metadata
  [metadata default-metadata]
  (->> metadata
       (map (fn [[k v]]
              (let [transformer (or (metadata-transformers k)
                                    (metadata-transformers :default))]
                [k (transformer v)])))
       (into {})
       (merge default-metadata)))
```

`md-to-meta` returns a list of values for each key, since MultiMarkdown allows
including a key multiple times, so you can say something like:

``` markdown
Author: Some Awesome Person
Author: Some Equally Awesome Person
```

That's why our default metadata transformer is
[`first`](https://clojuredocs.org/clojure.core/first), so it turns a list of one
value into just the value.

Tags are slightly more complicated, since the way I have chosen to represent
them is a comma-delimited list. That's why the transformer first uses `first` to
get the value, then uses
[`clojure.string/split`](https://clojuredocs.org/clojure.string/split) to turn
the comma-delimited string into a list of tags, then uses
[`set`](https://clojuredocs.org/clojure.core/set) to turn that list into a set,
since the order of tags doesn't matter.

Finally, `transform-metadata` turns the list of pairs returned by
[`map`](https://clojuredocs.org/clojure.core/map) back into a hashmap, then
merges it with `default-metadata`, which I haven't talked about before, but is
passed in from `render_blog.clj` and looks like this:

``` clojure
(def default-metadata
  {:author "Josh Glover"
   :copyright "cc/by-nc/4.0"})
```

The point of this is so that I don't have to include the author and copyright at
the top of every file.

The last thing that `load-post` needs to do to the metadata is to add the post's
filename, which is useful for all sorts of reasons that we'll see later on.

OK, that covers building the metadata for a post, so now let's turn our roving
eye to the content:

``` clojure
(defn load-post
  [file default-metadata]
  {:html (delay (markdown->html file))
   :metadata (do :stuff)})
```

This [`delay`](https://clojuredocs.org/clojure.core/delay) looks kind of
interesting, but let's ignore it for now and look at
[`markdown->html`](https://github.com/jmglov/jmglov.net/blob/6cc42e6927b1c0c2bd8621a01a774d5185608fa1/lib.clj#L93)
first:

``` clojure
(defn markdown->html [file]
  (let [markdown (slurp file)]
    (println "Processing markdown for file:" (str file))
    (-> markdown
        pre-process-markdown
        (md/md-to-html-string-with-meta :reference-links? true)
        :html
        post-process-markdown)))
```

After we slurp in the file, we feed it to the [`pre-process-markdown`](https://github.com/jmglov/jmglov.net/blob/6cc42e6927b1c0c2bd8621a01a774d5185608fa1/lib.clj#L75)
function:

``` clojure
(defn pre-process-markdown [markdown]
  (-> markdown
      h/highlight-clojure
      ;; make links without markup clickable
      (str/replace #"http[A-Za-z0-9/:.=#?_-]+([\s])"
                   (fn [[match ws]]
                     (format "[%s](%s)%s"
                             (str/trim match)
                             (str/trim match)
                             ws)))
      ;; allow links with markup over multiple lines
      (str/replace #"\[[^\]]+\n"
                   (fn [match]
                     (str/replace match "\n" "$$RET$$")))))
```

This is some super-duper [borkdude](https://github.com/borkdude) magic which I
copied with pride that adds syntax highlighting to Clojure code blocks like the
one above, and also makes links a bit nicer.

After enriching the Markdown, we render it to HTML by using markdown-clj's
`md-to-html-string-with-meta` function, which returns a map with keys
`:metadata` and `:html`. Since we handle the metadata separately, all we care
about is the value of the `:html` key. The final thing we need to do is to send
it on to [`post-process-markdown`](https://github.com/jmglov/jmglov.net/blob/6cc42e6927b1c0c2bd8621a01a774d5185608fa1/lib.clj#L90) to finish the job that
`pre-process-markdown` started:

``` clojure
(defn post-process-markdown [html]
  (str/replace html "$$RET$$" "\n"))
```

OK, back to that mysterious `delay` (as an aside, my wife's name is Delyana, so
whenever I try to type "delay", my fingers produce "delya" instead, kind of like
when I try to type my friend Linus's name and always type "Linux" instead). What
`delay` does is:

> Takes a body of expressions and yields a `Delay` object that will invoke the
> body only the first time it is forced (with `force` or `deref`/`@`), and will
> cache the result and return it on all subsequent `force` calls.

The reason we want to delay evaluation of `markdown->html` is that rendering
Markdown is two orders of magnitude more expensive than simply parsing the
metadata:

``` clojure
(let [file (fs/file "blog" "posts" "2022-07-15-hacking-blog-actually-caching.md")]
  (time
   (do
     (println "Processing metadata for file" (str file))
     (-> (slurp file)
         md/md-to-meta
         (transform-metadata {})
         (assoc :file (.getName file)))))

  (time (markdown->html file)))

;; Processing metadata for file blog/posts/2022-07-15-hacking-blog-actually-caching.md
;; "Elapsed time: 1.210838 msecs"
;; Processing markdown for file: blog/posts/2022-07-15-hacking-blog-actually-caching.md
;; "Elapsed time: 841.44889 msecs"
```

If the post hasn't changed since last it was rendered, we won't need to render
it (this is a tiny lie, but more on that later), so delaying evaluation lets us
return right away but allow access to the rendered HTML when needed, as we shall
see.

Whew, that was a lot! The reason that we wanted to look at how posts are loaded
is so that we can understand how they can be cached. If we look back at
`render.clj`, we'll see that there's a second step in loading the posts:

``` clojure
(def posts (->> (lib/load-posts posts-dir default-metadata)
                (lib/add-modified-metadata posts-dir out-dir)))
```

Let's look at what
[`lib/add-modified-metadata`](https://github.com/jmglov/jmglov.net/blob/6cc42e6927b1c0c2bd8621a01a774d5185608fa1/lib.clj#L132)
is doing:

``` clojure
(defn add-modified-metadata
  "Adds :modified? to each post showing if it is new or modified more recently than `out-dir`"
  [posts-dir out-dir posts]
  (let [post-files (map #(fs/file posts-dir (get-in % [:metadata :file])) posts)
        html-file-exists? #(->> (get-in % [:metadata :file])
                                lib/html-file
                                (fs/file out-dir)
                                fs/exists?)
        new-posts (->> (remove html-file-exists? posts)
                       (map (comp :file :metadata))
                       set)
        modified-posts (->> post-files
                            (fs/modified-since out-dir)
                            (map #(str (.getFileName %)))
                            set)
        new-or-modified-posts (set/union new-posts modified-posts)]
    (map #(assoc-in %
                    [:metadata :modified?]
                    (contains? new-or-modified-posts
                               (get-in % [:metadata :file])))
         posts)))
```

OK, there is a lot going on here at first glance, but it isn't really as
complicated as it might look. Let's walk through it step by step:
1. Get the filenames of each post by grabbing the `:file` key from its metadata
2. Define a function `html-file-exists?` that constructs the filename of the
   HTML file that will be written once the post is rendered and processed by the
   [Selmer](https://github.com/yogthos/Selmer) templating system, then checks if
   that file exists
3. Create a set of new posts by removing the posts for which `html-file-exists?`
   is true
4. Create a set of modified posts by using `fs/modified-since` to grab the posts
   that have been modified more recently than the output directory, then map
   them over `#(str (.getFilename %))` to turn the path into a regular old
   filename
5. Create a set of new or modified posts by taking the union of the two sets
6. Map over the posts with a function that checks whether each post's filename
   exists in the set of new or modified files and saves the result in the post's
   metadata under the `:modified` key

So running this on a list of posts would yield something like this:

```clojure
(->> (lib/load-posts posts-dir default-metadata)
     (take 2)
     (lib/add-modified-metadata posts-dir out-dir))
;; => ({:html #<Delay@78c390f1: :not-delivered>,
;;      :metadata
;;      {:author "Josh Glover",
;;       :copyright "cc/by-nc/4.0",
;;       :tags #{"clojure" "blog" "babashka"},
;;       :preview "true",
;;       :title "Hacking the blog: actually caching",
;;       :date "2022-07-15",
;;       :file "2022-07-15-hacking-blog-actually-caching.md",
;;       :modified? true}}
;;     {:html #<Delay@7393dd51: :not-delivered>,
;;      :metadata
;;      {:author "Josh Glover",
;;       :copyright "cc/by-nc/4.0",
;;       :tags #{"clojure" "blog" "babashka"},
;;       :title "Hacking the blog: REPLing to victory",
;;       :date "2022-07-14",
;;       :file "2022-07-14-hacking-blog-repl.md",
;;       :modified? false}})
```

Nice! Now that we have a list of posts with metadata telling us whether they
have been modified, let's look at how the posts are rendered:

``` clojure
(def bodies (atom {}))  ; re-used when generating atom.xml

(doseq [post posts]
  (lib/write-post! {:page-template page-template
                    :bodies bodies
                    :discuss-fallback discuss-fallback
                    :out-dir out-dir
                    :post-template post-template
                    :posts-dir posts-dir
                    :rendering-system-files rendering-system-files}
                   post))
```

The important section of
[`lib/write-post!`](https://github.com/jmglov/jmglov.net/blob/6cc42e6927b1c0c2bd8621a01a774d5185608fa1/lib.clj#L199)
looks like this:

``` clojure
(if (or modified?
        (rendering-modified? rendering-system-files out-file))
  (let [body (selmer/render post-template {:body @html
                                           :title title
                                           :date date
                                           :discuss discuss
                                           :tags tags})
        rendered-html (render-page config page-template
                                   {:title title
                                    :body body})]
    (println "Writing post:" (str out-file))
    (spit out-file rendered-html)
    (let [legacy-dir (fs/file out-dir
                              (str/replace date "-" "/")
                              (str/replace file ".md" ""))]))
  (println file "not modified; using cached version"))
```

If the post file itself has been modified or the rendering system has been
modified, we call `selmer/render` with the `:body` template variable set to the
result of **dereferencing** the post's `:html` key. Dereferencing forces the
delayed evaluation to happen and gives us back the result, which in this case is
calling `markdown->html` on the file contents of the post.

If the post hasn't been modified, there's no need to re-render the template, so
we'll just notify the user of this fact, leave it alone, and move on with our
life.

Alright, that covers posts, so now we can move onto...

## Archive file

This one is quite a bit more straightforward: we only need to re-render the
archive page if any post has changed or the rendering system has been modified
more recently than the archive file was last rendered. Here's the code:

``` clojure
(let [archive-file (fs/file out-dir "archive.html")
      rendering-modified? (lib/rendering-modified? rendering-system-files
                                                   archive-file)]
  (if (or rendering-modified? (lib/some-post-modified posts))
    (do
      (println "Writing archive page" (str archive-file))
      (spit archive-file
            (selmer/render page-template
                           {:skip-archive true
                            :title (str blog-title " - Archive")
                            :body (hiccup/html (lib/post-links {} "Archive" posts))})))
    (println "No posts modified; skipping archive file")))
```

The cool thing to note here is that the archive page doesn't use the contents of
posts at all, only their metadata, so we never deference `:html`, thus never
force a re-render of posts. This covers the case where we've changed the name,
date, or tags of a post, since those are the only things that appear in the
archive page.

## Tags

For tags, we generate a page for each tag linking to all of the posts with that
tag, and an index file linking to each tag:

``` clojure
(def posts-by-tag (lib/posts-by-tag posts))
(def tags-dir (fs/create-dirs (fs/file out-dir "tags")))

(let [tags-file (fs/file tags-dir "index.html")
      rendering-modified? (lib/rendering-modified? rendering-system-files
                                                   tags-dir)]
  (if (or rendering-modified? (lib/some-post-modified posts))
    (do
      (println "Writing tags page" (str tags-file))
      (spit tags-file
            (selmer/render page-template
                           {:skip-archive true
                            :title (str blog-title " - Tags")
                            :relative-path "../"
                            :body (hiccup/html (lib/tag-links "Tags" posts-by-tag))}))
      (doseq [tag-and-posts posts-by-tag]
        (lib/write-tag! {:page-template page-template
                         :blog-title blog-title
                         :tags-dir tags-dir}
                        tag-and-posts)))
    (println "No posts modified; skipping tag files")))
```

These pages only need to be written if any posts have changed or if the
rendering system has been modified more recently than the `tags/` subdirectory
of the output directory. Just like with the archive page, the tags pages only
use the posts' metadata, so re-rendering these pages won't force the posts
themselves to be re-rendered.

## Index page

The top-level `index.html` page contains the *n* most recent posts (where *n =
3* for my blog), so it needs to be re-rendered if any of the *n* most recent
posts have changed or the rendering system has been modified more recently than
the index page was last written. The index page **does** use the rendered HTML
from each post, but as `delay` caches its result, posts that have been modified
will already have been written by `lib/write-post!`, so at least we won't
double-render a post. 🎉

## RSS feeds

We write two RSS feeds for the blog, one specific to [Planet
Clojure](http://planet.clojure.in/) which contains only posts tagged with
"clojure" or "clojurescript", and one containing all posts.

These feeds are just like the archive, tag, and index pages in that they only
need to be regenerated when any post has changed or when the rendering system
has been modified more recently than each RSS feed file:

``` clojure
(let [feed-file (fs/file out-dir "atom.xml")
      clojure-feed-file (fs/file out-dir "planetclojure.xml")
      clojure-posts (filter
                     (fn [{:keys [metadata]}]
                       (some (:tags metadata) ["clojure" "clojurescript"]))
                     posts)]
  (if (or (lib/rendering-modified? rendering-system-files clojure-feed-file)
          (lib/some-post-modified clojure-posts))
    (do
      (println "Writing Clojure feed" (str clojure-feed-file))
      (spit clojure-feed-file
            (atom-feed clojure-posts)))
    (println "No Clojure posts modified; skipping Clojure feed"))
  (if (or (lib/rendering-modified? rendering-system-files feed-file)
          (lib/some-post-modified posts))
    (do
      (println "Writing feed" (str feed-file))
      (spit feed-file
            (atom-feed posts)))
    (println "No posts modified; skipping main feed")))
```

Note that the Clojure feed is only re-rendered when one of the Clojure posts
changes, so me writing a post about the [Women's EURO
2022](2022-07-10-hands-off-womens-football.html) won't cause the Clojure feed to
be regenerated.

And that's all there is to it! Let's see what happens if we render our blog from
scratch:

```
[jmglov@laurana:~/Documents/code/jmglov.net]$ bb clean
[jmglov@laurana:~/Documents/code/jmglov.net]$ time bb render-blog
Reading metadata for file: blog/posts/2022-07-06-hacking-blog-categories.md
Reading metadata for file: blog/posts/2022-06-17-creating-a-blog-with-clojure.md
Reading metadata for file: blog/posts/2022-07-12-stuff-i-learned.md
[...]
Writing public/blog/assets/2022-06-29-rover-cake.jpg
Writing public/blog/assets/2022-07-10-reverse-sexism.png
Writing public/blog/assets/2022-07-10-all-for-it.png
[...]
Writing public/blog/style.css
Processing markdown for file: blog/posts/2022-07-15-hacking-blog-actually-caching.md
Writing post: public/blog/2022-07-15-hacking-blog-actually-caching.html
Processing markdown for file: blog/posts/2022-07-14-hacking-blog-repl.md
Writing post: public/blog/2022-07-14-hacking-blog-repl.html
Processing markdown for file: blog/posts/2022-07-13-omg-what-have-i-done.md
Writing post: public/blog/2022-07-13-omg-what-have-i-done.html
[...]
Writing archive page public/blog/archive.html
Writing tags page public/blog/tags/index.html
Writing tag page: public/blog/tags/football.html
Writing tag page: public/blog/tags/euro2022.html
[...]
Writing index page public/blog/index.html
Writing Clojure feed public/blog/planetclojure.xml
Writing feed public/blog/atom.xml

real	0m9.766s
user	0m8.987s
sys	0m0.116s
```

Ten whole seconds! 😭 But let's try rendering it again without making any
changes:

```
[jmglov@laurana:~/Documents/code/jmglov.net]$ time bb render-blog
Reading metadata for file: blog/posts/2022-07-06-hacking-blog-categories.md
Reading metadata for file: blog/posts/2022-06-17-creating-a-blog-with-clojure.md
Reading metadata for file: blog/posts/2022-07-12-stuff-i-learned.md
[...]
2022-07-15-hacking-blog-actually-caching.md not modified; using cached version
2022-07-14-hacking-blog-repl.md not modified; using cached version
2022-07-13-omg-what-have-i-done.md not modified; using cached version
[...]
No posts modified; skipping archive file
No posts modified; skipping tag files
None of the 3 most recent posts modified; skipping index page
No Clojure posts modified; skipping Clojure feed
No posts modified; skipping main feed

real	0m0.211s
user	0m0.153s
sys	0m0.058s
```

Even an impatient person like me is willing to wait 200 milliseconds for my blog
to render. 😉

Let's try re-running it again with changes to just one post (the one I'm typing
right now):

```
[jmglov@laurana:~/Documents/code/jmglov.net]$ time bb render-blog
Reading metadata for file: blog/posts/2022-07-06-hacking-blog-categories.md
Reading metadata for file: blog/posts/2022-06-17-creating-a-blog-with-clojure.md
Reading metadata for file: blog/posts/2022-07-12-stuff-i-learned.md
[...]
Processing markdown for file: blog/posts/2022-07-15-hacking-blog-actually-caching.md
Writing post: public/blog/2022-07-15-hacking-blog-actually-caching.html
2022-07-14-hacking-blog-repl.md not modified; using cached version
2022-07-13-omg-what-have-i-done.md not modified; using cached version
2022-07-12-stuff-i-learned.md not modified; using cached version
[...]
Writing archive page public/blog/archive.html
Writing tags page public/blog/tags/index.html
Writing tag page: public/blog/tags/football.html
Writing tag page: public/blog/tags/euro2022.html
[...]
Writing index page public/blog/index.html
Processing markdown for file: blog/posts/2022-07-14-hacking-blog-repl.md
Processing markdown for file: blog/posts/2022-07-13-omg-what-have-i-done.md
Writing Clojure feed public/blog/planetclojure.xml
Processing markdown for file: blog/posts/2022-07-11-hacking-blog-caching.md
Processing markdown for file: blog/posts/2022-07-06-hacking-blog-categories.md
Processing markdown for file: blog/posts/2022-07-05-hacking-blog-favicon.md
[...]
Writing feed public/blog/atom.xml
Processing markdown for file: blog/posts/2022-07-12-stuff-i-learned.md
Processing markdown for file: blog/posts/2022-07-10-hands-off-womens-football.md
Processing markdown for file: blog/posts/2022-07-09-story-of-a-mediocre-fan-4.md
[...]
Processing markdown for file: blog/posts/2022-06-15-summertime.md

real	0m9.701s
user	0m8.912s
sys	0m0.123s
```

Well 💩! Why is it taking nearly 10 seconds to render one post? I mean, things
start out so well:

```
[jmglov@laurana:~/Documents/code/jmglov.net]$ time bb render-blog
Reading metadata for file: blog/posts/2022-07-06-hacking-blog-categories.md
Reading metadata for file: blog/posts/2022-06-17-creating-a-blog-with-clojure.md
Reading metadata for file: blog/posts/2022-07-12-stuff-i-learned.md
[...]
Processing markdown for file: blog/posts/2022-07-15-hacking-blog-actually-caching.md
Writing post: public/blog/2022-07-15-hacking-blog-actually-caching.html
2022-07-14-hacking-blog-repl.md not modified; using cached version
2022-07-13-omg-what-have-i-done.md not modified; using cached version
2022-07-12-stuff-i-learned.md not modified; using cached version
```

but then devolve into sadness:

```
Writing index page public/blog/index.html
Processing markdown for file: blog/posts/2022-07-14-hacking-blog-repl.md
Processing markdown for file: blog/posts/2022-07-13-omg-what-have-i-done.md
Writing Clojure feed public/blog/planetclojure.xml
Processing markdown for file: blog/posts/2022-07-11-hacking-blog-caching.md
Processing markdown for file: blog/posts/2022-07-06-hacking-blog-categories.md
Processing markdown for file: blog/posts/2022-07-05-hacking-blog-favicon.md
[...]
Writing feed public/blog/atom.xml
Processing markdown for file: blog/posts/2022-07-12-stuff-i-learned.md
Processing markdown for file: blog/posts/2022-07-10-hands-off-womens-football.md
Processing markdown for file: blog/posts/2022-07-09-story-of-a-mediocre-fan-4.md
[...]
Processing markdown for file: blog/posts/2022-06-15-summertime.md
```

Oh yeah, like I said, the index page needs the rendered HTML for the three most
recent posts, which were `2022-07-15-hacking-blog-actually-caching.md`,
`2022-07-14-hacking-blog-repl.md`, and `2022-07-13-omg-what-have-i-done`. Since
`2022-07-15-hacking-blog-actually-caching.md` was actually modified, it was
rendered by `lib/write-post!`, whereas the other two files weren't modified and
thus weren't rendered until the index page tried to use them.

And then the Clojure feed needs all of the Clojure posts, which causes those to
render, and then the main feed needs all of the posts, which causes them to
render, meaning we've now rendered all of our posts. 😭

But never fear! Since we succeeded in simplifying things, we have all of the
pieces we need to fix this problem as well. However, I've been writing and
you've been reading for quite some time now, so let's leave that for another day
(and maybe another post, who knows?).
