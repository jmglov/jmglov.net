Today's post is coming pretty late in the day due to me spending a few hours
working on the final chapter of "[Story of a mediocre
fan](2022-06-16-story-of-a-mediocre-fan.html)" (not quite finished, but I'll
wrap it up in the morning), playing some Civ V (I'm running the Mongol empire
these days), actually doing the thing I'm about to write about, then watching
the last two episodes of season 2 of [Star Trek:
Picard](https://www.imdb.com/title/tt8806524/), and then watching the first half
of the opening match of the [Women's Euro 2022
tournament](https://www.uefa.com/womenseuro/). England (boo!) are leading
Austria 1-0, but at least the goalscorer was Arsenal's own Beth Mead, so I can't
be too mad. My approach to the tournament is to cheer for Sweden and then
whatever team has the most Arsenal players (with ties being broken by which team
has the most former Arsenal players). In this particular match, England have two
current players (Leah Williamson and Beth Mead), and Austria do as well (Manu
Zinsberger and Laura Wienroither), but then Austria also have a former Arsenal
player (Viki Schnaderbeck), so I'm for them in this match.

But I'm not writing about football today (or at least, doing my best not to),
I'm writing about the latest hacking I've done on the blog.

Though you can't currently tell this, each blog post has one or more categories
associated with it. This post, for example, is categorised as "clojure" and
"blog". These categories are currently only used to determine which posts should
be included in the [Planet Clojure](http://planet.clojure.in/) feed, but I
thought it would be cool to be able to browse all posts from a given category,
so I hacked it together this afternoon.

This blog already has an [archive page](archive.html) which lists all of the
posts, so my idea was to create a similar page for each category, called
something like [`category/aws.html`](category/aws.html).

The first step was to build a data structure which contains all of the posts for
each category. The post metadata lives in
[`posts.edn`](https://github.com/jmglov/jmglov.net/blob/main/blog/posts.edn),
which looks like this:

``` clojure
{:title "Blambda!"
 :file "2022-07-03-blambda.md"
 :categories #{"aws" "s3" "lambda" "clojure"}
 :date "2022-07-03"}
{:title "Dogfooding Blambda! : revenge of the pod people"
 :file "2022-07-04-dogfooding-blambda-1.md"
 :categories #{"aws" "s3" "lambda" "clojure" "blambda"}
 :date "2022-07-04"}
{:title "Hacking the blog: favicon"
 :file "2022-07-05-hacking-blog-favicon.md"
 :categories #{"clojure" "blog"}
 :date "2022-07-05"}
{:title "Hacking the blog: categories"
 :file "2022-07-06-hacking-blog-categories.md"
 :categories #{"clojure" "blog"}
 :date "2022-07-06"}
```

The post metadata is loaded in
[`render.clj`](https://github.com/jmglov/jmglov.net/blob/main/blog/render.clj)
like this:

``` clojure
(def posts (sort-by :date (comp - compare)
                    (edn/read-string (format "[%s]"
                                             (slurp "posts.edn")))))
```

This gives me a list of posts, with each post having one or more categories.
What I need for my category pages, however, is a map like this:

``` clojure
{"aws" [{:title "Blambda!"
         :file "2022-07-03-blambda.md"
         :categories #{"aws" "s3" "lambda" "clojure"}
         :date "2022-07-03"}
        {:title "Dogfooding Blambda! : revenge of the pod people"
         :file "2022-07-04-dogfooding-blambda-1.md"
         :categories #{"aws" "s3" "lambda" "clojure" "blambda"}
         :date "2022-07-04"}]
 "clojure" [{:title "Blambda!"
             :file "2022-07-03-blambda.md"
             :categories #{"aws" "s3" "lambda" "clojure"}
             :date "2022-07-03"}
            {:title "Dogfooding Blambda! : revenge of the pod people"
             :file "2022-07-04-dogfooding-blambda-1.md"
             :categories #{"aws" "s3" "lambda" "clojure" "blambda"}
             :date "2022-07-04"}
            {:title "Hacking the blog: favicon"
             :file "2022-07-05-hacking-blog-favicon.md"
             :categories #{"clojure" "blog"}
             :date "2022-07-05"}
            {:title "Hacking the blog: categories"
             :file "2022-07-06-hacking-blog-categories.md"
             :categories #{"clojure" "blog"}
             :date "2022-07-06"}]}
```

Here's how we can achieve that:

``` clojure
(def posts-by-category
  (->> posts
       (sort-by :date)
       (mapcat (fn [{:keys [categories] :as post}]
                 (map (fn [category] [category post]) categories)))
       (reduce (fn [acc [category post]]
                 (update acc category #(conj % post)))
               {})))
```

The `mapcat` step takes each post, which looks like this:

``` clojure
{:title "Hacking the blog: categories"
 :file "2022-07-06-hacking-blog-categories.md"
 :categories #{"clojure" "blog"}
 :date "2022-07-06"}
```

and maps over the `:categories` list (that `{:keys [categories]}` bit is [key
destructuring](https://clojure.org/guides/destructuring#_associative_destructuring),
if you haven't seen it before), turning each category into a tuple of `[category
post]`. For this specific post, this would yield:

``` clojure
[["clojure" {:title "Hacking the blog: categories"
             :file "2022-07-06-hacking-blog-categories.md"
             :categories #{"clojure" "blog"}
             :date "2022-07-06"}]
 ["blog" {:title "Hacking the blog: categories"
          :file "2022-07-06-hacking-blog-categories.md"
          :categories #{"clojure" "blog"}
          :date "2022-07-06"}]]
```

Each post is turned inside out like this, yielding a list of lists of tuples, or
at least before the "cat" part of `mapcat` goes to work. The difference between
`map` and `mapcat` is that `mapcat` flattens the resulting list (according to
[the docs](https://clojuredocs.org/clojure.core/mapcat), it "returns the result
of applying concat to the result of applying `map` to `f` and `colls`", but I
like my explanation better), so instead of a list of lists of tuples, I get a
list of tuples.

I reduce that list with this function, initialising `acc` to an empty map:

``` clojure
(fn [acc [category post]]
  (update acc category #(conj % post)))
```

For each entry in the list, the key in `acc` corresponding to the category is
updated by adding the current post to the end of the list of posts with that
category.

Now that I have the data structure I need, let's see how the archive page is
currently built:

``` clojure
;;;; Generate archive page

(defn post-links []
  [:div {:style "width: 600px;"}
   [:h1 "Archive"]
   [:ul.index
    (for [{:keys [file title date preview]} posts
          :when (not preview)]
      [:li [:span
            [:a {:href (str/replace file ".md" ".html")}
             title]
            " - "
            date]])]])

(spit (fs/file out-dir "archive.html")
      (selmer/render base-html
                     {:skip-archive true
                      :title (str blog-title " - Archive")
                      :body (hiccup/html (post-links))}))
```

I can do something very similar to build my category pages:

``` clojure
(defn category-links [category posts]
  [:div {:style "width: 600px;"}
   [:h1 (str "Category - " category)]
   [:ul.index
    (for [{:keys [file title date preview]} posts
          :when (not preview)]
      [:li [:span
            [:a {:href (str "../" (str/replace file ".md" ".html"))}
             title]
            " - "
            date]])]])

(def categories-dir (fs/create-dirs (fs/file out-dir "category")))

(doseq [[category posts] posts-by-category
        :let [category-slug (str/replace category #"[^A-z0-9]" "-")]]
  (spit (fs/file categories-dir (str category-slug ".html"))
        (selmer/render base-html
                       {:skip-archive true
                        :title (str blog-title " - Category - " category)
                        :relative-path "../"
                        :body (hiccup/html (category-links category posts))})))
```

Since I decided to put my category pages under the `category/` path, I need to
adjust all of the links to go up one level. This required adding the
`:relative-path "../"` to the list of variables passed to
[Selmer](https://github.com/yogthos/Selmer) when rendering
[`templates/base.html`](https://github.com/jmglov/jmglov.net/blob/main/blog/templates/base.html)
and updating the template with stuff like:

``` html
<link rel="stylesheet" href="{{relative-path | safe}}style.css">
```

Once I did that, all that was left to do was `bb publish`, and now you can enjoy
all of the posts about this blog here: https://jmglov.net/blog/category/blog.html

You can see all of the changes required to implement this here: https://github.com/jmglov/jmglov.net/commit/6ea911d2b4c0418d01e74e4aceb2686a3f1b86a3

This is obviously a work in progress feature. It would be nice to do the following:
- Display the categories each post is tagged with
- Rename "category" to "tag", since that more properly describes the concept
- Link the tags on the post to each tag's archive page
- Add a page somewhere listing all of the tags and linking to the archive pages
  for each

We'll see what I get around to that stuff. 😉
