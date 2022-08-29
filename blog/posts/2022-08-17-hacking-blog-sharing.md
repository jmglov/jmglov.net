Title: Hacking the blog: social sharing
Date: 2022-08-17
Tags: clojure,blog,babashka
Description: It's been a month and a day since I last hacked the blog, and five days since I've blogged at all! I hope the amazing preview image above makes up for my long absence.
Twitter-Handle: jmglov
Image: assets/2022-08-17-preview.png
Image-Alt: A laptop sits open on a desk next to camping gear

It appears that it has been one month and one day since [I last hacked the
blog](2022-07-15-hacking-blog-actually-caching.html). Hard to believe! It's
easier to believe that it's been ~~five~~ six days (I started this post
yesterday but didn't finish it until today 😬) since [I last
blogged](2022-08-11-dogfooding-blambda-cli-ier.html). I went camping over the
weekend, and still haven't finished putting my gear away! 😅

![A desk with a computer and a lot of camping gear spread all over it][desk]

As much fun as I've been having with the actual blogging, I must say I've been
having less fun sharing blog posts on Twitter, since when I do, the only thing I
see is a boring old URL.

![A tweet with a link to one of my blog posts which is just a URL][boring]

By contrast, when I share an excellent post about an excellent Arsenal
performance by an excellent blogger, I see an excellent preview thingy with a
picture and a title and a summary and I'm now super engaged and want to click!

![A tweet with a link to a 7amkickoff blog post with a nice image and a summary][awesome]

I want nice things too!

But luckily, since I'm the owner / operator of my blog, I can just make nice
things for myself and then have those nice things (but not eat them, because
apparently you can't have a thing and eat it too, because then you won't have it
anymore).

The first order of business is figuring out what to search for. I tried "website
thumbnail image" and found a great article by Michelle Mannering: "[How to add a
social media share card to any
website](https://dev.to/mishmanners/how-to-add-a-social-media-share-card-to-any-website-ha8)".
OK, so let's call this thingy a "share card" from now on.

According to Michelle, these are the tags that control the share card:

``` html
    <!-- Primary Meta Tags --> <!-- this is the default metadata which all websites can draw on --> 
    <title>YOUR_WEBSITE</title>
    <meta name="title" content="YOUR_HEADING">
    <meta name="description" content="YOUR_SUMMARY">

    <!-- Open Graph / Facebook --> <!-- this is what Facebook and other social websites will draw on -->
    <meta property="og:type" content="website">
    <meta property="og:url" content="YOUR_URL">
    <meta property="og:title" content="YOUR_HEADING">
    <meta property="og:description" content="YOUR_SUMMARY">
    <meta property="og:image" content="YOUR_IMAGE_URL">

    <!-- Twitter --> <!-- You can have different summary for Twitter! -->
    <meta name="twitter:card" content="summary_large_image">
    <meta name="twitter:url" content="YOUR_URL">
    <meta name="twitter:title" content="YOUR_HEADING">
    <meta name="twitter:description" content="YOUR_SUMMARY">
    <meta name="twitter:image" content="YOUR_IMAGE_URL">
```

(The article actually says `<meta property="twitter:...">`, but according to
Twitter's [Cards
documentation](https://developer.twitter.com/en/docs/twitter-for-websites/cards/guides/getting-started),
it should be `<meta name="twitter:...">`, so I'll use that instead.)

If I slap these tags into the `<head>` of my document, I should win!

But what to put in the content of these tags? Let's take them one by one, using
the 7amkickoff sharing card as a reference:
- `YOUR_HEADING`: "Summer days". This looks like the page title.
- `YOUR_URL`: ???. I guess this is the page URL.
- `YOUR_SUMMARY`: "Summer days are meant to be spent doing something quiet in
  the early morning, followed by...". OK, this is a preview of the post's
  content.
- `YOUR_IMAGE_URL`: logo with the "7". The thumbnail image (called a "featured
  image" by Wordpress and Medium, IIRC).

Now we can go page by page, filling these in as we go.

1. Index page:
   - `YOUR_HEADING`: page title
     ([quickblog](https://github.com/borkdude/quickblog)'s `:blog-title` key)
   - `YOUR_URL`: page URL (quickblog: `:blog-root` + "index.html")
   - `YOUR_SUMMARY`: let's use a description of the blog here (quickblog:
     `:blog-description`)
   - `YOUR_IMAGE_URL`: we can put a blog logo here (let's add a new
     `:blog-image` key to quickblog)
2. Archive page:
   - `YOUR_HEADING`: page title (quickblog: `:blog-title` + " - Archive")
   - `YOUR_URL`: page URL (quickblog: `:blog-root` + "archive.html")
   - `YOUR_SUMMARY`: (quickblog: "Archive - " + `:blog-description`)
   - `YOUR_IMAGE_URL`: (quickblog: `:blog-image`)
3. Tags page (i.e. the page listing all of the tags):
   - `YOUR_HEADING`: page title (quickblog: `:blog-title` + " - Tags")
   - `YOUR_URL`: page URL (quickblog: `:blog-root` + "tags/index.html")
   - `YOUR_SUMMARY`: (quickblog: "Tags - " + `:blog-description`)
   - `YOUR_IMAGE_URL`: (quickblog: `:blog-image`)
4. Tag pages (i.e. pages for individual tags with links to the posts with that
   tag):
   - `YOUR_HEADING`: page title (quickblog: `:blog-title` + " - Tag - " + tag
     name)
   - `YOUR_URL`: page URL (quickblog: `:blog-root` + "tags/{{tag}}.html")
   - `YOUR_SUMMARY`: (quickblog: "Posts tagged '{{tag}}' - " +
     `:blog-description`)
   - `YOUR_IMAGE_URL`: (quickblog: `:blog-image`)
5. Posts:
   - `YOUR_HEADING`: page title, which is the value of the post's `title`
     metadata (specified in Markdown as `Title: Something or other`, as detailed
     in the [very first Hacking the blog
     post](2022-07-14-hacking-blog-repl.html))
   - `YOUR_URL`: page URL (quickblog: `:blog-root` + "{{file}}.html"; assuming
     the post's Markdown file is called `something.md`, `file` will be
     "something")
   - `YOUR_SUMMARY`: let's add a new piece of metadata to the Markdown file
     called `Description:` (I know the article I referenced is calling it
     `YOUR_SUMMARY`, but I figure it's less surprising for this to match the
     name of the meta tags where we'll put it)
   - `YOUR_IMAGE_URL`: let's add an `Image:` metadata for this

Having figured out what to put in the meta tags, let's actually implement this!
The nice thing about my blog being powered by quickblog is that all of the
changes happen there (and are thus available to all quickblog users). Let's
start by cloning quickblog. I'll open a terminal, change to the parent directory
of my blog, and then run:

``` text
$ git clone git@github.com:borkdude/quickblog.git
```

Now I have a `quickblog` directory as a sibling of the `jmglov.net` directory
that contains my blog. In order for my blog to pick up the local changes I'm
about to make to quickblog, I need to change my dependency from using quickblog
from Github to use the local copy instead.

My `bb.edn` currently looks like this:

``` clojure
{:deps {io.github.borkdude/quickblog
        #_"You use the newest SHA here:"
        {:git/sha "1c26f244003e590863ae6bba0b25b2ba6a258ac9"}}
 ;; ...
 }
```

I'll change it to this:

``` clojure
{:deps {io.github.borkdude/quickblog {:local/root "../quickblog"}
        #_"You use the newest SHA here:"
        #_{:git/sha "1c26f244003e590863ae6bba0b25b2ba6a258ac9"}}
 ;; ...
 }
```

I left the `{:git/sha "1c26f244003e590863ae6bba0b25b2ba6a258ac9"}` bit there for
reference, but commented it out with the `#_` [reader
macro](https://clojure.org/reference/reader#_dispatch), which causes Clojure's
reader to ignore the next form. You can think of it as more or less the `/* ...
*/` style comment in languages like Java and C.

Now, any changes I make to my local quickblog directory will be reflected in my
blog when I run `bb render`.

Now that we're all set up, let's take a look at the quickblog source code and
figure out how we're going to do this. The place to start is the page template,
[`base.html`](https://github.com/borkdude/quickblog/blob/1c26f244003e590863ae6bba0b25b2ba6a258ac9/resources/quickblog/templates/base.html).
If we open it up and take a look at the `<head>` section, here's what we see:

``` html
  <head>
    <title>{{title}}</title>
    <meta charset="utf-8"/>
    <link type="application/atom+xml" rel="alternate" href="{{relative-path | safe}}atom.xml" title="{{title}}">
    <link rel="stylesheet" href="{{relative-path | safe}}style.css">
    <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.28.0/prism.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.28.0/components/prism-clojure.min.js"></script>
    {{watch | safe }}
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.28.0/themes/prism.min.css">

{% if favicon-tags %}{{favicon-tags | safe}}{% endif %}
  </head>
```

The `{% ... %}` and `{{...}}` stuff are
[Selmer](https://github.com/yogthos/Selmer) tags and variables. The `{% if ...
%}` tag includes the stuff before the `{% endif %}` if the condition is true,
and the `{{foo}}` is substituted with the value of the `foo` template variable,
or the empty string if the `foo` template variable is undefined or `nil`.

Let's add our social sharing tags below the `<% if favicon-tags %>` line (which
you may remember from the "[Hacking the blog:
favicon](2022-07-05-hacking-blog-favicon.html)" post). Since all pages have a
title, we can include those tags unconditionally:

``` html
    <!-- Social sharing (Facebook, Twitter, LinkedIn, etc.) -->
    <meta name="title" content="{{title}}">
    <meta name="twitter:title" content="{{title}}">
    <meta property="og:title" content="{{title}}">
```

We can also throw in `og:type`, since that should always be "website" for our
purposes:

``` html
    <meta property="og:type" content="website">
```

Since the template is already using `{{title}}`, we feel confident that the
quickblog rendering code is providing it. Let's move on now to the description
(`YOUR_SUMMARY`, I know, it's confusing; sorry). Let's add the tags to the
template:

``` html
{% if sharing.description %}
    <meta name="description" content="{{sharing.description}}">
    <meta name="twitter:description" content="{{sharing.description}}">
    <meta property="og:description" content="{{sharing.description}}">
{% endif %}
```

This is something new. When we include a `.` in a template variable, what we're
saying is the bit before the dot is a map which contains a field named the bit
after the dot. In this case, we expect a template variable called `sharing` to
be provided like this:

``` clojure
:sharing {:description "something"}
```

We'll wrap this whole thing in an `{% if %} ... {% endif %}` so that nothing
will be added to the template if the `sharing.description` variable is
undefined.

Let's have faith that future us will find a way to provide the
`sharing.description` variable somehow and forge on with our template. Next up
is the URL:

``` html
{% if sharing.url %}
    <meta name="twitter:url" content="{{sharing.url}}">
    <meta property="og:url" content="{{sharing.url}}">
{% endif %}
```

Again, we'll have faith in our future selves, competent programmers that we are!
The final piece of the puzzle is the image. We'll follow the same pattern, but
with one small tweak:

``` html
{% if sharing.image %}
    <meta name="twitter:image" content="{{sharing.image}}">
    <meta name="twitter:card" content="summary_large_image">
    <meta property="og:image" content="{{sharing.image}}">
    <meta property="og:image:alt" content="{{sharing.image-alt}}">
{% else %}
    <meta name="twitter:card" content="summary">
{% endif %}
```

The `og:image:alt` property is one that I tracked down in the [Open Graph
protocol documentation](https://ogp.me/), and it provides alt text for the
image, which is extremely important for making pages accessible to people using
screen readers. I highly recommend reading resources like "[Write good Alt Text
to describe
images](https://accessibility.huit.harvard.edu/describe-content-images)" to
learn more.

The `twitter:card` property has multiple options, according to Twitter's cards
documentation:
- `summary`
- `summary_large_image`
- `app`
- `player`

It does not specify what these mean. I guess Twitter needs to keep the mystery
alive! What we'll do for now is use `summary_large_image` when we have an image,
and regular old "summary" when we don't.

According to this page, Twitter has another couple of meta tags we can set:
- `twitter:site` - @username for the website used in the card footer
- `twitter:creator` - @username for the content creator / author

We might as well do that, since quickblog has a `:twitter-handle` option.

``` html
{% if sharing.author %}
    <meta name="twitter:creator" content="{{sharing.author-twitter-handle}}">
{% endif %}
{% if sharing.twitter-handle %}
    <meta name="twitter:site" content="{{sharing.twitter-handle}}">
{% endif %}
```

The reason for defining them separately is that `:twitter-handle` is the owner
of the blog, but the author of an individual post might be different, and we'll
allow that to be specified with the `Twitter-Handle:` metadata tag in the post.

OK, now we have everything taken care of in the template itself. Let's turn our
roving eye to the rendering code, starting with the index.

If we open up
[`src/quickblog/api.clj`](https://github.com/borkdude/quickblog/blob/1c26f244003e590863ae6bba0b25b2ba6a258ac9/src/quickblog/api.clj),
we'll find a
[`spit-index`](https://github.com/borkdude/quickblog/blob/1c26f244003e590863ae6bba0b25b2ba6a258ac9/src/quickblog/api.clj#L157)
function at line 157. It does some figuring out of which posts to include in the
index, then makes a call to `lib/write-page!`. This is where the template
variables are defined:

``` clojure
{:title blog-title
 :body body}
```

Looking back at our template, we want to add the following keys and values:
- `description`
- `url`
- `image`
- `author-twitter-handle`
- `twitter-handle`

All of the information we need is contained in the `opts` that are passed to the
function. Let's add the keys we need to the [destructuring
form](https://clojure.org/guides/destructuring#_associative_destructuring):

``` clojure
(defn- spit-index
  [{:keys [blog-title blog-description blog-image blog-image-alt
           blog-root twitter-handle
           posts cached-posts deleted-posts modified-posts num-index-posts
           out-dir]
    :as opts}]
```

Now we can fill in the map of template variables:

``` clojure
(lib/write-page! opts out-file
                 (base-html opts)
                 {:title blog-title
                  :body body
                  :sharing {:description blog-description
                            :author twitter-handle
                            :twitter-handle twitter-handle
                            :image (format "%s/%s" blog-root blog-image)
                            :image-alt blog-image-alt
                            :url (format "%s/index.html" blog-root)}})
```

In this case, both the author and site Twitter handles are the same, since this
is the index page of the entire blog.

There's only one thing here that is slightly worrisome: does the value of the
`:blog-root` option end in a `/` or not? quickblog's documentation is silent on
the matter, so we'd better handle both cases just to be safe. Let's add a
function to
[`internal.clj`](https://github.com/borkdude/quickblog/blob/1c26f244003e590863ae6bba0b25b2ba6a258ac9/src/quickblog/internal.clj)
to take care of this:

``` clojure
(defn blog-link [{:keys [blog-root] :as opts} relative-url]
  (when relative-url
    (format "%s%s%s"
            blog-root
            (if (str/ends-with? blog-root "/") "" "/")
            relative-url)))
```

And now we can use this in `spit-index`:

``` clojure
(defn- spit-index
  [{:keys [blog-title blog-description blog-image blog-image-alt twitter-handle
           posts cached-posts deleted-posts modified-posts num-index-posts
           out-dir]
    :as opts}]
  ;; ...
        (lib/write-page! opts out-file
                         (base-html opts)
                         {:title blog-title
                          :body body
                          :sharing {:description blog-description
                                    :author twitter-handle
                                    :twitter-handle twitter-handle
                                    :image (lib/blog-link opts blog-image)
                                    :image-alt blog-image-alt
                                    :url (lib/blog-link opts "index.html")}})))))
```

Note that we no longer need the `blog-root` key in our destructuring form, so
we've removed it to be neat and tidy.

Now onto the archive page. We see that there's a
[`spit-archive`](https://github.com/borkdude/quickblog/blob/1c26f244003e590863ae6bba0b25b2ba6a258ac9/src/quickblog/api.clj#L181)
function on line 181, so we'll do some very similar modifications there:

``` clojure
(defn- spit-archive [{:keys [blog-title blog-description
                             blog-image blog-image-alt twitter-handle
                             modified-metadata posts out-dir] :as opts}]
  ;; ...
        (lib/write-page! opts out-file
                         (base-html opts)
                         {:skip-archive true
                          :title title
                          :body (hiccup/html (lib/post-links "Archive" posts))
                          :sharing {:description (format "Archive - %s"
                                                         blog-description)
                                    :author twitter-handle
                                    :twitter-handle twitter-handle
                                    :image (lib/blog-link opts blog-image)
                                    :image-alt blog-image-alt
                                    :url (lib/blog-link opts "archive.html")}})))))
```

The tags page is now up, but there's no conveniently named `spit-tags` function,
so we'll have to figure out how this is generated. If we just search `api.clj`
for `tags`, we get a promising hit on [line
120](https://github.com/borkdude/quickblog/blob/1c26f244003e590863ae6bba0b25b2ba6a258ac9/src/quickblog/api.clj#L120):

``` clojure
(defn- gen-tags [{:keys [blog-title modified-tags posts
                         out-dir tags-dir]
                  :as opts}]
  ;; ...
      (lib/write-page! opts tags-file template
                       {:skip-archive true
                        :title (str blog-title " - Tags")
                        :relative-path "../"
                        :body (hiccup/html (lib/tag-links "Tags" posts-by-tag))})
      ;; ...
```

Ah, our old friend `lib/write-page!`. Let's rinse and repeat here:

``` clojure
(defn- gen-tags [{:keys [blog-title blog-description
                         blog-image blog-image-alt twitter-handle
                         modified-tags posts out-dir tags-dir]
                  :as opts}]
  ;; ...
      (lib/write-page! opts tags-file template
                       {:skip-archive true
                        :title (str blog-title " - Tags")
                        :relative-path "../"
                        :body (hiccup/html (lib/tag-links "Tags" posts-by-tag))
                        :sharing {:description (format "Tags - %s"
                                                       blog-description)
                                  :author twitter-handle
                                  :twitter-handle twitter-handle
                                  :image (lib/blog-link opts blog-image)
                                  :image-alt blog-image-alt
                                  :url (lib/blog-link opts "tags/index.html")}})
      ;; ...
```

`gen-tags` looks like it also handles the individual tag pages:

``` clojure
(doseq [tag-and-posts posts-by-tag]
  (lib/write-tag! opts tags-out-dir template tag-and-posts))
```

Let's drill into the `lib/write-tag!` function, defined on [line
383](https://github.com/borkdude/quickblog/blob/1c26f244003e590863ae6bba0b25b2ba6a258ac9/src/quickblog/internal.clj#L383)
of `internal.clj`:

``` clojure
(defn write-tag! [{:keys [blog-title modified-tags] :as opts}
                  tags-out-dir
                  template
                  [tag posts]]
  (let [tag-filename (fs/file tags-out-dir (tag-file tag))]
    (when (or (modified-tags tag) (not (fs/exists? tag-filename)))
      (write-page! opts tag-filename template
                   {:skip-archive true
                    :title (str blog-title " - Tag - " tag)
                    :relative-path "../"
                    :body (hiccup/html (post-links (str "Tag - " tag) posts
                                                   {:relative-path "../"}))}))))
```

Nice! There's a call to `write-page!`, so we know exactly what we need to do:

``` clojure
(defn write-tag! [{:keys [blog-title blog-description
                          blog-image blog-image-alt twitter-handle
                          modified-tags] :as opts}
                  tags-out-dir
                  template
                  [tag posts]]
  ;; ...
      (write-page! opts tag-filename template
                   {:skip-archive true
                    :title (str blog-title " - Tag - " tag)
                    :relative-path "../"
                    :body (hiccup/html (post-links (str "Tag - " tag) posts
                                                   {:relative-path "../"}))
                    :sharing {:description (format "Posts tagged \"%s\" - %s"
                                                   tag blog-description)
                              :author twitter-handle
                              :twitter-handle twitter-handle
                              :image (blog-link opts blog-image)
                              :image-alt blog-image-alt
                              :url (blog-link opts "tags/index.html")}}))
```

There's only one thing left to do: the post pages. Let's see if we can figure
out how they're rendered.

Back in `api.clj`, there's a
[`gen-posts`](https://github.com/borkdude/quickblog/blob/1c26f244003e590863ae6bba0b25b2ba6a258ac9/src/quickblog/api.clj#L89)
function at line 89. It's a bit long and scary looking, but there is a call to a
`lib/write-post!` function at [line
102](https://github.com/borkdude/quickblog/blob/1c26f244003e590863ae6bba0b25b2ba6a258ac9/src/quickblog/api.clj#L102),
so it looks like we can probably get away with leaving `gen-posts` as is and
making our changes in
[`lib/write-post!`](https://github.com/borkdude/quickblog/blob/1c26f244003e590863ae6bba0b25b2ba6a258ac9/src/quickblog/internal.clj#L353).
Let's have a look:

``` clojure
(defn write-post! [{:keys [discuss-fallback
                           cache-dir
                           out-dir
                           force-render
                           page-template
                           post-template
                           posts-dir]
                    :as opts}
                   {:keys [file title date discuss tags html]
                    :or {discuss discuss-fallback}}]
  (let [out-file (fs/file out-dir (html-file file))
        markdown-file (fs/file posts-dir file)
        cached-file (fs/file cache-dir (cache-file file))
        body (selmer/render post-template {:body @html
                                           :title title
                                           :date date
                                           :discuss discuss
                                           :tags tags})
        rendered-html (render-page opts page-template
                                   {:title title
                                    :body body})]
    (println "Writing post:" (str out-file))
    (spit out-file rendered-html)))
```

There are a few things to note here:
1. There are two `:keys` destructurings happening here. The first is our old
   friend `opts`, but the second has no name. The names of the keys look
   familiar, though. `Title:`, `Date:`, and `Tags:` are the pieces of metadata
   automatically added to new posts when we run the `bb new` command, so let's
   assume that this second set of keys is the metadata defined in the post
   itself, plus some extra metadata that quickblog attaches.
2. There's a call to `selmer/render` here, which appears to be rendering the
   body of the post. Since the `<meta>` tags we're adding go in the `<head>`
   section of the page, we can safely ignore this part.
3. There's no call to `write-page!`, but `render-page` looks pretty similar.
   Let's add our template variables there.

First, we'll add `twitter-handle` to the `opts` destructuring, give the second
argument a name, `post-metadata`, and add the `description`, `image`, and
`image-alt` keys to it:

``` clojure
(defn write-post! [{:keys [blog-root
                           twitter-handle
                           discuss-fallback
                           cache-dir
                           out-dir
                           force-render
                           page-template
                           post-template
                           posts-dir]
                    :as opts}
                   {:keys [file title date discuss tags html
                           description image image-alt]
                    :or {discuss discuss-fallback}
                    :as post-metadata}]
  ;; ...
```

Now, let's figure out what the values of the template variables should be.
`description` and `image-alt` are straightforward; it's what the post author
added as the `Description:` and `Image-Alt:` metadata in the post, so we can use
it as is.

`url` is only a bit more complicated. We can use the `blog-link` function as
usual, and the `relative-url` argument should be the name of the HTML file
corresponding to this post. We can see on [line
363](https://github.com/borkdude/quickblog/blob/1c26f244003e590863ae6bba0b25b2ba6a258ac9/src/quickblog/internal.clj#L363)
that the output file uses a function called `html-file`, which transforms the
post's `foo.md` file into `foo.html`. Just what we needed!

`twitter-handle`, which is the Twitter handle of the blog owner, can be used
straight up. For `author`, let's look first for a `twitter-handle` key in the
post metadata, and then fall back to the blog's `twitter-handle` otherwise:

``` clojure
author (-> (:twitter-handle post-metadata) (or twitter-handle))
```

Finally, we want the post's author to be able to add `Image:` metadata to the
post, which they should be able to specify either as an absolute URL or a
relative URL. We can handle that here:

``` clojure
image (when image (if (re-matches #"^https?://.+" image)
                    image
                    (blog-link opts image)))
```

Now we can just feed these keys to the `render-page` function:

``` clojure
rendered-html (render-page opts page-template
                           {:title title
                            :body body
                            :sharing (->map description
                                            author
                                            twitter-handle
                                            image
                                            image-alt
                                            url)})
```

Let's take a brief detour to look at this `->map` bit. It's a macro that lets us
define a map with keys named the same as the variables holding the values. Or in
other words, these two things are equivalent:

```
(->map description author twitter-handle image image-alt url)

{:description description
 :author author
 :twitter-handle twitter-handle
 :image image
 :image-alt image-alt
 :url url}
```

In case you're interested, the macro is defined at [line
27](https://github.com/borkdude/quickblog/blob/1c26f244003e590863ae6bba0b25b2ba6a258ac9/src/quickblog/internal.clj#L27):

``` clojure
(defmacro ->map [& ks]
  (assert (every? symbol? ks))
  (zipmap (map keyword ks)
          ks))
```

If you're interested but don't understand what's going on here, I can highly
recommend "[Mastering Clojure
Macros](https://pragprog.com/titles/cjclojure/mastering-clojure-macros/)", by
Colin Jones, or [Chapter 8](https://www.braveclojure.com/writing-macros/) of
"[Clojure for the Brave and True](https://www.braveclojure.com/)", by Daniel
Higgenbotham. You can read "Clojure for the Brave and True" for free online, but
if you can afford to show Daniel some monetary appreciation, you can order the
print version using his affiliate link: http://amzn.to/1H7MqmT.

OK, we actually have everything we need to make this work! Let's generate a new
post and test it out:

``` text
$ bb new --file test.md --title "Test post"
```

If we open up `posts/test.md`, we can add some metadata tags:

``` text
Title: Test post
Date: 2022-08-17
Tags: clojure
Twitter-Handle: jmglov
Description: This is an amazing blog post which tests the equally amazing social sharing functionality that we just added to quickblog!
Image: https://jmglov.net/test/2022-08-16-sharing-preview.png
Image-Alt: A leather-bound notebook lies open on a writing desk

Write a blog post here!
```

Now let's try things out! If we run:

``` text
$ bb watch
```

we can browse to our blog at http://localhost:1888/. We should see the index
page, and if we click on the [Test post](http://localhost:1888/test.html) link,
we can view the source of the page, look at the `<head>` section, and see:

``` html
<head>
  <!-- some boring stuff here -->

  <!-- Social sharing (Facebook, Twitter, LinkedIn, etc.) -->
  <meta name="title" content="Test post">
  <meta name="twitter:title" content="Test post">
  <meta property="og:title" content="Test post">
  <meta property="og:type" content="website">

  <meta name="description" content="This is an amazing blog post which tests the equally amazing social sharing functionality that we just added to quickblog!">
  <meta name="twitter:description" content="This is an amazing blog post which tests the equally amazing social sharing functionality that we just added to quickblog!">
  <meta property="og:description" content="This is an amazing blog post which tests the equally amazing social sharing functionality that we just added to quickblog!">

  <meta name="twitter:image" content="https://jmglov.net/test/2022-08-16-sharing-preview.png">
  <meta name="twitter:card" content="summary_large_image">
  <meta property="og:image" content="https://jmglov.net/test/2022-08-16-sharing-preview.png">
  <meta property="og:image:alt" content="A leather-bound notebook lies open on a writing desk">

  <meta name="twitter:creator" content="jmglov">
  <meta name="twitter:site" content="quickblog">
</head>
```

Awesome! But how can we know what this will look like when shared on social
media sites? Well, I've done us all the great service of uploading this page to
my website, so we can use [metatags.io](https://metatags.io/) to test it. If we
pop in https://jmglov.net/test/social-post.html to the text box at the top of
the site, we should see something like this:

![The metatags.io site showing a preview of the social sharing card for our test page][metatags]

The spectacularity of this accomplishment cannot be overstated, my friends! 🏆

In case you're a quickblog user and you want to benefit from this stuff without
having to do a bunch of typing, fear not! The latest version of quickblog
already includes this functionality. 🙂

[desk]:[assets/2022-08-17-desk.png "I'll put this away any day now." width=800px]
[boring]:[assets/2022-08-17-boring.png "Oh wow, so ex... zzz"]
[awesome]:[assets/2022-08-17-7amkickoff.png "Engagement reaching new heights!"]
[metatags]:[assets/2022-08-17-metatags.png "Spectacularity!" width=800px]
