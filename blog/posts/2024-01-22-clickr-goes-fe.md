Title: clickr goes frontend
Date: 2024-01-22
Tags: clojure,clojurescript,clickr,scittle,clonejure
Description: In which I get serious about CSS and stuff
Image: assets/2024-01-22-preview.jpg
Image-Alt: A line of astronauts standing on the moon with the sun mirrored in their visors
Discuss: https://clojurians.slack.com/archives/C8NUSGWG6/p1705914388678899

![A cartoon version of the Drake hotline bling meme: Drake is disgusted by the
S3 console and delighted by the Flickr UI][drake]
[drake]: assets/2024-01-22-drake.png "How many Clojurians does it take to clone Flickr?" width=800px

Previously on this blog, [my heart was filled with sadness as I realised that
archiving photos to S3 wasn't actually a Flickr
replacement](2024-01-17-clickr.html). Currently on this blog, my heart is filled
with a steely resolve as I take my destiny into my own hands and... I guess
write some CSS or something?

As cartoon Drake so helpfully points out above, the S3 console is missing a few
of the features that you would expect from an online photo album:
- A highlighted image for the album
- The album's name and description
- Visual display of the photos in the album

Clearly something must be done about this! Luckily for me, an S3 bucket can
actually be used to serve up a website—in fact, this very blog is coming to you
live from S3—so if I can just write some HTML alongside the photos, Robert
should be one of my parents' siblings. If you cast your mind back to last week,
you may recall that in the process of backing up albums from Flickr, we wrote
some code that had a pretty nice data representation of an album:

``` clojure
(comment

  (->> (get-albums ctx)
       first
       (download-album! ctx))
  ;; => {:id "72177720314024335",
  ;;     :title "clickr demo",
  ;;     :description "Photo album demo for my clickr blog post",
  ;;     :photos
  ;;     ({:description nil,
  ;;       :date-taken nil,
  ;;       :geo-data nil,
  ;;       :rotation -1,
  ;;       :width 0,
  ;;       :title "sean-hargreaves-phoenix-new-5-final-a",
  ;;       :filename "53460147147.jpg",
  ;;       :id "53460147147",
  ;;       :object
  ;;       #object[com.flickr4java.flickr.photos.Photo 0x4a25d150 "com.flickr4java.flickr.photos.Photo@14ea992b"],
  ;;       :height 0}
  ;;      ;; [...]
  ;;      {:description nil,
  ;;       :date-taken nil,
  ;;       :geo-data nil,
  ;;       :rotation -1,
  ;;       :width 0,
  ;;       :title "daniel-jennings-img-7554",
  ;;       :filename "53460151727.jpg",
  ;;       :id "53460151727",
  ;;       :object
  ;;       #object[com.flickr4java.flickr.photos.Photo 0x3fb5100f "com.flickr4java.flickr.photos.Photo@436e36e8"],
  ;;       :height 0}),
  ;;     :object
  ;;     #object[com.flickr4java.flickr.photosets.Photoset 0x2be46a3b "com.flickr4java.flickr.photosets.Photoset@2be46a3b"]}

  )
```

This is a fantastic starting point, because if Clojure is good at anything, it's
transforming data from one shape to another, and HTML is just data. Let's take a
closer look at what this album looks like in Flickr, and see if we can't
identify a basic layout to ~~steal~~ borrow with pride:

![The Flickr site, displaying the photos in the clickr demo album][flickralbum]
[flickralbum]:[assets/2024-01-17-album-flickr.png "OMG 🤩" width=800px]

If we were to lay this out in HTML, it could look something like this:

``` html
<body id="body">
    <div id="album">
        <div id="back">⬅ Back to albums list</div>
        <div id="album-header">
            <div id="album-title">clickr demo</div>
            <div id="album-description">Photo album demo for my clickr blog post</div>
        </div>
        <div id="photos">
            <img id="photo-53460147147" src="53460147147.jpg" />
            <!-- ... -->
        </div>
    </div>
</body>
```

You only have to squint at the Clojure data structure a little bit to see how it
could be massaged into this shape. So let's get massaging!

## Damn the hiccups, full speed ahead!

One of the classic ways to turn Clojure data into HTML is
[Hiccup](https://github.com/weavejester/hiccup), which I really like and have
used quite a bit. However, in the production of this blog, I got introduced to
[Selmer](https://github.com/yogthos/Selmer), which is the template system that
[quickblog](https://github.com/borkdude/quickblog) uses to render HTML, and is
so cool that it is one of the batteries included in
[Babashka](https://babashka.org/). All of this is to say: let's use Selmer here!
I can actually just take the HTML fragment above and Selmerise it with only a
few keystrokes!

``` html
<body id="body">
    <div id="album">
        <div id="back">⬅ Back to albums list</div>
        <div id="album-header">
            <div id="album-title">{{album.title}}</div>
            <div id="album-description">{{album.description}}</div>
        </div>
        <div id="photos">
            {% for photo in album.photos %}
            <img id="photo-{{photo.id}}" src="{{photo.filename}}" />
            {% endfor %}
        </div>
    </div>
</body>
```

OK, that was easy. But of course a `<body>` does not an HTML page make, so let's
create a `resources/templates/album.html` file and throw in the rest of the
stuff that we need:

``` html
<!doctype html>
<html class="no-js" lang="en">

<head>
    <title>{{album.title}}</title>
    <meta charset="utf-8">
    <meta http-equiv="x-ua-compatible" content="ie=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>

<body id="body">
    <!--[if lt IE 8]>
          <p class="browserupgrade">
          You are using an <strong>outdated</strong> browser. Please
          <a href="http://browsehappy.com/">upgrade your browser</a> to improve
          your experience.
          </p>
        <![endif]-->
    <div id="album">
        <div id="back">⬅ Back to albums list</div>
        <div id="album-header">
            <div id="album-title">{{album.title}}</div>
            <div id="album-description">{{album.description}}</div>
        </div>
        <div id="photos">
            {% for photo in album.photos %}
            <img id="photo-{{photo.id}}" src="{{photo.filename}}" />
            {% endfor %}
        </div>
    </div>
</body>

</html>
```

This is actually all we need to make an awesome photo album. Let's write the
Clojure code that will write the HTML that will make the browser happy!

First, we need to add Selmer to our `deps.edn`:

``` clojure
{:paths ["src" "dev"]
 :deps {babashka/fs {:mvn/version "0.4.19"}
        com.cognitect.aws/api {:mvn/version "0.8.686"}
        com.cognitect.aws/endpoints {:mvn/version "1.1.12.504"}
        com.cognitect.aws/s3 {:mvn/version "848.2.1413.0"}
        com.flickr4java/flickr4java {:mvn/version "3.0.1"}
        selmer/selmer {:mvn/version "1.12.59"}}}
```

Then let's create a namespace that will be responsible for producing
HTML:

``` clojure
(ns clickr.html
  (:require [selmer.parser :as selmer]))
```

Then we can write a function that turns an album into the HTML representation of
said album:

``` clojure
(defn album->html [_ctx album]
  (selmer/render (slurp "resources/templates/album.html")
                 {:album album}))
```

Yes, it really is that simple! But don't take my word for it; ask the REPL!

``` clojure
(comment

  (require '[clickr.flickr :as flickr])
  ;; => nil

  (def config {:api-key "beefface5678910"
               :secret "facecafe1234"
               :s3-bucket "photos.jmglov.net"
               :s3-prefix "clickr"
               :out-dir "/home/jmglov/Pictures/clickr"})
  ;; => #'clickr.html/config

  (def ctx (flickr/init-client config))
  ;; => #'clickr.html/ctx

  (def album (->> (flickr/get-albums ctx) first (flickr/download-album ctx)))
  ;; => #'clickr.html/album

  (album->html ctx album)
  ;; => "<!doctype html>\n<html class=\"no-js\" lang=\"en\">\n\n<head>\n    <title>clickr demo</title>\n ... \n</body>\n\n</html>\n"

  )
```

Of course, all of this HTML isn't very useful unless we write it somewhere a
browser can find it. Let's follow the same pattern we used for
`download-album!`: we'll take the data representation of an album, write an
`index.html` in its output directory (assuming it has already been downloaded),
and then assoc the location of the HTML file into the album.

Since we're building paths, let's require in our old friend `babashka.fs`:

``` clojure
(ns clickr.html
  (:require [babashka.fs :as fs]
            [selmer.parser :as selmer]))
```

Now we have everything we need to write our function.

``` clojure
(defn write-album-html! [ctx {:keys [out-dir] :as album}]
  (when-not out-dir
    (throw (ex-info "Album must be downloaded before writing it to HTML"
                    {:album album})))
  (let [html (album->html ctx album)
        html-file (fs/file out-dir "index.html")]
    (spit html-file html)
    (assoc album :html-file html-file)))

(comment

  (write-album-html! ctx album)
  ;; => {:id "72177720314024335",
  ;;     :title "clickr demo",
  ;;     :description "Photo album demo for my clickr blog post",
  ;;     :photos (...)
  ;;     :object
  ;;     #object[com.flickr4java.flickr.photosets.Photoset 0x2e486856 "com.flickr4java.flickr.photosets.Photoset@2e486856"],
  ;;     :out-dir #object[java.io.File 0x68210928 "/tmp/72177720314024335"],
  ;;     :html-file
  ;;     #object[java.io.File 0x6c47b5ea "/tmp/72177720314024335/index.html"]}

  )
```

If we open up `/tmp/72177720314024335/index.html` in a web browser, we are sure
to be greeted with a glorious sight!

![A webpage with three lines of text and no photos][disappointment]
[disappointment]: assets/2024-01-22-disappointment.png "This is not the album you're looking for" width=800px

OK, so that was a crushing disappointment. 🙁

Remember how amazing this looks in Flickr? Let's use the browser's inspector to
peek behind the curtain and see how Flickr does it:

![Web browser inspector with a photo div highlighted on Flickr][magic]
[magic]: assets/2024-01-22-flickr-magic.png "Pay full attention to the CSS behind the curtain" width=800px

Ah, so they're not using `<img>` tags at all; they're using `<div>` tags with
some magic
[background-image](https://developer.mozilla.org/en-US/docs/Web/CSS/background-image)
CSS property. So CSS is the key to this whole thing, eh? People, it looks like
we're gonna need a stylesheet!

## Getting stylish

Doing some clever reverse engineering of Flickr, we whip up the following
stylesheet:

``` css
body {
  font-family: Proxima Nova,helvetica neue,helvetica,arial,sans-serif;
}

#album {
  box-sizing: border-box;
  margin-left: auto;
  margin-right: auto;
}

#album-header {
  align-items: center;
  background-color: #000;
  background-position: 50%;
  background-size: cover;
  color: #fff;
  display: flex;
  flex-direction: column;
  height: 300px;
  justify-content: center;
  position: relative;
  text-shadow: 0 1px 1px #000;
}

#album-header > div {
  color: #ffffff;
  font-weight: 300;
  overflow: hidden;
  text-align: center;
  text-overflow: ellipsis;
  text-shadow: 0 1px 1px #000000;
}

#album-title {
  font-size: 2em;
  white-space: nowrap;
}

#album-description {
  font-size: 24px;
  font-style: italic;
  line-height: 29px;
  margin-top: 13px;
  max-height: 29px;
  word-wrap: break-word;
}

#photos {
  position: relative;
}

.photo {
  background-position: 50%;
  background-repeat: no-repeat;
  background-size: cover;
  position: absolute;
}
```

Now we need to write this to the album directory next to `index.html`. Let's
drop this CSS into `resources/templates/style.css` and write an `album->css`
function. Whilst `style.css` doesn't actually contain any template variables,
maybe it could one day, so let's just run it through Selmer, which is basically
a no-op on files not containing template variables.

``` clojure
(defn album->css [_ctx album]
  (selmer/render (slurp "resources/templates/style.css")
                 {:album album}))
```

Hrm... this looks exactly the same as `album->html` except for the filename, so
let's refactor a tiny bit:

``` clojure
(defn apply-album-template [_ctx template-file album]
  (selmer/render (slurp template-file)
                 {:album album}))

(defn album->html [ctx album]
  (apply-album-template ctx "resources/templates/album.html" album))

(defn album->css [ctx album]
  (apply-album-template ctx "resources/templates/style.css" album))
```

Now we just need to plug this into `write-album-html!` to write `style.css` into
the album directory:

``` clojure
(defn write-album-html! [ctx {:keys [out-dir] :as album}]
  (when-not out-dir
    (throw (ex-info "Album must be downloaded before writing it to HTML"
                    {:album album})))
  (let [html (album->html ctx album)
        html-file (fs/file out-dir "index.html")
        css (album->css ctx album)
        css-file (fs/file out-dir "style.css")]
    (spit html-file html)
    (spit css-file css)
    (assoc album :html-file html-file, :css-file css-file)))
```

Oh yeah, and include the stylesheet in the HTML template:

``` html
<head>
    <title>{{album.title}}</title>
    <meta charset="utf-8">
    <meta http-equiv="x-ua-compatible" content="ie=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="style.css">
</head>
```

Let's test this out in the REPL again:

``` clojure
(comment

  (write-album-html! ctx album)
  ;; => {:id "72177720314024335",
  ;;     :title "clickr demo",
  ;;     :description "Photo album demo for my clickr blog post",
  ;;     :photos (...)
  ;;     :object
  ;;     #object[com.flickr4java.flickr.photosets.Photoset 0x2e486856 "com.flickr4java.flickr.photosets.Photoset@2e486856"],
  ;;     :out-dir #object[java.io.File 0x68210928 "/tmp/72177720314024335"],
  ;;     :html-file
  ;;     #object[java.io.File 0x4e5079c0 "/tmp/72177720314024335/index.html"],
  ;;     :css-file #object[java.io.File 0x334cab6a "/tmp/72177720314024335/style.css"]}

  )
```

Cool! If we now reload the browser on `/tmp/72177720314024335/index.html`, we
see some stuff move around and fonts turn prettier and so on, so we've got our
styles.

## div and conquer

The next thing we need to do is replace our `<img>` tags with `<div>` ones.
While we're at it, we might as well also use the first photo in the album as the
background of the album header div, since Flickr does that and it looks pretty
daggone cool!

``` html
    <div id="album-header" style="background-image: url('{{album.photos.0.filename}}')">
        <div id="back">⬅ Back to albums list</div>
        <div id="album-header">
            <div id="album-title">{{album.title}}</div>
            <div id="album-description">{{album.description}}</div>
        </div>
        <div id="photos">
            {% for photo in album.photos %}
            <div id="photo-{{photo.id}}" class="photo" style="background-image: url('{{photo.filename}}');">
            </div>
            {% endfor %}
        </div>
    </div>
```

If we re-render our album, however

``` clojure
(comment

  (write-album-html! ctx album)
  ;; => {:id "72177720314024335",
  ;;     :title "clickr demo",
  ;;     :description "Photo album demo for my clickr blog post",
  ;;     :photos (...)
  ;;     :object
  ;;     #object[com.flickr4java.flickr.photosets.Photoset 0x2e486856 "com.flickr4java.flickr.photosets.Photoset@2e486856"],
  ;;     :out-dir #object[java.io.File 0x68210928 "/tmp/72177720314024335"],
  ;;     :html-file
  ;;     #object[java.io.File 0x4e5079c0 "/tmp/72177720314024335/index.html"],
  ;;     :css-file #object[java.io.File 0x334cab6a "/tmp/72177720314024335/style.css"]}

  )
```

Two things are surprising:
1. Our first photo isn't displayed as the background of the album header div,
   despite the [Selmer
   docs](https://github.com/yogthos/Selmer?tab=readme-ov-file#variables-and-tags)
   claiming that we can index into nested data stuctures.
2. Our photos have gone from being way too big to being way too small. So small,
   in fact, that even the world's most powerful scanning tunneling microscope
   could not detect them.

The first point is only surprising because I forgot how Clojure works. To see
why, let's try out the example from the Selmer docs for ourselves:

``` clojure
(comment

  (selmer/render "{{foo.bar.0.baz}}" {:foo {:bar [{"baz" "hi"}]}})
  ;; => "hi"

  )
```

OK, so [good ol' Yogthos](https://github.com/yogthos) isn't a liar, which is
good, because I've read a lot of his stuff and believed what he was saying.
Having trusted and verified, let's do the same thing with our album:

``` clojure
(comment

  (selmer/render "{{album.photos.0.filename}}" {:album album})
  ;; => ""

  (-> album :photos first :filename)
  ;; => "53460147147.jpg"

  )
```

What gives? He's got a vector of `:bar`s in his `:foo`, and we've got a list of
`:photos` in our `:album`, so what's the difference here? 🤔

Oh wait... I said he has a vector and we have a list. Those words are different.
And not only are they different, one of them is a flat out lie! 😬

``` clojure
(comment

  (def yogthos-data {:foo {:bar [{"baz" "hi"}]}})
  ;; => #'clickr.html/yogthos-data

  (type (get-in yogthos-data [:foo :bar]))
  ;; => clojure.lang.PersistentVector

  (type (:photos album))
  ;; => clojure.lang.LazySeq

  (get-in yogthos-data [:foo :bar 0 "baz"])
  ;; => "hi"

  (get-in album [:photos 0 :filename])
  ;; => nil

  )
```

So yeah, you can't index into a lazy sequence like you can a vector. Luckily,
it's easy to turn a lazy sequence into a vector:

``` clojure
(comment

  (get-in (vec (:photos album)) [0 :filename])
  ;; => "53460147147.jpg"

  )
```

Since we've been superDRY and extracted a function to do the templating stuff,
we can make a one line change to fix this:

``` clojure
(defn apply-album-template [_ctx template-file album]
  (selmer/render (slurp template-file)
                 {:album (update album :photos vec)}))

(comment

  (write-album-html! ctx album)
  ;; => { ... }

  )

```

If we reload the page in our browser, we see a cool space station photo in the
album header, but the second problem remains: no photos!

If we inspect one of the divs, we see what's going on pretty quickly:

![The album webpage with no visible photos][oops]
[oops]: assets/2024-01-22-oops.png "Wherefore art thou, dear photos?" width=800px

All of the photo divs are 0 pixels wide by 0 pixels high. 😬

If we look back at Flickr, we see that they set a `width` and `height` style on
each photo element. We can try that just to see what happens:

``` html
        <div id="photos">
            {% for photo in album.photos %}
            <div id="photo-{{photo.id}}" class="photo"
                style="background-image: url('{{photo.filename}}'); width: 300px; height: 180px;">
            </div>
            {% endfor %}
        </div>
```

OK, now we can see some photo, by which I mean only one photo. Inspecting the
page, we see that all of the divs are there and have the correct width and
height, but they seem to be on top of each other. Which is less than ideal,
bordering on decidedly suboptimal. 🙁

![The album webpage with only one visible photo][single]
[single]: assets/2024-01-22-single.png "Sometimes one is not enough" width=800px

OK, so maybe that's what all of those `transform: translate` CSS incantations
are about. Reading [the
documentation](https://developer.mozilla.org/en-US/docs/Web/CSS/transform-function/translate),
it seems that we can do cool stuff like

``` CSS
transform: translate(10px, 20px);
```

to move an element 10 pixels to the right and 20 pixels down. So if we want to
have a nice three column layout like Flickr's with 4 pixels between each image,
we could lay things out something like this:

``` html
<div style="transform: translate(0px,   4px);   ..."></div>
<div style="transform: translate(304px, 4px);   ..."></div>
<div style="transform: translate(608px, 4px);   ..."></div>
<div style="transform: translate(0px,   184px); ..."></div>
<div style="transform: translate(304px, 184px); ..."></div>
<div style="transform: translate(608px, 184px); ..."></div>
<div style="transform: translate(0px,   368px); ..."></div>
<div style="transform: translate(304px, 368px); ..."></div>
```

![The album webpage with only one visible photo][janked]
[janked]: assets/2024-01-22-janked.png "Sometimes one is not enough" width=800px

I mean, this is... better? Except all of the photos are cropped in an odd way,
and don't make use of the full width of the screen, and don't resize nicely like
Flickr's do and... well, kinda suck.

It looks like we're going to need some math and stuff to dig ourselves out of
this hole. 😱

## Tell me about yourself

The first order of business is to figure out what the dimensions of the photos
we download are. If we cast our eyes back to the data representation of a photo,
we see:

``` clojure
(comment

  (-> album :photos first)
  ;; => {:description nil,
  ;;     :date-taken nil,
  ;;     :geo-data nil,
  ;;     :rotation -1,
  ;;     :width 0,
  ;;     :title "sean-hargreaves-phoenix-new-5-final-a",
  ;;     :filename "53460147147.jpg",
  ;;     :id "53460147147",
  ;;     :out-file
  ;;     #object[java.io.File 0x13356709 "/tmp/72177720314024335/53460147147.jpg"],
  ;;     :object
  ;;     #object[com.flickr4java.flickr.photos.Photo 0x4291d927 "com.flickr4java.flickr.photos.Photo@14ea992b"],
  ;;     :height 0}

  )
```

The problem is that `width` and `height` are both 0, which is definitely not so
helpful. Since Flickr won't tell us what we need to know, let's see if Java can.

Luckily, there's a
[javax.imageio.ImageIO](https://docs.oracle.com/javase/8/docs/api/javax/imageio/ImageIO.html)
class that looks like it will do exactly what we need!

``` clojure
(comment

  (import '(javax.imageio ImageIO))
  ;; => javax.imageio.ImageIO

  (let [img (ImageIO/read (-> album :photos first :out-file))]
    {:width (.getWidth img)
     :height (.getHeight img)})
  ;; => {:width 1024, :height 576}

  )
```

Nice! Let's go back to our `clickr.flickr` namespace and add this to our
`download-photo!` function so that we get the correct width and height for each
photo. First, we need to import the `ImageIO` class:

``` clojure
(ns clickr.flickr
  (:require [babashka.fs :as fs]
            [clickr.util :as util]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (com.flickr4java.flickr Flickr
                                   RequestContext
                                   REST)
           (com.flickr4java.flickr.auth Permission)
           (com.flickr4java.flickr.photos Size)
           (com.flickr4java.flickr.util FileAuthStore)
           (java.io BufferedInputStream
                    FileOutputStream)
           (javax.imageio ImageIO)))
```

And then we can do the actual reading of the image file:

``` clojure
(defn download-photo! [{:keys [flickr out-dir] :as ctx}
                       {:keys [filename] :as photo}]
  (let [p-interface (.getPhotosInterface (:client flickr))
        out-file (fs/file out-dir filename)]
    (with-open [in (BufferedInputStream. (.getImageAsStream p-interface (:object photo) Size/LARGE))
                out (FileOutputStream. out-file)]
      (io/copy in out))
    (let [img (ImageIO/read out-file)]
      (assoc photo
             :out-file out-file
             :width (.getWidth img)
             :height (.getHeight img)))))

(comment

  (download-photo! ctx (-> album :photos first))
  ;; => {:description nil,
  ;;     :date-taken nil,
  ;;     :geo-data nil,
  ;;     :rotation -1,
  ;;     :width 1024,
  ;;     :title "sean-hargreaves-phoenix-new-5-final-a",
  ;;     :filename "53460147147.jpg",
  ;;     :id "53460147147",
  ;;     :out-file
  ;;     #object[java.io.File 0x7a5efcc7 "/home/jmglov/Pictures/clickr/53460147147.jpg"],
  ;;     :object
  ;;     #object[com.flickr4java.flickr.photos.Photo 0x3d11130b "com.flickr4java.flickr.photos.Photo@14ea992b"],
  ;;     :height 576}

  )
```

Lookin' good!

There's just one thing that annoys me here, which is that we're always
downloading the file, even if it already exists locally. Let's fix that real
quick whilst we're here:

``` clojure
(defn download-photo! [{:keys [flickr out-dir] :as ctx}
                       {:keys [filename] :as photo}]
  (let [p-interface (.getPhotosInterface (:client flickr))
        out-file (fs/file out-dir filename)]
    (when-not (fs/exists? out-file)
      (with-open [in (BufferedInputStream. (.getImageAsStream p-interface (:object photo) Size/LARGE))
                  out (FileOutputStream. out-file)]
        (io/copy in out)))
    (let [img (ImageIO/read out-file)]
      (assoc photo
             :out-file out-file
             :width (.getWidth img)
             :height (.getHeight img)))))
```

OK, now that we know the dimensions of each photo, what do we do with that? I
guess we could extend our template to add the width and height and translate
stuff, then do some calculations on the photos and write it to the HTML...

``` html
        <div id="photos">
            {% for photo in album.photos %}
            <div id="photo-{{photo.id}}" class="photo"
                style="background-image: url('{{photo.filename}}'); width: {{photo.width}}px; height: {{photo.height}}px; transform: translate({{photo.offset-x}}px, {{photo.offset-y}}px);">
            </div>
            {% endfor %}
        </div>
```

But this seems kinda yucky. Also we have no way of knowing how wide the browser
window will be, so we can't make good use of the space, and we can't dynamically
resize when the browser window is resized, and so on and so forth. 😢

My friends, I'm afraid we're going to have to resort to JavaScript! 😱

But wait... this is a Clojure blog (sort of), and we Clojurians have a secret
weapon!

![A photo of David Nolen and Mike Fikes with a ClojureScript logo][clojurescript]
[clojurescript]: assets/2024-01-22-clojurescript.png "Nolen and Fikes rock, ClojureScript reaches!" width=800px

## Mellon Collie and the Infinite Sadness of getting ClojureScript to build

The only problem with ClojureScript is that it transpiles to JavaScript, which
means you need to somehow get that JavaScript built and then stick it somewhere
the browser can find it and OMG that smacks of effort!

We have lovely tools such as
[shadow-cljs](https://github.com/thheller/shadow-cljs) which turn the infinite
sadness into finite sadness, but I'm going to go on record as not being too
happy about sadness (though sadness has a part to play in the human experience,
so maybe I should be happy about that?), so am I SOL (er, Sorta Outta Luck?) at
this point?

Of course not, because we Clojurians have more than one secret weapon!

![A photo of Michiel Borkent AKA borkdude saying 'Can I get a borkdude?'][borkdude]
[borkdude]: assets/2024-01-22-borkdude.png "Get borked, man!" width=800px

A friend and were discussing borkdude's prodigious output one day, and one of us
made the observation that there was something Unix-y about his stuff, that he
provided a bunch of basic pieces that could be combined to do pretty much
whatever you need to do, but without sucking in all of Maven Central as
transitive dependencies. I don't know if this is a great comparison, or if
borkdude himself would agree, but hey! this is my blog, and borkdude can write a
polemic about how much I suck on his own blog if he wants to.

Anyway.

One of the most amazing things borkdude provideth is this thing called
[Scittle](https://github.com/babashka/scittle/), which allows you to "execute
Clojure(Script) directly from browser script tags via SCI". This is an absolute
game changer for lazy programmers like me, or even non-lazy programmers unlike
me who want to put some ClojureScript on a page but aren't setting out to build
complex Single Page Apps.

So let's drop a scittle in our `<head>` and get to ClojureScripting!

``` html
<head>
    <title>{{album.title}}</title>
    <meta charset="utf-8">
    <meta http-equiv="x-ua-compatible" content="ie=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="style.css">

    <script src="https://cdn.jsdelivr.net/npm/scittle@0.6.15/dist/scittle.js" type="application/javascript"></script>
</head>
```

With this one little line, I now have the full power of ClojureScript at my
disposal! I can now solve problems like knowing how wide my window is:

``` html
<head>
    <!-- ... -->

    <script src="https://cdn.jsdelivr.net/npm/scittle@0.6.15/dist/scittle.js" type="application/javascript"></script>
    <script type="application/x-scittle">
      (println (str "Window width: " (.-innerWidth js/window) "px"))
    </script>
</head>
```

Having dropped this into my `resources/templates/album.html`, if I
`write-album-html!` again, reload my browser, and take a look at the JavaScript
console, wondrous sights fill my eyes:

![The album webpage, displaying 'Window width: 1362px' in the JavaScript console][width]
[width]: assets/2024-01-22-width.png "Behold the wonder of Scittle!" width=800px

Of course, ClojureScript without a REPL is like a lovely bowl of mango sorbet
without a spoon, so let's visit the [Scittle
docs](https://github.com/babashka/scittle/tree/main/doc/nrepl) and get ourselves
a lovely silver spoon!

According to that page, all I need to do is drop the following into my HTML
file:

``` html
<head>
    <!-- ... -->

    <script src="https://cdn.jsdelivr.net/npm/scittle@0.6.15/dist/scittle.js" type="application/javascript"></script>
    <script>var SCITTLE_NREPL_WEBSOCKET_PORT = 1340;</script>
    <script src="https://cdn.jsdelivr.net/npm/scittle@0.6.15/dist/scittle.nrepl.js"
        type="application/javascript"></script>
    <script type="application/x-scittle" src="album.cljs"></script>
</head>
```

Then I can pop an `album.cljs` right next to my `index.html` and win!

Let's try this out. I'll create a basic `resources/templates/album.cljs`:

``` clojure
(ns album)

(defn print-window-width []
  (println (str "Window width: " (.-innerWidth js/window) "px")))
```

And back in my `src/clickr/html.clj`, write it to the output dir along with the
other files:

``` clojure
(defn write-album-html! [ctx {:keys [out-dir] :as album}]
  (when-not out-dir
    (throw (ex-info "Album must be downloaded before writing it to HTML"
                    {:album album})))
  (let [html (album->html ctx album)
        html-file (fs/file out-dir "index.html")
        css (album->css ctx album)
        css-file (fs/file out-dir "style.css")
        cljs (apply-album-template ctx "resources/templates/album.cljs" album)
        cljs-file (fs/file out-dir "album.cljs")]
    (spit html-file html)
    (spit css-file css)
    (spit cljs-file cljs)
    (assoc album :html-file html-file, :css-file css-file, :cljs-file cljs-file)))

(comment

  (write-album-html! ctx album)
  ;; => { ... }

  )
```

But when we reload our page, we get some gross stuff about `DOMException`s and
`CORS` and other things that make us go "hmm?".

``` text
scittle.nrepl.js:18 Uncaught DOMException: Failed to construct 'WebSocket': The URL 'ws://:1340/_nrepl' is invalid.
    at https://cdn.jsdelivr.net/npm/scittle@0.6.15/dist/scittle.nrepl.js:18:158
    at https://cdn.jsdelivr.net/npm/scittle@0.6.15/dist/scittle.nrepl.js:20:4
index.html:1 Access to XMLHttpRequest at 'file:///tmp/72177720314024335/album.cljs' from origin 'null' has been blocked by CORS policy: Cross origin requests are only supported for protocol schemes: http, isolated-app, brave, https, chrome-untrusted, data, chrome-extension, chrome.
scittle.js:1881 GET file:///tmp/72177720314024335/album.cljs net::ERR_FAILED
```

This is the opposite of fun. `(complement fun)`, if you will. It looks like
we're going to need an actual webserver to continue here. Luckily, if we but
read a tiny bit further in `scittle/doc/nrepl/README.md`, it seems that we can
have a webserver quite easily:

> When you run bb dev in this directory, and then open http://localhost:1341 you
> should be able evaluate expressions in playground.cljs. See a demo here.
> 
> Note that the nREPL server connection stays alive even after the browser
> window refreshes.

So if we grab the
[bb.edn](https://github.com/babashka/scittle/blob/main/doc/nrepl/bb.edn) from
here and drop it in our `resources/templates/` directory, and add it to our
ever-expanding `write-album-html!` function...

``` clojure
(defn write-album-html! [ctx {:keys [out-dir] :as album}]
  (when-not out-dir
    (throw (ex-info "Album must be downloaded before writing it to HTML"
                    {:album album})))
  (let [html (album->html ctx album)
        html-file (fs/file out-dir "index.html")
        css (album->css ctx album)
        css-file (fs/file out-dir "style.css")
        cljs (apply-album-template ctx "resources/templates/album.cljs" album)
        cljs-file (fs/file out-dir "album.cljs")
        bb-edn (apply-album-template ctx "resources/templates/bb.edn" album)
        bb-edn-file (fs/file out-dir "bb.edn")]
    (spit html-file html)
    (spit css-file css)
    (spit cljs-file cljs)
    (spit bb-edn-file bb-edn)
    (assoc album :html-file html-file, :css-file css-file
           :cljs-file cljs-file, :bb-edn-file bb-edn-file)))

(comment

  (write-album-html! ctx album)
  ;; => { ... }

  )
```

Now we can pop over to `/tmp/72177720314024335/` and fire up Babashka:

``` text
: jmglov@laurana; cd /tmp/72177720314024335/
: jmglov@laurana; bb dev
Serving static assets at http://localhost:1341
nREPL server started on port 1339...
Websocket server started on 1340...
```

and then load up http://localhost:1341/ in our web browser, open up
`/tmp/72177720314024335/album.cljs` in Emacs, run `cider-connect-cljs`, select
`localhost` then port `1339`, select `nbb` as the ClojureScript REPL type, maybe
hit `C-g` a few times if we see something like

``` text
Fri Jan 19 09:03:52 CET 2024 [worker-3] ERROR - handle websocket frame org.httpkit.server.Frame$TextFrame@1de1c580
java.lang.RuntimeException: No reader function for tag object
        at clojure.lang.EdnReader$TaggedReader.readTagged(EdnReader.java:801)
        at clojure.lang.EdnReader$TaggedReader.invoke(EdnReader.java:783)
        [...]
```

in the terminal where we're running `bb dev` (there's a [whole
thread](https://clojurians.slack.com/archives/C034FQN490E/p1703238977455889)
over on Clojurians Slack about this where borkdude and Benjamin fixed this, but
I switched laptops since then—a long story for another time—and it came back,
but it doesn't hurt anything, so I just carried on with my disgusting
workaround), and then whack `C-c C-k` (your keybindings may vary, of course, so
it's `cider-load-buffer` you want) and drop a [Rich
comment](https://betweentwoparens.com/blog/rich-comment-blocks/#rich-comment) in
the file

``` clojure
(comment

  (print-window-width)
  ;; => nil

  )
```

and then `C-c C-v f c e` (`cider-pprint-eval-last-sexp-to-comment`) and finally
pop a bottle of emoji! 🥂

![The album webpage, displaying 'Window width: 1362px' in the JavaScript console][repl]
[repl]: assets/2024-01-22-repl.png "Now we're cooking with REPL!" width=800px

OK, so let's take stock of what we've done here. We now have a webpage which
loads Scittle which in turn loads our shiny new `album.cljs`, which we can load
in our web browser by virtue of a Babashka webserver configured by the `bb.edn`
we dropped in the album dir, and oh by the way, we can connect a REPL to it and
cause stuff to happen. That's something we can now build a Flickr clone on!

## Sizing things up

Let's make a few design decisions about how the album should look. And by "make
design decisions", I mean "rip design decisions off from Flickr". Here's what we
want:

1. If the browser window is reasonably wide, the album should be 80% the width
   of the window
2. If the browser window is reasonably wide, display 3 photos per row
3. All rows should be the same width as the album header
4. All photos in a row should be scaled to the same height
5. As the window is resized, photos should be rescaled to fit nicely
6. If the window is too small to display three photos per row, display two per
   row instead, and expand the album to fill the width of the window
7. If the window is too small to display two photos per row, display one per
   row instead

Let's start off by just assuming the window is reasonably wide and seeing if we
can't get the photos displayed three to a row, with the row being the same width
as the album header and all photos in each row being the same size.

The first order of business is to expose the photos metadata to ClojureScript.
Thanks to our decision to run all of our resource files through Selmer, this
turns out to be a one-liner in `album.cljs`:

``` clojure
(ns album)

(def photos {{photos-edn | safe}})
```

Of course, that `photos-edn` has to come from somewhere, so let's pop into
our `clickr.html` namespace and turn our photos datastructure into some EDN:

``` clojure
(comment

  (def album (->> (flickr/get-albums ctx)
                  first
                  (flickr/download-album! ctx)))
  ;; => #'clickr.html/album

  (with-out-str
    (prn (:photos album)))
  ;; => "({:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 1024, :title \"sean-hargreaves-phoenix-new-5-final-a\", :filename \"53460147147.jpg\", :id \"53460147147\", :object #object[com.flickr4java.flickr.photos.Photo 0x11aef77d \"com.flickr4java.flickr.photos.Photo@14ea992b\"], :height 576} {:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 1024, :title \"patryk-urbaniak-for-all-mankind-004\", :filename \"53461405604.jpg\", :id \"53461405604\", :object #object[com.flickr4java.flickr.photos.Photo 0x63127ec5 \"com.flickr4java.flickr.photos.Photo@22e889e0\"], :height 576} {:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 1024, :title \"jared-michael-forallmankind-011\", :filename \"53461091151.jpg\", :id \"53461091151\", :object #object[com.flickr4java.flickr.photos.Photo 0x46c7dec8 \"com.flickr4java.flickr.photos.Photo@33f7f318\"], :height 512} {:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 1024, :title \"sean-hargreaves-transport-ship-new-final-1b\", :filename \"53461088046.jpg\", :id \"53461088046\", :object #object[com.flickr4java.flickr.photos.Photo 0x25078237 \"com.flickr4java.flickr.photos.Photo@88e20c4f\"], :height 576} {:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 1024, :title \"sean-hargreaves-057-asteroid-mining-ship-platformcables-1b-sh-2022-7-08\", :filename \"53460163402.jpg\", :id \"53460163402\", :object #object[com.flickr4java.flickr.photos.Photo 0x43748cf5 \"com.flickr4java.flickr.photos.Photo@451d67cd\"], :height 576} {:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 722, :title \"jean-luc-sabourin-fam-season-2-soviet-notext\", :filename \"53460161007.jpg\", :id \"53460161007\", :object #object[com.flickr4java.flickr.photos.Photo 0x33e03608 \"com.flickr4java.flickr.photos.Photo@c948c4a2\"], :height 1023} {:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 666, :title \"trung-doan-mankind\", :filename \"53461214223.jpg\", :id \"53461214223\", :object #object[com.flickr4java.flickr.photos.Photo 0x4b299f22 \"com.flickr4java.flickr.photos.Photo@1a270f5c\"], :height 1000} {:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 1023, :title \"daniel-jennings-img-7554\", :filename \"53460151727.jpg\", :id \"53460151727\", :object #object[com.flickr4java.flickr.photos.Photo 0x5226aa4a \"com.flickr4java.flickr.photos.Photo@436e36e8\"], :height 299})\n"

  )
```

OK, that technically worked, but... gross! It would be much nicer if this mess
was human readable in some way. Luckily for us, Rich Hickey provideth, in the
form of [clojure.pprint/pprint](https://clojuredocs.org/clojure.pprint/pprint).
Let's just swap that in for `prn` and see how it goes:

``` clojure

(comment

  (require '[clojure.pprint :as pprint])
  ;; => nil

  (with-out-str
    (pprint/pprint (:photos album)))
  ;; => "({:description nil,\n  :date-taken nil,\n  :geo-data nil,\n  :rotation -1,\n  :width 1024,\n  :title \"sean-hargreaves-phoenix-new-5-final-a\",\n  :filename \"53460147147.jpg\",\n  :id \"53460147147\",\n  :object\n  #object[com.flickr4java.flickr.photos.Photo 0x11aef77d \"com.flickr4java.flickr.photos.Photo@14ea992b\"],\n  :height 576}\n ... {:description nil,\n  :date-taken nil,\n  :geo-data nil,\n  :rotation -1,\n  :width 1023,\n  :title \"daniel-jennings-img-7554\",\n  :filename \"53460151727.jpg\",\n  :id \"53460151727\",\n  :object\n  #object[com.flickr4java.flickr.photos.Photo 0x5226aa4a \"com.flickr4java.flickr.photos.Photo@436e36e8\"],\n  :height 299})\n"

  )
```

OK, that still looks silly in my REPL, but if I print it, I get:

``` clojure
({:description nil,
  :date-taken nil,
  :geo-data nil,
  :rotation -1,
  :width 1024,
  :title "sean-hargreaves-phoenix-new-5-final-a",
  :filename "53460147147.jpg",
  :id "53460147147",
  :out-file
  #object[java.io.File 0x13356709 "/tmp/72177720314024335/53460147147.jpg"],
  :object
  #object[com.flickr4java.flickr.photos.Photo 0x4291d927 "com.flickr4java.flickr.photos.Photo@14ea992b"],
  :height 576}
 {:description nil,
  :date-taken nil,
  :geo-data nil,
  :rotation -1,
  :width 1024,
  :title "patryk-urbaniak-for-all-mankind-004",
  :filename "53461405604.jpg",
  :id "53461405604",
  :out-file
  #object[java.io.File 0x17191ef4 "/tmp/72177720314024335/53461405604.jpg"],
  :object
  #object[com.flickr4java.flickr.photos.Photo 0xb886372 "com.flickr4java.flickr.photos.Photo@22e889e0"],
  :height 576}
 ;; ...
 )
```

This is better, in that it can be read by a human. Let's make sure that it can
be read by a machine, though:

``` clojure
(comment

  (require '[clojure.edn :as edn])
  ;; => nil

  (-> (with-out-str
        (pprint/pprint (:photos album)))
      edn/read-string)
  ;; => Execution error at clickr.html/eval27054 (REPL:243).
  ;;    No reader function for tag object

  )
```

Oopsy! I think those `#object` literals are causing trouble. No worries, we can
just dissoc them:

``` clojure
(comment

  (-> (with-out-str
        (pprint/pprint (-> album
                           :photos
                           (map #(dissoc % :out-file :object)))))
      edn/read-string)
  ;; => Execution error (IllegalArgumentException) at clickr.html/eval27066$fn (REPL:241).
  ;;    Don't know how to create ISeq from: clickr.html$eval27066$fn__27067$fn__27068

  )
```

Arg! Looks like the reader is having trouble with that lazy sequence. Sounds
familiar, eh? 🙄 Let's turn it into a vector:

``` clojure

(comment

  (-> (with-out-str
        (pprint/pprint (->> album
                            :photos
                            (map #(dissoc % :out-file :object))
                            vec)))
      edn/read-string)
  ;; => [{:description nil,
  ;;      :date-taken nil,
  ;;      :geo-data nil,
  ;;      :rotation -1,
  ;;      :width 1024,
  ;;      :title "sean-hargreaves-phoenix-new-5-final-a",
  ;;      :filename "53460147147.jpg",
  ;;      :id "53460147147",
  ;;      :height 576}
  ;;     [...]
  ;;     {:description nil,
  ;;      :date-taken nil,
  ;;      :geo-data nil,
  ;;      :rotation -1,
  ;;      :width 1023,
  ;;      :title "daniel-jennings-img-7554",
  ;;      :filename "53460151727.jpg",
  ;;      :id "53460151727",
  ;;      :height 299}]

  )
```

Nice, now we can round-trip our EDN, so let's plug this into the CLJS template.

``` clojure
(ns clickr.html
  (:require [babashka.fs :as fs]
            [selmer.parser :as selmer]
            [clojure.pprint :as pprint]))

(defn ->edn [data]
  (with-out-str (pprint/pprint data)))

(defn apply-album-template [_ctx template-file album]
  (selmer/render (slurp template-file)
                 {:album (update album :photos vec)
                  :photos-edn (->> album
                                   :photos
                                   (map #(dissoc % :out-file :object))
                                   vec
                                   ->edn)}))

(comment

  (write-album-html! ctx album)
  ;; => { ... }

  )
```

If we take a look at the resulting `/tmp/72177720314024335/album.cljs`, we see
that by golly do we ever have photos!

``` clojure
(ns album)

(def photos [{:description nil,
  :date-taken nil,
  :geo-data nil,
  :rotation -1,
  :width 0,
  :title "sean-hargreaves-phoenix-new-5-final-a",
  :filename "53460147147.jpg",
  :id "53460147147",
  :height 0}
 ;; [...]
 {:description nil,
  :date-taken nil,
  :geo-data nil,
  :rotation -1,
  :width 0,
  :title "daniel-jennings-img-7554",
  :filename "53460151727.jpg",
  :id "53460151727",
  :height 0}]
)
```

Let's open up `/tmp/72177720314024335/album.cljs` directly in Emacs so we can
continue our REPL-driven development without the need to keep evaluating
`write-album-html!`. We'll start out by reloading the browser to make sure it's
picking up the latest `album.cljs`. This should be the last time we'll need to
reload the page, unless something goes wrong. Next, let's evaluate the entire
buffer (`C-c C-k` or `cider-load-buffer` or however you do stuff in your
editor), and finally make sure all is good with our REPL connection:

``` clojure
(comment

  (println "OK, I'm reloaded!")
  ;; => nil

  )
```

Opening up the JavaScript console in our browser, we see the message we printed
out, so all is good there. Time to get to actually computing stuff and things!

Our first rule was:

**1. If the browser window is reasonably wide, the album should be 80% the width of the window**

Let's add a function to get the current window width:

``` clojure
(defn get-window-width []
  (.-innerWidth js/window))

(comment

  (get-window-width)
  ;; => 1513

  )
```

Great! Now let's define what we mean by "reasonably wide". We want to display 3
photos side by side, and having them be 300 pixels wide seems like a good size.
Then we're going to need 4 pixels of padding between each photo, so that adds up
to 908 pixels. This is the width of the album div, though, and it will need to
fit into 80% of the window width, meaning that the window needs to be... um,
some number of pixels wide?

OK, instead of dividing by 0.8, let's actually write our code with the album div
in mind instead of the window width. We can encode all of these rules in some
nice Clojure data:

``` clojure
(def config
  {:album-min-width 900
   :album-width-pct 0.8
   :num-photos-per-row 3
   :photo-padding 4})
```

Now let's write a function that computes the width of the album div based on all
those computations that I was trying to do in my head:

``` clojure
(defn provisional-album-width [{:keys [album-width-pct] :as config}]
  (* (get-window-width) album-width-pct))

(comment

  (provisional-album-width config)
  ;; => 1210.4

  )
```

Cool, so at the current window width, the album is wide enough. Let's now set
the width to that provisional width. This requires us to know the name of the
album div so we can grab it from the DOM, so let's add that to our config:

``` clojure
(def config
  {:album-div-name "album"
   :album-min-width 900
   :album-width-pct 0.8
   :num-photos-per-row 3
   :photo-padding 4})

(comment

  (.getElementById js/document (:album-div-name config))
  ;; => #object[HTMLDivElement [object HTMLDivElement]]

  )
```

Looks promising! To set the width of that div, we need to set a style property
on it:

``` clojure
(comment

  (-> (.getElementById js/document (:album-div-name config))
      .-style
      (.setProperty "width" "1210.4px"))
  ;; => nil

  )
```

As soon as we evaluate this in the REPL, the div resizes live in our browser
window! 🤯

![The album webpage with the album div 80% of the window width][pctwidth]
[pctwidth]: assets/2024-01-22-pctwidth.png "Such REPL driving!" width=800px

Now that we've figured out all the pieces, let's write a function that sets the
album width:

``` clojure
(defn set-album-width!
  "Sets the width of the album div based on the current window size and returns
   the new size of the album div."
  [{:keys [album-div-name album-min-width num-photos-per-row photo-padding]
    :as config}]
  (let [provisional-width (provisional-album-width config)
        padding-width (* photo-padding (dec num-photos-per-row))
        min-width (+ album-min-width padding-width)
        new-width (if (>= provisional-width min-width)
                    provisional-width
                    (* (get-window-width) 0.95))]
    (-> (.getElementById js/document album-div-name)
        .-style
        (.setProperty "width" (str new-width "px")))
    new-width))

(comment

  (set-album-width! config)
  ;; => 1210.4

  )
```

The div has resized again, this time to 80% of the window width! 🎉

## Scaling photos is easier than scaling Everest, right?

Let's have a look at our next rule:

**2. If the browser window is reasonably wide, display 3 photos per row**

Splitting the photos into rows is quite straightforward:

``` clojure
(comment

  (partition-all 3 photos)
  ;; => (({:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 1024, :title "sean-hargreaves-phoenix-new-5-final-a", :filename "53460147147.jpg", :id "53460147147", :height 576}
  ;;      {:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 1024, :title "patryk-urbaniak-for-all-mankind-004", :filename "53461405604.jpg", :id "53461405604", :height 576}
  ;;      {:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 1024, :title "jared-michael-forallmankind-011", :filename "53461091151.jpg", :id "53461091151", :height 512})
  ;;     ({:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 1024, :title "sean-hargreaves-transport-ship-new-final-1b", :filename "53461088046.jpg", :id "53461088046", :height 576}
  ;;      {:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 1024, :title "sean-hargreaves-057-asteroid-mining-ship-platformcables-1b-sh-2022-7-08", :filename "53460163402.jpg", :id "53460163402", :height 576}
  ;;      {:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 722, :title "jean-luc-sabourin-fam-season-2-soviet-notext", :filename "53460161007.jpg", :id "53460161007", :height 1023})
  ;;     ({:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 666, :title "trung-doan-mankind", :filename "53461214223.jpg", :id "53461214223", :height 1000}
  ;;      {:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 1023, :title "daniel-jennings-img-7554", :filename "53460151727.jpg", :id "53460151727", :height 299}))

  )
```

Moving on to the next rule:

**3. All rows should be the same width as the album header**

The width of a row is the width of the photos in the row plus the padding
between them, so given the width of a row and the number of photos in it, we can
figure out how wide each photo should be like so:

``` clojure
(comment

  (let [row-width (set-album-width! config)
        padding-width (* (dec (:num-photos-per-row config))
                         (:photo-padding config))]
    (-> row-width
        (- padding-width)
        (/ (:num-photos-per-row config))))
  ;; => 400.8

  )
```

Now we can scale the photos like this:

``` clojure
(defn scale-photos [_config target-width photos]
  (map #(assoc % :width target-width) photos))

(comment

  (->> photos
       (scale-photos config 400.8)
       (map :width))
  ;; => (400.8 400.8 400.8 400.8 400.8 400.8 400.8 400.8)

  )
```

Let's write a function that updates the styles of the div corresponding to a
specific photo:

``` clojure
(defn set-photo-styles! [_config {:keys [id width height] :as photo}]
  (let [div-id (str "photo-" id)]
    (doto (-> (.getElementById js/document div-id) -style)
      (.setProperty "width" (str width "px"))
      (.setProperty "height" (str height "px")))
    photo))

(comment

  (->> photos
       (scale-photos config 400.8)
       (map (partial set-photo-styles! config)))
  ;; => ({:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 400.8, :title "sean-hargreaves-phoenix-new-5-final-a", :filename "53460147147.jpg", :id "53460147147", :height 576} ... )

  )
```

OK, this is progress. We've successfully set the width of all photo divs:

![The album webpage with all photos set to 400.8 pixels width][scaled]
[scaled]: assets/2024-01-22-scaled.png "Don't let perfect be the enemy of wrong" width=800px

The next step is to use the `transform: translate()` stuff we learned about
earlier to arrange the photos how we want them. Given our desired width of 400.8
pixels and height of 576 pixels (the height of the first photo, which must be
the height of all the photos, right?), we want something like this:

``` html
<div style="transform: translate(0px,     4px);    ..."></div>
<div style="transform: translate(404.8px, 4px);    ..."></div>
<div style="transform: translate(809.6px, 4px);    ..."></div>
<div style="transform: translate(0px,     580px);  ..."></div>
<div style="transform: translate(404.8px, 580px);  ..."></div>
<div style="transform: translate(809.6px, 580px);  ..."></div>
<div style="transform: translate(0px,     1160px); ..."></div>
<div style="transform: translate(404.8px, 1160px); ..."></div>
```

Let's see if we can figure out how to make this happen, starting with a single
row. To make it easier to see what's going on, let's hide all the photos not in
the first row:

``` clojure
(comment

  (->> photos
       (drop 3)
       (map (fn [{:keys [id width height] :as photo}]
              (let [div-id (str "photo-" id)]
                (-> (.getElementById js/document div-id)
                    .-style
                    (.setProperty "display" "none")))))
       doall)
  ;; => (nil nil nil nil nil)

  )
```

Having done this, let's update our `set-photo-styles!` function to set the
`transform` CSS property based on the `:x-offset` and `:y-offset` keys of a
photo:

``` clojure
(defn set-photo-styles!
  [_config {:keys [id width height x-offset y-offset] :as photo}]
  (let [div-id (str "photo-" id)
        transform (str "translate(" x-offset "px, " y-offset "px)")]
    (doto (-> (.getElementById js/document div-id) .-style)
      (.setProperty "width" (str width "px"))
      (.setProperty "height" (str height "px"))
      (.setProperty "transform" transform))
    photo))

(comment

  (let [[p1 p2 p3 & _] (scale-photos config 400.8 photos)]
    (set-photo-styles! config (assoc p1 :x-offset 0, :y-offset 4))
    (set-photo-styles! config (assoc p2 :x-offset 404.8, :y-offset 4))
    (set-photo-styles! config (assoc p3 :x-offset 809.6, :y-offset 4)))
  ;; => {:description nil, :date-taken nil, :geo-data nil, :y-offset 4, :rotation -1, :width 400.8, :title "jared-michael-forallmankind-011", :filename "53461091151.jpg", :id "53461091151", :height 512, :x-offset 809.6}

  )
```

We cheated a bit there, but the results look good:

![The album webpage with the first row laid out correctly][manual]
[manual]: assets/2024-01-22-manual.png "Don't underestimate the power of hard-coding!" width=800px

Let's see if we can do this without hard-coding all the things. We basically
want to set the x-offset of each photo to the x-offset of the previous photo,
plus the width of the previous photo, plus the padding, so we need a function
like `map`, except that it remembers stuff from the previous item being mapped
over. There is of course a function like that:
[reduce](https://clojuredocs.org/clojure.core/reduce). Let's try it out:

``` clojure
(comment

  (->> photos
       (scale-photos config 400.8)
       (take 3)
       (reduce (fn [{:keys [x-offset] :as acc} {:keys [width] :as photo}]
                 (let [new-x-offset (+ x-offset width (:photo-padding config))]
                   (-> acc
                       (assoc :x-offset new-x-offset)
                       (update :arranged conj (assoc photo :x-offset x-offset)))))
               {:x-offset 0, :arranged []})
       :arranged
       (map (partial set-photo-styles! config))
       (map :x-offset))
  ;; => (0 404.8 809.6)

  )
```

What we're doing here is computing the x-offset for the next photo in row as we
iterate over the photos, then setting the x-offset of the current photo to the
previously computed x-offset and appending it to the vector of arranged photos.
We can see that it worked since the x-offsets of each photo are the same as we
computed by hand above, and when we fed the row to `set-photo-styles!`, nothing
moved in the browser! 🎉

Let's give this function a name to make it a little less mysterious:

``` clojure
(defn arrange-row [{:keys [photo-padding] :as config} photos]
  (->> photos
       (reduce (fn [{:keys [x-offset] :as acc} {:keys [width] :as photo}]
                 (let [new-x-offset (+ x-offset width photo-padding)]
                   (-> acc
                       (assoc :x-offset new-x-offset)
                       (update :arranged conj (assoc photo :x-offset x-offset)))))
               {:x-offset 0, :arranged []})
       :arranged))

(comment

  (->> photos
       (scale-photos config 400.8)
       (take 3)
       (arrange-row config)
       (map (partial set-photo-styles! config))
       (map :x-offset))
  ;; => (0 404.8 809.6)

  )
```

Much better!

## Getting down 2D

Now that we've dealt with a single row, let's see if we can apply the same
strategy to the y-offset for all rows. But first, let's make all the photos
visible again:

``` clojure
(comment

  (->> photos
       (drop 3)
       (map (fn [{:keys [id width height] :as photo}]
              (let [div-id (str "photo-" id)]
                (-> (.getElementById js/document div-id)
                    .-style
                    (.removeProperty "display")))))
       doall)
  ;; => ("none" "none" "none" "none" "none")

  )
```

We want to start by scaling the photos to our desired width and then
partitioning them into rows:

``` clojure
(comment

  (->> photos
       (scale-photos config 400.8)
       (partition-all 3))
  ;; => (({:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 400.8, :title "sean-hargreaves-phoenix-new-5-final-a", :filename "53460147147.jpg", :id "53460147147", :height 576}
  ;;      {:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 400.8, :title "patryk-urbaniak-for-all-mankind-004", :filename "53461405604.jpg", :id "53461405604", :height 576}
  ;;      {:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 400.8, :title "jared-michael-forallmankind-011", :filename "53461091151.jpg", :id "53461091151", :height 512})
  ;;     ({:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 400.8, :title "sean-hargreaves-transport-ship-new-final-1b", :filename "53461088046.jpg", :id "53461088046", :height 576}
  ;;      {:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 400.8, :title "sean-hargreaves-057-asteroid-mining-ship-platformcables-1b-sh-2022-7-08", :filename "53460163402.jpg", :id "53460163402", :height 576}
  ;;      {:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 400.8, :title "jean-luc-sabourin-fam-season-2-soviet-notext", :filename "53460161007.jpg", :id "53460161007", :height 1023})
  ;;     ({:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 400.8, :title "trung-doan-mankind", :filename "53461214223.jpg", :id "53461214223", :height 1000} 
  ;;      {:description nil, :date-taken nil, :geo-data nil, :rotation -1, :width 400.8, :title "daniel-jennings-img-7554", :filename "53460151727.jpg", :id "53460151727", :height 299}))

  )
```

Having done that, we can `reduce` over the rows, calculating the y-offset for
the next row as the current y-offset plus the height of the row:

``` clojure
(comment

  (->> photos
       (scale-photos config 400.8)
       (partition-all 3)
       (reduce (fn [{:keys [y-offset] :as acc} row-photos]
                 (let [new-y-offset (+ y-offset
                                       (:height (first row-photos))
                                       (:photo-padding config))
                       arranged-row (->> row-photos
                                         (arrange-row config)
                                         (map #(assoc % :y-offset y-offset)))]
                   (-> acc
                       (assoc :y-offset new-y-offset)
                       (update :arranged concat arranged-row))))
               {:y-offset (:photo-padding config), :arranged []})
       :arranged
       (map (partial set-photo-styles! config))
       (map (juxt :x-offset :y-offset)))
  ;; => ([0 4] [404.8 4] [809.6 4] [0 584] [404.8 584] [809.6 584] [0 1164] [404.8 1164])

  )
```

OK, this seems about right. Let's clean this mess up a bit:

``` clojure
(defn arrange-photos
  [{:keys [num-photos-per-row photo-padding] :as config} photos]
  (->> photos
       (partition-all num-photos-per-row)
       (reduce (fn [{:keys [y-offset] :as acc} row-photos]
                 (let [arranged-row (->> row-photos
                                         (arrange-row config)
                                         (map #(assoc % :y-offset y-offset)))
                       new-y-offset (+ y-offset
                                       (:height (first arranged-row))
                                       photo-padding)]
                   (-> acc
                       (assoc :y-offset new-y-offset)
                       (update :arranged concat arranged-row))))
               {:y-offset photo-padding, :arranged []})
       :arranged))

(defn display-album!
  [{:keys [num-photos-per-row photo-padding] :as config} photos]
  (let [album-width (set-album-width! config)
        padding-width (* photo-padding (dec num-photos-per-row))
        photo-width (-> (- album-width padding-width)
                        (/ num-photos-per-row))]
    (->> photos
         (scale-photos config photo-width)
         (arrange-photos config)
         (map (partial set-photo-styles! config))
         doall)))

(comment

  (display-album! config photos)
  ;; => ( ... )

  )
```

Now, let's have a quick look at the browser. Yeah, things look reasonably
nice... until we scroll down. 😬

Let's compare what our album looks like to what it looks like on Flickr:

![A cartoon version of the Drake hotline bling meme: Drake is disgusted by our album and delighted by the Flickr UI][compare]
[compare]: assets/2024-01-22-compare.png "OMG my eeeeeyyyeees!" width=800px

Oh good lord Paladine what in the actual abyss?! Our photos are cropped weirdly
and apparently we forgot all about this:

**4. All photos in a row should be scaled to the same height**

Let the self-flagellation begin! Or maybe we just fix the height thing.
Whichever.

## Opting for the latter is the brave thing to do

We have a function called `scale-photos`, but what it's actually doing is
setting the width. Scaling would be resizing both width and height, maintaining
the same aspect ratio. Let's see if we can fix that:

``` clojure
(defn scale-photos [_config scaling-factor photos]
  (->> photos
       (map (fn [photo]
              (-> photo
                  (update :height / scaling-factor)
                  (update :width / scaling-factor))))))
```

So if we want to scale each photo down to half size, we'd do this:

``` clojure
(comment

  (->> photos
       (scale-photos config 2)
       (map (juxt :width :height)))
  ;; => ([512 288] [512 288] [512 256] [512 288] [512 288] [361 511.5] [333 500] [511.5 149.5])

  (->> photos
       (map (juxt :width :height)))
  ;; => ([1024 576] [1024 576] [1024 512] [1024 576] [1024 576] [722 1023] [666 1000] [1023 299])
  
  )
```

That looks pretty good. Now, how to compute the scaling factor? Let's say we
have a row of three photos, with a total width of 3072, and an album width of
1210.4:

``` clojure
(comment

  (->> photos
       (take 3)
       (map :width)
       (reduce +))
  ;; => 3072

  (provisional-album-width config)
  ;; => 1210.4

  )
```

To get the scaling factor, we just need to divide the total width of the photos
by the width of the album, taking padding into consideration, of course:

``` clojure
(comment

  (let [{:keys [num-photos-per-row photo-padding]} config]
    (-> (->> photos
             (take num-photos-per-row)
             (map :width)
             (reduce +))
        (/ (- (provisional-album-width config)
              (* photo-padding (dec num-photos-per-row))))))
  ;; => 2.5548902195608783

  )
```

This has just revealed something interesting that we should have noticed far
earlier: the total width of the photos in a row varies, unless all of the photos
just happen to be the same width (which is not the case for us):

``` clojure
(comment

  (->> photos
       (partition-all 3)
       (map #(reduce + (map :width %))))
  ;; => (3072 2770 1689)

  )
```

This means that we need to scale each row independently, which we can actually
do right in `arrange-row`, as long as we provide album width in the config map.
Let's write a function to compute the scale factor so as not to make a mess in
`arrange-row`:

``` clojure
(defn get-scale-factor [{:keys [album-width photo-padding] :as config} photos]
  (let [row-width (->> photos
                       (map :width)
                       (reduce +))
        available-width (- album-width (* photo-padding (dec (count photos))))]
    (/ row-width available-width)))

(comment

  (->> photos
       (partition-all 3)
       (map (partial get-scale-factor (assoc config :album-width 1210.4))))
  ;; => (2.5548902195608783 2.303725881570193 1.4000331564986737)

  )
```

Now we can add the scaling to `arrange-row`:

``` clojure
(defn arrange-row [{:keys [photo-padding] :as config} photos]
  (let [scale-factor (get-scale-factor config photos)]
    (->> photos
         (scale-photos config scale-factor)
         (reduce (fn [{:keys [x-offset] :as acc} {:keys [width] :as photo}]
                   (let [new-x-offset (+ x-offset width photo-padding)]
                     (-> acc
                         (assoc :x-offset new-x-offset)
                         (update :arranged conj (assoc photo :x-offset x-offset)))))
                 {:x-offset 0, :arranged []})
         :arranged)))
```

And finally we need to add the album width to the config map in
`display-album!` and remove the call to `scale-photos`, since that's happening
for each row now:

``` clojure
(defn display-album!
  [{:keys [num-photos-per-row photo-padding] :as config} photos]
  (let [album-width (set-album-width! config)
        config (assoc config :album-width album-width)
        padding-width (* photo-padding (dec num-photos-per-row))
        photo-width (-> (- album-width padding-width)
                        (/ num-photos-per-row))]
    (->> photos
         (arrange-photos config)
         (map (partial set-photo-styles! config))
         doall)))

(comment

  (->> (display-album! config photos)
       (map #(select-keys % [:width :height :x-offset :y-offset])))
  ;; => ({:width 400.8, :height 225.45, :x-offset 0, :y-offset 4}
  ;;     {:width 400.8, :height 225.45, :x-offset 404.8, :y-offset 4}
  ;;     {:width 400.8, :height 200.4, :x-offset 809.6, :y-offset 4}
  ;;     {:width 444.4973285198556, :height 250.0297472924188, :x-offset 0, :y-offset 584}
  ;;     {:width 444.4973285198556, :height 250.0297472924188, :x-offset 448.4973285198556, :y-offset 584}
  ;;     {:width 313.4053429602888, :height 444.0632490974729, :x-offset 896.9946570397112, :y-offset 584}
  ;;     {:width 475.7030195381883, :height 714.2687981053879, :x-offset 0, :y-offset 1164}
  ;;     {:width 730.6969804618118, :height 213.56637063351096, :x-offset 479.7030195381883, :y-offset 1164})

  )
```

We're almost there now! The final missing piece is to normalise the height of
the photos before scaling them for the row width. Let's write a function that
scales down photos so they're all the same height as the shortest photo:

``` clojure
(defn normalise-height [_config photos]
  (let [min-height (apply min (map :height photos))]
    (->> photos
         (map (fn [{:keys [height width] :as photo}]
                (if (> height min-height)
                  (assoc photo
                         :height min-height
                         :width (/ min-height (/ height width)))
                  photo))))))

(comment

  (->> photos
       (take 3)
       (normalise-height config)
       (map (juxt :width :height)))
  ;; => ([910.2222222222222 512] [910.2222222222222 512] [1024 512])

  )
```

Now we just need to plug this into `arrange-row`:

``` clojure
(defn arrange-row [{:keys [photo-padding] :as config} photos]
  (let [normalised-photos (normalise-height config photos)
        scale-factor (get-scale-factor config normalised-photos)]
    (->> normalised-photos
         (scale-photos config scale-factor)
         (reduce (fn [{:keys [x-offset] :as acc} {:keys [width] :as photo}]
                   (let [new-x-offset (+ x-offset width photo-padding)]
                     (-> acc
                         (assoc :x-offset new-x-offset)
                         (update :arranged conj (assoc photo :x-offset x-offset)))))
                 {:x-offset 0, :arranged []})
         :arranged)))

(comment

  (->> (display-album! config photos)
       (map #(select-keys % [:width :height :x-offset :y-offset])))
  ;; => ({:width 384.76800000000003, :height 216.43200000000002, :x-offset 0, :y-offset 4}
  ;;     {:width 384.76800000000003, :height 216.43200000000002, :x-offset 388.76800000000003, :y-offset 4}
  ;;     {:width 432.86400000000003, :height 216.43200000000002, :x-offset 777.5360000000001, :y-offset 4}
  ;;     {:width 501.6282612020187, :height 282.16589692613553, :x-offset 0, :y-offset 224.43200000000002}
  ;;     {:width 501.6282612020187, :height 282.16589692613553, :x-offset 505.6282612020187, :y-offset 224.43200000000002}
  ;;     {:width 199.1434775959627, :height 282.16589692613553, :x-offset 1011.2565224040374, :y-offset 224.43200000000002}
  ;;     {:width 196.57030865682486, :height 295.1506135988361, :x-offset 0, :y-offset 510.59789692613555}
  ;;     {:width 1009.8296913431751, :height 295.1506135988361, :x-offset 200.57030865682486, :y-offset 510.59789692613555})

  )
```

With great trepidation, we glance at our browser window... and see wonderful
things!

![A cartoon version of the Drake hotline bling meme: Drake is delighted by our album and delighted by the Flickr UI][yay]
[yay]: assets/2024-01-22-yay.png "They keep callin' on my cellphone, my cellphone" width=800px

I would actually argue that our album is more aesthetically pleasing than the
Flickr version, since it doesn't have a huge gap at the lower right-hand corner.
😍

## Are you feeling dynamic?

As happy as we are with ourselves (and deservedly so), we still have three rules
left to follow:

**5. As the window is resized, photos should be rescaled to fit nicely**

**6. If the window is too small to display three photos per row, display two per row instead, and expand the album to fill the width of the window**

**7. If the window is too small to display two photos per row, display one per row instead**

Rule #5 is fairly straightforward. If we create a function that will update the
page by calling `display-album!` with the correct arguments, we can add an event
handler to the browser
[window](https://developer.mozilla.org/en-US/docs/Web/API/Window):

``` clojure
(defn update-page! [& _]
  (display-album! config photos))

(.addEventListener js/window "resize" update-page!)
(update-page!)
```

We'll also call `update-page!` when the page loads for the first time. If we
evaluate the buffer, nothing seems to happen, but if we resize the window now,
our photos rescale! 🎉

Now let's figure out how to handle rules #6 and #7. What we can do is define a
list of minimum photo widths that will cause a reduction in the number of
photos per row:

``` clojure
(def config
  {:album-div-name "album"
   :album-min-width 900
   :album-width-pct 0.8
   :min-photo-widths [275 240]
   :num-photos-per-row 3
   :photo-padding 4})
```

We can then compute the number of photos per row by trying the default value,
and if that results in the average photo width dipping below the first minimum
width, reduce the photos per row by one and see if the new average width is
smaller than the next minimum width, and so on. If you don't know what I mean,
don't worry; this is probably easier to say in Clojure than in English:

``` clojure
(defn get-num-photos-per-row
  [{:keys [min-photo-widths num-photos-per-row] :as config} album-width]
  (loop [num-photos num-photos-per-row
         [min-width & min-widths] min-photo-widths]
    (let [avg-width (/ album-width num-photos)]
      (if (or (nil? min-width)
              (= 1 num-photos)
              (>= avg-width min-width))
        num-photos
        (recur (dec num-photos) min-widths)))))

(comment

  (get-num-photos-per-row config 1200)
  ;; => 3

  (get-num-photos-per-row config 800)
  ;; => 2

  (get-num-photos-per-row config 400)
  ;; => 1

  (get-num-photos-per-row config 100)
  ;; => 1

  (get-num-photos-per-row (assoc config :num-photos-per-row 4) 1600)
  ;; => 4

  )
```

Now if we hook this into `display-album!`, we should see the number of photos
per row change as we resize the window:

``` clojure
(defn display-album!
  [{:keys [num-photos-per-row photo-padding] :as config} photos]
  (let [album-width (set-album-width! config)
        num-photos-per-row (get-num-photos-per-row config album-width)
        config (assoc config
                      :album-width album-width
                      :num-photos-per-row num-photos-per-row)
        padding-width (* photo-padding (dec num-photos-per-row))
        photo-width (-> (- album-width padding-width)
                        (/ num-photos-per-row))]
    (->> photos
         (arrange-photos config)
         (map (partial set-photo-styles! config))
         doall)))
```

Let's see:

![The album with a window size of 906 and 3 photos per row][three]
[three]: assets/2024-01-22-three.png "906 pixels gives us 3 photos per row"

![The album with a window size of 754 and 3 photos per row][two]
[two]: assets/2024-01-22-two.png "754 pixels gives us 3 photos per row"

![The album with a window size of 451 and 3 photos per row][one]
[one]: assets/2024-01-22-one.png "451 pixels gives us 3 photos per row"

Pretty pretty pretty pretty cool.

And with that, let's declare victory! There are of course many features that
could be added, such as:

- Making the "← Back to albums list" actually take you back to a list of albums
  (which doesn't currently exist)
- Clicking on a photo to display it full size
- Displaying the photo title and description when mousing over it, like Flickr
  does

and so on and so forth. And who knows what the future will bring; there may yet
be another post describing how to implement one or more of those things, but I
think that 10,924 words is enough for one post, don't you?

Part 1: [clickr, or a young man's Flickr clonejure](2024-01-17-clickr.html)
