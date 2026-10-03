Title: Scittlecastr
Date: FIXME
Tags: clojure,cljcastr,scittle,clojurescript
Description: In which I scittle up a podcast player from scratch
Discuss: FIXME
Image: assets/draft-podcast-player-preview.jpg
Image-Alt: Arab woman listening to Scittle podcast with earphones and dancing outdoors by Javier Sanchez Mingorance from Noun Project
Preview: true

![Arab woman listening to Scittle podcast with earphones and dancing outdoors][preview]
[preview]: assets/draft-podcast-player-preview.jpg "Photo by Javier Sanchez Mingorance from Noun Project"

I've mentioned cljcastr here before, but [in a very tongue in cheek
fashion](/blog/2024-02-22-cljcastr.html). Since then, cljcastr has turned into a
serious project to create a set of tools for producing and self-hosting
podcasts. The project has two simple principles:

1. **No surveillance. Ever.** cljcastr runs on your computer and only your
   computer. It does not connect to any external services, store data on the
   cloud (AKA other people's computers), or track you in any way, shape, or
   form.
2. **No AI.** All cljcastr code is written by hand, by humans. Nothing in this
   repository has been AI generated, and no AI generated pull requests will even
   be considered for inclusion.

cljcastr is hosted on Codeberg at
[jmglov/cljcastr](https://codeberg.org/jmglov/cljcastr).

In this blog post, I'm going to demonstrate the cljcastr ethos by implementing a
simple podcast player in ClojureScript, using the mighty
[Scittle](https://github.com/babashka/scittle/).

## Building the skeleton

To get started, we're going to need a Babashka project set up to serve files
from a local webserver. Let's create a `cljcastr` directory:

``` text
mkdir cljcastr
```

And now pop a `bb.edn` into it:

``` clojure
{:deps {io.github.babashka/sci.nrepl
        {:git/sha "80ec956af5e6610b040089ba62eecb5d223e9e76"}
        io.github.babashka/http-server
        {:git/sha "b38c1f16ad2c618adae2c3b102a5520c261a7dd3"}}

 :tasks
 {http-server {:doc "Starts http server for serving static files"
               :requires ([babashka.http-server :as http])
               :task (do (http/serve {:port 1341 :dir "resources/public"})
                         (println "Serving static assets at http://localhost:1341"))}

  browser-nrepl {:doc "Start browser nREPL"
                 :requires ([sci.nrepl.browser-server :as bp])
                 :task (bp/start! {})}

  -dev {:depends [browser-nrepl http-server]}

  dev {:doc "Development build. Starts webserver and watches for changes."
       :task (do (run '-dev {:parallel true})
                 (deref (promise)))}}
}
```

Now create a `resources/public` directory:

``` text
mkdir -p resources/public/
```

Drop an `index.html` in that directory:

``` html
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
  <title>cljcastr episode player</title>

  <script src="https://cdn.jsdelivr.net/npm/scittle@0.8.33/dist/scittle.js" type="application/javascript"></script>
  <script src="https://cdn.jsdelivr.net/npm/scittle@0.8.33/dist/scittle.promesa.js" type="application/javascript"></script>
  <script>var SCITTLE_NREPL_WEBSOCKET_PORT = 1340;</script>
  <script src="https://cdn.jsdelivr.net/npm/scittle@0.8.33/dist/scittle.nrepl.js" type="application/javascript"></script>

  <script type="application/x-scittle" src="player.cljs"></script>
</head>

<body>
</body>

</html>
```

And finally, a `player.cljs` in the same directory that does naught but declare
a namespace:

``` clojure
(ns cljcastr.player)

(js/console.log "Ready to play!")
```

Thus equipped, invoke Babashka from the top-level `cljcastr` directory thusly:

``` text
bb dev
```

Babashka will helpfully tell you where you can find your player:

``` text
Serving static assets at http://localhost:1341
nREPL server started on port 1339...
Websocket server started on 1340...
```

If you browse to [localhost:1341](http://localhost:1341), you'll see an
amazingly blank page titled "cljcastr episode player". If you open the
JavaScript console in dev tools (**Ctrl + Shift + i** in many browsers), you
should see the player reporting for duty:

``` text
  Ready to play!                                                    core.cljs:80
> 
```

So let's play!

## Interactive development

In addition to the HTTP server serving up static files from our
`resources/public` directory, we have an nREPL server listening on port 1339 and
a websocket server on port 1340. When we loaded the `index.html` webpage in the
browser, `scittle.nrepl` opened a websocket connection to port 1340 and started
listening for commands. In order to send it some commands, we will need to
connect an nREPL client to the nREPL server on port 1339. Let's do it!

This section assumes that you are a reasonable person and are thus using Emacs
with [CIDER](https://github.com/clojure-emacs/cider), but if you've unreasonably
chosen to use a lesser editor or one of those new-fangled "IDEs" like
[Cursive](https://cursive-ide.com) or [Calva](https://calva.io), you probably
know how to operate nREPL in that editor, so just translate accordingly.

First, we open up `player.cljs` in our editor. Next, we use CIDER's
`cider-connect-cljs` command (**C-c M-C**) to connect to `localhost` on port
`1339`, choosing REPL type `nbb` when prompted. CIDER should open a REPL buffer
that says something like this:

``` text
;; Connected to nREPL server - nrepl://localhost:1339
;; CIDER 1.12.0 (Split)
;;
;; ClojureScript REPL type: nbb
;;
nil> 
```

Now we're all set up for interactive development, so let's interact. First,
execute `cider-load-buffer` (**C-c C-k**) in the `player.cljs` buffer. If you're
watching the JS console in your browser, you'll see something like the following
pop up:

``` text
  Ready to play!                                                   nrepl.cljs:10
> 
```

And meanwhile in your terminal, you'll see the nREPL server log some output:

``` text
:msg "{:value \"nil\", :id \"246\", :session \"e443955d-e285-4d5b-bbde-9b7f21f1561c\", :ns \"cljcastr.player\"}"
:msg "{:status [\"done\"], :id \"246\", :session \"e443955d-e285-4d5b-bbde-9b7f21f1561c\", :ns \"cljcastr.player\"}"
```

We've now completed the circle of LISP, meaning we can control everything on our
player page with ClojureScript! 💥

Let's try some simple arithmetic just to get a sense of the power at our
fingertips. In the `player.cljs` buffer, we'll add a [Rich
comment](https://betweentwoparens.com/blog/rich-comment-blocks/#rich-comment)
and some code:

``` clojure
(comment

  (+ 1 2)

  )
```

Positioning our cursor right after the `(+ 1 2)` form, we hit **C-c C-v f c e**
to execute `cider-pprint-eval-last-sexp-to-comment`, and are rewarded by a nice
comment being inserted on the next line:

``` clojure
(comment

  (+ 1 2)
  ;; => 3

  )
```

We can also print this result to the browser console by adding another line
inside the `(comment)` form:

``` clojure
(comment

  ;; ...
  
  (js/console.log (+ 1 2))

  )
```

If you're using CIDER with an autocomplete setup, you should actually see
completion candidates popping up as you type. 🤯

If you position the cursor after the form and evaluate it again with **C-c C-v f
c e**, you'll get a comment on the next line saying

``` clojure
  ;; => nil
```

This is somewhat underwhelming, but if you take a peek at your browser console,
you'll be more satisfied:

``` text
  3                                                                nrepl.cljs:10
> 
```

Can we have our cake and eat it too? (Note that from here on, I'll assume you're
whacking **C-c C-v f c e** after the new form you add, which will produce the
`;; =>` comment on the next line.)

``` clojure
(comment

  ;; ...
  
  (doto (+ 1 2)
    js/console.log)
  ;; => 3

  )
```

Now we're cooking with biofuel!

Let's try something a little more complex:

``` clojure
(comment

  ;; ...
  
  (doto (map inc (range 5))
    js/console.log)
  ;; => (1 2 3 4 5)

  )
```

This is great, but what in the world is happening over in the browser console?

``` text
  ‣ $APP.gg {J: null, na: null, I: null, F: 32374988, ma: ƒ, …}    nrepl.cljs:10
> 
```

If we click on the ‣, things get even wilder:

``` text
  ‣ $APP.gg {J: null, na: null, I: null, F: 32374988, ma: ƒ, …}    nrepl.cljs:10
      F: 32374988
      I: null
      J: null
      M: 1
      ma: null
    ‣ na: $APP.lg {hc: kg, Zc: $…P.gg, J: null, I: null, F: 31850732, …}
    ‣ [[Prototype]]: Object
>
```

What we're seeing is a JavaScript representation of a ClojureScript vector. This
is of course super cool and all, but not so readable (unless you're Rich Hickey
and you don't even see the [persistent data
structures](https://www.cosmiclearn.com/clojure/immutability.php) but just the
[woman in the red dress](https://matrix.fandom.com/wiki/Woman_in_Red)). Let's
see if we can do better:

``` clojure
(comment

  ;; ...
  
  (doto (->> (map inc (range 5))
             clj->js)
    js/console.log)
  ;; => #js [1 2 3 4 5]

  )
```

The browser console is looking good!

``` text
  ‣ (5) [1, 2, 3, 4, 5]                                            nrepl.cljs:10
```

But life inside our `(comment)` form is less idyllic. What's up with this
nonsense?

``` clojure
  ;; => #js [1 2 3 4 5]
```

Now we're seeing ClojureScript's JavaScript data literal. 😅

Let's try one more thing:

``` clojure
(comment

  ;; ...
  
  (let [res (map inc (range 5))]
    (js/console.log (clj->js res))
    res)
  ;; => (1 2 3 4 5)

  )
```

That's much better! Now how about we extract this to a function so we can use it
to log stuff?

``` clojure
(ns cljcastr.player)

(defn log [v]
  (js/console.log (clj->js v))
  v)

(comment

  ;; ...

  (log (map inc (range 5)))
  ;; => (1 2 3 4 5)

  )
```

Good stuff. Thus equipped, we can start sprucing up this empty webpage.

## Getting down to brass HTML

Let's add an
[h1](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/Heading_Elements)
HTML element to the page to let users know what's up. The obvious way to do this
would be to open up `index.html` and drop in something like this:

``` html
<body>
  <h1>cljcastr player</h1>
</body>
```

However, this would require a page reload for us to see the changes, and that
would blow away the state we've carefully built up in our REPL. 😭

Let's see if we can do this in the REPL instead. The basic idea is this:
1. Create an **h1** element.
2. Set its inner text to "cljcastr player".
3. Grab the **body** element that we helpfully put in our HTML page skeleton.
4. Add the new **h1** element as a child of the **body** element.

And if we were to call an imaginary `eng->clj` function, that would look
something like this:

``` clojure
(comment

  ;; ...
  
  ;; 1. Create an **h1** element.
  (def h1 (js/document.createElement "h1"))
  ;; => #'cljcastr.player/h1

  ;; 2. Set its inner text to "cljcastr player".
  (set! (.-textContent h1) "cljcastr player")
  ;; => "cljcastr player"

  ;; 3. Grab the **body** element that we helpfully put in our HTML page skeleton.
  (def body (js/document.querySelector "body"))
  ;; => #'cljcastr.player/body

  ;; 4. Add the new **h1** element as a child of the **body** element.
  (.appendChild body h1)
  ;; => #object[HTMLHeadingElement [object HTMLHeadingElement]]

  )
```

Et voilà! Our boring page has been transformed into an amazing feast for the
eyes!

Before moving on, let's take a brief detour into a new JavaScript literal:

``` clojure
  ;; => #object[HTMLHeadingElement [object HTMLHeadingElement]]
```

This is a bit opaque, but we can always feed it to our `log` function to make it
nicely inspectable in the console:

``` clojure
(comment

  ;; ...
  
  (log h1)
  ;; => #object[HTMLHeadingElement [object HTMLHeadingElement]]

  )
```

The console will render this as:

``` text
  <h1>cljcastr player</h1>                                         nrepl.cljs:10
```

## Play that funky music

Let's grab ourselves an MP3 file of a random podcast episode to play with. From
the top-level `cljcastr` directory, run:

``` text
curl https://sphinx.acast.com/p/open/s/f5b64019-68c3-57d4-b70b-043e63e5cbf6/e/6aa70cbfb54f356fc779e282/media.mp3 \
  -o resources/public/episode.mp3 \
  --follow
```

Once the file is in place, we can add an
[audio](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/audio)
element to the page:

``` clojure
(comment

  ;; ...

  (def audio (js/document.createElement "audio"))
  ;; => #'cljcastr.player/audio

  (set! (.-controls audio) true)
  ;; => true

  (set! (.-src audio)
        "https://audio.buzzsprout.com/w99dr716dc81mqan68yj446jkm0o?response-content-disposition=inline")
  ;; => "https://audio.buzzsprout.com/w99dr716dc81mqan68yj446jkm0o?response-content-disposition=inline"

  (->> audio
       (.appendChild body))
  ;; => #object[HTMLAudioElement [object HTMLAudioElement]]

  (log audio)
  ;; => #object[HTMLAudioElement [object HTMLAudioElement]]

)
```

Now we should see an audio player pop up on screen!

![Simple podcast player with a heading and an audio player][player1]
[player1]: assets/draft-podcast-player-1.png "Click play, I dare you!" class="border"

And if we click play, we hear Akela Lacy welcome us to The Intercept Briefing.

We could declare victory right here, but we're missing a few things that a
podcast player usually has, such as:
1. Sweet custom controls instead of the ugly HTML5 default audio player.
2. The ability to play any podcast episode, not just one we've helpfully
   downloaded ahead of time.
3. If a transcript is available, display it with clickable timestamps.

But before we do that, let's turn our attention to one drawback of our current
workflow.

## All you need is persistence

It's great to be able to create HTML elements on the fly and update the page
without having to reload, but what's not great is that if we do reload, we lose
all the state of our running program. Go ahead, try it.

![Blank webpage][player2]
[player2]: assets/draft-podcast-player-2.png "Where did my stuff go?" class="border"

Since we wisely did our interactive development in a Rich comment, we can build
our interface back up by evaluating our file (**C-c C-v**), then stepping
through the Rich comment, evaluating each form one by one (you can use
`cider-eval-last-sexp` (**C-c C-e**) for this, since you don't need the comment
inserted again).

That is quite a pain, though. Is there a way to effectively export the running
program as HTML so we could save it to our `index.html`? Well, let's give it a
go.

What we'd like is to turn the **body** DOM node into an HTML fragment. We can
actually already do this in a clunky way:

``` clojure
(comment

  ;; ...

  (-> (js/document.querySelector "body")
      log)

  )
```

![JavaScript console with body element and children][player3]
[player3]: assets/draft-podcast-player-3.png "Nice HTML you got there" class="border"

Unfortunately, we can't directly use the console output in the REPL. We could of
course just copy and paste it from the console window, but that feels too
efficient, and we'd rather spend some time shaving those yaks, so let's think
about how we might generate our own HTML from the DOM tree anchored at the
**body** element.

First, we need to do a depth-first traversal of the children of **body**. This
is quite easy for the time being, since body only has children and no
grandchildren or great-grandchildren or whatever. Let's try to count those
children:

``` clojure
(comment

  ;; ...
  
  (def children (-> (js/document.querySelector "body")
                    (.-childNodes)
                    log))
  ;; => #'cljcastr.player/children

  (count children)
  ;; => "#error {:message \"No protocol method ICounted.-count defined ..."

  )
```

OMG wat? Apparently the `.childNodes` property of our [DOM
node](https://developer.mozilla.org/en-US/docs/Web/API/Node) is a
[NodeList](https://developer.mozilla.org/en-US/docs/Web/API/NodeList) instead of
a simple JavaScript array, and there's no nice way to iterate over it. 🤦

Well, luckily we're in ClojureScript not JavaScript, and thus sayeth the David
Nolan: [seq](https://clojuredocs.org/clojure.core/seq) and you will find!

``` clojure
(comment

  ;; ...
  
  (def children (-> (js/document.querySelector "body")
                    (.-childNodes)
                    seq
                    log))
  ;; => #'cljcastr.player/children

  (count children)
  ;; => 7

  )
```

Wait, what's going on here? The JS console actually holds the key. Looking at
the previously logged line, we see:

``` text
  ‣ NodeList(7) [audio.audio-for-speech,                            nrepl.cljs:10
    div.translate-tooltip-mtz.blue.sm-root.translate.hidden_translate,
    span.translate-button-mtz.hidden_translate.blue, text, protonpass-root-41ca,
    h1, audio]
```

Looks like my [Proton Pass](https://proton.me/pass) and
[Translator](https://chromewebstore.google.com/detail/translator-dictionary-acc/bebmphofpgkhclocdbgomhnjcpelbenh?hl=en-GB)
extensions are adding some elements to the body, and I have no idea what that
`<audio class="audio-for-speech">` element is. 😬

But that gives me an idea. What if we add a class to all of the elements we
create so we have an easy way to grab just those elements?

Luckily, we bound our **h1** and **audio** elements to ClojureScript vars, so we
can easily add the appropriate class to each:

``` clojure
(comment

  ;; ...

  (doseq [el [h1 audio]]
    (-> el .-classList (.add "cljcastr")))
  ;; => nil

  (log h1)
  ;; => #object[HTMLHeadingElement [object HTMLHeadingElement]]

  (log audio)
  ;; => #object[HTMLAudioElement [object HTMLAudioElement]]

  )
```

If we return our gaze to the console, we can see that we were victorious!

``` text
  <h1 class="cljcastr">cljcastr player</h1>                         nrepl.cljs:10
  <audio controls src="episode.mp3" class="cljcastr"></audio>       nrepl.cljs:10
```

Now let's try grabbing those kids again, this time using a [CSS
selector](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Selectors)
to limit the elements to the ones we want:

``` clojure
(comment

  ;; ...

  (def children (-> (js/document.querySelectorAll "body > .cljcastr")
                    seq
                    log))
  ;; => #'cljcastr.player/children

  (count children)
  ;; => 2

  )
```

Excellent! Now let's see what the element names of the children are:

``` clojure
(comment

  ;; ...
  
  (->> children
       (map #(.-localName %))
       log)
  ;; => ("h1" "audio")

  )
```

And their attributes?

``` clojure
(comment

  ;; ...
  
  (->> children
       (map #(.-attributes %))
       log)
  ;; => (#object[NamedNodeMap [object NamedNodeMap]]
  ;;     #object[NamedNodeMap [object NamedNodeMap]])

  )
```

Ah yes, thank you for not using a POJSO (Plain 'Ol JavaScript Object, ofc)
there. I guess we'll need some sort of function to turn a
[NamedNodeMap](https://developer.mozilla.org/en-US/docs/Web/API/NamedNodeMap)
into a Clojure map. No matter, we can write some simple Clojure to fix this:

``` clojure
(comment

  ;; ...
  
  (->> children
       (map (fn [el]
              (let [attrs (.-attributes el)]
                (->> (range (.-length attrs))
                     (map #(let [attr (.item attrs %)]
                             [(.-name attr) (.-value attr)]))
                     (into {})))))
       log)
  ;; => ({"class" "cljcastr"}
  ;;     {"controls" "", "src" "episode.mp3", "class" "cljcastr"})

  )
```

Um. This is... yeah. I think it's time to write a few functions to decomplect
this mess. Let's head back up to the top of the file and add a few little
functions:

``` clojure
(ns cljcastr.player)

(defn log [v]
  (js/console.log (clj->js v))
  v)

(defn attr->clj
  "Given an `Attr`, returns a vector of the name and value of the attribute."
  [attr]
  [(.-name attr) (.-value attr)])

(defn attrs->clj
  "Given a `NamedNodeMap`, returns a map of attr name to attr value."
  [attrs]
  (->> (range (.-length attrs))
       (map #(->> % (.item attrs) attr->clj))
       (into {})))

(js/console.log "Ready to play!")
```

Don't forget to evaluate the buffer (**C-c C-v**)!

Now we can write some much simpler code in our `comment` form:

``` clojure
(comment

  ;; ...
  
  (->> children
       (map #(attrs->clj (.-attributes %)))
       log)
  ;; => ({"class" "cljcastr"}
  ;;     {"controls" "", "src" "episode.mp3", "class" "cljcastr"})

  )
```

Now that we know how to get both the name of the tag and its attributes, let's
combine them into a function that we can use to map over a list of elements and
transform them into a nice Clojurey data structure. Climbing into our hammock
ever so briefly, we sketch out the following:

``` clojure
{:tag "{{tag-name}}"
 :attrs {"{{k1}}" "{{v1}}"
         "{{k2}}" "{{v2}}"}}
```

Given this data structure and the functions we've already written, it's not too
difficult to write the code:

``` clojure
(comment

  ;; ...
  
  (->> children
       (map (fn [el]
              {:tag (.-localName el)
               :attrs (attrs->clj (.-attributes el))}))
       log)
  ;; => ({:tag "h1", :attrs {"class" "cljcastr"}}
  ;;     {:tag "audio",
  ;;      :attrs {"controls" "", "src" "episode.mp3", "class" "cljcastr"}})

  )
```

The only thing missing from this is the inner text for the **h1** element. Let's
add a `:text` key to our map for elements that have text:

``` clojure
(comment

  ;; ...
  
  (->> children
       (map (fn [el]
              {:tag (.-localName el)
               :attrs (attrs->clj (.-attributes el))
               :text (or (.-textContent el)
                         (.-innerText el))}))
       log)
  ;; => ({:tag "h1", :attrs {"class" "cljcastr"}, :text "cljcastr player"}
  ;;     {:tag "audio",
  ;;      :attrs {"controls" "", "src" "episode.mp3", "class" "cljcastr"},
  ;;      :text ""})

  )
```

Let's turn this into a few nice functions:

``` clojure
(ns cljcastr.player)

;; ...

(defn get-name
  "Returns the tag name of an element."
  [el]
  (.-localName el))

(defn get-text
  "Returns the inner text of an element."
  [el]
  (or (.-textContent el)
      (.-innerText el)))

(defn el->clj
  "Given an HTML element, returns a map containing :tag, :text, and :attrs."
  [el]
  {:tag (get-name el)
   :attrs (attrs->clj (.-attributes el))
   :text (get-text el)})

(js/console.log "Ready to play!")
```

After evaluating the buffer, we can test things out back in our Rich comment:

``` clojure
(comment

  ;; ...
  
  (->> children
       (map el->clj)
       log)
  ;; => ({:tag "h1", :attrs {"class" "cljcastr"}, :text "cljcastr player"}
  ;;     {:tag "audio",
  ;;      :attrs {"controls" "", "src" "episode.mp3", "class" "cljcastr"},
  ;;      :text ""})

  )
```

Looking good! From this, it's pretty straightforward to turn this into an HTML
string. First, let's pull in `clojure.string` to help us out:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))
```

Now we can play around in the Rich comment (remembering to evaluate the buffer,
of course):

``` clojure
(comment

  ;; ...
  
  (->> children
       (map el->clj)
       (map (fn [{:keys [tag attrs text]}]
              (let [attrs-str (if (empty? attrs)
                                ""
                                (str " "
                                     (->> attrs
                                          (map (fn [[k v]] (str k "=\"" v "\"")))
                                          (str/join " "))))]
                (str "<" tag attrs-str ">" text "</" tag ">"))))
       (str/join "\n")
       log)
  ;; => "<h1 class=\"cljcastr\">cljcastr player</h1>\n
  ;;     <audio controls=\"\" src=\"episode.mp3\" class=\"cljcastr\"></audio>"

  )
```

That looks pretty reasonable, so let's extract a few functions:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn attrs->str
  "Given a map of attributes, returns a string of k=\"v\"..."
  [attrs]
  (->> attrs
       (map (fn [[k v]] (str k "=\"" v "\"")))
       (str/join " ")))

(defn el->html
  "Given an element (or the Clojure representation of an element), returns an
   HTML string."
  [el]
  (let [{:keys [tag attrs text]} (if (map? el) el (el->clj el))
        attrs-str (attrs->str attrs)]
    (str "<" tag (if (empty? attrs-str) "" (str " " attrs-str)) ">"
         text
         "</" tag ">")))

(js/console.log "Ready to play!")
```

Then in the comment:

``` clojure
(comment

  ;; ...
  
  (->> children
       (map el->html)
       (str/join "\n")
       log)
  ;; => "<h1 class=\"cljcastr\">cljcastr player</h1>\n
  ;;     <audio controls=\"\" src=\"episode.mp3\" class=\"cljcastr\"></audio>"

  )
```

## Escaping from el

Just as we're about to paste our HTML into `index.html`, a thought occurs to us.
We wrote some mucky DOM-tacular JS-interop code to create elements, but now we
have an elegant Clojure data structure to describe our elements, and we can turn
that into HTML, so why not into DOM nodes as well?

``` clojure
(comment

  ;; ...

  (let [{:keys [tag attrs text]} {:tag "h1", :attrs {}, :text "cljcastr-player"}
        el (js/document.createElement tag)]
    (doseq [[k v] attrs]
      (.setAttribute el (name k) v))
    (when (not-empty text)
      (set! (.-textContent el) text))
    (log el))
  ;; => #object[HTMLHeadingElement [object HTMLHeadingElement]]
  
  )
```

Looking at the browser console, we're convinced we're onto something, so time to
extract some functions:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn set-attrs! [el attrs]
  (doseq [[k v] attrs]
    (.setAttribute el (name k) v))
  el)

(defn set-text! [el text]
  (when (not-empty text)
    (set! (.-textContent el) text))
  el)

(defn create-el
  "Given a Clojure map representation of an element, returns an element."
  [{:keys [tag attrs text]}]
  (let [el (js/document.createElement tag)]
    (set-attrs! el attrs)
    (set-text! el text)
    el))

(js/console.log "Ready to play!")
```

Let's see if we can create an audio element using this:

``` clojure
(comment

  ;; ...

  (log (create-el {:tag "audio", :attrs {"controls" true, "src" "episode.mp3"}}))
  ;; => #object[HTMLAudioElement [object HTMLAudioElement]]

  )
```

Looks great! Now that we have a nicer way to build up elements interactively, as
well as a way to dehydrate our body into HTML, let's see about those
sweet-looking custom controls, shall we?

## Escaping from vanilla

The standard HTML audio control is functional, but a bit boring. In our hubris,
we decide we can do way better than that!

Let's look at [a typical podcast player](https://overcast.fm/+AAdZ_Hp6MfY) for
inspiration:

![Overcast podcast player][overcast]
[overcast]: assets/draft-podcast-player-overcast.png "That's a nice looking player!" class="border"

It looks like we'll want the following buttons:
- Play
- Pause
- Skip backward
- Skip forward
- Playback speed

Let's see if we can find some nice vector graphics for our buttons. [SVG
Repo](https://www.svgrepo.com/) is a great place to start, and a search there
for "play" turns up a nice looking SVG named [Play Circle Fill
SVG](https://www.svgrepo.com/svg/436852/play-circle-fill), which is provided
under the [MIT Licence](https://www.svgrepo.com/page/licensing/#MIT). That looks
promising!

Better yet, the SVG is part of a collection called [Framework7 Filled Icons]
(https://www.svgrepo.com/collection/framework7-filled-icons/). Let's click into
that and see if we can find some SVGs for other useful media player buttons.
Searching within the collection, we find a nice [Pause button](https://www.svgrepo.com/svg/436829/pause-circle-fill), but nothing for
the other three we need. 😭

No matter! We widen our search to all of SVG Repo and find nice graphics for the
rest. Here's where we land:
- [Play](https://www.svgrepo.com/svg/436852/play-circle-fill)
- [Pause](https://www.svgrepo.com/svg/436829/pause-circle-fill)
- [Skip forward](https://www.svgrepo.com/svg/495321/forward-15-seconds) (from
  [Iconsax Bold Oval Icons](https://www.svgrepo.com/collection/iconsax-bold-oval-icons/))
- [Skip backward](https://www.svgrepo.com/svg/495036/backward-15-seconds) (from
  [Iconsax Bold Oval Icons](https://www.svgrepo.com/collection/iconsax-bold-oval-icons/))
- [Playback speed](https://www.svgrepo.com/svg/151785/speed-meter) (from
  [Awesome Set](https://www.svgrepo.com/collection/awesome-set/))

Let's create a set of HTML images for each. We'll start by defining a map to
describe them:

``` clojure
(comment

  ;; ...

  (def playback-controls
    {:play {:src "https://www.svgrepo.com/show/436852/play-circle-fill.svg"
            :label "Play"}
     :pause {:src "https://www.svgrepo.com/show/436829/pause-circle-fill.svg"
             :label "Pause"}
     :forward {:src "https://www.svgrepo.com/show/495321/forward-15-seconds.svg"
               :label "Forward {{seek-dur-sec}} seconds"}
     :backward {:src "https://www.svgrepo.com/show/495036/backward-15-seconds.svg"
             :label "Back {{seek-dur-sec}} seconds"}
     :speed {:src "https://www.svgrepo.com/show/151785/speed-meter.svg"
             :label "Playback speed"}})
  ;; => #'cljcastr.player/playback-controls

  (def seek-dur-sec 15)
  ;; => #'cljcastr.player/seek-dur-sec

  )
```

Now we can map over the map (yeah yeah, I know) to create our
[img](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/img)
elements:

``` clojure
(comment

  ;; ...

  (def playback-buttons
    (->> playback-controls
         (map (fn [[control-name {:keys [src label]}]]
                (create-el {:tag "img"
                            :attrs {:class "cljcastr playback-icon"
                                    :id (str "button-" (name control-name))
                                    :src src
                                    :draggable false}})))
         log))
  ;; => #'cljcastr.player/playback-buttons

  )
```

Note that we're adding a `playback-control` class to the element in addition to
our standard `cljcastr` class, and also giving the element an ID so it's easy to
grab it with a CSS selector later. We also set the
[draggable](https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/draggable)
attribute to `false` to prevent drag-and-drop silliness from happening.

Now, how about a container to hold them?

``` clojure
(comment

  ;; ...

  (def playback-controls-container
    (-> (create-el {:tag "div"
                    :attrs {:class "cljcastr"
                            :id "playback-controls"}})
        log))
  ;; => #'cljcastr.player/playback-controls-container

  (doseq [el playback-buttons]
    (.appendChild playback-controls-container el))
  ;; => nil

  )
```

Finally, let's display the controls!

``` clojure
(comment

  ;; ...

  (doseq [el playback-buttons]
    (.appendChild playback-controls-container el))
  ;; => nil

  (.appendChild body playback-controls-container)
  ;; => #object[HTMLDivElement [object HTMLDivElement]]

  )
```

![Player with a huge play button][huge]
[huge]: assets/draft-podcast-player-huge.png "That's frickin' uuuuge!" class="border"

Let's set the width of the images to a more reasonable size:

``` clojure
(comment

  ;; ...

  (doseq [el playback-buttons]
    (set! (.-style el) "width: 35px"))
  ;; => nil

  )
```

That's much better! However, this black and white page is starting to bum us
out.

## Rainbows and unicorns

Since our controls are SVGs, we can use the magic of [CSS
filters](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Properties/filter)
to colour them.

Let's start by picking a colour, and what better colour for the sake of this
demo than a lovely amber sort of shade called `#f7931e`. Of course, CSS filters
can't be specified in something as prosaic as hex colour codes, so we'll use a
[magic CodePen thingy](https://codepen.io/sosuke/pen/Pjoqqp) to generate a
filter from our hex code. (We could also port the JavaScript from [this
spectacular StackOverflow
answer](https://stackoverflow.com/questions/42966641/how-to-transform-black-into-any-given-color-using-only-css-filters/43960991#43960991)
to ClojureScript, but we kinda don't feel like it.)

What we come up with is this:

``` css
filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);
```

We can now add this as a style to our **img** tags:

``` clojure
(comment

  ;; ...

  (doseq [el playback-buttons]
    (set! (.-style el)
          "width: 35px; filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);"))
  ;; => nil

  )
```

Better!

![Player with a orange playback controls][orange]
[orange]: assets/draft-podcast-player-orange.png "Trapped in amber" class="border"

One thing that's slightly annoying is that we had to remember to include to
include the width style when we added the filters. Why don't we write some code
that adds a style to an existing element whilst preserving existing styles?

``` clojure
(comment

  ;; ...

  ;; What do styles look like?
  (-> playback-buttons
      first
      (.getAttribute "style"))
  ;; => "width: 35px; filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);"

  ;; Parse into a list of styles
  (-> playback-buttons
      first
      (.getAttribute "style")
      (str/split #";\s+"))
  ;; => ["width: 35px"
  ;;     "filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);"]

  ;; Turn it into a map
  (let [styles (-> playback-buttons
                   first
                   (.getAttribute "style")
                   (str/split #";\s+"))]
    (->> styles
         (map #(let [[k v] (str/split % #":\s+")]
                 [(keyword k) v]))
         (into {})))
  ;; => {:width "35px",
  ;;     :filter "invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);"}

  )
```

Now we have a thing that we can easily turn into a function!

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn get-styles
  "Returns the inline styles of an element as a map of style name to value."
  [el]
  (let [styles (-> el
                   (.getAttribute "style")
                   (str/split #";\s+"))]
    (->> styles
         (map #(let [[k v] (str/split % #":\s+")]
                 [(keyword k) v]))
         (into {}))))

;; ...

(comment

  ;; ...

  (-> playback-buttons
      first
      get-styles)
  ;; => {:width "35px",
  ;;     :filter "invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);"}

  )
```

How about going the other direction, i.e. turning a map of styles into a style
string?

``` clojure
(comment

  ;; ...

  (->> {:width "35px", :height "35px"}
       (map (fn [[k v]]
              (str (name k) ": " v)))
       (str/join "; "))
  ;; => "width: 35px; height: 35px"

  )
```

From this, 'tis merely a hop, skip, and a jump to write a function that sets the
styles of an element:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn set-styles!
  "Sets the styles of an element from a map of style name to value."
  [el styles]
  (let [styles-str (->> styles
                        (map (fn [[k v]]
                               (str (name k) ": " v)))
                        (str/join "; "))]
    (set! (.-style el) styles-str))
  el)

;; ...

(comment

  ;; ...

  ;; Back to black!
  (doseq [el playback-buttons]
    (set-styles! el {:width "35px"}))
  ;; => nil

  )
```

Finally, let's put this together and create a function that adds a style instead
of replacing all the existing ones:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn add-style!
  "Adds a style to an element"
  [el style-name style-value]
  (let [styles (get-styles el)]
    (set-styles! el (assoc styles (name style-name) style-value)))
  el)

;; ...

(comment

  ;; ...

  ;; Amber is the new black
  (doseq [el playback-buttons]
    (add-style! el
                :filter "invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%)"))
  ;; => nil

  )
```

## Play it again, Sam

Now that we have visually stunning buttons, let's actually make them do
something.

``` clojure
(comment

  ;; ...

  ;; Grab the play button
  (def play-button (js/document.querySelector "#button-play"))
  ;; => #'cljcastr.player/play-button

  ;; Add a click handler
  (.addEventListener play-button "click"
                     (fn [ev]
                       (log ev)
                       (.play audio)))
  ;; => nil

  )
```

If we click the button, we hear the podcast start playing, and can see the
playback position in the standard HTML audio element above.

![Player playing an episode][play]
[play]: assets/draft-podcast-player-play.png "Play ball!" class="border"

Now we can do the same for the pause button:

``` clojure
(comment

  ;; ...

  ;; Grab the pause button
  (def pause-button (js/document.querySelector "#button-pause"))
  ;; => #'cljcastr.player/play-button

  ;; Add a click handler
  (.addEventListener pause-button "click"
                     (fn [ev]
                       (log ev)
                       (.pause audio)))
  ;; => nil

  )
```

That was easy! Skipping forward will be a little more interesting, since there's
no JS function to tell the audio object "skip forward x seconds", there's only a
`currentTime` property that we can query and set:

``` clojure
(comment

  ;; ...

  (.-currentTime audio)
  ;; => 29.396358

  (set! (.-currentTime audio) 15.0)
  ;; => 15

  )
```

Given this, it's not too hard to figure out how to skip forward and backward an
arbitrary number of seconds:

``` clojure
(comment

  ;; ...

  ;; Forward
  (let [cur (.-currentTime audio)]
    (set! (.-currentTime audio) (+ cur seek-dur-sec)))
  ;; => 32.231097

  ;; Backward
  (let [cur (.-currentTime audio)]
    (set! (.-currentTime audio) (- cur seek-dur-sec)))
  ;; => 19.23822

  )
```

It's probably a good idea not to skip forward past the end of the episode or
backward past the beginning, so let's see if we can put a stop to that
silliness:

``` clojure
(comment

  ;; ...

  ;; Forward with guard
  (let [cur (.-currentTime audio)]
    (set! (.-currentTime audio) (min (+ cur seek-dur-sec)
                                     (.-duration audio))))
  ;; => 47.231097

  ;; The end is nigh!
  (set! (.-currentTime audio) (- (.-duration audio) 5))
  ;; => 3880.147438

  ;; Don't worry, all is well
  (let [cur (.-currentTime audio)]
    (set! (.-currentTime audio) (min (+ cur seek-dur-sec)
                                     (.-duration audio))))
  ;; => 3885.147438

  (set! (.-currentTime audio) (- (.-duration audio) 5))

  ;; Backward
  (let [cur (.-currentTime audio)]
    (set! (.-currentTime audio) (max (- cur seek-dur-sec)
                                     0)))
  ;; => 0

  )
```

Armed with this code, let's make some functions:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn skip-forward! [audio-el sec]
  (let [cur (.-currentTime audio-el)]
    (set! (.-currentTime audio-el) (min (+ cur sec)
                                        (.-duration audio-el)))))

(defn skip-backward! [audio-el sec]
  (let [cur (.-currentTime audio-el)]
    (set! (.-currentTime audio-el) (max (- cur sec)
                                        0))))

;; ...

(comment

  ;; ...

  (skip-forward! audio seek-dur-sec)
  ;; => 32.612276

  (skip-backward! audio seek-dur-sec)
  ;; => 28.937271000000003

  )
```

Now we can wire up the skip buttons as well:

``` clojure
(comment

  ;; ...

  (def forward-button (js/document.querySelector "#button-forward"))
  ;; => #'cljcastr.player/forward-button

  (def backward-button (js/document.querySelector "#button-backward"))
  ;; => #'cljcastr.player/backward-button

  (.addEventListener forward-button "click"
                     (fn [ev]
                       (log ev)
                       (skip-forward! audio seek-dur-sec)))
  ;; => nil

  (.addEventListener backward-button "click"
                     (fn [ev]
                       (log ev)
                       (skip-backward! audio seek-dur-sec)))
  ;; => nil

  )
```

Look at this! All of our buttons work! (Except the playback speed one, but
that's a little more complicated, so let's save it for later. 😉)

The only thing that's obviously wrong with this amazing player is that it has
two sets of controls, the standard HTML one and our amazing buttons. There's
also something that's not obviously wrong to most readers, unless they happen to
be using a screen reader: the accessibility of our buttons sucks! In fact, the
only way we know they're buttons is by their appearance, because we're using
**img** elements, which are semantically just images. They could be images of an
elephant for all a screen reader knows.

Let's see what we can do about that!

## A11y spells ally

From the excellent MDN article on
[Accessibility](https://developer.mozilla.org/en-US/docs/Web/Accessibility):

> **Accessibility** (often abbreviated to **A11y** — as in, "a", then 11
> characters, and then "y") in web development means enabling as many people as
> possible to use websites, even when those people's abilities are limited in
> some way.
>
> For many people, technology makes things easier. For people with disabilities,
> technology makes things possible. Accessibility means developing content to be
> as accessible as possible, no matter an individual's physical and cognitive
> abilities and how they access the web.

The [W3C Accessibility
Initiative](https://www.w3.org/standards/webdesign/accessibility) puts it in
even more inspiring terms:

> **The Web is fundamentally designed to work for all people**, whatever their
> hardware, software, language, location, or ability. When the Web meets this
> goal, it is accessible to people with a diverse range of hearing, movement,
> sight, and cognitive ability.

We certainly want **all** people to adopt our superior podcast player, so let's
make sure it works for them, using the power of ARIA!

![Arya Stark after killing the Frey soldier with a knife][arya]
[arya]: assets/draft-podcast-player-arya.png "A girl always pays her debts" class="border"

No, not that one. 😉

This one:
[ARIA](https://developer.mozilla.org/en-US/docs/Learn_web_development/Core/Accessibility/WAI-ARIA_basics).

If we wrap each of our **img** elements in a
[button](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/button)
element, we're now conveying semantic meaning to a screen reader (or any other
user agent, for that matter; imagine a script that is trying to identify all the
buttons on a page... such as the one we're in the process of writing). And if we
set that button's
[aria-label](https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Reference/Attributes/aria-label)
attribute, we're now identifying what the button is meant to do.

Remember the `:label` key we included in our `playback-controls` map but never
used way back when?

``` clojure
(comment

  ;; ...

  (def playback-controls
    {:play {:src "https://www.svgrepo.com/show/436852/play-circle-fill.svg"
            :label "Play"}
     :pause {:src "https://www.svgrepo.com/show/436829/pause-circle-fill.svg"
             :label "Pause"}
     :forward {:src "https://www.svgrepo.com/show/495321/forward-15-seconds.svg"
               :label "Forward {{seek-dur-sec}} seconds"}
     :backward {:src "https://www.svgrepo.com/show/495036/backward-15-seconds.svg"
             :label "Back {{seek-dur-sec}} seconds"}
     :speed {:src "https://www.svgrepo.com/show/151785/speed-meter.svg"
             :label "Playback speed"}})

  )
```

Turns out we had accessibility in mind all along!

Let's try wrapping a button:

``` clojure
(comment

  ;; ...

  (let [button-name :play
        {:keys [src label]} {:src "https://www.svgrepo.com/show/495321/forward-15-seconds.svg"
                             :label "Forward {{seek-dur-sec}} seconds"}]
    (let [button-id (str "button-" (name button-name))
          button-el (create-el {:tag "button"
                                :attrs {:class "cljcastr"
                                        :id button-id
                                        :aria-label label}})
          img-el (create-el {:tag "img"
                             :attrs {:class "cljcastr playback-icon"
                                     :id (str button-id "-icon")
                                     :src src
                                     :draggable false}})]
      (.appendChild button-el img-el)
      (log button-el)))
  ;; => #object[HTMLButtonElement [object HTMLButtonElement]]

  )
```

This looks pretty reasonable in the JS console:

``` text
  ‣ <button class="cljcastr" id="button-play"                      nrepl.cljs:10
            aria-label="Forward {{seek-dur-sec}} seconds">
      <img class="cljcastr playback-icon" id="button-play-icon"
           src="https://www.svgrepo.com/show/495321/forward-15-seconds.svg"
           draggable="false">
    </button>
```

Except this bit, that is:

``` text
aria-label="Forward {{seek-dur-sec}} seconds"
```

Looks like we got a bit clever there with some
[Selmer](https://github.com/yogthos/Selmer)-style templating to allow for
changing the number of seconds we're skipping around. Let's quickly figure out
how to interpolate such strings:

``` clojure
(comment

  ;; ...

  (let [s "Something {{something}} something"]
    (str/replace s "{{something}}" "SOMETHING"))
  ;; => "Something SOMETHING something"

  (let [s "Something {{something}} something"
        ctx {:something "SOMETHING"}]
    (str/replace s #"\{\{(something)\}\}" (fn [[_ k]] (get ctx (keyword k)))))
  ;; => "Something SOMETHING something"

  )
```

This may appear slightly inscrutable at first glance, but if we look at the
documentation for
[clojure.string/replace](https://clojuredocs.org/clojure.string/replace), we see
that the `replacement` argument can actually be a function, in which case it's
passed the match as an argument and whatever it returns is the replacement
string. When the match contains a [capturing
group](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Regular_expressions/Capturing_group),
the argument to the function is a vector of `[match group1 group2 ...]`. For
example:

``` clojure
(comment

  ;; ...

  (let [s "It's easy as 123, ABC, baby you and me"]
    (str/replace s #"(\d)+.+([A-Z]+)"
                 (fn [match-vector] (str "<<<" (pr-str match-vector) ">>>"))))
  ;; => "It's easy as <<<[\"123, ABC\" \"3\" \"C\"]>>>, baby you and me"

  )
```

Let's turn this into a proper function:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn render
  "Given a string `s` and a context map `ctx`, replaces template variables of the
   form {{k}} with the value of the corresponding key in the context map. If
   the key `k` doesn't exist in the context map, the template variable won't be
   replaced (i.e. \"{{foo}}\" will remain \"{{foo}}\" in the interpolated
   string)."
  [s ctx]
  (str/replace s #"\{\{([^}]+)\}\}"
               (fn [[match k]] (get ctx (keyword k) match))))

;; ...

(comment

  ;; ...

  (render "foo {{bar}} baz {{blah}} {{barf}}" {:bar 27, :blah 42})
  ;; => "foo 27 baz 42 {{barf}}"

  )
```

Now we can make use of our new `render` function in our button-wrapping code:

``` clojure
(comment

  ;; ...

  (let [button-name :forward
        {:keys [src label]} {:src "https://www.svgrepo.com/show/495321/forward-15-seconds.svg"
                             :label "Forward {{seek-dur-sec}} seconds"}]
    (let [button-id (str "button-" (name button-name))
          button-el (create-el {:tag "button"
                                :attrs {:class "cljcastr"
                                        :id button-id
                                        :aria-label (render label {:seek-dur-sec seek-dur-sec})}})
          img-el (create-el {:tag "img"
                             :attrs {:class "cljcastr playback-icon"
                                     :id (str button-id "-icon")
                                     :src src
                                     :draggable false}})]
      (.appendChild button-el img-el)
      (log button-el)))
  ;; => #object[HTMLButtonElement [object HTMLButtonElement]]

  )
```

Quoth the console:

``` text
  ‣ <button class="cljcastr" id="button-forward"                      nrepl.cljs:10
            aria-label="Forward 15 seconds">
      <img class="cljcastr playback-icon" id="button-forward-icon"
           src="https://www.svgrepo.com/show/495321/forward-15-seconds.svg"
           draggable="false">
    </button>
```

Now we can extract a nice function to create a button:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn create-button
  ([button-name button]
   (create-button button-name button {}))
  ([button-name {:keys [label src click-handler]} ctx]
   (let [button-id (str "button-" (name button-name))
         img-el (create-el {:tag "img"
                            :attrs {:class (render "cljcastr {{img-class}}" ctx)
                                    :id (str button-id "-icon")
                                    :src src
                                    :draggable false}})]
     (-> (create-el {:tag "button"
                     :attrs {:class (render "cljcastr {{button-class}}" ctx)
                             :id button-id
                             :aria-label (render label ctx)}})
         (add-event-listener! "click" click-handler)
         (add-child! img-el)))))

;; ...

(comment

  ;; ...

  (->> (create-button :forward
                      {:src "https://www.svgrepo.com/show/495321/forward-15-seconds.svg"
                       :label "Forward {{seek-dur-sec}} seconds"}
                      {:seek-dur-sec seek-dur-sec
                       :button-class "playback-button"
                       :img-class "playback-icon"})
       log)
  ;; => #object[HTMLButtonElement [object HTMLButtonElement]]

  )
```

We've pulled another nice thing out of Clojure's bag of tricks: [multi-arity
functions](https://clojure.org/guides/learn/functions#_multi_arity_functions):

``` clojure

(defn create-button
  ([button-name button]
   (create-button button-name button {}))
  ([button-name {:keys [label src]} ctx]
   ;; ...
  ))
```

This allows us to call `create-button` without a context:

``` clojure
(create-button :play {:label "Play", :src "play.svg"})
```

This will call the version of `create-button` that takes two arguments, which in
turn calls the three-argument version, passing along the `button-name` and
`button` arguments as is, and adding an empty map as the `ctx` argument.

OK, now we can use our `create-button` function to sew new a11y'd buttons onto
our `playback-controls` **div**:

``` clojure
(comment

  ;; ...

  ;; Throw away the current crappy buttons
  (.replaceChildren playback-controls-container)
  ;; => nil

  ;; Let's fix the order of our buttons whilst we're at it
  (def playback-buttons
    (->> [:backward :play :pause :forward :speed]
         (map (fn [button-name]
                [button-name (get playback-controls button-name)]))
         (map (fn [[button-name button]]
                (let [button-el (create-button button-name button
                                               {:seek-dur-sec seek-dur-sec
                                                :button-class "playback-button"
                                                :img-class "playback-icon"})]
                  (-> button-el
                      .-firstChild
                      (set-styles! {:width "35px"
                                    :filter "invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%)"}))
                  button-el)))
         log))
  ;; => #'cljcastr.player/playback-buttons

  (doseq [el playback-buttons]
    (.appendChild playback-controls-container el))
  ;; => nil

  ;; Grab the new buttons
  (def play-button (js/document.querySelector "#button-play"))
  ;; => #'cljcastr.player/play-button

  (def pause-button (js/document.querySelector "#button-pause"))
  ;; => #'cljcastr.player/play-button

  (def forward-button (js/document.querySelector "#button-forward"))
  ;; => #'cljcastr.player/forward-button

  (def backward-button (js/document.querySelector "#button-backward"))
  ;; => #'cljcastr.player/backward-button

  ;; Attach the click handlers to them
  (.addEventListener play-button "click"
                     (fn [ev]
                       (log ev)
                       (.play (js/document.querySelector "audio.cljcastr"))))
  ;; => nil

  (.addEventListener pause-button "click"
                     (fn [ev]
                       (log ev)
                       (.pause (js/document.querySelector "audio.cljcastr"))))
  ;; => nil

  (.addEventListener forward-button "click"
                     (fn [ev]
                       (log ev)
                       (skip-forward! audio seek-dur-sec)))
  ;; => nil

  (.addEventListener backward-button "click"
                     (fn [ev]
                       (log ev)
                       (skip-backward! audio seek-dur-sec)))
  ;; => nil

  )
```

![Player with buttons with backgrounds and borders][buttons]
[buttons]: assets/draft-podcast-player-buttons.png "But those buttons look like buttons!" class="border"

The only problem with this approach is that our buttons now look like, well,
buttons. 😅

Let's restyle them a bit so they look like they did before:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn create-button
  ([button-name button]
   (create-button button-name button {}))
  ([button-name {:keys [label src click-handler]} ctx]
   (let [button-id (str "button-" (name button-name))
         img-el (create-el {:tag "img"
                            :attrs {:class (render "cljcastr {{img-class}}" ctx)
                                    :id (str button-id "-icon")
                                    :src src
                                    :draggable false}})]
     (-> (create-el {:tag "button"
                     :attrs {:class (render "cljcastr {{button-class}}" ctx)
                             :id button-id
                             :aria-label (render label ctx)}})
         (set-styles! {:padding 0
                       :margin 0
                       :border 0
                       :background "transparent"
                       :cursor "pointer"})
         (add-event-listener! "click" click-handler)
         (add-child! img-el)))))

;; ...

(comment

  ;; ...

  (.replaceChildren playback-controls-container)
  ;; => nil

  (def playback-buttons
    (->> [:backward :play :pause :forward :speed]
         (map (fn [button-name]
                [button-name (get playback-controls button-name)]))
         (map (fn [[button-name button]]
                (let [button-el (create-button button-name button
                                               {:seek-dur-sec seek-dur-sec
                                                :button-class "playback-button"
                                                :img-class "playback-icon"})]
                  (-> button-el
                      .-firstChild
                      (set-styles! {:width "35px"
                                    :filter "invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%)"}))
                  button-el)))
         log))
  ;; => #'cljcastr.player/playback-buttons

  (doseq [el playback-buttons]
    (.appendChild playback-controls-container el))
  ;; => nil

  (def play-button (js/document.querySelector "#button-play"))
  ;; => #'cljcastr.player/play-button

  (def pause-button (js/document.querySelector "#button-pause"))
  ;; => #'cljcastr.player/play-button

  (def forward-button (js/document.querySelector "#button-forward"))
  ;; => #'cljcastr.player/forward-button

  (def backward-button (js/document.querySelector "#button-backward"))
  ;; => #'cljcastr.player/backward-button

  (.addEventListener play-button "click"
                     (fn [ev]
                       (log ev)
                       (.play (js/document.querySelector "audio.cljcastr"))))
  ;; => nil

  (.addEventListener pause-button "click"
                     (fn [ev]
                       (log ev)
                       (.pause (js/document.querySelector "audio.cljcastr"))))
  ;; => nil

  (.addEventListener forward-button "click"
                     (fn [ev]
                       (log ev)
                       (skip-forward! audio seek-dur-sec)))
  ;; => nil

  (.addEventListener backward-button "click"
                     (fn [ev]
                       (log ev)
                       (skip-backward! audio seek-dur-sec)))
  ;; => nil

  )
```

Now our buttons look like they did before! 🎉

However, recreating these buttons is getting a little tedious. Let's do some
cleaning up.

## Sweeping up the shop

First of all, our `log` function is a little limited. It would be nice if we
could include a log message in addition to whatever data we're logging. In other
words, we'd like to do this:

``` clojure
(comment

  ;; ...

  (log "Something happened!" {:foo 1, :bar 2})
  ;; => "Something happened!"

  )
```

But if we look at the console now, we only see the message logged and not the
data:

``` text
  Something happened!                                                 nrepl.cljs:10
```

Luckily, the fix is pretty straightforward:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

(defn log [& args]
  (apply js/console.log (map clj->js args))
  (last args))

;; ...

(comment

  ;; ...

  (log "Something happened!" {:foo 1, :bar 2})
  ;; => {:foo 1, :bar 2}

  )
```

The value returned is what we wanted, and the console looks good as well:

``` text
  Something happened!  ‣ {foo: 1, bar: 2}                             nrepl.cljs:10
```

Next annoyance: all those `js/document.querySelector` calls. Extraction!

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn get-el
  "Returns the element matching the specified CSS selector. If `selector` is
   already an element, just returns the element."
  [selector]
  (if (instance? js/Element selector)
    selector
    (js/document.querySelector selector)))

;; ...

(comment

  ;; ...

  (get-el "audio.cljcastr")
  ;; => #object[HTMLAudioElement [object HTMLAudioElement]]

  )
```

Might as well have a function to get a list of elements too:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn get-els
  "Returns a sequence containing the elements matching the specified CSS
   selector."
  [selector]
  (->> (js/document.querySelectorAll selector)
       seq))

;; ...

(comment

  ;; ...

  (get-els "#playback-controls > button")
  ;; => (#object[HTMLButtonElement [object HTMLButtonElement]] 
  ;;     #object[HTMLButtonElement [object HTMLButtonElement]] 
  ;;     #object[HTMLButtonElement [object HTMLButtonElement]] 
  ;;     #object[HTMLButtonElement [object HTMLButtonElement]] 
  ;;     #object[HTMLButtonElement [object HTMLButtonElement]])

  )
```

How about one to return the children of an element?

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn get-children [el]
  (-> (get-el el)
      .-childNodes
      seq))

;; ...

(comment

  ;; ...

  (get-children "#playback-controls")
  ;; => (#object[HTMLButtonElement [object HTMLButtonElement]] 
  ;;     #object[HTMLButtonElement [object HTMLButtonElement]] 
  ;;     #object[HTMLButtonElement [object HTMLButtonElement]] 
  ;;     #object[HTMLButtonElement [object HTMLButtonElement]] 
  ;;     #object[HTMLButtonElement [object HTMLButtonElement]])

  )
```

We added another nicety there: by using `get-el`, our function can take either
an element or a CSS selector as its argument. Let's continue to use this pattern
as we add a function for adding a child to an element:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn add-child! [parent-el el]
  (let [parent-el (get-el parent-el)]
    (-> parent-el
        (.appendChild el))
    parent-el))

;; ...

(comment

  ;; ...

  (->> (add-child! "#playback-controls"
                   (create-el {:tag "span"
                               :text "Some nonsense"}))
       (log "I got a new child"))
  ;; => #object[HTMLDivElement [object HTMLDivElement]]

  )
```

Wanna clear all the children of an element?

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn clear-children! [el]
  (let [el (get-el el)]
    (.replaceChildren el)
    el))

;; ...

(comment

  ;; ...

  (->> (clear-children! "#playback-controls")
       (log "Empty nest"))
  ;; => #object[HTMLDivElement [object HTMLDivElement]]

  )
```

Set the children of an element?

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn set-children! [parent-el els]
  (let [parent-el (get-el parent-el)]
    (clear-children! parent-el)
    (doseq [el els]
      (add-child! parent-el el))
    parent-el))

;; ...

(comment

  ;; ...

  (->> (set-children! "#playback-controls" playback-buttons)
       log)
  ;; => #object[HTMLDivElement [object HTMLDivElement]]

  )
```

Final annoyance: all the repetition in the click handlers. Let's extract a
function for that as well:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn add-event-listener! [el event-type f]
  (let [el (get-el el)]
    (.addEventListener el event-type
                       (fn [ev]
                         (log (str "Handling " event-type " on")
                              el ev)
                         (f ev)))
    (log "Added listener for" event-type "events to" el)
    el))

;; ...

(comment

  ;; ...

  (add-event-listener! "#button-speed" "click"
                       #(log "Should probably do something with this event, eh?" %))
  ;; => #object[HTMLButtonElement [object HTMLButtonElement]]

  )
```

And if we now click the playback speed button, the console rewards us with some
rich logging:

![Console log of event handler][event]
[event]: assets/draft-podcast-player-event.png "Now we're clicking with gas!" class="border"

With all these awesome functions, we can finally recreate our buttons much more
concisely:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn create-button
  ([button-name button]
   (create-button button-name button {}))
  ([button-name {:keys [label src click-handler]} ctx]
   (let [button-id (str "button-" (name button-name))
         img-el (create-el {:tag "img"
                            :attrs {:class (render "cljcastr {{img-class}}" ctx)
                                    :id (str button-id "-icon")
                                    :src src
                                    :draggable false}})]
     (-> (create-el {:tag "button"
                     :attrs {:class (render "cljcastr {{button-class}}" ctx)
                             :id button-id
                             :aria-label (render label ctx)}})
         (set-styles! {:padding 0
                       :margin 0
                       :border 0
                       :background "transparent"
                       :cursor "pointer"})
         (add-event-listener! "click" click-handler)
         (add-child! img-el)))))

;; ...

(comment

  ;; ...

  (def playback-controls
    {:play {:src "https://www.svgrepo.com/show/436852/play-circle-fill.svg"
            :label "Play"
            :click-handler #(.play (get-el "audio.cljcastr"))}
     :pause {:src "https://www.svgrepo.com/show/436829/pause-circle-fill.svg"
             :label "Pause"
             :click-handler #(.pause (get-el "audio.cljcastr"))}
     :forward {:src "https://www.svgrepo.com/show/495321/forward-15-seconds.svg"
               :label "Forward {{seek-dur-sec}} seconds"
               :click-handler #(skip-forward! (get-el "audio.cljcastr") seek-dur-sec)}
     :backward {:src "https://www.svgrepo.com/show/495036/backward-15-seconds.svg"
                :label "Back {{seek-dur-sec}} seconds"
                :click-handler #(skip-backward! (get-el "audio.cljcastr") seek-dur-sec)}
     :speed {:src "https://www.svgrepo.com/show/151785/speed-meter.svg"
             :label "Playback speed"}})
  ;; => #'cljcastr.player/playback-controls

  (def playback-buttons
    (->> [:backward :play :pause :forward :speed]
         (map (fn [button-name]
                [button-name (get playback-controls button-name)]))
         (map (fn [[button-name button]]
                (let [button-el (create-button button-name button
                                               {:seek-dur-sec seek-dur-sec
                                                :class "playback-control"})]
                  (-> (get-children button-el)
                      first
                      (set-styles! {:width "35px"
                                    :filter "invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%)"}))
                  button-el)))
         log))
  ;; => #'cljcastr.player/playback-buttons

  (set-children! "#playback-controls" playback-buttons)
  ;; => #object[HTMLDivElement [object HTMLDivElement]]

  )
```

OK, so we've fixed the non-obvious (to those of us not using screen readers)
problem with the player. Scratching our heads, we try to recall what the obvious
problem was. 🤔

Oh yeah! Two sets of controls, right. Let's fix.

## Sweeping audio under the carpet

Making our audio element disappear isn't that hard. Let's use this as an excuse
to spread the `get-el` pattern a little wider as well:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn get-id
  "Returns the ID of an element."
  [el]
  (let [el (get-el el)]
    (.-id el)))

(defn set-id!
  "Sets the ID of an element."
  [el id]
  (let [el (get-el el)]
    (when-not (empty? id)
      (set-attr! el :id id))
    el))

(defn get-name
  "Returns the tag name of an element."
  [el]
  (let [el (get-el el)]
    (.-localName el)))

(defn get-text
  "Returns the inner text of an element."
  [el]
  (let [el (get-el el)]
    (or (.-textContent el)
        (.-innerText el))))

;; ...

(defn set-attrs! [el attrs]
  (let [el (get-el el)]
    (doseq [[k v] attrs]
      (.setAttribute el (name k) v))
    el))

(defn set-classes!
  "Sets the classes of the specified element. Classes may either be a list or a
   string.

   Example:

     ;; These two are equivalent
     (set-classes el [\"foo\" \"bar\"])
     (set-classes el \"foo bar\")"
  [el classes]
  (let [el (get-el el)]
    (when-not (empty? classes)
      (set! (.-classList el) (if (string? classes)
                               classes
                               (str/join " " classes))))
    el))

(defn set-text! [el text]
  (let [el (get-el el)]
    (when-not (empty? text)
      (set! (.-textContent el) text))
    el))

(defn get-styles
  "Returns the inline styles of an element as a map of style name to value."
  [el]
  (let [styles (-> (get-el el)
                   (.getAttribute "style")
                   (str/split #";\s+"))]
    (->> styles
         (map #(let [[k v] (str/split % #":\s+")]
                 [(keyword k) v]))
         (into {}))))

(defn set-styles!
  "Sets the styles of an element from a map of style name to value."
  [el styles]
  (let [el (get-el el)]
    (when-not (empty? styles)
      (set! (.-style el) (->> styles
                              (map (fn [[k v]]
                                     (str (name k) ": " v)))
                              (str/join "; "))))
    el))

(defn add-styles!
  "Given a map of style name to value, adds the styles to the element."
  [el styles]
  (-> el
      (set-styles! (merge (get-styles el) styles))))

(defn add-style!
  "Adds a style to an element"
  [el style-name style-value]
  (-> el
      (add-styles! {style-name style-value})))

;; ...

(comment

  ;; ...

  (add-style! "audio.cljcastr" :display "none")
  ;; => #object[HTMLAudioElement [object HTMLAudioElement]]

  )
```

![Player with just buttons][nude]
[nude]: assets/draft-podcast-player-nude.png "The buttons, the whole buttons, and nothing but the buttons" class="border"

Testing our controls, we verify that they still work. However, we've lost three
important things:
1. The progress bar, which gave us a visual indication of where we are and a way
   to move to an arbitrary point in the episode by clicking or dragging.
2. The display of the current position in the episode.
3. The display of the duration of the episode.

Actually, on the third point, we like Overcast's choice to display the time
remaining instead of the duration, so let's do that as well.

Having made these bold design decisions, let's get to implementing! We'll start
with the easy stuff, displaying the time elapsed and remaining:

``` clojure
(comment

  ;; ...

  (def elapsed-time (create-el {:tag "span"
                                :text (str (.-currentTime audio))
                                :attrs {:class "cljcastr"
                                        :id "elapsed-time"}}))
  ;; => #'cljcastr.player/cur-time

  (def remaining-time (create-el {:tag "span"
                                  :text (str (- (.-duration audio)
                                                (.-currentTime audio)))
                                  :attrs {:class "cljcastr"
                                          :id "remaining-time"}}))
  ;; => #'cljcastr.player/remaining-time

  (doseq [el [elapsed-time remaining-time]]
    (add-child! "#playback-controls" el))
  ;; => nil

  )
```

![Times displayed as fractional seconds][times]
[times]: assets/draft-podcast-player-times.png "Too much math, too ugly!" class="border"

Well, this is something, at least. Something incredibly confusing and also ugly.
😭

Let's enumerate the problems:
1. Times are displayed as fractional seconds, not the more familiar `h:mm:ss`.
2. The spans are side by side with no padding, so it's not possible to tell
   where elapsed time ends and remaining time begins.
3. The font size is too large compared to the buttons. And maybe black isn't the
   best choice of colour anyway.
4. We'd like the times displayed above the buttons (and the progress bar, when
   we get around to adding it).

Now that we know what's wrong, let's tackle things one at a time.

## Hours, minutes, and seconds, oh my!

How can we convert from seconds to `h:mm:ss`?

``` clojure
(comment

  ;; ...

  (let [secs (.-duration audio)
        h (-> secs (/ 3600) int)
        mm (-> secs (/ 60) (- (* h 60)) int)
        ss (-> secs (mod 60) int)]
    (str (int h) ":" (int mm) ":" (int ss)))
  ;; => "1:0:45"

  )
```

OK, not a bad start. Let's zero-pad the minutes and seconds, though, because
"1:00:45" looks way better than "1:0:45". 😉

``` clojure
(comment

  ;; ...

  (let [secs (.-duration audio)
        h (-> secs (/ 3600) int)
        mm (-> secs (/ 60) (- (* h 60)) int)
        ss (-> secs (mod 60) int)
        zero-pad #(-> % int str (str/replace #"^(\d)$" "0$1"))]
    (str (int h) ":" (zero-pad mm) ":" (zero-pad ss)))
  ;; => "1:00:45"

  (let [secs (.-currentTime audio)
        h (-> secs (/ 3600) int)
        mm (-> secs (/ 60) (- (* h 60)) int)
        ss (-> secs (mod 60) int)
        zero-pad #(-> % int str (str/replace #"^(\d)$" "0$1"))]
    (str (int h) ":" (zero-pad mm) ":" (zero-pad ss)))
  ;; => "0:00:46"

  )
```

Hrm, maybe one more tweak. It's conventional to omit the hour(s) portion of
timestamps less than an hour:

``` clojure
(comment

  ;; ...

  (let [secs (.-currentTime audio)
        h (-> secs (/ 3600) int)
        mm (-> secs (/ 60) (- (* h 60)) int)
        ss (-> secs (mod 60) int)
        zero-pad #(-> % int str (str/replace #"^(\d)$" "0$1"))]
    (str (if (pos? (int h))
           (str (int h) ":")
           "")
         (zero-pad mm) ":" (zero-pad ss)))
  ;; => "00:46"

  )
```

Nice! Now let's turn this into a function:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn secs->ts
  "Given a number of seconds, returns a timestamp of the form h:mm:ss."
  [secs]
  (let [h (-> secs (/ 3600) int)
        mm (-> secs (/ 60) (- (* h 60)) int)
        ss (-> secs (mod 60) int)
        zero-pad #(-> % int str (str/replace #"^(\d)$" "0$1"))]
    (str (if (pos? (int h))
           (str (int h) ":")
           "")
         (zero-pad mm) ":" (zero-pad ss))))

(comment

  ;; ...

  ;; Elapsed time
  (secs->ts (.-currentTime audio))
  ;; => "00:46"

  ;; Remaining time
  (secs->ts (- (.-duration audio) (.-currentTime audio)))
  ;; => "1:00:58"

  (-> elapsed-time
      (set-text! (secs->ts (.-currentTime audio)))
      log)
  ;; => #object[HTMLSpanElement [object HTMLSpanElement]]

  (-> remaining-time
      (set-text! (str "-" (secs->ts (- (.-duration audio) (.-currentTime audio)))))
      log)
  ;; => #object[HTMLSpanElement [object HTMLSpanElement]]
  
  )
```

OK, we've solved problem number 1. 🎉

Problems 2 and 3 are just styling, so the fixes are pretty straightforward:

``` clojure
(comment

  ;; ...

  (doseq [el [elapsed-time remaining-time]]
    (add-style! el :margin "0 5px 0 5px"))
  ;; => nil

  (doseq [el [elapsed-time remaining-time]]
    (add-style! el :font-size "0.7rem"))
  ;; => nil

  (doseq [el [elapsed-time remaining-time]]
    (add-style! el :color "grey"))
  ;; => nil

  )
```

![Timestamps displayed in smaller, grey text][grey]
[grey]: assets/draft-podcast-player-grey.png "Grey is the new black!" class="border"

We've accomplished what we set out to do, but the repetition is annoying. It's
almost as if we're missing a function...

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn add-styles!
  "Given a map of style name to value, adds the styles to the element."
  [el styles]
  (set-styles! el (merge (get-styles el) styles))
  el)

(defn add-style!
  "Adds a style to an element"
  [el style-name style-value]
  (add-styles! el {style-name style-value})
  el)

;; ...

(comment

  ;; ...

  (doseq [el [elapsed-time remaining-time]]
    (set-styles! el {}))
  ;; => nil

  (doseq [el [elapsed-time remaining-time]]
    (add-styles! el {:margin "0 5px 0 5px"
                     :font-size "0.7rem"
                     :color "grey"}))
  ;; => nil

  )
```

Note the update to `add-style!` to call `add-styles!`.

Whilst we're at it, let's fix another annoyance: with our current `create-el`
function, we either have to call `add-styles!` (or `set-styles!`) afterwards, or
do the stringifying of the styles ourselves:

``` clojure
(create-el {:tag "div"
            :attrs {:id "whatever"
                    :style "margin: 0 5px 0 5px; font-size: 0.7rm; color: grey"}})
```

We also have the same issue with classes and ID. 🙁

But instead of wallowing in misery, let's modify `create-el` to take three new
keys, `:classes`, `:id`, and `:styles`:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn get-id
  "Returns the ID of an element."
  [el]
  (.-id el))

(defn set-id!
  "Sets the ID of an element."
  [el id]
  (when-not (empty? id)
    (.setAttribute el "id" id))
  el)

;; ...

(defn set-classes!
  "Sets the classes of the specified element. Classes may either be a list or a
   string.

   Example:

     ;; These two are equivalent
     (set-classes el [\"foo\" \"bar\"])
     (set-classes el \"foo bar\")"
  [el classes]
  (when-not (empty? classes)
    (set! (.-classList el) (if (string? classes)
                             classes
                             (str/join " " classes))))
  el)

;; ...

(defn create-el
  "Given a Clojure map representation of an element, returns an element."
  [{:keys [tag attrs classes id styles text]}]
  (-> (js/document.createElement tag)
      (set-attrs! attrs)
      (set-classes! classes)
      (set-id! id)
      (set-styles! styles)
      (set-text! text)))

;; ...

(comment

  ;; ...

  (-> (create-el {:tag "span"
                  :id "elapsed-time"
                  :classes "cljcastr"
                  :text (secs->ts 123)
                  :styles {:margin "0 5px 0 5px"
                           :font-size "0.7rem"
                           :color "grey"}})
      log)
  ;; => #object[HTMLSpanElement [object HTMLSpanElement]]

  )
```

Now, let's address the last problem.

## Laying out the law

It is now time to employ the most awesome technology known to humankind to
figure out our layout: ASCII art!

``` text
+-----------------------------------+
|            Podcast title          |
+-----------------------------------+
|            Episode title          |
+-----------------------------------+
|            Release date           |
+-----------------------------------+
|            Episode art            |
+-----------------------------------+
|     Elapsed time | Remaining time |
+-----------------------------------+
|            Progress bar           |
+-----------------------------------+
| Backward | Play | Pause | Forward |
+-----------------------------------+
|           Playback speed          |
+-----------------------------------+
```

We've been using the **body** element to hold our player, but we should probably
deploy a **div** at this point so we can style things according to our liking.
We can also group similar things together in **div**s, something like this:

``` text
+ episode-container --------------------+
|                                       |
| + episode-intro --------------------+ |
| |            Podcast title          | |
| +-----------------------------------+ |
| |            Episode title          | |
| +-----------------------------------+ |
| |            Release date           | |
| +-----------------------------------+ |
|                                       |
+---------------------------------------+
|              Episode art              |
+---------------------------------------+
|            Audio (hidden)             |
+---------------------------------------+
|                                       |
| + progress -------------------------+ |
| |   Elapsed time | Remaining time   | |
| +-----------------------------------+ |
| |            Progress bar           | |
| +-----------------------------------+ |
|                                       |
+---------------------------------------+
|                                       |
| + playback-controls ----------------+ |
| | Backward | Play | Pause | Forward | |
| +-----------------------------------+ |
|                                       |
+---------------------------------------+
|             Playback speed            |
+---------------------------------------+
```

Let's start by creating the new nested containers (we already have one for
playback controls) and populating them with some hard-coded data:

``` clojure
(comment

  ;; ...

  (def episode-container (create-el {:tag "div"
                                     :id "episode-container"
                                     :classes "cljcastr"
                                     :styles {:width "100%"
                                              :display "flex"
                                              :flex-direction "column"
                                              :align-items "center"}}))
  ;; => #'cljcastr.player/episode-container

  (def episode-info
    (-> (create-el {:tag "div"
                    :id "episode-info"
                    :classes "cljcastr"
                    :styles {:max-width "420px"
                             :text-align "center"}})
        (set-children! [(create-el {:tag "h3"
                                    :text "Tech Won't Save Us"
                                    :attrs {:class "cljcastr"
                                            :id "podcast-title"}})
                        (create-el {:tag "h2"
                                    :text "Government is Putting AI Before People and the Planet w/ Matt Haugen"
                                    :attrs {:class "cljcastr"
                                            :id "episode-title"}})
                        (create-el {:tag "span"
                                    :text "August 13, 2026"
                                    :attrs {:class "cljcastr"
                                            :id "episode-date"}
                                    :styles {:color "grey"}})])
        log))
  ;; => #'cljcastr.player/episode-info

  (def episode-art
    (-> (create-el {:tag "div"
                    :id "episode-art-container"
                    :classes "cljcastr"})
        (add-child! (create-el {:tag "img"
                                :attrs {:id "episode-art"
                                        :class "cljcastr"
                                        :src "https://storage.buzzsprout.com/tutsi25s4vue63c1xwucuexriowa"}
                                :styles {:width "400px"
                                         :border-radius "5%"
                                         :padding "10px"}}))
        log))
  ;; => #'cljcastr.player/episode-art

  (def progress (create-el {:tag "div"
                            :id "progress"
                            :classes "cljcastr"
                            :styles {:width "390px"
                                     :margin "1.5em auto 0 auto"}}))
  ;; => #'cljcastr.player/progress

  (def playback-controls-container (-> (create-el {:tag "div"
                                                   :id "playback-controls"
                                                   :classes "cljcastr"})
                                       (set-children! playback-buttons)
                                       log))
  ;; => #'cljcastr.player/playback-controls-container

  (def playback-speed (create-el {:tag "div"
                                  :id "playback-speed"
                                  :classes "cljcastr"}))
  ;; => #'cljcastr.player/playback-speed

  (-> episode-container
      (set-children! [episode-info
                      episode-art
                      (-> audio
                          (set-styles! {:display "none"}))
                      progress
                      playback-controls-container
                      playback-speed])
      log)
  ;; => #object[HTMLDivElement [object HTMLDivElement]]

  (set-children! "body" [episode-container])
  ;; => #object[HTMLBodyElement [object HTMLBodyElement]]

  )
```

![Player with Tech Won't Save Us episode][twsu]
[twsu]: assets/draft-podcast-player-twsu.png "How do you say regulatory capture again?" class="border"

This is looking really good (relatively speaking), but there's one major
component missing.

## Slipping and sliding

We have no idea where in the episode we are! Let's see about getting that
progress bar in place.

Luckily, HTML has got us covered! There just so happens to be a
[progress](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/progress)
element designed exactly for what we want to do!

``` clojure
(comment

  ;; ...

  (def progress-bar
    (create-el {:tag "progress"
                :id "progress-bar"
                :classes "cljcastr"
                :attrs {:min 0
                        :max (.-duration audio)
                        :value (.-currentTime audio)}
                :styles {:appearance "none"
                         :height "8px"
                         :width "100%"
                         :border "0"
                         :border-radius "4px"
                         :margin "0"
                         :overflow "hidden"
                         :background-color "lightgray"
                         :color "#f7931e"}}))
  ;; => #'cljcastr.player/progress-bar

  )
```

Now that we have the progress bar, let's add it to our `#progress` div, along
with the current and elapsed time spans we created before:

``` clojure
(comment

  ;; ...

  (-> progress
      (set-children! [(-> elapsed-time
                          (add-styles! {:float "left"}))
                      (-> remaining-time
                          (add-styles! {:float "right"}))
                      progress-bar])
      log)
  ;; => #object[HTMLDivElement [object HTMLDivElement]]

  )
```

![Progress bar added][progress]
[progress]: assets/draft-podcast-player-progress.png "Now that's progress!" class="border"

Hurrah! I mean, it's a bit ugly, but at least it's there! 😅

Let's click the play button, then skip forward a few times. Now, we can try
setting the value of the progress bar to the current time of the audio element:

``` clojure
(comment

  ;; ...

  (set! (.-value progress-bar) (.-currentTime audio))
  ;; => 370.277385

  )
```

We should see the progress bar move along a bit.

This is great and all, but it would be lovely to have a way to actually move
forward and backward in the episode by clicking or dragging on the progress bar.
You know, the way every audio player ever works?

Sadly, the progress bar itself isn't really designed for this. We could
certainly hack it up with some "click" event listeners and a lot of arithmetic,
but surely there must be a better way, right?

And indeed there is! HTML has an [input element of
type
range](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/input/range)
that is designed for exactly this case! Let's try it out:

``` clojure
(comment

  ;; ...

  (def progress-slider
    (create-el {:tag "input"
                :id "progress-slider"
                :classes "cljcastr"
                :attrs {:type "range"
                        :min 0
                        :max (.-duration audio)
                        :value (.-currentTime audio)
                        :step "any"
                        :aria-label "Seek"
                        :aria-valuetext (str (secs->ts (.-currentTime audio))
                                             " of "
                                             (secs->ts (.-duration audio)))
                        :aria-valuemax (.-duration audio)}
                :styles {:appearance "none"
                         :cursor "pointer"
                         :height "28px"
                         :width "100%"
                         :border "0"
                         :margin "0"
                         :touch-action "manipulation"}}))
  ;; => #'cljcastr.player/progress-slider

  (-> progress
      (set-children! [(-> elapsed-time
                          (add-styles! {:float "left"}))
                      (-> remaining-time
                          (add-styles! {:float "right"}))
                      progress-slider
                      progress-bar])
      log)
  ;; => #object[HTMLDivElement [object HTMLDivElement]]

  )
```

OK, a blue dot has appeared. Let's skip forward in the episode a bit more, then
update the value of the slider to the current playback position. Oh yeah, might
as well update the value of the progress bar as well, whilst we're at it:

``` clojure
(comment

  ;; ...

  (doseq [el [progress-slider progress-bar]]
    (set! (.-value el) (.-currentTime audio)))
  ;; => nil

  )
```

Now, how about setting the playback position in the episode when we click or
drag the slider?

``` clojure
(comment

  ;; ...

  (add-event-listener! progress-slider "input"
                       #(->> % .-target .-value (set! (.-currentTime audio))))
  ;; => #object[HTMLInputElement [object HTMLInputElement]]

  )
```

We now have a pretty good representation of playback position:
- A span showing the elapsed time
- A span showing the remaining time
- A slider letting us control the playback position
- A progress bar giving us a visual indication of the playback position

Let's see if we can keep all of these in sync. First, some code to update them
all in one go:

``` clojure
(comment

  ;; ...

  (let [secs (.-currentTime audio)
        duration-secs (.-duration audio)]
    (set-text! elapsed-time (secs->ts secs))
    (set-text! remaining-time (str "-" (secs->ts (- duration-secs secs))))
    (.setAttribute progress-slider "aria-valuetext"
                   (str (secs->ts secs) " of " (secs->ts duration-secs)))
    (doseq [el [progress-slider progress-bar]]
      (set! (.-value el) (.-currentTime audio))))
  ;; => nil

  )
```

Looks like that works, so let's extract it into a function. And whilst we're at
it, maybe we can extract functions for the attribute and value setting so we
don't have to expose the raw DOM interactions. And for the sake of completeness,
we can add getters as well:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn get-attr
  "Returns the value of the specified attribute."
  [el attr]
  (let [el (get-el el)]
    (.getAttribute el (name attr))))

(defn set-attr!
  "Sets the value of the specified attribute."
  [el attr v]
  (let [el (get-el el)]
    (.setAttribute el (name attr) v)
    el))

(defn get-value
  "Returns the value attribute of the specified element."
  [el]
  (let [el (get-el el)]
    (.-value el)))

(defn set-value!
  "Sets the value attribute of the specified element."
  [el v]
  (let [el (get-el el)]
    (set! (.-value el) v)))

;; ...
;;
;; Let's add this right after the skip-backward! function

(defn update-playback-position-indicators!
  "Given a current playback time and duration (both in seconds) and a map of
   elements, updates the elements to reflect the current playback position. The
   element map should contain the following keys:
   `:elapsed-time` - text element (for example: label, span, etc.)
   `:remaining-time` - text element (for example: label, span, etc.)
   `:progress-slider` - input element of type range
   `:progress-bar` - progress element"
  [current-secs duration-secs
   {:keys [elapsed-time remaining-time progress-slider progress-bar]}]
  (set-text! elapsed-time (secs->ts current-secs))
  (set-text! remaining-time (str "-" (secs->ts (- duration-secs current-secs))))
  (set-attr! progress-slider :aria-valuetext
             (str (secs->ts current-secs) " of " (secs->ts duration-secs)))
  (doseq [el [progress-slider progress-bar]]
    (set-value! el current-secs)))

;; ...

(comment

  ;; ...

  (update-playback-position-indicators! (.-currentTime audio) (.-duration audio)
                                        {:elapsed-time elapsed-time
                                         :remaining-time remaining-time
                                         :progress-slider progress-slider
                                         :progress-bar progress-bar})
  ;; => nil

  )
```

There's one final touch we should apply here before we move on. It's getting a
bit silly to have to manually update these elements, right? How about we update
them automatically as the episode plays? For this, we can use the
[timeupdatechange](https://developer.mozilla.org/en-US/docs/Web/API/HTMLMediaElement/timeupdate_event)
event of our **audio** element:

``` clojure
(comment

  ;; ...

  (add-event-listener! audio "timeupdate"
                       #(update-playback-position-indicators!
                         (.-currentTime audio) (.-duration audio)
                         {:elapsed-time elapsed-time
                          :remaining-time remaining-time
                          :progress-slider progress-slider
                          :progress-bar progress-bar}))
  ;; => #object[HTMLAudioElement [object HTMLAudioElement]]

  )
```

Now things are running smoothly, but there's another thing that's bothering us.
The progress slider that we use to control the playback position is above the
progress bar, which just looks weird, and also goes against the expectations of
our hordes of users that there should be a handle on the progress bar itself
that they can drag around. You know, like this:

![Overcast web player][overcast]
[overcast]: assets/draft-podcast-player-overcast.png "The sun shines on Overcast" class="border"

## Getting into position

What we'd really like is to set our slider down right on top of the progress
bar. This will give us the seeking we seek (sorry) as well as a visual
indication of the current playback position. In order to do this, we're going to
have to learn a bit about [CSS
positioning](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Properties/position).

Basically, we'll need to create a new container to hold the progress bar and the
slider, and set its `position` style to "relative". Then, we can add our
progress bar and slider as children of it, setting their position to "absolute"
(relative to their parent container). Yes, I know, this is enough to make our
heads explode, but I guess it makes sense once you achieve enlightenment or
something?

We also need to set the z-indexes of the two elements. We set the progress bar
to 0 to put it at the bottom of the stack, and the slider to 1 so it's on top.
That means we need to make the background colour of the slider transparent so
the progress bar can shine through in all its glory.

Finally, we position both elements 0 pixels from the left of the enclosing
container, then the slider 0 pixels from the top and the progress bar 10 pixels
from the top. Why 10 pixels? Well, because:
- The slider is 28 pixels high.
- The progress bar is 8 pixels high.
- We want to vertically centre the progress bar underneath the slider.
- 20 - 8 is 10.

With that advanced mathematics out of the way, let's make it happen!

``` clojure
(comment

  ;; ...

  (def progress-container
    (create-el {:tag "div"
                :id "progress-container"
                :classes "cljcastr"
                :styles {:position "relative"
                         :height "28px"
                         :clear "both"}}))
  ;; => #'cljcastr.player/progress-container

  (-> progress-container
      (set-children! [(-> progress-bar
                          (add-styles! {:position "absolute"
                                        :top "10px"
                                        :left "0px"
                                        :z-index 0}))
                      (-> progress-slider
                          (add-styles! {:position "absolute"
                                        :top "0px"
                                        :left "0px"
                                        :z-index 1
                                        :background-color "transparent"}))])
      log)
  ;; => #object[HTMLDivElement [object HTMLDivElement]]

  (-> progress
      (add-styles! {:position "relative"})
      (set-children! [(-> elapsed-time
                          (add-styles! {:float "left"}))
                      (-> remaining-time
                          (add-styles! {:float "right"}))
                      progress-container])
      log)
  ;; => #object[HTMLDivElement [object HTMLDivElement]]

  )
```

![Player with a nice progress bar and slider][position]
[position]: assets/draft-podcast-player-position.png "Perfection on a plate!" class="border"

This is looking pretty good! There are a few more things we might want to do,
but before we continue tweaking, let's make a permanent record of what we've
done so far.

## Exporting HTML

Remember the `el->html` function we wrote way back when? Well, let's give it a
shot on the page we've built:

``` clojure
(comment

  ;; ...

  (->> (get-els "body > .cljcastr")
       (map el->html)
       (str/join "\n")
       log)
  ;; => "<div class=\"cljcastr\" id=\"episode-container\"
  ;;          style=\"width: 100%; display: flex; flex-direction: column; align-items: center;\">
  ;;       Tech Won't Save UsGovernment is Putting AI Before People and the Planet w/ Matt HaugenAugust 13, 202600:00-52:18
  ;;     </div>"

  )
```

Hrm, that's not very helpful. First of all, we're only getting the direct
children of our **body** element, where what we'd actually like is the entire
tree of elements rooted there. Let's see if we can figure out how to do that:

``` clojure
(comment

  ;; ...

  (->> (get-els "body > .cljcastr")
       (map (fn [el]
              (let [{:keys [tag attrs text]} (if (map? el) el (el->clj el))
                    attrs-str (attrs->str attrs)
                    tag-open (str "<" tag (if (empty? attrs-str) "" (str " " attrs-str)) ">")
                    tag-close (str "</" tag ">")
                    children (get-children el)]
                (if (seq children)
                  (str/join "\n  "
                            (concat [tag-open]
                                    (map el->html children)
                                    [tag-close]))
                  (str tag-open text tag-close)))))
       (str/join "\n")
       log)
  ;; => "<div class=\"cljcastr\" id=\"episode-container\"
  ;;          style=\"width: 100%; display: flex; flex-direction: column; align-items: center;\">\n
  ;;       <div class=\"cljcastr\" id=\"episode-info\"
  ;;            style=\"max-width: 420px; text-align: center;\">
  ;;         Tech Won't Save UsGovernment is Putting AI Before People and the Planet w/ Matt HaugenAugust 13, 2026
  ;;       </div>\n
  ;;       <div id=\"episode-art-container\" class=\"cljcastr\" style=\"\"></div>\n
  ;;       <audio controls=\"\" src=\"https://audio.buzzsprout.com/w99dr716dc81mqan68yj446jkm0o?response-content-disposition=inline\"
  ;;              style=\"display: none;\"></audio>\n
  ;;       <div class=\"cljcastr\" id=\"progress\"
  ;;            style=\"width: 390px; margin: 1.5em auto 0px; position: relative;\">00:00-52:18</div>\n
  ;;       <div class=\"cljcastr\" id=\"playback-controls\" style=\"\"></div>\n
  ;;       <div class=\"cljcastr\" id=\"playback-speed\" style=\"\"></div>\n
  ;;     </div>"

  )
```

OK, this is progress, but we're only grabbing the first level of children. In
order to include grandchildren and great-grandchildren and
great-great-grandchildren (and so on to the last syllable of recorded time),
we'll need to include our new code in `el->html` itself:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn el->html
  "Given an element (or the Clojure representation of an element), returns an
   HTML string."
  [el]
  (let [{:keys [tag attrs text]} (if (map? el) el (el->clj el))
        attrs-str (attrs->str attrs)
        tag-open (str "<" tag (if (empty? attrs-str) "" (str " " attrs-str)) ">")
        tag-close (str "</" tag ">")
        children (get-children el)]
    (if (seq children)
      (str/join "\n  "
                (concat [tag-open]
                        (map el->html children)
                        [tag-close]))
      (str tag-open text tag-close))))

;; ...

(comment

  ;; ...

  (->> (get-els "body > .cljcastr")
       (map el->html)
       (str/join "\n")
       log)
  ;; => "#error {:message \"Failed to execute 'querySelector' on 'Document':
  ;;             '[object Text]' is not a valid selector.\",
  ;;             :data {:type :sci/error, :line 48, :column 5, ..."

  )
```

Well, that doesn't look promising. Let's see if we can figure this out.

Looking back out at the HTML we generated in previously, we get a hint about
which element might be the culprit:

``` clojure
  ;;       <div class=\"cljcastr\" id=\"episode-info\"
  ;;            style=\"max-width: 420px; text-align: center;\">
  ;;         Tech Won't Save UsGovernment is Putting AI Before People and the Planet w/ Matt HaugenAugust 13, 2026
  ;;       </div>\n
```

Our `#episode-info` **div** looks a bit odd, doesn't it. Let's take a closer
look:

``` clojure
(comment

  ;; ...

  (get-children "#episode-info")
  ;; => (#object[HTMLHeadingElement [object HTMLHeadingElement]]
  ;;     #object[HTMLHeadingElement [object HTMLHeadingElement]]
  ;;     #object[HTMLSpanElement [object HTMLSpanElement]])

  (->> (get-children "#episode-info")
       (map get-name))
  ;; => ("h3" "h2" "span")

  )
```

OK, so `#episode-info` has three children: two headings and a span. Let's feed
the first child to `el->html` and see what happens:

``` clojure
(comment

  ;; ...

  (->> (get-children "#episode-info")
       first
       el->html)
  ;; => "#error {:message \"Failed to execute 'querySelector' on 'Document':
  ;;                        '[object Text]' is not a valid selector.\",
  ;;             :data {:type :sci/error, :line 48, :column 5, :message ...

  )
```

Ah yes, there it is. So there seems to be something special about a
[Text](https://developer.mozilla.org/en-US/docs/Web/API/Text) node that causes
`el->html` to blow up. Let's see what we can actually do with this type of
object:

``` clojure
(comment

  ;; ...

  (->> (get-children "#episode-info")
       first
       get-children
       first
       log)
  ;; => #object[Text [object Text]]

  (->> (get-children "#episode-info")
       first
       get-children
       first
       get-children)
  ;; => "#error {:message \"Failed to execute 'querySelector' on 'Document':
  ;;                        '[object Text]' is not a valid selector.\",
  ;;             :data {:type :sci/error, :line 48, :column 5, :message ...

  (->> (get-children "#episode-info")
       first
       get-children
       first
       get-text)
  ;; => "#error {:message \"Failed to execute 'querySelector' on 'Document':
  ;;                        '[object Text]' is not a valid selector.\",
  ;;             :data {:type :sci/error, :line 48, :column 5, :message ...

  (let [h3 (->> (get-children "#episode-info") first)
        child (->> (get-children h3) first)]
    (instance? js/Text child))
  ;; => true

  (let [h3 (->> (get-children "#episode-info") first)
        child (->> (get-children h3) first)]
    (when (instance? js/Text child)
      (get-text h3)))
  ;; => "Tech Won't Save Us"

  )
```

OK, so when we encounter a **Text** node, the way to get its content is to call
`get-text` on its parent.

Armed with this knowledge, we should be able to fix `el->html`:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn el->html
  "Given an element (or the Clojure representation of an element), returns an
   HTML string."
  [el]
  (let [{:keys [tag attrs text]} (if (map? el) el (el->clj el))
        attrs-str (attrs->str attrs)
        tag-open (str "<" tag (if (empty? attrs-str) "" (str " " attrs-str)) ">")
        tag-close (str "</" tag ">")
        children (get-children el)]
    (if (or (empty? children) (instance? js/Text (first children)))
      (str tag-open text tag-close)
      (str/join "\n  "
                (concat [tag-open]
                        (map el->html children)
                        [tag-close])))))

;; ...

(comment

  ;; ...

  (->> (get-els "body > .cljcastr")
       (map el->html)
       (str/join "\n")
       log)
  ;; => "<div class=\"cljcastr\" id=\"episode-container\"
  ;;          style=\"width: 100%; display: flex; flex-direction: column; align-items: center;\">\n
  ;;       <div class=\"cljcastr\" id=\"episode-info\"
  ;;            style=\"max-width: 420px; text-align: center;\">\n
  ;;         <h3 class=\"cljcastr\" id=\"podcast-title\" style=\"\">Tech Won't Save Us</h3>\n
  ;;         <h2 class=\"cljcastr\" id=\"episode-title\" style=\"\">Government is Putting AI Before People and the Planet w/ Matt Haugen</h2>\n
  ;;         <span class=\"cljcastr\" id=\"episode-date\"
  ;;           style=\"color: grey;\">August 13, 2026</span>\n
  ;;       </div>\n
  ;;       <div id=\"episode-art-container\" class=\"cljcastr\" style=\"\">\n
  ;;         <img id=\"episode-art\" class=\"cljcastr\"
  ;;              src=\"https://storage.buzzsprout.com/tutsi25s4vue63c1xwucuexriowa\"
  ;;              style=\"width: 400px; border-radius: 5%; padding: 10px;\"></img>\n
  ;;       </div>\n
  ;;       <audio controls=\"\"
  ;;              src=\"https://audio.buzzsprout.com/w99dr716dc81mqan68yj446jkm0o?response-content-disposition=inline\"
  ;;              style=\"display: none;\"></audio>\n
  ;;       <div class=\"cljcastr\" id=\"progress\"
  ;;            style=\"width: 390px; margin: 1.5em auto 0px; position: relative;\">\n
  ;;         <span class=\"cljcastr\" id=\"elapsed-time\"
  ;;               style=\"float: left; margin: 0px 5px; font-size: 0.7rem; color: grey;\">00:00</span>\n
  ;;         <span class=\"cljcastr\" id=\"remaining-time\"
  ;;               style=\"float: right; margin: 0px 5px; font-size: 0.7rem; color: grey;\">-52:18</span>\n
  ;;         <div id=\"progress-container\" class=\"cljcastr\"
  ;;              style=\"position: relative; height: 28px; clear: both;\">\n
  ;;           <progress id=\"progress-bar\" class=\"cljcastr\"
  ;;                     min=\"0\" max=\"3138.727422\" value=\"0\"
  ;;                     style=\"color: rgb(247, 147, 30); top: 10px; overflow: hidden; background-color: lightgray; width: 100%; z-index: 0; appearance: none; position: absolute; border: 0px; border-radius: 4px; height: 8px; left: 0px; margin: 0px;\"></progress>\n
  ;;           <input type=\"range\" id=\"progress-slider\" class=\"cljcastr\"
  ;;                  min=\"0\" max=\"3138.727422\" value=\"0\" step=\"any\"
  ;;                  aria-label=\"Seek\" aria-valuetext=\"00:00 of 52:18\" aria-valuemax=\"3138.727422\"
  ;;                  style=\"top: 0px; background-color: transparent; width: 100%; cursor: pointer; z-index: 1; appearance: none; position: absolute; border: 0px; touch-action: manipulation; height: 28px; left: 0px; margin: 0px;\"></input>\n
  ;;         </div>\n
  ;;       </div>\n
  ;;       <div class=\"cljcastr\" id=\"playback-controls\" style=\"\">\n
  ;;         <button class=\"cljcastr\" id=\"button-backward\" aria-label=\"Back 15 seconds\"
  ;;                 style=\"padding: 0px; margin: 0px; border: 0px; background: transparent; cursor: pointer;\">\n
  ;;           <img class=\"cljcastr playback-icon\" id=\"button-backward-icon\"
  ;;                src=\"https://www.svgrepo.com/show/495036/backward-15-seconds.svg\"
  ;;                draggable=\"false\"
  ;;                style=\"width: 35px; filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);\"></img>\n
  ;;         </button>\n
  ;;         <button class=\"cljcastr\" id=\"button-play\" aria-label=\"Play\"
  ;;                 style=\"padding: 0px; margin: 0px; border: 0px; background: transparent; cursor: pointer;\">\n
  ;;           <img class=\"cljcastr playback-icon\" id=\"button-play-icon\"
  ;;                src=\"https://www.svgrepo.com/show/436852/play-circle-fill.svg\"
  ;;                draggable=\"false\"
  ;;                style=\"width: 35px; filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);\"></img>\n
  ;;         </button>\n
  ;;         <button class=\"cljcastr\" id=\"button-pause\" aria-label=\"Pause\"
  ;;                 style=\"padding: 0px; margin: 0px; border: 0px; background: transparent; cursor: pointer;\">\n
  ;;           <img class=\"cljcastr playback-icon\" id=\"button-pause-icon\"
  ;;                src=\"https://www.svgrepo.com/show/436829/pause-circle-fill.svg\"
  ;;                draggable=\"false\"
  ;;                style=\"width: 35px; filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);\"></img>\n
  ;;         </button>\n
  ;;         <button class=\"cljcastr\" id=\"button-forward\" aria-label=\"Forward 15 seconds\"
  ;;                 style=\"padding: 0px; margin: 0px; border: 0px; background: transparent; cursor: pointer;\">\n
  ;;           <img class=\"cljcastr playback-icon\" id=\"button-forward-icon\"
  ;;                src=\"https://www.svgrepo.com/show/495321/forward-15-seconds.svg\"
  ;;                draggable=\"false\"
  ;;                style=\"width: 35px; filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);\"></img>\n
  ;;         </button>\n
  ;;         <button class=\"cljcastr\" id=\"button-speed\" aria-label=\"Playback speed\"
  ;;                 style=\"padding: 0px; margin: 0px; border: 0px; background: transparent; cursor: pointer;\">\n
  ;;           <img class=\"cljcastr playback-icon\" id=\"button-speed-icon\"
  ;;                src=\"https://www.svgrepo.com/show/151785/speed-meter.svg\"
  ;;                draggable=\"false\"
  ;;                style=\"width: 35px; filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);\"></img>\n
  ;;         </button>\n
  ;;       </div>\n
  ;;       <div class=\"cljcastr\" id=\"playback-speed\" style=\"\"></div>\n
  ;;     </div>"

  )
```

Now that's more like it! Better yet, if we look in the browser console, we see
some nicely formatted HTML:

![HTML code][html]
[html]: assets/draft-podcast-player-html.png "Such HTML!" class="border"

Let's copy this HTML from the console and paste it into our `index.html`:

``` html
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
  <title>cljcastr episode player</title>

  <script src="https://cdn.jsdelivr.net/npm/scittle@0.8.33/dist/scittle.js" type="application/javascript"></script>
  <script src="https://cdn.jsdelivr.net/npm/scittle@0.8.33/dist/scittle.promesa.js" type="application/javascript"></script>
  <script>var SCITTLE_NREPL_WEBSOCKET_PORT = 1340;</script>
  <script src="https://cdn.jsdelivr.net/npm/scittle@0.8.33/dist/scittle.nrepl.js" type="application/javascript"></script>

  <script type="application/x-scittle" src="player.cljs"></script>
</head>

<body>

<div class="cljcastr" id="episode-container" style="width: 100%; display: flex; flex-direction: column; align-items: center;">
  <div class="cljcastr" id="episode-info" style="max-width: 420px; text-align: center;">
  <h3 class="cljcastr" id="podcast-title" style="">Tech Won't Save Us</h3>
  <h2 class="cljcastr" id="episode-title" style="">Government is Putting AI Before People and the Planet w/ Matt Haugen</h2>
  <span class="cljcastr" id="episode-date" style="color: grey;">August 13, 2026</span>
  </div>
  <div id="episode-art-container" class="cljcastr" style="">
  <img id="episode-art" class="cljcastr" src="https://storage.buzzsprout.com/tutsi25s4vue63c1xwucuexriowa" style="width: 400px; border-radius: 5%; padding: 10px;"></img>
  </div>
  <audio controls="" src="https://audio.buzzsprout.com/w99dr716dc81mqan68yj446jkm0o?response-content-disposition=inline" style="display: none;"></audio>
  <div class="cljcastr" id="progress" style="width: 390px; margin: 1.5em auto 0px; position: relative;">
  <span class="cljcastr" id="elapsed-time" style="float: left; margin: 0px 5px; font-size: 0.7rem; color: grey;">00:00</span>
  <span class="cljcastr" id="remaining-time" style="float: right; margin: 0px 5px; font-size: 0.7rem; color: grey;">-52:18</span>
  <div id="progress-container" class="cljcastr" style="position: relative; height: 28px; clear: both;">
  <progress id="progress-bar" class="cljcastr" min="0" max="3138.727422" value="0" style="color: rgb(247, 147, 30); top: 10px; overflow: hidden; background-color: lightgray; width: 100%; z-index: 0; appearance: none; position: absolute; border: 0px; border-radius: 4px; height: 8px; left: 0px; margin: 0px;"></progress>
  <input aria-valuetext="00:00 of 52:18" min="0" aria-label="Seek" class="cljcastr" max="3138.727422" id="progress-slider" style="top: 0px; background-color: transparent; width: 100%; cursor: pointer; z-index: 1; appearance: none; position: absolute; border: 0px; touch-action: manipulation; height: 28px; left: 0px; margin: 0px;" value="0" aria-valuemax="3138.727422" type="range" step="any"></input>
  </div>
  </div>
  <div class="cljcastr" id="playback-controls" style="">
  <button class="cljcastr" id="button-backward" aria-label="Back 15 seconds" style="padding: 0px; margin: 0px; border: 0px; background: transparent; cursor: pointer;">
  <img class="cljcastr playback-icon" id="button-backward-icon" src="https://www.svgrepo.com/show/495036/backward-15-seconds.svg" draggable="false" style="width: 35px; filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);"></img>
  </button>
  <button class="cljcastr" id="button-play" aria-label="Play" style="padding: 0px; margin: 0px; border: 0px; background: transparent; cursor: pointer;">
  <img class="cljcastr playback-icon" id="button-play-icon" src="https://www.svgrepo.com/show/436852/play-circle-fill.svg" draggable="false" style="width: 35px; filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);"></img>
  </button>
  <button class="cljcastr" id="button-pause" aria-label="Pause" style="padding: 0px; margin: 0px; border: 0px; background: transparent; cursor: pointer;">
  <img class="cljcastr playback-icon" id="button-pause-icon" src="https://www.svgrepo.com/show/436829/pause-circle-fill.svg" draggable="false" style="width: 35px; filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);"></img>
  </button>
  <button class="cljcastr" id="button-forward" aria-label="Forward 15 seconds" style="padding: 0px; margin: 0px; border: 0px; background: transparent; cursor: pointer;">
  <img class="cljcastr playback-icon" id="button-forward-icon" src="https://www.svgrepo.com/show/495321/forward-15-seconds.svg" draggable="false" style="width: 35px; filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);"></img>
  </button>
  <button class="cljcastr" id="button-speed" aria-label="Playback speed" style="padding: 0px; margin: 0px; border: 0px; background: transparent; cursor: pointer;">
  <img class="cljcastr playback-icon" id="button-speed-icon" src="https://www.svgrepo.com/show/151785/speed-meter.svg" draggable="false" style="width: 35px; filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);"></img>
  </button>
  </div>
  <div class="cljcastr" id="playback-speed" style=""></div>
</div>

</body>

</html>
```

Saving the file, we take a deep breath and refresh the page in the browser. Et
voilà, our page reappears just as we'd left it! 🎉

Except clicking the buttons has no effect. 😅

But fear not! 'Tis but the work of a moment to fix them. Moving back over to our
`player.cljs` buffer, we mash **C-c C-k** (`cider-load-buffer`), then pop down
to the bottom of our `comment` form and search backwards for "(def
playback-controls". Once we find what we're looking for, we copy it and paste it
into our namespace, right before our "Ready to play!" message:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(def playback-controls
  {:play {:src "https://www.svgrepo.com/show/436852/play-circle-fill.svg"
          :label "Play"
          :click-handler #(.play (get-el "audio.cljcastr"))}
   :pause {:src "https://www.svgrepo.com/show/436829/pause-circle-fill.svg"
           :label "Pause"
           :click-handler #(.pause (get-el "audio.cljcastr"))}
   :forward {:src "https://www.svgrepo.com/show/495321/forward-15-seconds.svg"
             :label "Forward {{seek-dur-sec}} seconds"
             :click-handler #(skip-forward! (get-el "audio.cljcastr") seek-dur-sec)}
   :backward {:src "https://www.svgrepo.com/show/495036/backward-15-seconds.svg"
              :label "Back {{seek-dur-sec}} seconds"
              :click-handler #(skip-backward! (get-el "audio.cljcastr") seek-dur-sec)}
   :speed {:src "https://www.svgrepo.com/show/151785/speed-meter.svg"
           :label "Playback speed"}})

(js/console.log "Ready to play!")

(comment

  ;; ...

  )
```

We hit **C-c C-k** again, but are deeply saddened by an error message:

``` clojure
#error {:message "Unable to resolve symbol: seek-dur-sec",
        :data {:type :sci/error, :line 10, :column 30,
               :message "Unable to resolve symbol: seek-dur-sec",
               ;; ...
               },
        ;; ...
        }
```

Oops, looks like we need to grab `seek-dur-sec` as well:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(def seek-dur-sec 15)

(def playback-controls
  {:play {:src "https://www.svgrepo.com/show/436852/play-circle-fill.svg"
          :label "Play"
          :click-handler #(.play (get-el "audio.cljcastr"))}
   :pause {:src "https://www.svgrepo.com/show/436829/pause-circle-fill.svg"
           :label "Pause"
           :click-handler #(.pause (get-el "audio.cljcastr"))}
   :forward {:src "https://www.svgrepo.com/show/495321/forward-15-seconds.svg"
             :label "Forward {{seek-dur-sec}} seconds"
             :click-handler #(skip-forward! (get-el "audio.cljcastr") seek-dur-sec)}
   :backward {:src "https://www.svgrepo.com/show/495036/backward-15-seconds.svg"
              :label "Back {{seek-dur-sec}} seconds"
              :click-handler #(skip-backward! (get-el "audio.cljcastr") seek-dur-sec)}
   :speed {:src "https://www.svgrepo.com/show/151785/speed-meter.svg"
           :label "Playback speed"}})

(js/console.log "Ready to play!")

(comment

  ;; ...

  )
```

**C-c C-k** FTW?

Victory! 🏆

Or maybe more accurately, not defeat, since clicking the buttons still doesn't
work. Which isn't that surprising, since we didn't actually attach any event
listeners to anything. Let's see if we can figure out how to do that.

``` clojure
(comment

  ;; ...

  (doseq [[el-id {:keys [click-handler]}] playback-controls]
    (add-event-listener! (str "#button-" (name el-id)) "click"
                         click-handler))
  ;; => nil

  )
```

Now if we click the play button, the episode starts playing! We skip backwards
and forwards in the episode, try pausing it, and finally convince ourselves that
all the buttons work.

The only problem is that the elapsed time, remaining time, progress bar, and
slider aren't working. 🤔

Oh yeah, we had a handler for that as well. And we might as well attach the
listener to the slider that updates the playback position too:

``` clojure
(comment

  ;; ...

  (let [audio (get-el "audio.cljcastr")]
    (add-event-listener! "#progress-slider" "input"
                         #(->> % .-target .-value (set! (.-currentTime audio))))
    (add-event-listener! audio "timeupdate"
                         #(update-playback-position-indicators!
                           (.-currentTime audio) (.-duration audio)
                           (->> [:elapsed-time
                                 :remaining-time
                                 :progress-slider
                                 :progress-bar]
                                (map (fn [el-id]
                                       [el-id (str "#" (name el-id))]))
                                (into {}))))
    audio)
  ;; => #object[HTMLAudioElement [object HTMLAudioElement]]

  )
```

If we click play again, we see the timestamps updating and slider moving, and if
we move the slider ourselves, we can successfully jump around in the episode.
Life is good!

That is, until we hit **C-c C-v** again and realise that despite the console
cheerily telling us that we're "Ready to play!", the buttons don't work. 😢

Of course, we can evaluate the two forms we just added to the Rich comment, but
it's hardly a good user experience to expect our users to connect a
ClojureScript REPL, paste in some code, and evaluate it before they can actually
play an episode.

## Loading the UI

There is a pretty easy solution to this. If we just add the two magic forms
right before our "Ready to play!" log statement, the listeners should be
attached on page load. Let's try it:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(doseq [[el-id {:keys [click-handler]}] playback-controls]
  (add-event-listener! (str "#button-" (name el-id)) "click"
                       click-handler))

(let [audio (get-el "audio.cljcastr")]
  (add-event-listener! "#progress-slider" "input"
                       #(->> % .-target .-value (set! (.-currentTime audio))))
  (add-event-listener! audio "timeupdate"
                       #(update-playback-position-indicators!
                         (.-currentTime audio) (.-duration audio)
                         (->> [:elapsed-time
                               :remaining-time
                               :progress-slider
                               :progress-bar]
                              (map (fn [el-id]
                                     [el-id (str "#" (name el-id))]))
                              (into {}))))
  audio)

(js/console.log "Ready to play!")

(comment

  ;; ...

  )
```

If we save the file and reload the page, the buttons now work from the start.
This is good, but there is one oddity that we'll notice in the console if we
pause playback then evaluate the buffer with **C-c C-k** again:

``` text
  Added listener for click events to                                  nrepl.cljs:10
   ‣ <button class=​"cljcastr" id=​"button-speed"
             style=​"padding:​ 0px;​ margin:​ 0px;​ border:​ 0px;​ background:​ transparent;​ cursor:​ pointer;​"
             aria-label=​"Playback speed">​…​</button>​
  Added listener for input events to                                  nrepl.cljs:10
   ‣ <input class=​"cljcastr" id=​"progress-slider" type=​"range"
            min=​"0" max=​"3138.727422" value=​"0"step=​"any"
            style=​"top:​ 0px;​ background-color:​ transparent;​ width:​ 100%;​ cursor:​ pointer;​ z-index:​ 1;​ appearance:​ none;​ position:​ absolute;​ border:​ 0px;​ touch-action:​ manipulation;​ height:​ 28px;​ left:​ 0px;​ margin:​ 0px;​"
            aria-label=​"Seek" aria-valuetext=​"11:​58 of 52:​18" 
            aria-valuemax=​"3138.727422">​
  Added listener for timeupdate events to                             nrepl.cljs:10
   ‣ <audio class=​"cljcastr" controls style=​"display:​ none;​" 
            src=​"https:​/​/​audio.buzzsprout.com/​w99dr716dc81mqan68yj446jkm0o?response-content-disposition=inline">​</audio>​
  Ready to play!
```

And if we click play then pause again, we see that the click handlers are firing
twice:

``` text
  Handling click on                                                   nrepl.cljs:10
   ‣ <button class=​"cljcastr" id=​"button-pause"
             style=​"padding:​ 0px;​ margin:​ 0px;​ border:​ 0px;​ background:​ transparent;​ cursor:​ pointer;​"
             aria-label=​"Pause">​…​</button>​
   ‣ PointerEvent {isTrusted: true, pointerId: 1, width: 1, height: 1, pressure: 0, …}
  Handling click on                                                   nrepl.cljs:10
   ‣ <button class=​"cljcastr" id=​"button-pause"
             style=​"padding:​ 0px;​ margin:​ 0px;​ border:​ 0px;​ background:​ transparent;​ cursor:​ pointer;​"
             aria-label=​"Pause">​…​</button>​
   ‣ PointerEvent {isTrusted: true, pointerId: 1, width: 1, height: 1, pressure: 0, …}
```

If we evaluate the buffer again, then click play and pause, we see the handlers
firing thrice! 😱

``` text
  Handling click on                                                   nrepl.cljs:10
   ‣ <button class=​"cljcastr" id=​"button-pause"
             style=​"padding:​ 0px;​ margin:​ 0px;​ border:​ 0px;​ background:​ transparent;​ cursor:​ pointer;​"
             aria-label=​"Pause">​…​</button>​
   ‣ PointerEvent {isTrusted: true, pointerId: 1, width: 1, height: 1, pressure: 0, …}
  Handling click on                                                   nrepl.cljs:10
   ‣ <button class=​"cljcastr" id=​"button-pause"
             style=​"padding:​ 0px;​ margin:​ 0px;​ border:​ 0px;​ background:​ transparent;​ cursor:​ pointer;​"
             aria-label=​"Pause">​…​</button>​
   ‣ PointerEvent {isTrusted: true, pointerId: 1, width: 1, height: 1, pressure: 0, …}
  Handling click on                                                   nrepl.cljs:10
   ‣ <button class=​"cljcastr" id=​"button-pause"
             style=​"padding:​ 0px;​ margin:​ 0px;​ border:​ 0px;​ background:​ transparent;​ cursor:​ pointer;​"
             aria-label=​"Pause">​…​</button>​
   ‣ PointerEvent {isTrusted: true, pointerId: 1, width: 1, height: 1, pressure: 0, …}
```

OK, so it appears that every time we evaluate the buffer, we get a new click
handler. And if we think about it for a moment, it makes sense, because these
forms:

``` clojure
(doseq [[el-id {:keys [click-handler]}] playback-controls]
  (add-event-listener! (str "#button-" (name el-id)) "click"
                       click-handler))

(let [audio (get-el "audio.cljcastr")]
  (add-event-listener! "#progress-slider" "input"
                       #(->> % .-target .-value (set! (.-currentTime audio))))
  (add-event-listener! audio "timeupdate"
                       #(update-playback-position-indicators!
                         (.-currentTime audio) (.-duration audio)
                         (->> [:elapsed-time
                               :remaining-time
                               :progress-slider
                               :progress-bar]
                              (map (fn [el-id]
                                     [el-id (str "#" (name el-id))]))
                              (into {}))))
  audio)

(js/console.log "Ready to play!")
```

are getting evaluated every time, since they are at the top level of our
namespace. Luckily for us, if we whip out our microscope, we find a solution
(and a precipitate, but that's beside the point): the mighty atom!

## Atomic elbow

A common pattern in ClojureScript apps is to define a single atom that holds all
of the state we care about. It is conventional to call this atom `state`, so
let's follow said convention because we aren't iconoclastic hipsters, after all!

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

(defonce state (atom {}))

;; ...

(js/console.log "Ready to play!")
```

We use [defonce](https://clojuredocs.org/clojure.core/defonce) instead of plain
ol' [def](https://clojuredocs.org/clojure.core/def), because otherwise every
time we evaluated the buffer, our `state` var would be rebound to a fresh new
atom, thus defeating the point of keeping state in the first place.

Now that we've done this, let's move our code that attaches event listeners into
a function:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn init-ui! []
  (doseq [[el-id {:keys [click-handler]}] playback-controls]
    (add-event-listener! (str "#button-" (name el-id)) "click"
                         click-handler))

  (let [audio (get-el "audio.cljcastr")]
    (add-event-listener! "#progress-slider" "input"
                         #(->> % .-target .-value (set! (.-currentTime audio))))
    (add-event-listener! audio "timeupdate"
                         #(update-playback-position-indicators!
                           (.-currentTime audio) (.-duration audio)
                           (->> [:elapsed-time
                                 :remaining-time
                                 :progress-slider
                                 :progress-bar]
                                (map (fn [el-id]
                                       [el-id (str "#" (name el-id))]))
                                (into {}))))
    audio)
  (log "UI initialised"))

(js/console.log "Ready to play!")
```

Now if we evaluate the buffer, we don't see "Ready to play!" get printed in the
console. However, if we reload the page, we're back to our buttons not working,
which makes sense, because we're never calling our new `init-ui!` function.

Let's fix that!

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(when-not (:initialised? @state)
  (init-ui!)
  (swap! state assoc :initialised? true))

(js/console.log "Ready to play!")

```

If we evaluate the buffer now, we'll see our handlers getting installed and our
log statement telling us "UI initialised", as well as the usual chipper "Ready
to play!". If we evaluate the buffer a couple more times, we see "Ready to
play!" being logged, but nothing about handlers and initialisation.

![Console with initialisation messages][init]
[init]: assets/draft-podcast-player-init.png "Now we're actually ready!" class="border"

Better yet, everything seems to work!

## What has CSS ever done for us anyway?

Of course, we are not the sort of Clojurians to rest on our laurels, for there
are still improvements to be made! It is considered a bit tacky these days to
use inline styles, so let's see if we can take a page out of our `el->html` book
and export our styles.

In fact, we already have a function to get a map of styles from an element
(`get-styles`), and code in our `set-styles!` function that turns a map of
styles into a CSS string, so it's but the work of a moment to squish them
together:

``` clojure
(comment

  ;; ...
  
  (->> (get-styles "#progress-bar")
       (map (fn [[k v]]
              (str (name k) ": " v)))
       (str/join "; "))
  ;; => "color: rgb(247, 147, 30); top: 10px; overflow: hidden; background-color: lightgray; width: 100%; z-index: 0; appearance: none; position: absolute; border: 0px; border-radius: 4px; height: 8px; left: 0px; margin: 0px;"

  )
```

What would be really cool, though, would be to produce a snippet like this:

``` css
#progress-bar {
  color: rgb(247, 147, 30);
  top: 10px;
  overflow: hidden;
  background-color: lightgray;
  width: 100%;
  z-index: 0;
  appearance: none;
  position: absolute;
  border: 0px;
  border-radius: 4px;
  height: 8px;
  left: 0px;
  margin: 0px;
}
```

Honestly, that shouldn't be too tricky. Let's see what we can do:

``` clojure
(comment

  ;; ...

  (let [el (get-el "#progress-bar")
        styles (get-styles el)]
    (str "#" (get-id el) " {\n  "
         (->> styles
              (map (fn [[k v]]
                     (str (name k) ": " v)))
              (str/join ";\n  "))
         "\n}"))
  ;; => "#progress-bar {\n
  ;;       color: rgb(247, 147, 30);\n
  ;;       top: 10px;\n
  ;;       overflow: hidden;\n
  ;;       background-color: lightgray;\n
  ;;       width: 100%;\n
  ;;       z-index: 0;\n
  ;;       appearance: none;\n
  ;;       position: absolute;\n
  ;;       border: 0px;\n
  ;;       border-radius: 4px;\n
  ;;       height: 8px;\n
  ;;       left: 0px;\n
  ;;       margin: 0px;\n
  ;;     }"

  )
```

But what about elements without an ID?

``` clojure
(comment

  ;; ...

  (let [el (get-el "audio.cljcastr")
        styles (get-styles el)]
    (str "#" (get-id el) " {\n  "
         (->> styles
              (map (fn [[k v]]
                     (str (name k) ": " v)))
              (str/join ";\n  "))
         "\n}"))
  ;; => "# {\n  display: none;\n}"

  )
```

For that, let's fall back to the tag name, plus the class if it exists:

``` clojure
(comment

  ;; ...

  (let [el (get-el "audio.cljcastr")
        styles (get-styles el)
        id (get-id el)
        classes (get-classes el)
        sel (if (not-empty id)
              (str "#" (get-id el))
              (str (get-name el) (if (seq classes)
                                   (str "." (first classes)))))]
    (str sel " {\n  "
         (->> styles
              (map (fn [[k v]]
                     (str (name k) ": " v)))
              (str/join ";\n  "))
         "\n}"))
  ;; => "audio.cljcastr {\n  display: none;\n}"

  )
```

This is a bit clunky, though. It would be a lot cleaner if we used `el->clj` to
turn the element into a Clojure datastructure:

``` clojure
(comment

  ;; ...

  (let [el (get-el "audio.cljcastr")
        styles (get-styles el)
        id (get-id el)
        classes (get-classes el)
        sel (if (not-empty id)
              (str "#" (get-id el))
              (str (get-name el) (if (seq classes)
                                   (str "." (first classes)))))]
    (str sel " {\n  "
         (->> styles
              (map (fn [[k v]]
                     (str (name k) ": " v)))
              (str/join ";\n  "))
         "\n}"))
  ;; => "audio.cljcastr {\n  display: none;\n}"

  )
```

This code is so special it deserves a name! And lo! that name shall be
`el->css`, and it shall be good.

![Ramses I from The Ten Commandments saying 'So let it be written, so let it be done'][written]
[written]: assets/draft-podcast-player-written.jpg "Quoth the pharoah: nevermore!" class="border"

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn get-styles
  "Returns the inline styles of an element as a map of style name to value."
  [el]
  (let [styles (-> (get-el el)
                   (.getAttribute "style")
                   (str/split #";\s+"))]
    (->> (filter not-empty styles)
         (map #(let [[k v] (str/split % #":\s+")]
                 [(keyword k) v]))
         (into {}))))

;; ...

(defn format-css
  "Returns a CSS string with the specified selector and styles"
  [sel styles]
  (str sel " {\n  "
       (->> styles
            (map (fn [[k v]]
                   (str (name k) ": " v)))
            (str/join ";\n  "))
       "\n}"))

(defn el->css
  "Given an element (or the Clojure representation of an element), returns a CSS
   rule corresponding to the element's ID."
  [el]
  (let [{:keys [styles id tag classes]} (el->clj el)
        sel (if (not-empty id)
              (str "#" id)
              (str tag (if (seq classes)
                         (str "." (first classes)))))]
    (format-css sel styles)))

;; ...

(comment

  ;; ...

  (->> (el->css "#progress-bar")
       log)
  ;; => "#progress-bar {\n  color: rgb(247, 147, 30);\n  top: 10px;\n  overflow: hidden;\n  background-color: lightgray;\n  width: 100%;\n  z-index: 0;\n  appearance: none;\n  position: absolute;\n  border: 0px;\n  border-radius: 4px;\n  height: 8px;\n  left: 0px;\n  margin: 0px;\n}"

)
```

Note the upgrade to `get-styles` and the addition of `format-css`. 😉

This is pretty cool, so why don't we add a function to generate CSS for multiple
elements at a time?

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn els->css
  "Given an collection of elements (or Clojure representations of elements),
   returns CSS rules corresponding to the elements' IDs."
  [els]
  (->> els
       (map el->clj)
       (remove (comp empty? :styles))
       (map el->css)
       (str/join "\n\n")))

;; ...

(comment

  ;; ...

  (->> (els->css (get-els ".cljcastr"))
       log)
  ;; => "#episode-container {\n  width: 100%;\n  display: flex;\n  ...}"

)
```

Casting our gaze upon the console, joy fills our hearts:

![CSS code][css]
[css]: assets/draft-podcast-player-css.png "Such CSS!" class="border"

However, (and you knew the however was coming, eh dear reader?) our heart
rankles at the repetition inherent in the system.

## It’s your birthday. Someone gives you a calfskin wallet.

What people do when they write CSS is use classes to combine related sets of
elements. We're not going to be able to do that programmatically, but we can at
least spit out something that's a better starting point for a person to pick up
and shape to their liking.

How might we go about this?

``` clojure
(comment

  ;; ...

  (def style-groups
    (->> (get-els ".cljcastr")
         (map el->clj)
         (remove (comp empty? :styles))
         (group-by :styles)))
  ;; => #'cljcastr.player/style-groups

  (count style-groups)
  ;; => 13

  (->> style-groups
       (map (fn [[_styles els]]
              (map #(select-keys % [:tag :id :classes]) els))))
  ;; => (({:tag "div", :id "progress-container", :classes ("cljcastr")})
  ;;     ({:tag "div", :id "progress", :classes ("cljcastr")})
  ;;     ({:tag "progress", :id "progress-bar", :classes ("cljcastr")})
  ;;     ({:tag "img", :id "episode-art", :classes ("cljcastr")})
  ;;     ({:tag "input", :id "progress-slider", :classes ("cljcastr")})
  ;;     ({:tag "span", :id "episode-date", :classes ("cljcastr")})
  ;;     ({:tag "div", :id "episode-info", :classes ("cljcastr")})
  ;;     ({:tag "img", :id "button-backward-icon", :classes ("cljcastr" "playback-icon")}
  ;;      {:tag "img", :id "button-play-icon", :classes ("cljcastr" "playback-icon")}
  ;;      {:tag "img", :id "button-pause-icon", :classes ("cljcastr" "playback-icon")}
  ;;      {:tag "img", :id "button-forward-icon", :classes ("cljcastr" "playback-icon")}
  ;;      {:tag "img", :id "button-speed-icon", :classes ("cljcastr" "playback-icon")})
  ;;     ({:tag "audio", :id "", :classes ("cljcastr")})
  ;;     ({:tag "span", :id "elapsed-time", :classes ("cljcastr")})
  ;;     ({:tag "button", :id "button-backward", :classes ("cljcastr" "playback-button")}
  ;;      {:tag "button", :id "button-play", :classes ("cljcastr" "playback-button")}
  ;;      {:tag "button", :id "button-pause", :classes ("cljcastr" "playback-button")}
  ;;      {:tag "button", :id "button-forward", :classes ("cljcastr" "playback-button")}
  ;;      {:tag "button", :id "button-speed", :classes ("cljcastr" "playback-button")})
  ;;     ({:tag "span", :id "remaining-time", :classes ("cljcastr")})
  ;;     ({:tag "div", :id "episode-container", :classes ("cljcastr")}))

)
```

What we're doing here is getting all the elements having "cljcastr" as one of
their classes, turning them into Clojure data structures, removing the ones with
no inline styles, then grouping them by their styles. This results in a map
where the keys are styles and the values are the list of the elements with those
styles (see the documentation for the
[group-by](https://clojuredocs.org/clojure.core/group-by) function if this is
confusing). We then map over that to look at the tag, ID, and classes of the
elements with each unique set of styles.

Grouping elements like this seems useful, so let's create a function to do it:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn group-styles
  "Given a collection of DOM elements, returns a map keyed by styles. The value
   at each key will be a vector of the corresponding elements, in the order they
   appeared in the original list.

   Example:

     (let [els [(create-el {:tag \"div\", :id \"el1\", :styles {:color \"red\"}})
                (create-el {:tag \"div\", :id \"el2\", :styles {:color \"blue\"}})
                (create-el {:tag \"div\", :id \"el3\"})
                (create-el {:tag \"div\", :id \"el4\", :styles {:color \"red\"}})]]
       (->> (group-styles els)
            (map (fn [[styles els]]
                   [styles (map #(select-keys % [:id]) els)]))))
     ;; => ([{:color \"red;\"} ({:id \"el1\"} {:id \"el4\"})]
     ;;     [{:color \"blue;\"} ({:id \"el2\"})])"
  [els]
  (->> els
       (map el->clj)
       (remove (comp empty? :styles))
       (group-by :styles)))

;; ...

```

Now that we have this, let's see how many of these styles apply to multiple
elements:

``` clojure
(comment

  ;; ...

  (->> (get-els ".cljcastr")
       group-styles
       (filter (fn [[_ els]] (> (count els) 1)))
       (map (fn [[_styles els]]
              (map #(select-keys % [:tag :id :classes]) els))))
  ;; => (({:tag "img", :id "button-backward-icon", :classes ["cljcastr" "playback-icon"]}
  ;;      {:tag "img", :id "button-play-icon", :classes ["cljcastr" "playback-icon"]}
  ;;      {:tag "img", :id "button-pause-icon", :classes ["cljcastr" "playback-icon"]}
  ;;      {:tag "img", :id "button-forward-icon", :classes ["cljcastr" "playback-icon"]}
  ;;      {:tag "img", :id "button-speed-icon", :classes ["cljcastr" "playback-icon"]})
  ;;     ({:tag "button", :id "button-backward", :classes ["cljcastr" "playback-button"]}
  ;;      {:tag "button", :id "button-play", :classes ["cljcastr" "playback-button"]}
  ;;      {:tag "button", :id "button-pause", :classes ["cljcastr" "playback-button"]}
  ;;      {:tag "button", :id "button-forward", :classes ["cljcastr" "playback-button"]}
  ;;      {:tag "button", :id "button-speed", :classes ["cljcastr" "playback-button"]}))

)
```

These look like great candidates for consolidation! But what selector should we
use to write the rule?

Well, we know that all of our elements share the "cljcastr" class in common, so
if we exclude that, we see that both of these groups are left with a single
class!

``` clojure
(comment

  ;; ...

  (->> (get-els ".cljcastr")
       group-styles
       (filter (fn [[_ els]] (> (count els) 1)))
       (map (fn [[_styles els]]
              (map (fn [el]
                     (-> (select-keys el [:tag :id :classes])
                         (update :classes #(remove (fn [cls] (= "cljcastr" cls)) %))))
                   els))))
  ;; => (({:tag "img", :id "button-backward-icon", :classes ("playback-icon")}
  ;;      {:tag "img", :id "button-play-icon", :classes ("playback-icon")}
  ;;      {:tag "img", :id "button-pause-icon", :classes ("playback-icon")}
  ;;      {:tag "img", :id "button-forward-icon", :classes ("playback-icon")}
  ;;      {:tag "img", :id "button-speed-icon", :classes ("playback-icon")})
  ;;     ({:tag "button", :id "button-backward", :classes ("playback-button")}
  ;;      {:tag "button", :id "button-play", :classes ("playback-button")}
  ;;      {:tag "button", :id "button-pause", :classes ("playback-button")}
  ;;      {:tag "button", :id "button-forward", :classes ("playback-button")}
  ;;      {:tag "button", :id "button-speed", :classes ("playback-button")}))

)
```

Let's run those through `els->css` and see what we get:

``` clojure
(comment

  ;; ...

  (->> (get-els ".cljcastr")
       group-styles
       (filter (fn [[_ els]] (> (count els) 1)))
       (map (fn [[_styles els]]
              (->> els
                   (map (fn [el]
                          (update el :classes #(remove (fn [cls] (= "cljcastr" cls)) %))))
                   els->css)))
       (str/join "\n\n")
       log)
  ;; => "#button-backward-icon {\n  width: 35px;\n  ...}"

)
```

Looking at the console, we see that we're back where we started:

``` css
#button-backward-icon {
  width: 35px;
  filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);
}

#button-play-icon {
  width: 35px;
  filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);
}

#button-pause-icon {
  width: 35px;
  filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);
}

#button-forward-icon {
  width: 35px;
  filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);
}

#button-speed-icon {
  width: 35px;
  filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);
}

#button-backward {
  padding: 0px;
  margin: 0px;
  border: 0px;
  background: transparent;
  cursor: pointer;
}

#button-play {
  padding: 0px;
  margin: 0px;
  border: 0px;
  background: transparent;
  cursor: pointer;
}

#button-pause {
  padding: 0px;
  margin: 0px;
  border: 0px;
  background: transparent;
  cursor: pointer;
}

#button-forward {
  padding: 0px;
  margin: 0px;
  border: 0px;
  background: transparent;
  cursor: pointer;
}

#button-speed {
  padding: 0px;
  margin: 0px;
  border: 0px;
  background: transparent;
  cursor: pointer;
}
```

What we really want to do is generate a single rule for each group, with the
remaining class as the selector. If we update `el->css` with a second arity that
accepts a selector as an argument, we can accomplish this fairly easily:

``` clojure
(ns cljcastr.player
  (:require [clojure.string :as str]))

;; ...

(defn el->css
  "Given an element (or the Clojure representation of an element), returns a CSS
   rule corresponding to the element's ID. If a CSS selector is specified, it
   will be used for the rule instead of the default."
  ([el]
   (let [{:keys [id tag classes]} (el->clj el)
         sel (if (not-empty id)
               (str "#" id)
               (str tag (if (seq classes)
                          (str "." (first classes)))))]
     (el->css el sel)))
  ([el sel]
   (format-css sel (:styles el))))

;; ...

(comment

  ;; ...

  (->> (get-els ".cljcastr")
       group-styles
       (filter (fn [[_ els]] (> (count els) 1)))
       (map (fn [[_styles [el & _]]]
              (let [sel (str "." (->> (:classes el)
                                      (remove (partial = "cljcastr"))
                                      first))]
                (el->css el sel))))
       (str/join "\n\n")
       log)
  ;; => ".playback-icon {\n  width: 35px;\n  ...}"

)
```

And now the console fills our hearts with delight once again!

``` css
.playback-icon {
  width: 35px;
  filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);
}

.playback-button {
  padding: 0px;
  margin: 0px;
  border: 0px;
  background: transparent;
  cursor: pointer;
}
```

Let's now take a step back and think about how we want to generate the rules:
1. Group by styles
2. For groups with a single element, just call `el->css` on it
3. For groups with multiple elements, take the first one, drop "cljcastr" from
   its list of classes, generate a selector from the first remaining class, and
   then call `el->css` on the element, using the custom selector

And if we say this in Clojure, it looks like this:

``` clojure
(comment

  ;; ...

  (->> (get-els ".cljcastr")
       group-styles
       (map (fn [[_styles [el & rest-els]]]
              (if (seq rest-els)
                (let [sel (str "." (->> (:classes el)
                                        (remove (partial = "cljcastr"))
                                        first))]
                  (el->css el sel))
                (el->css el))))
       (str/join "\n\n")
       log)
  ;; => "#progress-container {\n  position: relative;\n  height: 28px;\n  ...}"

)
```

If we look at the console, it appears that all of our prayers are answered! 🎉

``` css
#progress-container {
  position: relative;
  height: 28px;
  clear: both;
}

#progress {
  width: 390px;
  margin: 1.5em auto 0px;
  position: relative;
}

#progress-bar {
  color: rgb(247, 147, 30);
  top: 10px;
  overflow: hidden;
  background-color: lightgray;
  width: 100%;
  z-index: 0;
  appearance: none;
  position: absolute;
  border: 0px;
  border-radius: 4px;
  height: 8px;
  left: 0px;
  margin: 0px;
}

#episode-art {
  width: 400px;
  border-radius: 5%;
  padding: 10px;
}

#progress-slider {
  top: 0px;
  background-color: transparent;
  width: 100%;
  cursor: pointer;
  z-index: 1;
  appearance: none;
  position: absolute;
  border: 0px;
  touch-action: manipulation;
  height: 28px;
  left: 0px;
  margin: 0px;
}

#episode-date {
  color: grey;
}

#episode-info {
  max-width: 420px;
  text-align: center;
}

.playback-icon {
  width: 35px;
  filter: invert(59%) sepia(92%) saturate(611%) hue-rotate(343deg) brightness(97%) contrast(99%);
}

audio.cljcastr {
  display: none;
}

#elapsed-time {
  margin: 0px 5px;
  font-size: 0.7rem;
  color: grey;
  float: left;
}

.playback-button {
  padding: 0px;
  margin: 0px;
  border: 0px;
  background: transparent;
  cursor: pointer;
}

#remaining-time {
  margin: 0px 5px;
  font-size: 0.7rem;
  color: grey;
  float: right;
}

#episode-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}
```

Let's finally create a new `main.css` file next to our `index.html` and paste
all of these styles in there. Now all we have to do to use the styles is two
things:
1. Import the stylesheet in `index.html`
2. Regenerate our HTML without the inline styles

The first part is simple, we just add one line to our `index.html`:

``` html
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
  <title>cljcastr episode player</title>
  <link rel="stylesheet" href="main.css">

  <!-- ... -->
</head>

<!-- ... -->
```

**HERE**

And now for the new HTML. Thanks to the magic `:exclude-styles?` option we snuck
into `el->html`, it's as easy as pie:

``` clojure
(comment

  ;; ...

  (-> (el->html "body" {:filter-class "cljcastr", :exclude-styles? true})
      log)
  ;; => "<body>\n  <div class=\"cljcastr\" id=\"episode-container\">\n ..."

)
```

Now we just copy from the console:

``` html
<body>
  <div class="cljcastr" id="episode-container">
  <div class="cljcastr" id="episode-info">
  <h3 class="cljcastr" id="podcast-title">Tech Won't Save Us</h3>
  <h2 class="cljcastr" id="episode-title">Government is Putting AI Before People and the Planet w/ Matt Haugen</h2>
  <span class="cljcastr" id="episode-date">August 13, 2026</span>
  </div>
  <div id="episode-art-container" class="cljcastr">
  <img id="episode-art" class="cljcastr" src="https://storage.buzzsprout.com/tutsi25s4vue63c1xwucuexriowa"></img>
  </div>
  <audio class="cljcastr" controls="" src="https://audio.buzzsprout.com/w99dr716dc81mqan68yj446jkm0o?response-content-disposition=inline"></audio>
  <div class="cljcastr" id="progress">
  <span class="cljcastr" id="elapsed-time">00:00</span>
  <span class="cljcastr" id="remaining-time">-52:18</span>
  <div id="progress-container" class="cljcastr">
  <input min="0" value="0" aria-valuemax="3138.727422" type="range" aria-valuetext="00:00 of 52:18" max="3138.727422" id="progress-slider" class="cljcastr" aria-label="Seek" step="any"></input>
  </div>
  </div>
  <div class="cljcastr" id="playback-controls">
  <button class="cljcastr playback-button" id="button-backward" aria-label="Back 15 seconds">
  <img class="cljcastr playback-icon" id="button-backward-icon" src="https://www.svgrepo.com/show/495036/backward-15-seconds.svg" draggable="false"></img>
  </button>
  <button class="cljcastr playback-button" id="button-play" aria-label="Play">
  <img class="cljcastr playback-icon" id="button-play-icon" src="https://www.svgrepo.com/show/436852/play-circle-fill.svg" draggable="false"></img>
  </button>
  <button class="cljcastr playback-button" id="button-pause" aria-label="Pause">
  <img class="cljcastr playback-icon" id="button-pause-icon" src="https://www.svgrepo.com/show/436829/pause-circle-fill.svg" draggable="false"></img>
  </button>
  <button class="cljcastr playback-button" id="button-forward" aria-label="Forward 15 seconds">
  <img class="cljcastr playback-icon" id="button-forward-icon" src="https://www.svgrepo.com/show/495321/forward-15-seconds.svg" draggable="false"></img>
  </button>
  <button class="cljcastr playback-button" id="button-speed" aria-label="Playback speed">
  <img class="cljcastr playback-icon" id="button-speed-icon" src="https://www.svgrepo.com/show/151785/speed-meter.svg" draggable="false"></img>
  </button>
  </div>
  <div class="cljcastr" id="playback-speed"></div>
  </div>
  </body>
```

and paste into `index.html`.

Holding our breath, we reload the page, and... see the same thing we were just
looking at. 😅

But actually, that's precisely what we were trying to achieve. If we open dev
tools and inspect the play button, we see two exciting things:
1. No inline styles in the **button** element
2. In the **Styles** panel, there is a `.playback-button` class being applied,
   defined in `main.css`

![Player with just buttons][stylesheet]
[stylesheet]: assets/draft-podcast-player-stylesheet.png "The buttons, the whole buttons, and nothing but the buttons" class="border"


WHERE DO WE PUT THIS STUFF?

``` clojure

(defn attr->clj
  "Given an `Attr`, returns a vector of the name (as a keyword) and value of the
   attribute."
  [attr]
  [(keyword (.-name attr)) (.-value attr)])

;; ...

(defn attrs->str
  "Given a map of attributes, returns a string of k=\"v\"..."
  [attrs]
  (->> attrs
       (map (fn [[k v]] (str (name k) "=\"" v "\"")))
       (str/join " ")))

(defn el->clj
  "Given an HTML element, returns a map containing :tag, :text, and :attrs."
  [el]
  (if (map? el)  ; if el is already a map, just return it
    el
    (let [el (get-el el)]
      {:element el  ; hold onto a reference to the element itself
       :tag (get-name el)
       :attrs (attrs->clj (.-attributes el))
       :id (.-id el)
       :classes (get-classes el)
       :styles (get-styles el)
       :text (get-text el)})))

;; ...

(defn get-classes
  "Returns a list of the element's classes."
  [el]
  (let [el (get-el el)]
    (-> el
        .-className
        (str/split ","))))

(defn contains-class?
  "Returns truthy if the element's class list contains the specified class,
   falsey otherwise."
  [el cls]
  (let [el (get-el el)]
    (when (instance? js/Element el)
      (-> (get-classes el)
          set
          (contains? cls)))))

;; ...

(defn el->html
  "Given an element (or the Clojure representation of an element), returns an
   HTML string."
  ([el]
   (el->html el {}))
  ([el {:keys [filter-class exclude-styles?] :as opts}]
   (let [{:keys [element tag attrs text]} (if (map? el) el (el->clj el))
         attrs (if exclude-styles? (log "excluding styles" (dissoc attrs :style)) attrs)
         attrs-str (attrs->str attrs)
         tag-open (str "<" tag (if (empty? attrs-str) "" (str " " attrs-str)) ">")
         tag-close (str "</" tag ">")
         children (if filter-class
                    (->> (get-children element)
                         (filter #(contains-class? % filter-class)))
                    (get-children element))]
     (if (or (empty? children) (instance? js/Text (first children)))
       (str tag-open text tag-close)
       (str/join "\n  "
                 (concat [tag-open]
                         (map #(el->html % opts) children)
                         [tag-close]))))))
```
