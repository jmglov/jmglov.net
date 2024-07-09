Title: Soundcljoud, or a young man's Soundcloud clonejure
Date: 2024-07-09
Tags: clojure,babashka,clonejure,clojurescript,soundcljoud
Description: In which I put Soundcloud out of business in 243 lines of Clojure
Discuss: https://clojurians.slack.com/archives/C8NUSGWG6/p1720504400388379
Image: assets/2024-07-09-soundcljoud-preview.jpg
Image-Alt: A stack of CDs. Photo by Brett Jordan on Unsplash.

![A stack of CDs. Photo by Brett Jordan on Unsplash.][preview]
[preview]: assets/2024-07-09-soundcljoud-preview.jpg "I should be allowed to glue my poster" width=800px

😱 **Warning!**

This blog post is ostensibly about Clojure (of the
[Babashka](https://babashka.org/) variety), but not long after starting to write
it, I found myself some 3100 words into some rambling exposition about the
history of audio technology and how it intersected with my life, and had not
typed the word "ClojureScript" even once (though it may appear that I've now
typed it twice, I actually wrote this bit *post scriptum*, but decided to attach
it before the post, which I suppose makes it a prelude and not a postscript, but
I digress).

Whilst this won't surprise returning readers, I thought it worth warning
first-timers, and offering all readers the chance to [skip
over](#omg_finally_stuff_about_clojure) all the stage-setting and other
self-indulgent nonsense, simply by clicking the link that says "skip over".

If you'd like to delay your gratification, you are in luck! Read on, my friend!

## Rambling exposition

Once upon a time there were vinyl platters into which the strategic etching of
grooves could encode sound waves. If one placed said platter on a table and
turned it say, 78 times a minute, and attached a needle to an arm and dropped it
onto the rotating platter, one could use the vibrations in the arm caused by the
needle moving left and right in the grooves to decode the sound waves, which you
then turn into a fluctuating electric current and cause it to flow through a
coil which vibrates some fabric and reproduces the sound waves. And this was
good, for we could listen to music.

The only problem with these "records", as they were called, is that they were
kinda big and you couldn't fit them in your backpack. So [some Dutch people and
some Japanese people teamed
up](https://en.wikipedia.org/wiki/Compact_Disc_Digital_Audio#History) and
compacted the records, and renamed them discs because a record is required by
law (in Japan) to be a certain diameter or you can't call it a record. They
decided to make them out of plastic instead of vinyl, and then realised that
they couldn't cut grooves into plastic because they kept breaking the
discs—perhaps the choice of hammer and chisel to cut the grooves wasn't ideal,
but who am I to judge? "だってさ、" said one of the Japanese engineers, "[針でディスクにちっちゃい穴やったら、どうなるかな](https://translate.google.com/?sl=ja&tl=en&text=%E9%87%9D%E3%81%A7%E3%83%87%E3%82%A3%E3%82%B9%E3%82%AF%E3%81%AB%E3%81%A1%E3%81%A3%E3%81%A1%E3%82%83%E3%81%84%E7%A9%B4%E3%82%84%E3%81%A3%E3%81%9F%E3%82%89%E3%80%81%E3%81%A9%E3%81%86%E3%81%AA%E3%82%8B%E3%81%8B%E3%81%AA&op=translate)？"
No one knew, so they just tried it, and lo! the disc didn't break! But also lo!
poking at the disc with a needle made little bumps on the other side of the
disc, because of the law of conservation of mass or something... I don't know, I
had to drop out of physics in uni because it apparently takes me 20 hours to
solve one simple orbital mechanics problem; I mean, come on, how hard is it to
calculate the orbit of a planet around three stars? Jeez.

But anyway, they made some bumps, which was annoying at first but then turned
out to be a very good thing indeed when someone had the realisation that if
squinted at the disc with a binary way of thinking, you could consider a bump to
be a 1, and a flat place on the disc to be a 0, and then if you were to build a
digital analyser that sampled the position of a sound wave, say, 44,100 times a
second and wrote down the results in binary, you could encode the resulting
string of 1s and 0s onto the disc with a series of bumps.

But how to decode the bumps when trying to play the thing back? The solution was
super obvious this time: a frickin' laser beam! (Frickin' laser beams were on
everyone's mind back in the early 80s because of Star Wars—the movies; the
[missile defence
system](https://en.wikipedia.org/wiki/Strategic_Defense_Initiative) wouldn't
show up until a few years later). If they just fired a frickin' laser beam
continuously whilst rotating the disc and added a photodiode next to the laser,
the light bouncing back off a bump would knock the wavelength of the light 1/2
out of phase, which would partially cancel the reflected light, lowering the
intensity, which the photodiode would pick up and interpret as a 1. Obviously.

Except for one thing. Try as they might, the engineers couldn't make the
frickin' laser beam bounce off the frickin' surface of the frickin'
polycarbonate. If the plastic was too dark, it just absorbed the light, and if
it was too light, it certainly reflected it, but not with high enough intensity
for the photodiode to tell the difference between a 1 and a 0. 😢

This was a real head-scratcher, and they were well and truly stuck until one
day one of the Dutch engineers was enjoying a beer from a frosty glass at a table
at an outdoor cafe on Museumplein on a hot day and the condensation on the glass
made the coaster stick to the bottom of the glass in the annoying way it does
when one doesn't put a little table salt on the coaster first—amateur!—and the
coaster fell into a nearby ashtray (people used to put these paper tubes stuffed
with tobacco in their mouths, light them on fire, and suck the smoke deep into
their lungs; ask your parents, kids) and got all coated in ash. The engineer
wrinkled their nose in disgust before having an amazing insight. "What if," they
thought to themselves, "we coated one side of the polycarbonate with something
shiny that would reflect the frickin' laser?" Their train of thought then
continued thusly: "And what is both reflective and cheap? Why, this selfsame
aluminium of which this here ashtray is constructed!"

And thus the last engineering challenge was overcome, and there was much
rejoicing!

The first test of the technology was a recording of [Richard Strauss's "An
Alpine
Symphony"](https://en.wikipedia.org/wiki/Compact_disc#Initial_launch_and_adoption)
made in the beginning of December 1981, which was then presented to the world
the following spring. It took a whole year before the first commercial compact
disc was released, and by 1983, the technology had really taken off, thus
introducing digital music to the world and ironically sowing the seeds of the
format's demise. But I'm getting ahead of myself again.

Sometime around 1992, give or take, my parents got me a portable CD player (by
this time, people, being ~lazy~ efficient by nature, had stopped saying "compact
disc" and started abbreviating it to "CD") and one disc: Aerosmith's tour de
force ["Get a
Grip"](https://www.discogs.com/release/370731-Aerosmith-Get-A-Grip). Thus began
a period of intense musical accumulation by yours truly.

But remember when I said the CD format contained within it the seeds of its own
demise? Quoth [Wikipedia](https://en.wikipedia.org/wiki/MP3#Background), and
verily thus:

> In 1894, the American physicist Alfred M. Mayer reported that a tone could be
> rendered inaudible by another tone of lower frequency. In 1959, Richard Ehmer
> described a complete set of auditory curves regarding this phenomenon. Between
> 1967 and 1974, Eberhard Zwicker did work in the areas of tuning and masking of
> critical frequency-bands, which in turn built on the fundamental research in
> the area from Harvey Fletcher and his collaborators at Bell Labs

You see where this is going, right? Good, because I wouldn't want to condescend
to you by mentioning things like space-efficient compression with transforming
Fouriers into [Fast
Fouriers](https://en.wikipedia.org/wiki/Fast_Fourier_transform) modifying
discrete cosines and other trivia that would bore any 3rd grade physics student.

So anyway, [some Germans](https://en.wikipedia.org/wiki/Fraunhofer_Society)
scribbled down an algorithm and convinced the [Motion Picture Experts
Group](https://en.wikipedia.org/wiki/Moving_Picture_Experts_Group) to
standardise it as the MPEG-1 Audio Layer III format, and those Germans somehow
patented this "innovation" that no one had even bothered to write down because
it was so completely obvious to anyone who bothered to think about it for more
than the time a CD takes to revolve once or twice. This patent enraged such
people as [Richard Stallman](https://en.wikipedia.org/wiki/Richard_Stallman)
(who, to be fair, is easily enraged by such minor things as people objecting to
his mysogyny and [opinions on the acceptability of romantic relationships with
minors](https://en.wikipedia.org/wiki/Richard_Stallman#Comments_about_Jeffrey_Epstein_scandal)),
leading some people to develop a technically superior **and** [free as in
beer](https://en.wiktionary.org/wiki/free_as_in_beer) audio coding format that
they named after a [Terry Pratchett
character](https://en.wikipedia.org/wiki/List_of_Discworld_characters#Vorbis)
and a [clone](https://en.wikipedia.org/wiki/Netrek) of a
[clone](https://en.wikipedia.org/wiki/Empire_(PLATO\)) of
[Spacewar!](https://en.wikipedia.org/wiki/Spacewar!). The name, if you haven't
guessed it by now from the copious amount of clues I've dropped here (just call
me [Colonel
Mustard](https://en.wikipedia.org/wiki/List_of_Cluedo_characters#Colonel_Mustard))
was [Ogg Vorbis](https://en.wikipedia.org/wiki/Vorbis).

By early summer 2005, I had accumulated a large quantity of CDs, which weighed
roughly a metric shit-tonne. In addition to the strain they placed on my poor
second- or third-hand bookshelves, I was due to move to Japan in the fall, and
suspected that the sheer mass of my collection would interfere with the ability
of whatever plane I would be taking to Tokyo to become airborne, which would be
a real bummer. However, a solution presented itself, courtesy of one of the
technical shortcomings of the compact disc technology itself.

Remember how CDs have this metallic layer that reflects the laser back at the
sensor? Turns out that this layer is quite vulnerable, and a scratch that
removes even a tiny bit of the metal results in the laser not being reflected as
the disc rotates past the missing metal, which causes that block of data to be
discarded by the player as faulty. To recover from this, the player would [do one
of the
following](https://en.wikipedia.org/wiki/Skip_(audio_playback)#Basic_players):

1. Repeat the previous block of audio
2. Skip the faulty block
3. Try and retry to read it, causing a stopping and starting of the music

For the listener, this is a sub-optimal auditory experience, and most listeners
don't like any sub mixed in with their optimal.

Luckily, consumer-grade CD recorders started appearing in the mid-90s, when [HP
released the first sub-$1000 model](https://en.wikipedia.org/wiki/CD-R). As a
teenager in the 90s, I certainly couldn't afford $1000, but in 1997, I started
working as a PC repair technician, and we had a CD "burner" (as they were known
back then, not to be confused with a "burner" phone, which didn't exist back
then, at least not in the cultural zeitgeist of the time) for such uses as
device drivers which were too big to fit on a 3.5 inch "floppy" disk (those
disks weren't actually floppy, but their 5.25 inch predecessors certainly were).
I sensed an opportunity to protect my investment in digital music by "ripping"
my discs (transferring the data on a CD onto the computer) and then burning them
back to recordable CDs, at which point I could leave the original CD in its
protective case and only expose my copy to the harsh elements.

Of course, one could also leave the ripped audio on one's computer and listen to
it at any time of one's choosing, which was really convenient since you didn't
have to change CDs when the disc ended or you were just in the mood to listen to
something different. The problem is that the raw audio from the CDs (encoded in
the WAV format that even modern people are probably familiar with) was fairly
large, with a single CD taking up as much as 700MB of space. That may not seem
like much until you know that most personal computers in the late 90s had
somewhere between 3 and 16 GB of storage, which was enough to store between 20
and 220 CDs, assuming you had nothing else on the drive, which was unlikely
since you needed to have software for playing back the files which meant you
needed an operating system such as Windows...

To move somewhat more rapidly to the point, one solution to the issue of space
was rooted in an even older technology than the compact disc (though younger
than the venerable phonograph record): the cassette tape! A cassette tape was...
OK, given that I've written nigh upon 2000 words at this point without
mentioning Soundcloud or ClojureScript, perhaps I'll just link you to [the
Wikipedia article on the cassette
tape](https://en.wikipedia.org/wiki/Cassette_tape) instead of attempting to
explain how it works in an amusing (to me) fashion. Interesting (to me)
sidenote, though: the cassette tape was also invented by our intrepid Dutch
friends over at Philips! 🤯

And my point was... oh yeah, mixtapes! Cassette tapes were one of the first
media that gave your average consumer access to a recorder at an affordable
price (the earliest such media that I know of was the [reel-to-reel
tape](https://en.wikipedia.org/wiki/Reel-to-reel_audio_tape_recording), which
was like a giant cassette tape without the plastic bit that protects the tape),
and in addition to [stuffing tissue in the top of a tape just to record Marley
Marl](https://genius.com/Mop-follow-instructions-lyrics) that we borrowed from
our friend down the street, we also made "mixtapes", an alchemical process
whereby we boiled down our tape collection and extracted only the bangers (or
tear-jerkers, or hopelessly optimistic love songs, or whatever mood we were
trying to capture) and recorded those onto a tape, giving us 60 minutes of magic
to blare in our cars or hand to that cutie in chemistry class to try and win
their affection.

With the invention of the CD and the burner, we were back in the mixtape
business, and this time we had up to 80 minutes to express ourselves. By the
time I entered university back in 19\*cough\*, I had saved up enough from my job
as a PC technician to buy my own burner, and at university, I gained somewhat of
a reputation as a mixtape maestro. People would bring me a stack of CDs and ask
me to produce a mixtape to light up the dancefloor or get heads nodding along to
the dope-ass DJs of the time (I'm looking at you,
[Premo](https://en.wikipedia.org/wiki/DJ_Premier)!), and also pick a cheeky
title to scrawl onto the recordable CD in Sharpie. The one that sticks in my
memory was called "The Wu Tang Clan ain't Nothin' to Fuck With"...

OK, but anyway, what if 80 minutes wasn't enough? Remember several minutes of
rambling ago when I mentioned the MPEG-1 Audio Layer III format, and you may (or
may not) have been like, "WTF is that?" What if I told you that MPEG-1 Audio
Layer III is usually referred to by its initials (kinda): MP3? Now you see where
I'm going, right? By taking raw CD audio and compressing it with the MP3
encoding algorithm, one could now fit something like 140 songs onto a recordable
CD (assuming 5MB per song and 700MB of capacity on the CD), or roughly 10
albums.

So back to the summer of 2005, when I'm getting ready to move to Japan and I
realise I can't realistically take all of my CDs with me. What do I do? I rip
them onto my computer, encode them as not as MP3s but as Ogg Vorbis files
because, y'know, freedom and stuff, burn them onto a recordable CD along with ~9
of their compatriots, and pack them in a box, write their names on a [bill of
lading](https://en.wikipedia.org/wiki/Bill_of_lading) which I tape to the box
once it gets full, and then store the box in my parents' basement. The freshly
recorded backup CD goes into once of those big CD case thingies that we used to
have:

![A black Case Logic 200 CD capacity case][case]
[case]: assets/2024-07-09-soundcljoud-cd-case.jpg "That's not a case, mate. This is a case!" width=800px

My CD ripping frenzy was concluded in time for my move to Japan, but did not end
there, because I ended up getting a job at [this bookstore that also sold CDs and
other stuff](https://amazon.co.jp), and publishers would send books and CDs to
the buyers that worked at said bookstore, who would then decided if and how many
copies of said books and CDs to buy for stock, and then usually put the books
and CDs on a shelf in a printer room, where random bookstore employees such as
myself were welcome to take them. So I got some cool books, and loads and loads
of CDs, many of them from Japanese artists, which were promptly ripped, Ogg
Vorbisified, and written to a 500GB USB hard drive that I had bought from the
bookstore with my employee discount. Hurrah!

And thus when 2008 rolled around and I left Tokyo for Dublin, I did so with the
vast majority of my music safely encoded into Ogg Vorbis format and written to
spinning platters. Sadly, my sojourn on the shamrock shores of the Emerald Isle
didn't last long, but happily, my next stop in Stockholm has been of the more
permanent variety. By the time I moved here in 2010, Apple and Amazon's MP3
stores were starting to become passé, with streaming services replacing them as
the Cool New Thing™, led a brash young Swedish startup called Spotify. And lo!
did my collection of Ogg Vorbis files seem unnecessary, since I could now play
every song ever recorded whenever I wanted to without having to lug around a
hard drive full of files.

Except, at some point, some artists decided that they didn't want their music on
Spotify, [some for admirable
reasons](https://www.billboard.com/business/streaming/neil-young-spotify-joe-rogan-vaccines-letter-remove-music-1235022525/)
and others for, um, other rea$on$, and now I couldn't listen to every song ever
recorded whenever I wanted to without having to lug around a hard drive full of
files. Plus Spotify never had a lot of the Japanese music that I had on file.
This was suboptimal to be sure, but my laziness overwhelmed my desire to listen
to all of my music, until one fateful day that I was sad about something and
decided that I absolutely had to listen to some really sad country music, and
the first song that came to mind was Garth Brook's "[Much too Young to Feel this
Damn Old](https://www.youtube.com/watch?v=nIHWhUVxJh8)". Much to my dismay,
Garth was one of those artists who had withheld their catalogue from Spotify,
meaning I had to resort to a cover of the song instead.

My sadness was replaced by rage, and I turned to Clojure to exact my revenge on
Spotify for not having reached terms to licence music from one of the greatest
Country & Western recording artists of all time!

## OMG finally stuff about Clojure

If you wisely clicked the link at the beginning to skip my [rambling
exposition](#Rambling_exposition), welcome to a discussion of how I solved a
serious problem caused by a certain [Country & Western super-duper
star](https://en.wikipedia.org/wiki/Garth_Brooks) (much like VA Beach legend
[Magoo](https://en.wikipedia.org/wiki/Timbaland_&_Magoo)—RIP—[on every CD, he
spits 48 bars](https://genius.com/Timbaland-and-magoo-cop-that-shit-lyrics))
wisely flicking the V at the odious [Daniel
Ek](https://en.wikipedia.org/wiki/Daniel_Ek) and the terrible [Tim
Cook](https://en.wikipedia.org/wiki/Tim_Cook) but [somehow being
A-OK](https://uproxx.com/indie/garth-brooks-spotify-amazon-streaming-apple-music/)
with [an even more repulsive
billionaire](https://en.wikipedia.org/wiki/Jeff_Bezos)'s streaming service.

To briefly recap, I really wanted to listen to some tear jerkin' country, and
Spotify doesn't carry Garth, but I had purchased [all of his
albums](https://en.wikipedia.org/wiki/Garth_Brooks_discography) on CD back in
the day (all the ones recorded before 1998, anyway) and ripped them into Ogg
Vorbis format. Which is great, because I can listen to Garth anytime I want, as
long as that desire occurs whilst I happen to be sitting within reach of the
laptop onto which I copied all of those files. However, I like to do such things
as not sit within reach of my laptop all the time, so now I'm back to square
almost one.

One day, as I was bemoaning my fate, I had a flash of inspiration! What if I put
those files somewhere a web browser could reach them, and then I could listen to
them anytime I happened to be sitting within reach of a web browser, which is
basically always, since I have a web browser that fits in my pocket (I think it
can also make "phone calls", whatever those are). For example, I could upload
them to [Soundcloud](https://soundcloud.com/). The only problem with that is
that Soundcloud would claim that I was infringing on Garth's copyright, and
they'd kinda have a point, since not only could I listen to "[The Beaches of
Cheyenne](https://en.wikipedia.org/wiki/The_Beaches_of_Cheyenne)" anytime I
wanted to, having obtained a licence to do so by virtue of forking over $15 back
in 1996 for a piece of plastic dipped in metal, but so could any random person
with an internet connection.

This left me with only one option: clone Soundcloud! With Clojure! And call it
Soundcljoud because I just can't help myself! And write a long and frankly
absurdly self-indulgent blog post about it!

## OK really Clojure now I promise

As I mentioned, I have a bunch of Ogg Vorbis files on my laptop:

``` text
: jmglov@alhana; ls -1 ~/Music/g/Garth\ Brooks/
'Beyond the Season'
'Double Live (Disc 1)'
'Double Live (Disc 2)'
'Fresh Horses'
'Garth Brooks'
'In Pieces'
'No Fences'
"Ropin' the Wind"
Sevens
'The Chase'
'The Hits'
```

I also have [Babashka](https://babashka.org/):

![A logo of a face wearing a red hoodie with orange sunglasses featuring the Soundcloud logo][bb]
[bb]: assets/2024-07-09-soundcljoud-bb.png "Above the clouds, infinite skills create miracles"

So let's get to cloning!

The basic idea is to turn these Ogg Vorbis files into MP3 files, which the
standard [&lt;audio&gt; HTML
element](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/audio) knows
how to play, and then wrap a little ClojureScript around that element to stuff
my sweet sweet country music into the `<audio>` element and then call it a day.

We'll accomplish the first part with Babashka and some command-line tools. I'll
start by creating a new directory and dropping a `bb.edn` into it:

``` clojure
{:paths ["src" "resources"]}
```

Now I can create a `src/soundcljoud/main.clj` like this:

``` clojure
(ns soundcljoud.main
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.string :as str]))
```

Firing up a REPL in my trusty Emacs with `C-c M-j` and then evaluating the
buffer with `C-c C-k`, let me introduce Babashka to good ol' Garth:

``` clojure
(comment

  (def dir (fs/file (fs/home) "Music/g/Garth Brooks/Fresh Horses")) ; C-c C-v f c e
  ;; => #'soundcljoud.main/dir

)
```

If you're a returning reader, you'll of course have translated `C-c C-k` to
Control + c <pause> Control + k in your head and `C-c C-v f c e` to Control + c
<pause> Control + v <pause> f <pause> c <pause> e and understood that they mean
`cider-load-buffer` and `cider-pprint-eval-last-sexp-to-comment`, respectively.
If you're a first-timer, what's happening here is that I'm using a so-called [Rich
comment](https://betweentwoparens.com/blog/rich-comment-blocks/#rich-comment) (which
protects the code within the `(comment)` form from evaluation when the buffer is
evaluated) to evaluate forms one at a time as I REPL-drive my way towards a
working program, for this is The Lisp Way.

Let's take a look at the Ogg Vorbis files in this directory:

``` clojure
(comment

  (->> (fs/glob dir "*.ogg")
       (map str))
  ;; => ("~/Music/g/Garth Brooks/Fresh Horses/Garth Brooks - Ireland.ogg"
  ;;     "~/Music/g/Garth Brooks/Fresh Horses/Garth Brooks - The Fever.ogg"
  ;;     "~/Music/g/Garth Brooks/Fresh Horses/Garth Brooks - She's Every Woman.ogg"
  ;;     "~/Music/g/Garth Brooks/Fresh Horses/Garth Brooks - The Old Stuff.ogg"
  ;;     "~/Music/g/Garth Brooks/Fresh Horses/Garth Brooks - Rollin'.ogg"
  ;;     "~/Music/g/Garth Brooks/Fresh Horses/Garth Brooks - The Beaches of Cheyenne.ogg"
  ;;     "~/Music/g/Garth Brooks/Fresh Horses/Garth Brooks - That Ol' Wind.ogg"
  ;;     "~/Music/g/Garth Brooks/Fresh Horses/Garth Brooks - It's Midnight Cinderella.ogg"
  ;;     "~/Music/g/Garth Brooks/Fresh Horses/Garth Brooks - The Change.ogg"
  ;;     "~/Music/g/Garth Brooks/Fresh Horses/Garth Brooks - Cowboys and Angels.ogg")

)
```

Knowing my fastidious nature, I bet I wrote some useful tags into those Ogg
files. Let's use `vorbiscomment` to check:

``` clojure
(comment

  (->> (fs/glob dir "*.ogg")
       (map str)
       first
       (p/shell {:out :string} "vorbiscomment")
       :out
       str/split-lines)
  ;; => ["title=Ireland" "artist=Garth Brooks" "album=Fresh Horses"]

)
```

Most excellent! With a tiny bit more work, we can turn these strings into a map:

``` clojure
(comment

  (->> (fs/glob dir "*.ogg")
       (map str)
       first
       (p/shell {:out :string} "vorbiscomment")
       :out
       str/split-lines
       (map #(let [[k v] (str/split % #"=")] [(keyword k) v]))
       (into {}))
  ;; => {:title "Ireland", :artist "Garth Brooks", :album "Fresh Horses"}

)
```

And now I think we're ready to write a function that takes a filename and
returns this info:

``` clojure
(defn track-info [filename]
  (->> (p/shell {:out :string} "vorbiscomment" filename)
       :out
       str/split-lines
       (map #(let [[k v] (str/split % #"=")] [(keyword k) v]))
       (into {})
       (merge {:filename filename})))
```

Now that we've established that we have some Ogg Vorbis files with appropriate
metadata, let's jump in the hammock for a second and think about how we want to
proceed. What we're actually trying to accomplish is to make these tracks
playable on the web. What if we create a podcast RSS feed per album, then we can
use any podcast app to play the album?

## Faking a podcast with Selmer

Let's go this route, since it seems like very little work! We'll start by
creating a [Selmer](https://github.com/yogthos/Selmer) template in
`resources/album-feed.rss`:

``` xml
<?xml version='1.0' encoding='UTF-8'?>
<rss version="2.0"
     xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"
     xmlns:atom="http://www.w3.org/2005/Atom">
  <channel>
    <atom:link
        href="{{base-url}}/{{album|urlescape}}/album.rss"
        rel="self"
        type="application/rss+xml"/>
    <title>{{artist}} - {{album}}</title>
    <link>{{link}}</link>
    <pubDate>{{date}}</pubDate>
    <lastBuildDate>{{date}}</lastBuildDate>
    <ttl>60</ttl>
    <language>en</language>
    <copyright>All rights reserved</copyright>
    <webMaster>{{owner-email}}</webMaster>
    <description>Album: {{artist}} - {{album}}</description>
    <itunes:subtitle>Album: {{artist}} - {{album}}</itunes:subtitle>
    <itunes:owner>
      <itunes:name>{{owner-name}}</itunes:name>
      <itunes:email>{{owner-email}}</itunes:email>
    </itunes:owner>
    <itunes:author>{{artist}}</itunes:author>
    <itunes:explicit>no</itunes:explicit>
    <itunes:image href="{{image}}"/>
    <image>
      <url>{{image}}</url>
      <title>{{artist}} - {{album}}</title>
      <link>{{link}}</link>
    </image>
    {% for track in tracks %}
    <item>
      <itunes:title>{{track.title}}</itunes:title>
      <title>{{track.title}}</title>
      <itunes:author>{{artist}}</itunes:author>
      <enclosure
          url="{{base-url}}/{{artist|urlescape}}/{{album|urlescape}}/{{track.mp3-filename|urlescape}}"
          length="{{track.mp3-size}}" type="audio/mpeg" />
      <pubDate>{{date}}</pubDate>
      <itunes:duration>{{track.duration}}</itunes:duration>
      <itunes:episode>{{track.number}}</itunes:episode>
      <itunes:episodeType>full</itunes:episodeType>
      <itunes:explicit>false</itunes:explicit>
    </item>
    {% endfor %}
  </channel>
</rss>
```

If you're not familiar with Selmer, the basic idea is that anything inside
`{{}}` tags is a variable, and you also have some looping constructs like
`{% for %}` and so on. So let's look at the variables that we slapped in
that template:

General info:
- base-url
- owner-name
- owner-email

Album-specific stuff:
- album
- artist
- link
- date
- image

Track-specific stuff:
- track.title
- track.mp3-filename
- track.mp3-size
- track.duration
- track.number

OK, so where are we going to get all this? The general info is easy; we can just
decide what we want it to be and slap it in a variable:

``` clojure
(comment

  (def opts {:base-url "http://localhost:1341"
             :owner-name "Josh Glover"
             :owner-email "jmglov@jmglov.net"})
  ;; => #'soundcljoud.main/opts

)
```

The album-specific stuff is a little more challenging. `album` and `artist` we
can get from our `track-info` function, and `link` can be something like
`base-url` + `artist` + `album`, but what about `date` (the date the album was
released) and `image` (the cover image of the album)? Well, for this we can use
a music database that offers API access, such as
[Discogs](https://www.discogs.com/). Let's start by creating an account and then
visiting the [Developers settings
page](https://www.discogs.com/settings/developers) to generate a personal access
token, which we'll save in `resources/discogs-token.txt`. With this in hand,
let's try searching for an album. We'll need to add an HTTP client (luckily,
[Babashka ships with one](https://github.com/babashka/http-client)), a JSON
parser (luckily, [Babashka ships with one](https://github.com/dakrone/cheshire))
and a way to load the `resources/discogs-token.txt` to our namespace, then we
can use the API.

``` clojure
(ns soundcljoud.main
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.string :as str]
            ;; ⬇⬇⬇ New stuff ⬇⬇⬇
            [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.java.io :as io]))

(comment

  (def discogs-token (-> (io/resource "discogs-token.txt")
                         slurp
                         str/trim-newline))
  ;; => #'soundcljoud.main/discogs-token

  (def album-info (->> (fs/glob dir "*.ogg")
                       (map str)
                       first
                       track-info))
  ;; => #'soundcljoud.main/album-info

  (-> (http/get "https://api.discogs.com/database/search"
                {:query-params {:artist (:artist album-info)
                                :release_title (:album album-info)
                                :token discogs-token}
                 :headers {:User-Agent "SoundCljoud/0.1 +https://jmglov.net"}})
      :body
      (json/parse-string keyword)
      :results
      first)
  ;;  {:format ["CD" "Album"],
  ;;   :master_url "https://api.discogs.com/masters/212114",
  ;;   :cover_image
  ;;   "https://i.discogs.com/0eLXmM1tK1grkH8cstgDT6eV2TlL0NvgWPZBoyScJ_8/rs:fit/g:sm/q:90/h:600/w:600/czM6Ly9kaXNjb2dz/LWRhdGFiYXNlLWlt/YWdlcy9SLTY4NDcx/Ny0xNzE3NDU5MDIy/LTMxNjguanBlZw.jpeg",
  ;;   :title "Garth Brooks - Fresh Horses",
  ;;   :style ["Country Rock" "Pop Rock"],
  ;;   :year "1995",
  ;;   :id 212114,
  ;;   ...
  ;;  }

)
```

This looks very promising indeed! We now have the release year, which we can put
in our RSS feed as `date`, and the cover image, which we can put in `image`. Now
let's grab info for the tracks:

``` clojure
(comment

  (def master-url (:master_url *1))
  ;; => #'soundcljoud.main/master-url

)
```

That `(:master_url *1)` thing might be new to you, so let me explain before we
continue. The REPL keeps track of the result of the last three evaluations, and
binds them to `*1`, `*2`, and `*3`. So `(:master_url *1)` says "give me the
`:master_url` key of the result of the last evaluation, which I assume is a map
or I'm
[SOL](https://www.urbandictionary.com/define.php?term=shit%20out%20of%20luck)".

OK, back to the fetching track info:

``` clojure
(comment

  (def master-url (:master_url *1))
  ;; => #'soundcljoud.main/master-url

  (-> (http/get master-url
                {:query-params {:token discogs-token}
                 :headers {:User-Agent "SoundCljoud/0.1 +https://jmglov.net"}})
      :body
      (json/parse-string keyword)
      :tracklist)
  ;; => [{:position "1",
  ;;      :title "The Old Stuff",
  ;;      :duration "4:12"}
  ;;     {:position "2",
  ;;      :title "Cowboys And Angels",
  ;;      :duration "3:16"}
  ;;     ...
  ;;    ]

)
```

We now have all the pieces, so let's clean this up by turning it into a series
of functions:

``` clojure
(def discogs-base-url "https://api.discogs.com")
(def user-agent "SoundCljoud/0.1 +https://jmglov.net")

(defn load-token []
  (-> (io/resource "discogs-token.txt")
      slurp
      str/trim-newline))

(defn api-get
  ([token path]
   (api-get token path {}))
  ([token path opts]
   (let [url (if (str/starts-with? path discogs-base-url)
               path
               (str discogs-base-url path))]
     (-> (http/get url
                   (merge {:headers {:User-Agent user-agent}}
                          opts))
         :body
         (json/parse-string keyword)))))

(defn search-album [token {:keys [artist album]}]
  (api-get token "/database/search"
           {:query-params {:artist artist
                           :release_title album
                           :token token}}))

(defn album-info [token {:keys [artist album] :as metadata}]
  (let [{:keys [cover_image master_url year]}
        (->> (search-album token metadata)
             :results
             first)
        {:keys [tracklist]} (api-get token master_url)]
    (merge metadata {:link master_url
                     :image cover_image
                     :year year
                     :tracks (map (fn [{:keys [title position]}]
                                    {:title title
                                     :artist artist
                                     :album album
                                     :number position
                                     :year year})
                                  tracklist)})))
```

Putting it all together, let's load all the album info in a format that's
amenable to stuffing into our RSS template:

``` clojure
(comment

  (let [tracks (->> (fs/glob dir "*.ogg")
                    (map (comp track-info fs/file)))]
    (album-info (load-token) (first tracks))))
  ;; => {:title "Ireland",
  ;;     :artist "Garth Brooks",
  ;;     :album "Fresh Horses",
  ;;     :link "https://api.discogs.com/masters/212114",
  ;;     :image "https://i.discogs.com/0eLXmM1tK1grkH8cstgDT6eV2TlL0NvgWPZBoyScJ_8/rs:fit/g:sm/q:90/h:600/w:600/czM6Ly9kaXNjb2dz/LWRhdGFiYXNlLWlt/YWdlcy9SLTY4NDcx/Ny0xNzE3NDU5MDIy/LTMxNjguanBlZw.jpeg",
  ;;     :year "1995",
  ;;     :tracks
  ;;     ({:title "The Old Stuff",
  ;;       :artist "Garth Brooks",
  ;;       :album "Fresh Horses",
  ;;       :year "1995",
  ;;       :number 1}
  ;;      {:title "Cowboys and Angels",
  ;;       :artist "Garth Brooks",
  ;;       :album "Fresh Horses",
  ;;       :year "1995",
  ;;       :number 2}
  ;;      ...
  ;;      {:title "Ireland",
  ;;       :artist "Garth Brooks",
  ;;       :album "Fresh Horses",
  ;;       :year "1995",
  ;;       :number 10})}

)
```

Now that we have a big ol' map containing all the metadata an RSS feed could
possibly desire, let's use Selmer to turn our template into some actual RSS!
We'll need to add Selmer itself to our namespace, and also grab some `java.time`
stuff in order to produce the [RFC 2822](http://www.faqs.org/rfcs/rfc2822.html)
datetime [required by the podcast RSS
format](https://podcasters.apple.com/support/823-podcast-requirements), then we
can get onto the templating itself.

``` clojure
(ns soundcljoud.main
  (:require ...
            [selmer.parser :as selmer])
  (:import (java.time ZonedDateTime)
           (java.time.format DateTimeFormatter)))

(def dt-formatter
  (DateTimeFormatter/ofPattern "EEE, dd MMM yyyy HH:mm:ss xxxx"))

(defn ->rfc-2822-date [date]
  (-> (Integer/parseInt date)
      (ZonedDateTime/of 1 1 0 0 0 0 java.time.ZoneOffset/UTC)
      (.format dt-formatter)))

(defn album-feed [opts album-info]
  (let [template (-> (io/resource "album-feed.rss") slurp)]
    (->> (update album-info :tracks
                 (partial map #(update % :mp3-filename fs/file-name)))
         (merge opts {:date (->rfc-2822-date (:year album-info))})
         (selmer/render template))))

(comment

  (let [tracks (->> (fs/glob dir "*.ogg")
                    (map (comp track-info fs/file)))]
    (->> (album-info (load-token) (first tracks))
         (album-feed opts)))
  ;; => java.lang.NullPointerException soundcljoud.main
  ;; {:type :sci/error, :line 3, :column 53, ...}
  ;;  at sci.impl.utils$rethrow_with_location_of_node.invokeStatic (utils.cljc:135)
  ;;  ...
  ;; Caused by: java.lang.NullPointerException: null
  ;;  at babashka.fs$file_name.invokeStatic (fs.cljc:182)
  ;;  ...

)
```

Oops! It appears that `fs/file-name` is angry at us. Searching for it, we
identify the culprit:

``` clojure
(partial map #(update % :mp3-filename fs/file-name))
```

Nowhere in our `album-info` map have we mentioned `:mp3-filename`, which
actually makes sense given that we only have an Ogg Vorbis file and not an MP3.
Let's see what we can do about that, shall we? (Spoiler: we shall.)

## Converting from Ogg to MP3

We'll honour Rich Hickey by decomplecting this problem into two problems:
1. Converting an Ogg Vorbis file into a WAV
2. Converting a WAV into an MP3

Let's start with problem #1 by taking a look at what we get back from
`album-info`:

``` clojure
(comment

  (let [tracks (->> (fs/glob dir "*.ogg")
                    (map (comp track-info fs/file)))]
    (album-info (load-token) (first tracks))))
  ;; => {:title "Ireland",
  ;;     :artist "Garth Brooks",
  ;;     :album "Fresh Horses",
  ;;     :link "https://api.discogs.com/masters/212114",
  ;;     :image "https://i.discogs.com/0eLXmM1tK1grkH8cstgDT6eV2TlL0NvgWPZBoyScJ_8/rs:fit/g:sm/q:90/h:600/w:600/czM6Ly9kaXNjb2dz/LWRhdGFiYXNlLWlt/YWdlcy9SLTY4NDcx/Ny0xNzE3NDU5MDIy/LTMxNjguanBlZw.jpeg",
  ;;     :year "1995",
  ;;     :tracks
  ;;     ({:title "The Old Stuff",
  ;;       :artist "Garth Brooks",
  ;;       :album "Fresh Horses",
  ;;       :year "1995",
  ;;       :number 1}
  ;;      {:title "Cowboys and Angels",
  ;;       :artist "Garth Brooks",
  ;;       :album "Fresh Horses",
  ;;       :year "1995",
  ;;       :number 2}
  ;;      ...
  ;;      {:title "Ireland",
  ;;       :artist "Garth Brooks",
  ;;       :album "Fresh Horses",
  ;;       :year "1995",
  ;;       :number 10})}

)
```

The problem here is that we've lost the filename that came from `fs/glob`, so we
have no idea which files we need to convert. Let's fix this by tweaking
`album-info` to take the token and directory, rather than just the track info of
the first file in the directory:

``` clojure
(defn normalise-title [title]
  (-> title
      str/lower-case
      (str/replace #"[^a-z]" "")))

(defn album-info [token tracks]
  (let [{:keys [artist album] :as track} (first tracks)
        track-filename (->> tracks
                            (map (fn [{:keys [filename title]}]
                                   [(normalise-title title) filename]))
                            (into {}))
        {:keys [cover_image master_url year]}
        (->> (search-album token track)
             :results
             first)
        {:keys [tracklist]} (api-get token master_url)]
    (merge track {:link master_url
                  :image cover_image
                  :year year
                  :tracks (map (fn [{:keys [title position]}]
                                 {:title title
                                  :artist artist
                                  :album album
                                  :number position
                                  :year year
                                  :filename (track-filename (normalise-title title))})
                               tracklist)})))

(comment

  (->> (fs/glob dir "*.ogg")
       (map (comp track-info fs/file))
       (album-info (load-token)))
  ;; => {:artist "Garth Brooks",
  ;;     :album "Fresh Horses",
  ;;     :link "https://api.discogs.com/masters/212114",
  ;;     :image
  ;;     "https://i.discogs.com/0eLXmM1tK1grkH8cstgDT6eV2TlL0NvgWPZBoyScJ_8/rs:fit/g:sm/q:90/h:600/w:600/czM6Ly9kaXNjb2dz/LWRhdGFiYXNlLWlt/YWdlcy9SLTY4NDcx/Ny0xNzE3NDU5MDIy/LTMxNjguanBlZw.jpeg",
  ;;     :year "1995",
  ;;     :tracks
  ;;     ({:title "The Old Stuff",
  ;;       :artist "Garth Brooks",
  ;;       :album "Fresh Horses",
  ;;       :number "1",
  ;;       :year "1995",
  ;;       :filename
  ;;       #object[java.io.File 0x96d79f0 "~/Music/g/Garth Brooks/Fresh Horses/Garth Brooks - The Old Stuff.ogg"]}
  ;; ...
  ;;      {:title "Ireland",
  ;;       :artist "Garth Brooks",
  ;;       :album "Fresh Horses",
  ;;       :number "10",
  ;;       :year "1995",
  ;;       :filename
  ;;       #object[java.io.File 0x13968577 "~/Music/g/Garth Brooks/Fresh Horses/Garth Brooks - Ireland.ogg"]})}

)
```

Much better! Given this, let's convert this file into a WAV:

``` clojure
(comment

  (def info (->> (fs/glob dir "*.ogg")
                 (map (comp track-info fs/file))
                 (album-info (load-token))))
  ;; => #'soundcljoud.main/info

  (def tmpdir (fs/create-dirs "/tmp/soundcljoud"))
  ;; => #'soundcljoud.main/tmpdir

  (let [{:keys [filename] :as track} (->> info :tracks first)
        out-filename (fs/file tmpdir (str/replace (fs/file-name filename)
                                                  ".ogg" ".wav"))]
    (p/shell "oggdec" "-o" out-filename filename)
    (assoc track :wav-filename out-filename))
  ;; => {:title "The Old Stuff",
  ;;     :artist "Garth Brooks",
  ;;     :album "Fresh Horses",
  ;;     :number "1",
  ;;     :year "1995",
  ;;     :filename
  ;;     #object[java.io.File 0x96d79f0 "~/Music/g/Garth Brooks/Fresh Horses/Garth Brooks - The Old Stuff.ogg"],
  ;;     :wav-filename
  ;;     #object[java.io.File 0x4221dcb2 "/tmp/soundcljoud/Garth Brooks - The Old Stuff.wav"]}

)
```

Lovely! Let's make a nice function out of this:

``` clojure
(defn ogg->wav [{:keys [filename] :as track} tmpdir]
  (let [out-filename (fs/file tmpdir (str/replace (fs/file-name filename)
                                                  ".ogg" ".wav"))]
    (println (format "Converting %s -> %s" filename out-filename))
    (p/shell "oggdec" "-o" out-filename filename)
    (assoc track :wav-filename out-filename)))
```

Now let's see if problem #2 is equally tractable.

``` clojure
(comment

  (let [{:keys [filename artist album title year number] :as track}
        (->> info :tracks first)
        wav-file (fs/file tmpdir
                          (-> (fs/file-name filename)
                              (str/replace #"[.][^.]+$" ".wav")))
        mp3-file (str/replace wav-file ".wav" ".mp3")
        ffmpeg-args ["ffmpeg" "-i" wav-file
                     "-vn"  ; no video
                     "-q:a" "2"  ; dynamic bitrate averaging 192 KB/s
                     "-y"  ; overwrite existing files without prompting
                     mp3-file]]
    (p/shell "ffmpeg" "-i" wav-file
             "-vn"       ; no video
             "-q:a" "2"  ; dynamic bitrate averaging 192 KB/s
             "-y"        ; overwrite existing files without prompting
             mp3-file))
  ;; => {:exit 0,
  ;;     ...
  ;;     }

  (fs/size "/tmp/soundcljoud/Garth Brooks - The Old Stuff.mp3")
  ;; => 5941943

)
```

Nice! There's one annoying thing about this, though. My Ogg Vorbis file had
metadata tags telling me stuff and also things about the contents of the file,
whereas my MP3 is inscrutable, save for the filename. Let's ameliorate this with
our good friend [id3v2](https://id3v2.sourceforge.net/):

``` clojure
(comment

  (let [{:keys [filename artist album title year number] :as track}
        (->> info :tracks first)
        wav-file (fs/file tmpdir
                          (-> (fs/file-name filename)
                              (str/replace #"[.][^.]+$" ".wav")))
        mp3-file (str/replace wav-file ".wav" ".mp3")
        ffmpeg-args ["ffmpeg" "-i" wav-file
                     "-vn"  ; no video
                     "-q:a" "2"  ; dynamic bitrate averaging 192 KB/s
                     "-y"  ; overwrite existing files without prompting
                     mp3-file]]
    (p/shell "id3v2"
             "-a" artist "-A" album "-t" title "-y" year "-T" number
             mp3-file))
  ;; => {:exit 0,
  ;;     ...
  ;;     }

  (->> (p/shell {:out :string}
                "id3v2" "--list"
                "/tmp/soundcljoud/Garth Brooks - The Old Stuff.mp3")
       :out
       str/split-lines)
  ;; => ["id3v1 tag info for /tmp/soundcljoud/Garth Brooks - The Old Stuff.mp3:"
  ;;     "Title  : The Old Stuff                   Artist: Garth Brooks"
  ;;     "Album  : Fresh Horses                    Year: 1995, Genre: Unknown (255)"
  ;;     "Comment:                                 Track: 1"
  ;;     "id3v2 tag info for /tmp/soundcljoud/Garth Brooks - The Old Stuff.mp3:"
  ;;     "TPE1 (Lead performer(s)/Soloist(s)): Garth Brooks"
  ;;     "TALB (Album/Movie/Show title): Fresh Horses"
  ;;     "TIT2 (Title/songname/content description): The Old Stuff"
  ;;     "TRCK (Track number/Position in set): 1"]

)
```

There's an awful lot of copy and paste code here, so let's consolidate MP3
conversion and tag writing into a single function. We should also make sure that
function returns a track info map that contains all the good stuff that our RSS
template needs. Casting our mind back to the track-specific stuff, we need:
- track.title
- track.number
- track.mp3-filename
- track.mp3-size
- track.duration

`mp3-filename` we have, and `m3-size` we can get with the same `fs/size` call
that we previously used to check if the MP3 file existed. `duration` is a little
more interesting. What the RSS feed standard is looking for is a duration in one
of the following formats:
- hours:minutes:seconds
- minutes:seconds
- seconds

We can use the [ffprobe](https://ffmpeg.org/ffprobe.html) tool that ships with
[FFmpeg](https://ffmpeg.org/) to get some info about the MP3:

``` clojure
(comment

  (-> (p/shell {:out :string}
               "ffprobe -v quiet -print_format json -show_format -show_streams"
               "/tmp/soundcljoud/01 - Garth Brooks - The Old Stuff.mp3")
      :out
      (json/parse-string keyword)
      :streams
      first)
  ;; => {:tags {:encoder "Lavc60.3."},
  ;;     :r_frame_rate "0/0",
  ;;     :sample_rate "44100",
  ;;     :channel_layout "stereo",
  ;;     :channels 2,
  ;;     :duration "252.473469",
  ;;     :codec_name "mp3",
  ;;     :bit_rate "188278",
  ;;     ...
  ;;     :codec_tag "0x0000"}

)
```

Cool! `ffprobe` reports duration in seconds (with some extra nanoseconds that we
don't need), so let's write a function that grabs the duration and chops off
everything after the decimal place, then we can consolidate the WAV -> MP3
conversion and ID3 tag writing in another function:

``` clojure
(defn mp3-duration [filename]
  (-> (p/shell {:out :string}
               "ffprobe -v quiet -print_format json -show_format -show_streams"
               filename)
      :out
      (json/parse-string keyword)
      :streams
      first
      :duration
      (str/replace #"[.]\d+$" "")))

(defn wav->mp3 [{:keys [filename artist album title year number] :as track} tmpdir]
  (let [wav-file (fs/file tmpdir
                          (-> (fs/file-name filename)
                              (str/replace #"[.][^.]+$" ".wav")))
        mp3-file (str/replace wav-file ".wav" ".mp3")
        ffmpeg-args ["ffmpeg" "-i" wav-file
                     "-vn"  ; no video
                     "-q:a" "2"  ; dynamic bitrate averaging 192 KB/s
                     "-y"  ; overwrite existing files without prompting
                     mp3-file]
        id3v2-args ["id3v2"
                    "-a" artist "-A" album "-t" title "-y" year "-T" number
                    mp3-file]]
    (println (format "Converting %s -> %s" wav-file mp3-file))
    (apply println (map str ffmpeg-args))
    (apply p/shell ffmpeg-args)
    (println "Writing ID3 tag")
    (apply println id3v2-args)
    (apply p/shell (map str id3v2-args))
    (assoc track
           :mp3-filename mp3-file
           :mp3-size (fs/size mp3-file)
           :duration (mp3-duration mp3-file))))

(comment

  (-> info :tracks first (wav->mp3 tmpdir))
  ;; => {:number "1",
  ;;     :duration "252",
  ;;     :artist "Garth Brooks",
  ;;     :title "The Old Stuff",
  ;;     :year "1995",
  ;;     :filename
  ;;     #object[java.io.File 0x96d79f0 "~/Music/g/Garth Brooks/Fresh Horses/Garth Brooks - The Old Stuff.ogg"],
  ;;     :mp3-filename "/tmp/soundcljoud/Garth Brooks - The Old Stuff.mp3",
  ;;     :album "Fresh Horses",
  ;;     :mp3-size 5943424}

)
```

Looking good! Now we should have everything we need for the RSS feed, so let's
try to put it all together:

``` clojure
(defn process-track [track tmpdir]
  (-> track
      (ogg->wav tmpdir)
      (wav->mp3 tmpdir)))

(defn process-album [opts dir]
  (let [info (->> (fs/glob dir "*.ogg")
                  (map (comp track-info fs/file))
                  (album-info (load-token)))
        tmpdir (fs/create-temp-dir {:prefix "soundcljoud."})]
    (spit (fs/file tmpdir "album.rss") (rss/album-feed opts info))
    (assoc info :out-dir tmpdir)))

(comment

  (process-album opts dir)
  ;; => {:out-dir "/tmp/soundcljoud.12524185230907219576"
  ;;     :artist "Garth Brooks",
  ;;     :album "Fresh Horses",
  ;;     :link "https://api.discogs.com/masters/212114",
  ;;     :image
  ;;     "https://i.discogs.com/0eLXmM1tK1grkH8cstgDT6eV2TlL0NvgWPZBoyScJ_8/rs:fit/g:sm/q:90/h:600/w:600/czM6Ly9kaXNjb2dz/LWRhdGFiYXNlLWlt/YWdlcy9SLTY4NDcx/Ny0xNzE3NDU5MDIy/LTMxNjguanBlZw.jpeg",
  ;;     :year "1995",
  ;;     :tracks
  ;;     ({:number "1",
  ;;       :duration "252",
  ;;       :artist "Garth Brooks",
  ;;       :title "The Old Stuff",
  ;;       :year "1995",
  ;;       :filename
  ;;       #object[java.io.File 0x344bc92b "~/Music/g/Garth Brooks/Fresh Horses/Garth Brooks - The Old Stuff.ogg"],
  ;;       :mp3-filename
  ;;       "/tmp/soundcljoud.12524185230907219576/Garth Brooks - The Old Stuff.mp3",
  ;;       :album "Fresh Horses",
  ;;       :wav-filename
  ;;       #object[java.io.File 0x105830d2 "/tmp/soundcljoud.12524185230907219576/Garth Brooks - The Old Stuff.wav"],
  ;;       :mp3-size 5943424}
  ;;      ...
  ;;      {:number "10",
  ;;       :duration "301",
  ;;       :artist "Garth Brooks",
  ;;       :title "Ireland",
  ;;       :year "1995",
  ;;       :filename
  ;;       #object[java.io.File 0x59ba6e31 "~/Music/g/Garth Brooks/Fresh Horses/Garth Brooks - Ireland.ogg"],
  ;;       :mp3-filename
  ;;       "/tmp/soundcljoud.12524185230907219576/Garth Brooks - Ireland.mp3",
  ;;       :album "Fresh Horses",
  ;;       :wav-filename
  ;;       #object[java.io.File 0x4de1472 "/tmp/soundcljoud.12524185230907219576/Garth Brooks - Ireland.wav"],
  ;;       :mp3-size 6969472})}
)
```

We also have a `/tmp/soundcljoud.12524185230907219576/album.rss` file
containing:

``` xml
<?xml version='1.0' encoding='UTF-8'?>
<rss version="2.0"
     xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"
     xmlns:atom="http://www.w3.org/2005/Atom">
  <channel>
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
          url="http://localhost:1341/Garth+Brooks/Fresh+Horses/01+-+Garth+Brooks+-+The+Old+Stuff.mp3"
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

In theory, if we put this RSS file and our MP3 somewhere a podcast player can
find them, we should be able to listen to some Garth Brooks! However,
http://localhost:1341/ is not likely to be reachable by a podcast player, so
perhaps we should put a webserver there and whilst we're at it, just write our
own little Soundcloud clone webapp. Seems reasonable, right?

We'll get into that in the next instalment of "Soundcljoud, or a young man's
Soundcloud clonejure."

Part 2: [Soundcljoud gets more cloudy](2024-07-20-soundcljoud-cloudy.html)
