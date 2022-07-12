Title: Hacking the blog: caching
Tags: clojure,blog,babashka
Date: 2022-07-11

Well, it had to come to this, didn't it? At some point in the life of every
programmer, you have to take a deep sigh, realise that you have a problem where
caching is the least bad solution, and get to it. That point in my life was
today, when I finally got tired of waiting 30 seconds for my blog to publish,
and knew that the reason it was taking so long is that my rendering process was
repeating work that had already been done every time I ran it, and that resulted
in rewriting files that made my publishing process think they had changes, and
upload them to S3. The horror!

Let me quickly sketch out how publishing my blog works. I use [S3 static website
hosting](https://docs.aws.amazon.com/AmazonS3/latest/userguide/WebsiteHosting.html),
which means that publishing my website is nothing more than uploading files to
my `jmglov.net` S3 bucket. I'm using
[Babashka](https://github.com/babashka/babashka) to manage all this, so I have a
task like this in my
[`bb.edn`](https://github.com/jmglov/jmglov.net/blob/main/bb.edn):

``` clojure
{:tasks
 {publish-blog {:doc "Publish blog"
                :depends [render-blog]
                :task (shell "aws s3 sync --delete public/blog/ s3://jmglov.net/blog/")}}}
```

The `aws sync` uses some `rsync`-like logic to only upload files that have
changed. This is good for me, as I have a bunch of images, and I don't want to
upload them over and over again, as it takes time, and eventually AWS will
charge me for the bandwidth.

Let's take a look at [how the blog is
rendered](https://github.com/jmglov/jmglov.net/blob/main/render_blog.clj):
1. Copy all of the images from `assets/` to `public/blog/assets/`
2. Copy the `style.css` file to `public/blog/`
3. Read in all of the posts from `posts.edn`
4. For each post, read the markdown source file, render it to HTML, insert it as
   the body into the [page
   template](https://github.com/jmglov/jmglov.net/blob/main/blog/templates/base.html)
   template with [Selmer](https://github.com/yogthos/Selmer), and write the
   resulting HTML file to `public/blog/`
5. Create an `archive.html` page with links to all the posts
6. Create a `tags/index.html` page with links to all of the tags
7. For each tag, create a page with links to all the posts with that tag
8. Create a top-level `index.html` page with the last three posts
9. Create an `atom.xml` RSS feed with all of the posts
10. Create a `planetclojure.xml` RSS feed with posts tagged "clojure" or
    "clojurescript"

Each one of these steps is creating a file in `public/blog` that will be
uploaded to S3 if the local file is newer than the file on S3. Without any
caching, all of these files will be created every time I render the blog, which
means they will always be uploaded, and this was what I was running into.

Here's how the asset files [used to be
handled](https://github.com/jmglov/jmglov.net/blob/a9f05b17f2459257e48c6807622df5fbc8951d5f/render_blog.clj#L32):

``` clojure
(ns render-blog
  (:require
   [babashka.fs :as fs]
   [lib]))

(def blog-dir (fs/file "blog"))
(def out-dir (fs/file "public" "blog"))
(def asset-dir (fs/create-dirs (fs/file out-dir "assets")))

(fs/copy-tree (fs/file blog-dir "assets") asset-dir
              {:replace-existing true})
```

`fs/copy-tree` is basically the same thing as `cp -r`: it copies all of the
files from `blog/assets` to `public/blog/assets`. The problem is that changes
the modification timestamp on the file, thus making `s3 sync` think it's a newer
file and upload it. What I would like to do instead is only copy the new and
modified asset files to `public/blog/assets`.

In order to do this, I wrote a new
function,
[`copy-tree-modified`](https://github.com/jmglov/jmglov.net/blob/fb1d1d28c9ef1289309cb539bd85e1ddb9400916/lib.clj#L23),
and used it like this:

``` clojure
(lib/copy-tree-modified (fs/file blog-dir "assets")
                        asset-dir
                        (.getParent out-dir))
```

Here's what the function looks like:

``` clojure
(defn copy-tree-modified [src-dir target-dir out-dir]
  (let [modified-paths (fs/modified-since (fs/file target-dir)
                                          (fs/file src-dir))
        new-paths (->> (fs/glob src-dir "**")
                       (remove #(fs/exists? (fs/file out-dir %))))]
    (doseq [path (concat modified-paths new-paths)
            :let [target-path (fs/file out-dir path)]]
      (fs/create-dirs (.getParent target-path))
      (println "Writing" (str target-path))
      (fs/copy (fs/file path) target-path))))
```

I'll walk you through what's going on here:
1. [`fs/modified-since`](https://babashka.org/fs/codox/babashka.fs.html#var-modified-since)
   returns a list of the files in `src-dir` (which in the case of my assets, is
   `blog/assets`) which have been modified since the time `target-dir`
   (`public/blog/assets`) was last modifed.
2. Since this will not pick up files that have been added to `src-dir` after
   `target-dir` was last modified, I do an
   [`fs/glob`](https://babashka.org/fs/codox/babashka.fs.html#var-glob) to get a
   list of all of the files in `src-dir`, then remove the ones that already
   exist in `target-dir`.
3. I concatenate the modified files and the new files and then `doseq` over
   them, creating subdirectories as needed, and then copy them into `out-dir`
   (`public/blog`).

This handles recursively copying directories, but how about single files? We can
take a look at how the `style.css` used to be handled:

``` clojure
(let [style-src (fs/file templates-dir "style.css")
      style-target (fs/file out-dir "style.css")]
  (fs/copy style-src style-target))
```

Now it's subtly changed to use a new `copy-modified` library function:

``` clojure
(let [style-src (fs/file templates-dir "style.css")
      style-target (fs/file out-dir "style.css")]
  (lib/copy-modified style-src style-target))
```

The
[`copy-modified`](https://github.com/jmglov/jmglov.net/blob/fb1d1d28c9ef1289309cb539bd85e1ddb9400916/lib.clj#L14)
function looks like this:

``` clojure
(defn stale? [src target]
  (seq (fs/modified-since target src)))

(defn copy-modified [src target]
  (when (stale? src target)
    (println "Writing" (str target))
    (fs/create-dirs (.getParent (fs/file target)))
    (fs/copy src target)))
```

We're using `fs/modified-since` in a slightly different way here. When both the
`target` and the `src` are files, `fs/modified-since` will notice when `src`
exists but `target` doesn't (meaning that `src` has been added since last time
we rendered). Wrapping it in a `seq` will make it return `nil` when the list of
modified files is empty, which we use as a truthy value.

The final piece of the puzzle is how to handle things like posts and archives
and tags, which should only be written when there is a new post, an updated
post, or something has changed with the rendering code or templates. I'll
illustrate this by showing how the archive page is handled:

``` clojure
(def posts-file "posts.edn")
(def rendering-system-files ["render_blog.clj" templates-dir])

(let [archive-file (fs/file out-dir "archive.html")
      new-posts? (lib/stale? posts-file archive-file)
      rendering-modified? (lib/rendering-modified? rendering-system-files
                                                   archive-file)]
  (when (or rendering-modified? new-posts?)
    (println "Writing archive page" (str archive-file))
    (spit archive-file
          (selmer/render base-html
                         {:skip-archive true
                          :title (str blog-title " - Archive")
                          :body (hiccup/html (lib/post-links {} "Archive" posts))}))))
```

To determine if there are new posts, we check if the archive file is stale with
respect to the posts file, meaning that the posts file has changed since we last
wrote the archive file.

To determine if any of the rendering code has changed, we use
[`lib/rendering-modified?`](https://github.com/jmglov/jmglov.net/blob/fb1d1d28c9ef1289309cb539bd85e1ddb9400916/lib.clj#L11):

``` clojure
(defn rendering-modified? [rendering-system-files target-file]
  (seq (fs/modified-since target-file rendering-system-files)))
```

What we're asking here is if `render_blog.clj` or any of the template files have
changed since we last wrote the archive file. If so, we want to re-render the
archive file.

If you're interested in seeing this in action, take a look at
[`render_blog.clj`](https://github.com/jmglov/jmglov.net/blob/main/render_blog.clj)
and [`lib.clj`](https://github.com/jmglov/jmglov.net/blob/main/lib.clj). Just
note that things are not very polished, and there are likely to be bugs. 😬

**Need to add fixes.**
