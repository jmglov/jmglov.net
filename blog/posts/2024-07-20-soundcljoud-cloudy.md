Title: Soundcljoud gets more cloudy
Date: 2024-07-20
Tags: clojure,babashka,scittle,clonejure,clojurescript,soundcljoud
Description: In which I actually clone Soundcloud with a judicious application of Scittle
Discuss: https://clojurians.slack.com/archives/C8NUSGWG6/p1721475406408939
Image: assets/2024-07-20-soundcljoud-cloudy-preview.png
Image-Alt: A logo of a face wearing a red hoodie with orange sunglasses featuring the Soundcloud logo

![A logo of a face wearing a red hoodie with orange sunglasses featuring the Soundcloud logo][preview]
[preview]: assets/2024-07-20-soundcljoud-bb.png "Above the clouds, infinite skills create miracles"

[Last time](2024-07-09-soundcljoud.html) on "Soundcljoud, or a young man's
Soundcloud clonejure", I promised to clone Soundcloud, but then got bogged down
in telling [the story of my
life](2024-07-09-soundcljoud.html#rambling_exposition) and never got around to
the actual cloning part. 😬

To be fair to myself, I did do [a bunch of
stuff](2024-07-09-soundcljoud.html#omg_finally_stuff_about_clojure) to prepare
for cloning, so now we can get to it with no further ado! (Skipping the ado bit
is very out of character for me, I know. I'll just claim this parenthetical as
my ado and thus fulfil your expectations of me as the most verbose writer in the
Clojure community. You're welcome!)

## Popping in a Scittle

If you've followed along with any of [my other cloning
adventures](tags/clonejure.html), you'll know where I'm going with this:
straight to [Scittle](https://github.com/babashka/scittle/) Town!

I'll start by creating a `player` directory and dropping a `bb.edn` into it:

``` clojure
{:deps {io.github.babashka/sci.nrepl
        {:git/sha "2f8a9ed2d39a1b09d2b4d34d95494b56468f4a23"}
        io.github.babashka/http-server
        {:git/sha "b38c1f16ad2c618adae2c3b102a5520c261a7dd3"}}
 :tasks {http-server {:doc "Starts http server for serving static files"
                      :requires ([babashka.http-server :as http])
                      :task (do (http/serve {:port 1341 :dir "public"})
                                (println "Serving static assets at http://localhost:1341"))}

         browser-nrepl {:doc "Start browser nREPL"
                        :requires ([sci.nrepl.browser-server :as bp])
                        :task (bp/start! {})}

         -dev {:depends [http-server browser-nrepl]}

         dev {:task (do (run '-dev {:parallel true})
                        (deref (promise)))}}}
```

In short, what's happening here is I'm setting up a Babashka project with a
`dev` task that starts a webserver on port 1341 serving up the files in the
`public/` directory, starts an nREPL server on port 1339 that we can connect to
with Emacs (or any inferior text editor of your choosing), and a websocket
server on port 1340 that is connected to the nREPL server on one end and waiting
for a ClojureScript app to connect to the other end.

Speaking of the `public/` directory, I need a `public/index.html` file to serve
up:

``` html
<!doctype html>
<html class="no-js" lang="">

<head>
    <meta charset="utf-8">
    <meta http-equiv="x-ua-compatible" content="ie=edge">
    <title>Soundcljoud</title>
    <meta name="description" content="">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="style.css">

    <script src="https://cdn.jsdelivr.net/npm/scittle@0.6.15/dist/scittle.js" type="application/javascript"></script>
    <script src="https://cdn.jsdelivr.net/npm/scittle@0.6.15/dist/scittle.promesa.js" type="application/javascript"></script>
    <script>var SCITTLE_NREPL_WEBSOCKET_PORT = 1340;</script>
    <script src="https://cdn.jsdelivr.net/npm/scittle@0.6.15/dist/scittle.nrepl.js"
        type="application/javascript"></script>
    <script type="application/x-scittle" src="soundcljoud.cljs"></script>
</head>

<body>
  <h1>Soundcljoud</h1>
  <div id="wrapper" style="display: none;">
    <div id="player">
      <div class="cover-image">
        <img src="" alt="" />
      </div>
      <div id="controls">
        <audio controls src=""></audio>
        <div id="tracks" style=""></div>
      </div>
    </div>
  </div>
</body>

</html>
```

The `index.html` file loads three JavaScript scripts:
1. Scittle itself, which knows how to interpret ClojureScript scripts
2. The Scittle [Promesa](https://github.com/funcool/promesa) plugin, which
   provides some niceties for dealing with promises
3. The Scittle nREPL plugin, which will connect to that websocket server on port
   1340 and complete the circuit that will allow us to REPL-drive our browser
   from Emacs (or the inferior text editor of your choosing)

Once this JavaScript is in place, `index.html` loads the `soundcljoud.cljs`
ClojureScript file, which we'll come to in just a second.

For a (much) more detailed explanation, refer to the [Popping in a
Scittle](2024-02-22-cljcastr.html#popping_in_a_scittle) section of my [cljcastr,
or a young man's Zencastr clonejure](2024-02-22-cljcastr.html) blog post.

The body of `index.html` is all about setting up a basic HTML page with this
structure:

``` text
+----------------------+
| Soundcljoud          |
+-------+--------------+  <---
| Album | Audio player |      }
| cover +--------------+      } <div id="wrapper">
| image | Tracks list  |      }
+-------+--------------+  <---
```

Note that everything inside the wrapper div is hidden from the start:

``` html
  <div id="wrapper" style="display: none;">
```

We don't know anything about the album we want to display yet, and there's no
point in showing a bunch of empty divs until we do.

Let's drop a `public/style.css` in as well:

``` css
body {
  font:
    1.2em Helvetica,
    Arial,
    sans-serif;
  margin: 20px;
  padding: 0;
}

img {
  max-width: 100%;
}

#wrapper {
  max-width: 960px;
  margin: 2em auto;
}

#controls {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

#tracks {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

@media screen and (min-width: 900px) {
  #wrapper {
    display: flex;
  }

  #player {
    display: flex;
    gap: 3%;
  }

  #cover-image {
    margin-right: 5%;
    max-width: 60%;
  }

  #controls {
    width: 25%;
  }
}
```

All of this stuff is about using screen real estate effectively. The first chunk
of CSS applies universally, but the bit inside this:

``` css
@media screen and (min-width: 900px) {
  /* ... */
}
```

only applies to windows at least 900px wide. So our page defaults to a layout
that's appropriate for phones (or really narrow browser windows), but then
adjusts to move more content "above the fold" so you can probably see the entire
UI without scrolling if you're viewing the page on a standard computer.

Now that we have all of the HTML and CSS plumbing in place, let's add a
`public/soundcljoud.cljs` file to get started with some ClojureScripting:

``` clojure
(ns soundcljoud
  (:require [promesa.core :as p]))
```

## Firing up the REPL

Before we can start REPL-driving, we need to put the key in the ignition and
give it a right twist! In other words, we open up a terminal in the top-level
`player/` directory and invoke Babashka:

``` text
: jmglov@alhana; bb dev
Serving static assets at http://localhost:1341
nREPL server started on port 1339...
Websocket server started on 1340...
```

If we now connect to [http://localhost:1341/](http://localhost:1341/), we'll be
rewarded with a simple webpage:

![Screenshot of a web browser window saying Soundcljoud][boring]
[boring]: assets/2024-07-20-soundcljoud-cloudy-boring.png "All that buildup for this?" width=800 border=1

This by itself is of course monumentally boring, so let's inject some excitement
into our lives by jumping into `soundcljoud.cljs` and pressing `C-c l C`
(`cider-connect-cljs`), selecting `localhost`, port 1339, and `nbb` for the REPL
type (assuming you're in Emacs; if you're using some other editor, perform the
incantations necessary to connect your ClojureScript REPL to localhost:1339).

If everything went according to plan, you should see something like this in your
terminal window:

``` text
:msg "{:versions
       {\"scittle-nrepl\"
        {\"major\" \"0\", \"minor\" \"0\", \"incremental\" \"1\"}},
       :ops
       {\"complete\" {}, \"info\" {}, \"lookup\" {}, \"eval\" {},
        \"load-file\" {}, \"describe\" {}, \"close\" {}, \"clone\" {},
        \"eldoc\" {}},
       :status [\"done\"],
       :id \"3\",
       :session \"3264dc1e-1b46-48a6-b11a-f606fea032b7\",
       :ns \"soundcljoud\"}"
:msg "{:value \"nil\",
       :id \"5\",
       :session \"3264dc1e-1b46-48a6-b11a-f606fea032b7\",
       :ns \"soundcljoud\"}"
:msg "{:status [\"done\"],
       :id \"5\",
       :session \"3264dc1e-1b46-48a6-b11a-f606fea032b7\",
       :ns \"soundcljoud\"}"
```

And something like this in your editor's REPL window:

``` text
;; Connected to nREPL server - nrepl://localhost:1339
;; CIDER 1.12.0 (Split)
;;
;; ClojureScript REPL type: nbb
;;
nil> 
```

Let's prove that it works by evaluating the buffer with `C-c C-k`
(`cider-load-buffer`), adding a [Rich
comment](https://betweentwoparens.com/blog/rich-comment-blocks/#rich-comment),
putting some ClojureScript in there that grabs our wrapper div, positioning our
cursor at the end of the form, and evaluating that sucker with `C-c C-v f c e`
(`cider-pprint-eval-last-sexp-to-comment`):

``` clojure
(ns soundcljoud
  (:require [promesa.core :as p]))

(comment

  (js/document.querySelector "#wrapper")
  ;; => #object[HTMLDivElement [object HTMLDivElement]]

)
```

We've proven that we can evaluate ClojureScript code in the running browser
process from our REPL buffer, which is nifty for sure, but our page still bores
us, and the result of evaluating that code is pretty useless:

``` text
#object[HTMLDivElement [object HTMLDivElement]]
```

Let's actually do something with the div we've pulled down, and whilst we're at
it, provide a useful way of logging stuff:

``` clojure
(ns soundcljoud
  (:require [promesa.core :as p]))

(defn log
  ([msg]
   (log msg nil))
  ([msg obj]
   (if obj
     (js/console.log msg obj)
     (js/console.log msg))
   obj))

(comment

  (let [div (js/document.querySelector "#wrapper")]
    (set! (.-style div) "display: flex")
    (log "All is revealed!" div))
  ;; => #object[HTMLDivElement [object HTMLDivElement]]

)
```

![Screenshot of a web browser window with an audio player][revealed]
[revealed]: assets/2024-07-20-soundcljoud-cloudy-revealed.png "Players players everywhere but not a track to play!" width=800 border=1

Fantastic! By using `js/document.log` (by the way, that `js/` prefix is the way
you instruct ClojureScript to do some JavaScript interop; it's basically saying
"look for the next symbol in the top-level scope in JavaScript land"), we now
get the fancy inspection tools in the browser's JavaScript console so we can
expand parts of the object and drill down to see stuff we're interested in.

Now that we've established a baseline, we can get stuck in and do some real
work. 💪🏻

## Reading some RSS

Do you remember the [MP3 files and RSS
feed](2024-07-09-soundcljoud.html#converting_from_ogg_to_mp3) we prepared in the
previous blog post? Let's plop those down in our `public/` directory so we can
access them from the webapp we're slowly constructing:

``` text
: jmglov@alhana; mkdir -p 'public/Garth Brooks/Fresh Horses'

: jmglov@alhana; cp /tmp/soundcljoud.12524185230907219576/*.{rss,mp3} !$

: jmglov@alhana; ls -1 !$
album.rss
'Garth Brooks - Cowboys and Angels.mp3'
'Garth Brooks - Ireland.mp3'
"Garth Brooks - It's Midnight Cinderella.mp3"
"Garth Brooks - Rollin'.mp3"
"Garth Brooks - She's Every Woman.mp3"
"Garth Brooks - That Ol' Wind.mp3"
'Garth Brooks - The Beaches of Cheyenne.mp3'
'Garth Brooks - The Change.mp3'
'Garth Brooks - The Fever.mp3'
'Garth Brooks - The Old Stuff.mp3'
```

Now that our files are in place, let's see about loading the RSS feed from
ClojureScript:

``` clojure
(comment

  (def base-path "/Garth+Brooks/Fresh+Horses")
  ;; => #'soundcljoud/base-path

  (p/->> (js/fetch (js/Request. (str base-path "/album.rss")))
         (.text)
         (log "Fetched XML:"))
  ;; => #<Promise[~]>

)
```

In our console, we can see what we fetched:

``` text
Fetched XML: <?xml version='1.0' encoding='UTF-8'?>
<rss version="2.0"
     xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"
     xmlns:atom="http://www.w3.org/2005/Atom">
  <channel>
    <atom:link
        href="http://localhost:1341/Garth+Brooks/Fresh+Horses/album.rss"
        rel="self"
        type="application/rss+xml"/>
    <title>Garth Brooks - Fresh Horses</title>
    <link>https://api.discogs.com/masters/212114</link>
    <pubDate>Sun, 01 Jan 1995 00:00:00 +0000</pubDate>
    <itunes:subtitle>Album: Garth Brooks - Fresh Horses</itunes:subtitle>
    <itunes:author>Garth Brooks</itunes:author>
    <itunes:image href="https://i.discogs.com/0eLXmM1tK1grkH8cstgDT6eV2TlL0NvgWPZBoyScJ_8/rs:fit/g:sm/q:90/h:600/w:600/czM6Ly9kaXNjb2dz/LWRhdGFiYXNlLWlt/YWdlcy9SLTY4NDcx/Ny0xNzE3NDU5MDIy/LTMxNjguanBlZw.jpeg"/>
    
    <item>
      <itunes:title>The Old Stuff</itunes:title>
      <title>The Old Stuff</title>
      <itunes:author>Garth Brooks</itunes:author>
      <enclosure
          url="http://localhost:1341/Garth+Brooks/Fresh+Horses/Garth+Brooks+-+The+Old+Stuff.mp3"
          length="5943424" type="audio/mpeg" />
      <pubDate>Sun, 01 Jan 1995 00:00:00 +0000</pubDate>
      <itunes:duration>252</itunes:duration>
      <itunes:episode>1</itunes:episode>
      <itunes:episodeType>full</itunes:episodeType>
      <itunes:explicit>false</itunes:explicit>
    </item>
   ... 
    <item>
      <itunes:title>Ireland</itunes:title>
      <title>Ireland</title>
      <itunes:author>Garth Brooks</itunes:author>
      <enclosure
          url="http://localhost:1341/Garth+Brooks/Fresh+Horses/Garth+Brooks+-+Ireland.mp3"
          length="6969472" type="audio/mpeg" />
      <pubDate>Sun, 01 Jan 1995 00:00:00 +0000</pubDate>
      <itunes:duration>301</itunes:duration>
      <itunes:episode>10</itunes:episode>
      <itunes:episodeType>full</itunes:episodeType>
      <itunes:explicit>false</itunes:explicit>
    </item>
    
  </channel>
</rss>
```

That looks quite familiar! That also looks like a bunch of text, which is not
the nicest thing to extract data from. Luckily, that's a bunch of structured
text, and more luckily, it's XML (XML is great, and don't let anyone tell you
otherwise! And don't get me started on how we've reinvented XML but poorly with
JSON Schema and all of this other nonsense we've built up around JSON because we
realised that things like data validation are important when exchanging data
between machines. 🤦🏼‍♂️), and most luckily of all, browsers know how to parse
XML (which makes sense, as modern HTML is in fact XML):

``` clojure
(defn parse-xml [xml-str]
  (.parseFromString (js/window.DOMParser.) xml-str "text/xml"))

(comment

  (p/->> (js/fetch (js/Request. (str base-path "/album.rss")))
         (.text)
         parse-xml
         (log "Fetched XML:"))
  ;; => #<Promise[~]>

)
```

![Screenshot of a web browser window with an XML document in the JS console][xml]
[xml]: assets/2024-07-20-soundcljoud-cloudy-xml.png "XML, anyone?" width=800 border=1

Let's do the right thing and make a function out of this:

``` clojure
(defn fetch-xml [path]
  (p/->> (js/fetch (js/Request. path))
         (.text)
         parse-xml
         (log "Fetched XML:")))
```

Now that we know how to fetch and parse XML, let's see how to extract useful
information from it. Looking at the log output, we can see that the parsed XML
is of type `#document`, just like our good friend `js/document` (the current
webpage that the browser is displaying). That's right, we have a Document Object
Model, which means we can use all the tasty DOM functions we're used to, such as
`document.querySelector()` to grab a node using an XPATH query.

Let's start with the album title:

``` clojure
(comment

  (p/let [title (p/-> (fetch-xml (str base-path "/album.rss"))
                      (.querySelector "title")
                      (.-innerHTML))]
    (set! (.-innerHTML (js/document.querySelector "h1")) title))
  ;; => #<Promise[~]>

)
```

Cool! We now see "Garth Brooks - Fresh Horses" as our page heading! Let's see
about grabbing the album art next:

``` clojure
(comment

  (p/let [xml (fetch-xml (str base-path "/album.rss"))
          title (p/-> xml
                      (.querySelector "title")
                      (.-innerHTML))
          image (p/-> xml
                      (.querySelector "image")
                      (.getAttribute "href"))]
    (set! (.-innerHTML (js/document.querySelector "h1")) title)
    (set! (.-src (js/document.querySelector ".cover-image > img")) image)
    (set! (.-style (js/document.querySelector "#wrapper")) "display: flex;"))
  ;; => #<Promise[~]>

)
```

![Screenshot of a web browser window with the album art for Fresh Horses and an audio player][garth]
[garth]: assets/2024-07-20-soundcljoud-cloudy-garth.png "Almost time to ride out!" width=800 border=1

Before we go any further, let's create some functions from this big blob of
code. At the moment, we're complecting two things:
1. Extracting data from the XML DOM
2. Updating the HTML DOM to display the data

Let's do the functional programming thing and create a purely functional core
and a mutable shell. Instead of extracting and updating, we'll create a function
that transforms the XML DOM representation of an album into a ClojureScript
representation:

``` clojure
(defn xml-get [el k]
  (-> el
      (.querySelector k)
      (.-innerHTML)))

(defn xml-get-attr [el k attr]
  (-> el
      (.querySelector k)
      (.getAttribute attr)))

(defn ->album [xml]
  {:title (xml-get xml "title")
   :image (xml-get-attr xml "image" "href")})

(defn load-album [path]
  (p/-> (fetch-xml path) ->album))

(comment

  (p/let [{:keys [title image] :as album} (load-album (str base-path "/album.rss"))]
    (set! (.-innerHTML (js/document.querySelector "h1")) title)
    (set! (.-src (js/document.querySelector ".cover-image > img")) image)
    (set! (.-style (js/document.querySelector "#wrapper")) "display: flex;"))
  ;; => #<Promise[~]>

)
```

Now that we have a nice ClojureScript data structure to represent our album,
let's tackle the DOM mutations we need to do to display the album:

``` clojure
(defn get-el [selector]
  (if (instance? js/HTMLElement selector)
    selector  ; already an element; just return it
    (js/document.querySelector selector)))

(defn set-styles! [el styles]
  (set! (.-style el) styles))

(defn display-album! [{:keys [title image] :as album}]
  (let [header (get-el "h1")
        cover (get-el ".cover-image > img")
        wrapper (get-el "#wrapper")]
    (set! (.-innerHTML header) title)
    (set! (.-src cover) image)
    (set-styles! wrapper "display: flex;")
    album))

(comment

  (p/-> (load-album (str base-path "/album.rss")) display-album!)
  ;; => #<Promise[~]>

)
```

## Tracking down the tracks

Displaying the album title and cover art is all well and good, but in order to
complete our Soundcloud clone, we need some way of actually listening to the
music on the album. If you recall, our RSS feed contains a series of `<item>`
tags representing the tracks:

``` xml
    <item>
      <itunes:title>The Old Stuff</itunes:title>
      <title>The Old Stuff</title>
      <itunes:author>Garth Brooks</itunes:author>
      <enclosure
          url="http://localhost:1341/Garth+Brooks/Fresh+Horses/Garth+Brooks+-+The+Old+Stuff.mp3"
          length="5943424" type="audio/mpeg" />
      <pubDate>Sun, 01 Jan 1995 00:00:00 +0000</pubDate>
      <itunes:duration>252</itunes:duration>
      <itunes:episode>1</itunes:episode>
      <itunes:episodeType>full</itunes:episodeType>
      <itunes:explicit>false</itunes:explicit>
    </item>
   ... 
    <item>
      <itunes:title>Ireland</itunes:title>
      <title>Ireland</title>
      <itunes:author>Garth Brooks</itunes:author>
      <enclosure
          url="http://localhost:1341/Garth+Brooks/Fresh+Horses/Garth+Brooks+-+Ireland.mp3"
          length="6969472" type="audio/mpeg" />
      <pubDate>Sun, 01 Jan 1995 00:00:00 +0000</pubDate>
      <itunes:duration>301</itunes:duration>
      <itunes:episode>10</itunes:episode>
      <itunes:episodeType>full</itunes:episodeType>
      <itunes:explicit>false</itunes:explicit>
    </item>
```

What we need from each item in order to display and play the track is:
- Song title
- Artist (for this album, all tracks are from Garth, but an album could be a
  compilation of songs by different artists, so let's grab the artist
  in case we later decide to display it)
- Track number
- URL of the source audio

Let's write an aspirational function that assumes it will be called with a DOM
element representing an `<item>` and transforms it into a ClojureScript map,
just as we did for the item itself:

``` clojure
(defn ->track [item-el]
  {:artist (xml-get item-el "author")
   :title (xml-get item-el "title")
   :number (-> (xml-get item-el "episode") js/parseInt)
   :src (xml-get-attr item-el "enclosure" "url")})
```

For the track number, we need to convert it to an integer, since the text
contents of an XML elements are, well, text, and we'll want to sort our tracks
numerically.

Now that we have a function to convert an `<item>` into a track, let's plug that
into our `->album` function to add a list of tracks to the album:

``` clojure
(defn ->album [xml]
  {:title (xml-get xml "title")
   :image (xml-get-attr xml "image" "href")
   :tracks (->> (.querySelectorAll xml "item")
                (map ->track)
                (sort-by :number))})
```

OK, we have data representing a list of tracks, so we need to consider how we
want to display it. If we cast our mind back to our HTML, we have a div where
the tracks should go:

``` html
<body>
  ...
  <div id="wrapper" style="display: none;">
    <div id="player">
      ...
      <div id="controls">
        ...
        <div id="tracks" style=""></div>
      </div>
    </div>
  </div>
</body>
```

What we can do is create a `<span>` for each track, something like this:

``` html
<span>1. The Old Stuff</span>
```

Let's go ahead and write that function:

``` clojure
(defn track->span [{:keys [number artist title] :as track}]
  (let [span (js/document.createElement "span")]
    (set! (.-innerHTML span) (str number ". " title))
    span))

(comment

  (p/->> (load-album (str base-path "/album.rss"))
         :tracks
         first
         track->span
         (log "The first track is:"))
  ;; => #<Promise[~]>

)
```

In the JavaScript console, we see:

``` text
The first track is: <span>1. The Old Stuff</span>
```

This is cool, because the `track->span` function is still pure—there's no
mutation occurring there. We have one and only one place where that's doing
mutation, and that's `display-album!`, which is where we can hook into our
functional core and display the tracks. In order to do that, we'll take our list
of tracks, turn them into a list of `<span>` elements, and then set them as the
children of the `#tracks` div.

``` clojure
(defn set-children! [el children]
  (.replaceChildren el)
  (doseq [child children]
    (.appendChild el child))
  el)

(defn display-album! [{:keys [title image tracks] :as album}]
  (let [header (get-el "h1")
        cover (get-el ".cover-image > img")
        wrapper (get-el "#wrapper")]
    (set! (.-innerHTML header) title)
    (set! (.-src cover) image)
    (->> tracks
         (map track->span)
         (set-children! (get-el "#tracks")))
    (set-styles! wrapper "display: flex;")
    album))

(comment

  (p/-> (load-album (str base-path "/album.rss")) display-album!)
  ;; => #<Promise[~]>

)
```

![Screenshot of a web browser window with the album art for Fresh Horses, an audio player, and a list of tracks][tracks]
[tracks]: assets/2024-07-20-soundcljoud-cloudy-tracks.png "That tracks" width=800 border=1

This is fantastic... if all we want to do is know what's on an album. But of
course my initial problem was wanting to **listen** to Garth and not having a
way to do that. Now I have written much Clojure and ClojureScript, and still
cannot listen to Garth. 🤔

## Play it again, Sam

Of course what I do have is an HTML `<audio>` element and an MP3 file with a
source URL, and I bet if I can just put these two things together, my ears will
soon be filled with the sweet sweet sounds of 90s country music.

Let's start out with the simplest thing we can do, which is to activate the
first track on the album once it's loaded. Since `display-album!` returns the
album, we can just add some code to the end of the pipeline:

``` clojure
(comment

  (def base-path "http://localhost:1341/Garth+Brooks/Fresh+Horses")
  ;; => #'soundcljoud/base-path

  (p/->> (load-album (str base-path "/album.rss"))
         display-album!
         :tracks
         first
         :src
         (set! (.-src (get-el "audio"))))
  ;; => #<Promise[~]>

)
```

As soon as we evaluate this code, the `<audio>` element comes to life,
displaying a duration and activating the play button. Pressing the play button,
we do in fact hear some Garth! 🎉

However, our UX is quite poor, since there's no visual representation of which
track is playing. We can fix this by emboldening the active track:

``` clojure
(comment

  (p/let [{:keys [number src] :as track}
          (p/->> (load-album (str base-path "/album.rss"))
                 display-album!
                 :tracks
                 first)]
    (-> (get-el "#tracks")
        (.-children)
        seq
        (nth (dec number))
        (set-styles! "font-weight: bold;"))
    (set! (.-src (get-el "audio")) src))
  ;; => #<Promise[~]>

)
```

![Screenshot of our UI with the first track highlighted and loaded in the audio element][play]
[play]: assets/2024-07-20-soundcljoud-cloudy-play.png "That's a bold move!" width=800 border=1

Speaking of UX, though, one would imagine that they'd be able to change to a track
by clicking on it. At the moment, clicking does nothing, but that's easy enough
to fix by adding an event handler to our span for each track that activates the
track. Let's create a function and shovel our track activating code in there:

``` clojure
(defn activate-track! [{:keys [number src] :as track}]
  (log "Activating track:" (clj->js track))
  (let [track-spans (seq (.-children (get-el "#tracks")))]
    (-> track-spans
        (nth (dec number))
        (set-styles! "font-weight: bold;")))
  (set! (.-src (get-el "audio")) src)
  track)
```

By the way, that `clj->js` function takes a ClojureScript data structure (in
this case, our track map) and recursively transforms it into a JavaScript object
so it can be printed nicely in the JS console.

OK, now that we have `activate-track!` as a function, we can use it in a click
handler:

``` clojure
(defn track->span [{:keys [number title] :as track}]
  (let [span (js/document.createElement "span")]
    (set! (.-innerHTML span) (str number ". " title))
    (.addEventListener span "click" (partial activate-track! track))
    span))

(comment

  (p/-> (load-album (str base-path "/album.rss"))
        display-album!
        :tracks
        first
        activate-track!)
  ;; => #<Promise[~]>

)
```

Evaluating this code activates the first track on the album as before, and then
clicking another track highlights it in bold and loads it into the `<audio>`
element. That's good, but what isn't so good is that the first track stays bold.
😬

Luckily, there's an easy fix for this. All we need to do is reset the weight of
all the track spans before bolding the active one in `activate-track!`:

``` clojure
(defn activate-track! [{:keys [number src] :as track}]
  (log "Activating track:" (clj->js track))
  (let [track-spans (seq (.-children (get-el "#tracks")))]
    (doseq [span track-spans]
      (set-styles! span "font-weight: normal;"))
    (-> track-spans
        (nth (dec number))
        (set-styles! "font-weight: bold;")))
  (set! (.-src (get-el "audio")) src)
  track)
```

Amazing!

Whilst we're ticking off UX issues, let's think about what should happen when
our user clicks on a different track. At the moment, we load the track into the
player and then the user has to click the play button to start listening to it.
That is perfectly reasonable when first loading the album, but if I'm listening
to a track and then select another one, I would kinda expect the new track to
start playing automatically instead of me having to click play manually.

Let's see how we can do this. According to the
[HTMLMediaElement](https://developer.mozilla.org/en-US/docs/Web/API/HTMLMediaElement)
documentation, our `<audio>` element should have `paused` attribute telling us
whether playback is happening. Let's try it out:

``` clojure
(comment

  (p/-> (load-album (str base-path "/album.rss"))
        display-album!
        :tracks
        first
        activate-track!)
  ;; => #<Promise[~]>

  (-> (get-el "audio")
      (.-paused))
  ;; => true

)
```

Now if we click the play button and check the value of the `paused` attribute
again:

``` clojure
(comment

  (-> (get-el "audio")
      (.-paused))
  ;; => false

)
```

Excellent! Now let's see how we programatically start playing a newly loaded
track. Referring back to the documentation, we discover a
[HTMLMediaElement.play()](https://developer.mozilla.org/en-US/docs/Web/API/HTMLMediaElement/play)
method. Let's try that out:

``` clojure
(comment

  (p/-> (load-album (str base-path "/album.rss"))
        display-album!
        :tracks
        second
        activate-track!)
  ;; => #<Promise[~]>

  (-> (get-el "audio")
      (.play))
  ;; => #<Promise[~]>

)
```

Evaluating this code results in "Cowboys and Angels" starting to play!

Now we can use what we've learned to teach `activate-track!` to start playing
the track when appropriate:

``` clojure
(defn activate-track! [{:keys [number src] :as track}]
  (log "Activating track:" (clj->js track))
  (let [track-spans (seq (.-children (get-el "#tracks")))
        audio-el (get-el "audio")
        paused? (.-paused audio-el)]
    (doseq [span track-spans]
      (set-styles! span "font-weight: normal;"))
    (-> track-spans
        (nth (dec number))
        (set-styles! "font-weight: bold;"))
    (set! (.-src audio-el) src)
    (when-not paused?
      (.play audio-el)))
  track)

(comment

  (p/-> (load-album (str base-path "/album.rss"))
        display-album!
        :tracks
        first
        activate-track!)
  ;; => #<Promise[~]>

)
```

When the album loads, the first track is activated but doesn't start playing.
Clicking on another track activates it but doesn't start playing it. However, if
we click the play button and start listening to the active track, then click on
another track, the new track is activated and immediately starts playing.

This, my friends, is some seriously good UX! Of course, we can improve it
further.

## Keep playing it, Sam

The next UX nit that we should pick is the fact that when a track ends, our poor
user has to manually click on the next track and then manually click the play
button just to keep listening to the album. This seems a bit mean of us, so
let's see what we can do in order to be the nice people that we know we are,
deep down inside.

Our good friend HTMLMediaElement has [a bunch of
events](https://developer.mozilla.org/en-US/docs/Web/API/HTMLMediaElement#events)
that tell us useful things about what's happening with the media, and one of
these events is `ended`:

> Fired when playback stops when end of the media (<audio> or <video>) is
> reached or because no further data is available.

This seems like it will fit the bill quite nicely. Hopping back in our hammock
for a minute, we think about what should happen when the end of a track is
reached:

- The next track is activated and starts playing, unless
- It's the last track on the album, in which case nothing should happen.

We can of course add a `ended` event listener to the `<audio>` element every time
a new track is activated, but this is problematic because we would then want to
remove the previous event listener, and it turns out that [removing event
listeners is a bit
complicated](https://developer.mozilla.org/en-US/docs/Web/API/EventTarget/removeEventListener#matching_event_listeners_for_removal).
What if we instead had an event listener that knew what track was currently
playing, where that track comes in the album, and what track (if any) is next?
Then we'd only have to attach a listener once, right after we load the album.
Let's think through how we could do that.

So far, we've been relying on the state of the DOM to tell us things like if the
track is paused. A much more functional approach would be to control the state
ourselves using immutable data structures and so on. A nice side effect of this
(sorry, Haskell folks, Clojurists are just fine with uncontrolled side effects)
is that it actually makes REPL-driven development easier as well! 🤯

Let's start by extracting a function to handle the tedium of loading the album,
displaying it, and then activating the first track:

``` clojure
(defn load-ui! [dir]
  (p/->> (load-album (str dir "/album.rss"))
         display-album!
         :tracks
         first
         activate-track!))
```

Now that we have this, we'll define a top-level
[atom](https://clojure.org/reference/atoms) to hold the state, then update our
`load-ui!` function to stuff the album into the atom once it's loaded:

``` clojure
(def state (atom nil))

(defn load-ui! [dir]
  (p/->> (load-album (str dir "/album.rss"))
         display-album!
         (assoc {} :album)
         (reset! state)
         :album
         :tracks
         first
         activate-track!))
```

What we're doing here is creating a map to hold the state, then assoc-ing the
loaded album into the map under the `:album` key, then putting that map into the
`state` atom with [reset!](https://clojuredocs.org/clojure.core/reset%21), which
returns the new value saved in the atom, which is the one we just put in there,
which will look like this:

``` clojure
{:title "Garth Brooks - Fresh Horses",
 :image "https://i.discogs.com/.../LTMxNjguanBlZw.jpeg",
 :tracks
 ({:artist "Garth Brooks",
   :title "The Old Stuff",
   :number 1,
   :src "http://localhost:1341/Garth+Brooks/Fresh+Horses/Garth+Brooks+-+The+Old+Stuff.mp3"}
  ...
  {:artist "Garth Brooks",
   :title "Ireland",
   :number 10,
   :src "http://localhost:1341/Garth+Brooks/Fresh+Horses/Garth+Brooks+-+Ireland.mp3"}), :paused? true}
```

We'll then grab the album back out of the map and proceed as before to activate
the first track. This is a little gross, but we'll clean it up as we go.

Oh yeah, and remember when I promised this would make debugging easier? Check
this out:

``` clojure
(comment

  (load-ui! "http://localhost:1341/Garth+Brooks/Fresh+Horses")
  ;; => #<Promise[~]>

  @state
  ;; => {:album {:title "Garth Brooks - Fresh Horses",
  ;;             :image "https://i.discogs.com/.../LTMxNjguanBlZw.jpeg",
  ;;             :tracks
  ;;             ({:artist "Garth Brooks",
  ;;               :title "The Old Stuff",
  ;;               :number 1,
  ;;               :src "http://localhost:1341/Garth+Brooks/Fresh+Horses/Garth+Brooks+-+The+Old+Stuff.mp3"}
  ;;               ...
  ;;               {:artist "Garth Brooks",
  ;;                :title "Ireland",
  ;;                :number 10,
  ;;                :src "http://localhost:1341/Garth+Brooks/Fresh+Horses/Garth+Brooks+-+Ireland.mp3"})}}

)
```

That's right, we no longer have to rely on logging stuff to the JS console in
our promise chains!

OK, but we haven't really changed anything other than making the `load-ui!`
function more complicated. Let's add a little more to our state atom so we can
actually tackle the problem of auto-advancing tracks. First, we'll add a
`:paused?` key:

``` clojure
(defn load-ui! [dir]
  (p/->> (load-album (str dir "/album.rss"))
         display-album!
         (assoc {:paused? true} :album)
         (reset! state)
         :album
         :tracks
         first
         activate-track!))

(comment

  (load-ui! "http://localhost:1341/Garth+Brooks/Fresh+Horses")
  ;; => #<Promise[~]>

  @state
  ;; => {:paused? true, :album {...}}

)
```

Now let's add an event listener to the `<audio>` element that updates the state
when the play button is pressed, doing a little cleanup of the `load-ui!`
function whilst we're at it:

``` clojure
(defn load-ui! [dir]
  (p/let [album (load-album (str dir "/album.rss"))]
    (display-album! album)
    (reset! state {:paused? true, :album album})
    (->> album
         :tracks
         first
         activate-track!)
    (.addEventListener (get-el "audio") "play"
                       #(swap! state assoc :paused? false))))

(comment

  (load-ui! "http://localhost:1341/Garth+Brooks/Fresh+Horses")
  ;; => #<Promise[~]>

  (:paused? @state)
  ;; => true

  ;; Click the play button and...
  (:paused? @state)
  ;; => false

)
```

If you're not familiar with
[swap!](https://clojuredocs.org/clojure.core/swap%21), it takes an atom and a
function which will be called with the current value of the atom, then sets the
next value of the atom to whatever the function returns, just like
[update](https://clojuredocs.org/clojure.core/update) does for plain old maps.
And also just like `update`, it has a shorthand form so that instead of writing
this:

``` clojure
(swap! state #(assoc % :paused? false))
```

you can write this:

``` clojure
(swap! state assoc :paused? false)
```

in which case `swap!` will treat the arg after the atom as a function which will
be called with the current value first, then the rest of the args to `swap!`.
You can imagine that `swap!` is written something like this:

``` clojure
(defn swap!
  ([atom f]
   (reset! atom (f @atom)))
  ([atom f & args]
   (reset! atom (apply f @atom args))))
```

It's obviously not written like that, even though that would technically
probably maybe work. It's actually written like
[this](https://github.com/clojure/clojure/blob/clojure-1.11.1/src/clj/clojure/core.clj#L2362):

``` clojure
(defn swap!
  "Atomically swaps the value of atom to be:
  (apply f current-value-of-atom args). Note that f may be called
  multiple times, and thus should be free of side effects.  Returns
  the value that was swapped in."
  {:added "1.0"
   :static true}
  ([^clojure.lang.IAtom atom f] (.swap atom f))
  ([^clojure.lang.IAtom atom f x] (.swap atom f x))
  ([^clojure.lang.IAtom atom f x y] (.swap atom f x y))
  ([^clojure.lang.IAtom atom f x y & args] (.swap atom f x y args)))
```

But you get the point.

Aaaaaanyway, I seem to have digressed—which is firmly on brand for this blog,
so I apologise for nothing!

But yeah, at this point, we're back to the functionality that we had before. If
we click on a track whilst the player is paused, the new track is selected but
doesn't start playing, and if we click on a new track whilst the player is
playing, the player plays on by playing the new track. Got it?

However, `activate-track!` is still relying on the DOM to keep track of whether
the player is paused. Let's fix this by checking the `state` atom instead:

``` clojure
(defn activate-track! [{:keys [number src] :as track}]
  (log "Activating track:" (clj->js track))
  (let [track-spans (seq (.-children (get-el "#tracks")))
        audio-el (get-el "audio")
        ;; Instead of this 👇
        ;; paused? (.-paused audio-el)
        ;; Do this! 👇
        {:keys [paused?]} @state]
      ;; ...
    )
  track)
```

Next, let's write a function to advance to the next track:

``` clojure
(defn advance-track! []
  (let [{:keys [active-track album]} @state
        {:keys [tracks]} album
        last-track? (= active-track (count tracks))]
    (when-not last-track?
      (activate-track! (nth tracks active-track)))))
```

Oops, this is relying on `:active-track` being present in the `state` atom.
Let's put it there in `activate-track!`

``` clojure
(defn activate-track! [{:keys [number src] :as track}]
  (log "Activating track:" (clj->js track))
  (let [track-spans (seq (.-children (get-el "#tracks")))
        audio-el (get-el "audio")
        {:keys [paused?]} @state]
    ;; ...
    )
  ;; Swappity swap swap! 👇
  (swap! state assoc :active-track number)
  track)

(comment

  (load-ui! "http://localhost:1341/Garth+Brooks/Fresh+Horses")
  ;; => #<Promise[~]>

  @state
  ;; => {:paused? true,
  ;;     :active-track 1,
  ;;     :album {...}}

)
```

Now we should be able to actually call `advance-track!` to, well, advance the
track:

``` clojure
(comment

  (load-ui! "http://localhost:1341/Garth+Brooks/Fresh+Horses")
  ;; => #<Promise[~]>

  (:active-track @state)
  ;; => 1

  (advance-track!)
  ;; => {:artist "Garth Brooks", :title "Cowboys And Angels", :number 2, :src "http://localhost:1341/Garth+Brooks/Fresh+Horses/Garth+Brooks+-+Cowboys+and+Angels.mp3"}

  (:active-track @state)
  ;; => 2

)
```

We will have also seen the highlighted track change when we evaluated the
`(advance-track!)` form! 🎉

## Is this the end?

What we're building up to is of course the ability to play our album
continuously. When one track ends, the next should begin. And our good friend
`<audio>` has just what we need, in the form of the
[ended](https://developer.mozilla.org/en-US/docs/Web/API/HTMLMediaElement/ended_event)
event. If we add one line of code to register `advance-track!` as the listener
for the `ended` event:

``` clojure
(defn load-ui! [dir]
  (p/let [album (load-album (str dir "/album.rss"))]
    (display-album! album)
    (reset! state {:paused? true, :album album})
    (->> album
         :tracks
         first
         activate-track!)
    (.addEventListener (get-el "audio") "play"
                       #(swap! state assoc :paused? false))
    (.addEventListener (get-el "audio") "ended"
                       advance-track!)))

(comment

  (load-ui! "http://localhost:1341/Garth+Brooks/Fresh+Horses")
  ;; => #<Promise[~]>

  ;; Click ▶️ and witness the glory!
)
```

We win!

![Screenshot of our UI with the last track highlighted and the console showing activating track for all previous tracks][autoplay]
[autoplay]: assets/2024-07-20-soundcljoud-cloudy-autoplay.png "Autoplays assemble!" width=800 border=1

Winners who have won before and know how to win will of course know that the
best thing to do after winning is to stride triumphantly to the podium, receive
your 🥇, wave to your adoring public, soak up the applause like warm sunshine on
a July day (unless you're in the southern hemisphere, in which case the warm
sunshine is best appreciated in December, unless you're close enough to the
equator to appreciate warm sunshine whenever you damn well please, unless you're
too close to the equator and that sunshine is too warm to appreciate because
you're sweating like wild), and then head home, find a comfy chair and open a
bottle of champagne or fizzy water or tasty whiskey or whatever.

I, of course, am no such winner, so instead of retiring to my comfy chair with a
glass of
[Lagavulin](https://thewateroflife.org/2023/02/26/lagavulin-offerman-edition/),
I want to jump ahead in a track, so I confidently reach for the audio control
and click ahead in the timeline, and... nothing happens WTF?

Reading more documentation, I discover that I can see the current time in
seconds in the track by reading its
[currentTime](https://developer.mozilla.org/en-US/docs/Web/API/HTMLMediaElement/currentTime)
property, and I can seek to an arbitrary time by setting `currentTime`, so let's
give that a try, shall we? (Spoiler: we shall.)

``` clojure
(comment

  (.-currentTime (get-el "audio"))
  ;; => 37.010544

  (set! (.-currentTime (get-el "audio")) 50)
  ;; => nil

  ;; Why did my track start over? 🤬
  
  (.-currentTime (get-el "audio"))
  ;; => 2.006649

)
```

To make a long story short, this all boils down to how the browser actually
implements seeking. When it first loads the audio track, it issues a request
like this:

``` text
GET /Garth+Brooks/Fresh+Horses/Garth+Brooks+-+The+Old+Stuff.mp3 HTTP/1.1
Range: bytes=0-
```

and expects a response like this:

``` text
HTTP/1.1 206 Partial Content
Accept-Ranges: bytes
Content-length: 5943424
Content-Range: bytes 0-1024000/5943424
Content-Type: audio/mpeg
```

It will then buffer the bytes it got back and make the track seekable within
those bytes, as described
[here](https://developer.mozilla.org/en-US/docs/Web/Media/Audio_and_video_delivery/buffering_seeking_time_ranges).
You can peer under the hood by inspecting the `buffered` and `seekable`
properties of the `<audio>` element:

``` javascript
audio.buffered.length; // returns 2
audio.buffered.start(0); // returns 0
audio.buffered.end(0); // returns 5
audio.buffered.start(1); // returns 15
audio.buffered.end(1); // returns 19
```

But if we do this in our player, we experience a deep feeling of melancholy:

``` clojure
(comment

  (let [b (-> (get-el "audio")
              (.-buffered))]
    [(.start b 0) (.end b 0)])
  ;; => [0 144.758]

  (-> (get-el "audio")
      (.-seekable)
      (.-length))
  ;; => 1

  (let [s (-> (get-el "audio")
              (.-seekable))]
    [(.start s 0) (.end s 0)])
  ;; => [0 0]

)
```

The buffering looks fine, but it seems that we can only seek between 0 seconds
and 0 seconds in the track, which kinda explains why attempting to set
`currentTime` to any number that isn't 0 results in seeking back to 0. 😭

Seeking apparently only works if we get that blessed `206 Partial Content`
response from the webserver, so the browser knows how to make subsequent range
requests to buffer more data, and unfortunately, the built-in
[babashka.http-server](https://github.com/babashka/http-server) that we're using
to serve up files in `public/` responds like this:

``` text
HTTP/1.1 200 OK
Content-length: 5943424
Content-Type: audio/mpeg
Server: http-kit
```

No partial content?

![Screenshot of a chef saying no seek for you, come back 1 year][noseek]
[noseek]: assets/2024-07-20-soundcljoud-cloudy-noseek.jpg "Call Antifa, quick!"

We may attempt to fix this next time on "Soundcljoud, or a young man's
Soundcloud clonejure", that is if there is a next time.

Part 1: [Soundcljoud, or a young man's Soundcloud clonejure](2024-07-09-soundcljoud.html)
