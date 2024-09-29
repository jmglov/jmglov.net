Title: I Heart Heart of Clojure
Date: FIXME
Tags: clojure,conference debrief
Description: In which I am reminded that what I love about Clojure isn't parens, it's people
Discuss: FIXME
Image: assets/draft-heart-of-clojure-2024-preview.png
Image-Alt: A Remarkable e-paper notebook lying on a table
Preview: true


![A Remarkable e-paper notebook lying on a table][preview]
[preview]: assets/draft-heart-of-clojure-2024-preview.png "This Remarkable is almost as remarkable as Heart of Clojure!" width=800px

The curtain rises to reveal a person sitting on an airplane, surrounded by
people, yet alone in their own thoughts. The person is wearing the standard tech
worker uniform of jeans and a tee shirt—somehow not emblazoned with the logo of
an obscure yet impossibly cool startup or xkcd reference and harder yet to
process, not black! Suddenly, the fourth wall dissolves just long enough for the
author to beg you, dear audience member, to suspend your disbelief until the end
of this paragraph, to come along on the journey. Then the fourth wall reforms
and your attention is drawn to the pen in the pink-shirted person's hand, poised
over a sheet of digital paper just so. The pen descends, starts to move across
the e-paper soundlessly. The actor somehow conveys with their face the deep
satisfaction that the tactile feel of the pen brings as the e-ink 'flows. The
pen pauses for a moment, then is lifted to the bearded chin of the person. As
they gradually raise their head, you inadvertently gasp—audibly so—as you
realise that it was me all along!

And by "me", of course, I mean me. Josh. You know, author of this blog. Writer
of thousands of superfluous words. Scarer of fellow Clojurians when David Raya
dives to get his hands to that penalty oh shit he's only pushed it back into the
path of the Atalanta player but omg how has he gotten up so quickly and YES!!!!!!! he
saves from the rebound YEEEESSSSS!!! sorry Eric, everything's fine, what were
you saying again?

Surprise surprise, I'm getting ahead of myself again and going on tangents and
just start from the bloody beginning already, Josh!

I was born in 19 *cough* on a farm in Virginia... oh wait, not that beginning?
Let's try again:

When [Arne Brasseur](https://github.com/plexus) announced earlier this year that
he and the [Gaiwan](https://gaiwan.co/) team would be bringing [Heart of
Clojure](https://2024.heartofclojure.eu/) back for a second instalment, my heart
leapt for joy, for I had missed the [original back in
2019](https://2019.heartofclojure.eu/) for reasons that I can't remember, and I
heard from every attendee that I talked to about it that it was the most
special, lovely, inclusive, inspiring, challenging, and amazing tech conference
they'd ever been to. I had been hoping for an encore, then the long 2020 began
and tech conferences stopped being a thing I thought much about for awhile, so I
was delightfully surprised when the announcement was made, then crestfallen when
I saw the dates, for I would be in London visiting family friends (and also the
Arsenal ground, but that surely wasn't the reason I picked that specific weekend
to travel; surely!).

So it was with a certain melancholy tempered with joy that I read the subsequent
posts from Arne on the conference, revealing more details as he and the team
finalised concepts and logistics and so on. About three weeks ago, after reading
another such post, I remarked to my wife that I was so bummed that this `(amazing
conference)`—rendered in fixed width front because `amazing` is a macro:

``` clojure
(defmacro conference [c]
  `(if (= ~c :heart-of-clojure)
     "Clojure conference organised in 2024 by this absolutely lovely guy Arne
      who I met at Dutch Clojure Days back in 2017 or was it 2018 that was all
      about bringing the heart of Clojure by which he meant the Clojurians
      because that's what we call ourselves because 'Clojure users' would be too
      on the nose innit anyway bringing us together to be together and by us he
      really meant **all of us** and by together he meant more than physically
      colocated but also connected by our shared love of what people were doing
      with Clojure the programming language or just ideas they had sparked by the
      odd things Closure tends to do to the minds of Clojurians or maybe they're
      not even Clojurians or even tech people but by god they're awesome because
      Arne thinks so or someone Arne thinks is awesome thinks so anyway it was a
      conference back in 2019 that I missed and I'm so sad that I did"
     (->> (-> (name ~c) (str/split #"-"))
          (map str/capitalize)
          (str/join " "))))
```

was happening again and I would miss it again because it was on the 18th-19th of
September and that's when we're visiting the Collinses. 😢

"Глупаче", she said, "we're going to London on the 26th-29th of September," and
I was like

![Brittany Spears saying 'Oops I did it again'][oops]
[oops]: assets/draft-heart-of-clojure-2024-oops.jpg "You fucked up the dates, can we be friends?"

Yeah... so I'm somewhat infamous amongst my family, friends, coworkers, and
Arlanda Airport parking attendants for mixing up dates and times and lions and
tigers and bears oh my.

"Go to the conference if it means so much to you, Сладърче. We'll take care of
Rover while you're in Belgium."

She didn't even make it to the full stop in that sentence before I completed the
[Tito](https://ti.to/home) checkout process for the conference and had my
digital ticket in hand (by the way, Tito is a fantastic event platform made by
[some lovely people in Dublin](https://teamtito.com/about) who got really
serious about ideas [Kim Crayton](https://kimcrayton.com/) was developing that
would become her fantastic book [Profit Without
Oppression](https://kimcrayton.com/profit-without-oppression/), so please please
please consider them for your next event!), shortly followed by a Brussels
Airlines ticket dropping into my digital wallet after a short intermission
during which I hugged my wife and told her how much I appreciated her kindness
and understanding of how special Clojure is to me. So anyway, this blog post is
about Heart of Clojure 2024, in scenic and cool Leuven, Belgium, and how I
experienced it.

## Day -1: Tuesday, 17 Sept

As the plane touched down at Brussels airport and started taxiing to the gate, I
pulled my phone out, flipped airplane mode off, and opened Signal to write my
friend Ray a message. He had beat me to it, though. "We're parked up close by.
Let me know when you arrive and I'll come to the Drop Off parking area to
collect you," said his message from three minutes prior. Just landed! 🛬" I
replied, "I'll ping you when I get off the plane." For Ray is a jolly good
fellow, and he lives "a 30 minute bike ride from Leuven" and if you put those
two facts into a [miniKanren](http://minikanren.org/) implementation and then
ask if Ray offered to put me up at his gaff for the duration of the conference,
said universal solver would return with a query of its own: "does the Pope
almost fall in a river in the woods?" to which you of course would respond in
the affirmative.

So yeah, Ray graciously invited me to stay with him and his lovely family and
also this coworker of his from his previous job at Funding Circle, a delightful
Bulgarian fellow named Laddo. (That of course wasn't his name, but it took me
quite some time to learn that he was actually named Ivo because Ray seems to
refer to him exclusively as Laddo.)

So Ray and ~~Ivo~~ Laddo picked me up from the airport, drove me back to Ray's
place, fed me a delicious bowl of homemade vegan ramen paired with an alcohol
free Brewdog Punk IPA, tucked me into bed in a spare room, and told me a
delightful story of a young Rich Hickey encountering a hammock for the first
time. I still don't know how the story ends, because my eyes grew heavy with
sleep and before I knew it, visions of syntax sugar plums were dancing in my
head.

## Day 0: Wed, 18 Sept

In addition to [writing Clojure](https://github.com/raymcdermott) and [writing
about Clojure](https://www.juxt.pro/blog/nbb-lambda/) and [talking about
Clojure](https://www.patreon.com/defn) and [writing Clojure whilst talking about
Clojure](https://www.youtube.com/channel/UC1UxEQuBvfLJgWR5tk_XIXA), Ray rides
bicycles. Bicycles that he owns. Four bicycles that he owns (though presumably
he doesn't ride all four at the same time). Ray, being the generous yet
mean-spirited-in-a-friendly-fashion bloke that he is, ~~demanded~~ ~~requested~~
~~suggested~~ asked me and Laddo if we wanted to cycle into the conference.
"It's only a 30 minute ride," he said. Laddo consulted his phone. "13
kilometres," he said, "Yeah. Why not?" Their eyes turned to me, daring me to be
a stick in the mud, a spanner in the works, a defiler of joy; in short, a right
prick.

So that's how I found myself on a racing bike for the first time, on one of
those skinny rock-hard saddles for the first time, just bloody **hurtling** down
this narrow rutted dirt path in a forest, mere millimetres from a raging river
swollen with snowmelt from the mighty Belgian Alps. The pain in my tender nether
regions was only held in check by the sheer terror in my heart, for to plunge
off the edge of the 100 metre drop into the river would surely make Ray and
Laddo late for the conference and I couldn't have that on my conscience, now
could I?

No, I could not. You did realise that was a rhetorical question,
right? ... That last one wasn't rhetorical, though; I'm waiting for an answer.
Gwon, shout it out if you know it.

![The teacher from Ferris Bueller's Day Off saying 'Bueller... Bueller... Anyone?'][anyone]
[anyone]: assets/draft-heart-of-clojure-2024-anyone.jpg "Still waiting..."

"Yes," comes a brave voice from the crowd, wavering at first but then increasing
in volume and certainty, "Yes! That was a rhetorical question. The first one, I
mean. The second one clearly was not, given that I am currently answering it.
Now watch my eyes closely, you absolute buffoon: 🙄"

70 minutes later and 5 minutes late to Arne's opening words, the three Bikemen
of the Apocalypse rolled our cycles through the mighty gates of the [Het
Depot](https://www.hetdepot.be/) and promptly encountered Vijay, who made a
crack about us being on "Belgian time" (whatever **that** means, you Dutchbag!)
and then laughed and I blamed it on Laddo and was then politely told to bloody
well keep it down because the door was open and Arne was saying stuff that
people probably wanted to hear more than they wanted to hear my stupid bullshit.

We quickly stowed our bikes and changed into our haute couture, then slunk into
the auditorium just in time for the opening keynote.

### What it means to be open

![Lu Wilson demonstrating their Dreamberd programming language][keynote1]
[keynote1]: assets/draft-heart-of-clojure-2024-keynote1.jpg "The perfect programming language at work!" width=800px

*Photo by Ben Lovell, licenced under [CC BY-SA
4.0](https://creativecommons.org/licenses/by-sa/4.0/deed.en)*

[Lu Wilson kicked the conference
off](https://2024.heartofclojure.eu/talks/what-it-means-to-be-open/) with a
fantastic keynote where they talked about their journey from hacking away behind
closed doors to putting themself and their work out there on the internet, warts
and all, and the unexpectedly amazing things that happened because of that.

Lu is a very cool person and an **incredible** speaker! They had the entire
audience leaning forward, feasting on every word, and laughing at every slide
and wisecrack. The gist of their talk is that everything they do is open source,
and what they do is write terrible Javascript (their words, not mine) that does
really fun things (my assessment, not theirs) with
[sand](https://sandpond.cool/) and other [surrealist cellular
automata](https://www.todepond.com/explore/cellpond/), make ["slightly surreal"
videos](https://www.youtube.com/@TodePond) about the experience, invent the
[perfect programming language](https://github.com/todepond/dreamberd)
(implementation left as an exercise for the user), and most importantly, do it
all out in the open. Lu strives to share one thing a day, every day, which might
be as small as an interesting thought sent out on
[Mastodon](https://mas.to/@TodePond) or
[Bluesky](https://bsky.app/profile/todepond.com), as medium as a cool new
feature in one of their many projects, or as big as a new video on their YouTube
channel!

At first, Lu felt very shy about sharing their messy code with the world, but
their "share one thing every day" decision helped them overcome that, even when
sharing code that

> was one Javascript file, 4000 lines long, global variables all over the
> place... of course, the real crime is not using Clojure.

The crowd of Clojurians appreciated the pandering and responded with the guffaw
that Lu was looking for.

One really cool thing that Lu does when they encounter an "impossible" problem
is to send a cry for help into the vastness of the internet, which often

> awakens 50 nerds in the world who take it as their god-given mission to solve
> this problem.

One example of this was a particularly tricky issue their 2D game engine, to
which a nerd came back after presumably spending 48 hours immersing themselves
in academic journals from the last 200 years of mathematics, fending off sleep
with massive amounts of coffee and sheer willpower, until they could come back
to Lu and confidently explain

> "It's just a matter of turning `s` and `t` back into coordinates linked to the
> other quad"—I have no idea what any of that means.

Lu rounded off their talk with some musings on what it means to be open,
concluding that it probably looks different for each and every one of us, and in
fact their understanding of openness is still evolving every day. I can't
remember the exact call to action Lu issued to the rapt crowd, because I was too
rapt myself to remember to scribble it down in my trusty conference
notebook, but it must have been a damned inspiring one, because the crowd
responded with 100+ decibels of applause before slowly sinking back in their
seats, minds collectively blown.

Once we regained our locomotive powers, we filed out into the hallway to cavort
with our fellow Clojurians. I myself was delighted to be immediately accosted by
old friends from my [Pitch](https://pitch.com/about) days: Ben, Jakob, Karlis,
Kathryn, Oscar, Phil, Tibor, and almost certainly others whom my blown mind
isn't currently able to recall.

After grabbing coffee and gabbing, we headed back into the main hall to take
in the second keynote.

### From hype to responsibility

![Anna Colom standing under a slide reading: Algorithms are nothing more than opinions embedded in code][keynote2]
[keynote2]: assets/draft-heart-of-clojure-2024-keynote2.jpg "And those opinions reflect the biases and oppressions of the status quo" width=800px

*Photo by Josh Glover, licenced under [CCO 1.0 Universal](https://creativecommons.org/publicdomain/zero/1.0/) (AKA public domain)*

Anna Colom's keynote also had nothing to do with Clojure, but everything to do
with the question of [what works and matters for whom in data and
AI?](https://2024.heartofclojure.eu/talks/from-hype-to-responsibility-what-works-and-matters-for-whom-in-data-and-ai/)
Before I get into my reflections on her talk, I shall step forward and deliver a
soliloquy, if I may. (That was a rhetorical question, because of course I may;
I'm writing this and not you! Unless of course you [submit a pull
request](https://github.com/jmglov/jmglov.net/tree/heart-of-clojure-2024), in
which it's you writing and not me and maybe you've replaced my soliloquy with a
meditation on the average air speed velocity of a laden swallow or some such.)

Heart of Clojure, as the name suggests, is a conference ostensibly about
Clojure, so how is it that neither of the opening keynotes had a single mention
of Clojure (other than Lu's joke about their criminal choice to use Javascript
rather than Clojure)?

You see, Clojure is more than a programming language (though it **is** a joyful
and pragmatic programming language): it's a community. I have no idea if this
was Arne's intention, but for me, the community **is** the heart of Clojure. The
idea of a modern Lisp for the JVM is what brought me to the language ago, but
the kindness, patience, helpfulness, enthusiasm, and inclusiveness of my fellow
Clojurians is what has kept me here for 15 years, excited about what the next 15
years will bring. Speaking of inclusion, the idea that we have so much to learn
from other disciples is something that pervades the community, so we are
constantly making the effort to include those insights into our language, our
programs, our ways of thinking, and most of all, our conferences. The vast
majority of the Clojure conferences I've attended have had at least one talk
that had nothing to do with Clojure, from [Nada Amin's closing
keynote](https://2017.euroclojure.org/nada-amin/) at EuroClojure 2017 in Berlin
where she used Scala to demonstrate the concept of towers of interpreters to
[Chris Ford's exploration of traditional central African polyphony and
polyrhythm](https://web.archive.org/web/20231129180740/https://skillsmatter.com/conferences/8783-clojure-exchange-2017#program)
at Clojure eXchange 2017 to [Jordan Miller showing how mentorship is essential
to growth during all stages of an engineer's
career](https://clojuredays.org/dcd2022.html#Got%20a%20Guru?) at Dutch Clojure
Days 2022. Long may this continue!

Now back to Anna's talk, which was an absolute banger, and hands down my
favourite talk of the conference (along with my five other favourite talks of
the conference 😅).

Boiled down to its essence, the talk was asking the question, "what do we do
about AI?" Anna introduced herself as a social science researcher currently
working at [The Data Tank](https://www.datatank.org), a research institute which
is making the case for more responsible reuse of data. She's been working for
the last 15 years as a researcher at the intersection of democratic processes
and technology, and she started the talk with a look at the promises that have
been made about tech as the solution to solving everything: governance problems,
climate change adaptation, public health issues, etc, and pointed to the fact
that whilst tech has changed a lot in the past 15 years, those promises have not
(and remain unmet, if I'm to editorialise a bit). Tech is central to so many
things in our lives, but the decisions around tech are made by a handful of
companies such as Google, Microsoft, Apple, Amazon, Meta, Oracle, and nVidia,
largely in a regulatory vacuum.

Before walking through the major risks of IT, Anna started by introducing the
concept of technological determinism, the idea that tech is an objective and
infallible tool for solving all problems. As an example of the dangers of
believing that computers never make mistakes, (which should be a laughable idea
for anyone who has ever programmed a computer—just because the logic gates and
complex circuits that execute our programs obey certain laws of physics, those
programs are written by humans, who are certainly fallible) Anna talked about
the [British Post Office "Horizon"
scandal](https://en.wikipedia.org/wiki/British_Post_Office_scandal). If you
haven't heard of this, the TLDR is that bugs in the Horizon accounting software
system led to more than 900 employees of the British Post office being convicted
of theft, fraud and false accounting between 1999 and 2015, leading to ruined
lives and at least four suicides. All this despite Fujitsu, the company that
created the software, knowing about the bugs and not disclosing them. According
to Anna,

> It shows the consequences it can have when we place more trust in tech than in
> humans.

She gave a couple more examples: the [Dutch childcare benefits
scandal](https://en.wikipedia.org/wiki/Dutch_childcare_benefits_scandal) and the
[Robodebt Scheme](https://en.wikipedia.org/wiki/Robodebt_scheme) in Australia,
before moving on to seven risks of AI:

1. Rights and democratic integrity: AI-generated newsfeeds spreading
   misinformation and disinformation, leading to the polarisation of society.
2. The climate emergency: not only massive electricity across the entire
   lifecycle of an LLM system (Google's emissions climbed 50% over the last 5
   years, mostly due to AI energy demand), and the water needed to cool
   datacentres (in 2023, ChatGPT alone used 500 millilitres of water for every
   5-50 prompts).
3. Economic and social inequalities: automation leading to job losses places
   those with few resources to learn new skills or move where there may be more
   job opportunities, and the exploitation of "data labelers" in countries such
   as Kenya, under poor working conditions and pay.
4. Bias and discrimination: "Algorithms are nothing more than opinions embedded
   in code" (to quote [Cathy O'Neil](https://cathyoneil.org/)) which reflect the
   existing biases in society based on where we live, our ethnicity, skin
   colour, gender, sexuality, etc, and thus amplify existing oppression. To
   hammer this point home, she showed [Dr. Joy
   Buolamwini's](https://poetofcode.com/art/) amazing "[AI, Ain't I a
   Woman?](https://www.youtube.com/watch?v=QxuyfWoVV98)" video. Stop reading
   this blog and go watch that now!
5. Human identity, agency, autonomy: what happens when our interactions with
   healthcare providers, public services, and our friends and lovers are
   mediated by AI?
6. Geopolitics and colonialism: which ways of seeing the world are we
   encouraging and amplifying through generative AI technologies? Which data
   from where and in which languages is feeding AI?
7. Misuse of data: data is being extracted for private use with no consideration
   for existing IP rules (flawed as they might be, to editorialise again) or
   legitimate ownership and data sovereignty and kept in silos to solve specific
   problems rather than to solve societal problems like mobility in cities or
   optimising energy use.

She ended the talk by asking, "where do we want to go?" and suggested that we
need regulation; to reclaim our agency to envision and build the societies we
want to live in; embed public participation in the lifecycle of AI; create
ecosystems for responsible and sustainable reuse of data; and build a
transparent, collaborative, and anti-oppressive data science.

These weren't actually the last words of the talk, but this was the last line
that I wrote down, and I think it sums up what I took away from it:

> We're up against a big balloon of hype that I believe really needs to pop.

I just realised that I wrote a fairly detailed summary of the talk, which wasn't
my intention. I usually just write down a few notes or quotes in my trusty
Babashka notebook (the paper kind, not the silicon kind) and then dash a few
words off in my blog about what I remembered from the talk and how it made me
feel. In this one, almost every line felt like an applause line to me. My neck
was sore afterwards from the frequency and ferocity of my nods in agreement, and
my hand was asleep from sitting on it so I didn't stick my fist up in the air
constantly.

I loved this talk!

### Lunch

![Mediterranean food in small dishes][habibi]
[habibi]: assets/draft-heart-of-clojure-2024-habibi.jpg "What a spread!" width=800px

After Anna's talk, I headed over to [Hal 5](https://www.hal5.be/), which is an
old railroad workshop that has now been converted into a big space with
restaurants, bars, etc. This is where lunch was served and also where the
workshops on the official conference agenda were held. There was also a
community space which was open for conference attendees to hold their own
activities. Arne and crew created a webapp called
[Compass](https://compass.heartofclojure.eu/) specifically for the conference,
and if you signed in with Discord as an attendee, you could create an activity
using the app. More on which later. 😉

Lunch itself was spectacular! It was catered by [Falafel
Habibi](https://www.facebook.com/falafelhabibi2020), a restaurant in the Hal 5
food court that served, well, falafel. And of course all the vegan Mediterranean
and Palestinian specialities that you'd expect alongside falafel: stuffed grape
leaves, olives, pita, and lots and lots of hummus! 🤤🇵🇸

After lunch, we helped clear away the tables in the activity space to make room
for the 🪩 [Dance / Movement Break with
Wendy](https://compass.heartofclojure.eu/sessions/17592186059795) 🪩, in which
some fun Zumba-style dance videos were projected, and one of the conference
volunteers (Wendy, maybe?) led a small group of brave Clojurians in some light
dancing. I was [frantically blogging
away](https://jmglov.net/blog/2024-09-18-podcast-soundcljoud.html) under a
self-imposed deadline due to a rash decision I had made a couple of days prior,
so I sat there next to the wall next to the dancefloor like an uncool dude being
uncool and just enjoyed the music whilst tapping away on my laptop.

After the dance break, attendees pitched in again to set up chairs in front of
the screen, and a certain naturally blond, nbb-hating Scittle-loving Clojurian
got to his feet, plugged the HDMI cable into his laptop, took a deep breath, and
launched into...

### Scittling up a Podcatcher

![A hand-drawn illustration of a person presenting a slide with the Babashka logo][scittle]
[scittle]: assets/draft-heart-of-clojure-2024-scittle.jpg "Scittle can catch pods without breaking a sweat!" width=800px

*Drawing by Evgeniy Latukhin, licenced under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/), remixed by Josh Glover (full drawing below)*

Remember how I mentioned that attendees could use the Compass app to create
their own activities? Well, a few days before the conference, I decided that it
would be "fun" to livecode a podcast player using
[Scittle](https://github.com/babashka/scittle/) to ~~shamelessly promote~~ share
with the world a podcast about union organising in Swedish tech companies that
Ray and I are hosting. 🇸🇪 [orgtech.se](https://orgtech.se) FTW! 🇸🇪

The idea is that I would present a quick minute intro explaining what Scittle is
and why I'm totally in love with it, then take 30 minutes to code up a web-based
podcast player in front of the audience, which included Scittle creator
[borkdude](https://github.com/borkdude) himself. No pressure, Josh! 😅

I somehow accomplished it in "30" minutes *cough cough ahem*, in no small part
due to the group of Clojurians in the audience helping me spot typos and debug
small bugs (a portent of what was to come on Thursday, but I'll get to that) and
be generally enthusiastic and supportive and lovely.

Sometime later, I actually realised that the session in the workshop area right
behind the activity space was none other than Chris McCormick of [Melody
Generator](https://dopeloop.ai/melody-generator/) and [Beat
Generator](https://play.google.com/store/apps/details?id=cx.mccormick.canofbeats&pli=1)
fame (but sadly not recipefinder.com fame and the accordant big bucks) showing
us how to [Build full-stack ClojureScript apps with and without
Sitefox](https://compass.heartofclojure.eu/sessions/17592186045440), using cool
tools like [Squint](https://github.com/squint-cljs/squint) and disgusting, awful
ones like [nbb](https://github.com/babashka/nbb). 🤮

borkdude was seated toward the back of my audience, and since only a curtain
separated the activity space from the workshop space, he said that he could
multitask and listen to both the Scittle and nbb talks at the same time. 😂

### The Shoulders of Giants or Uncovering the Foundational Ideas of Lisp

After my session was over, I walked outside into a lovely sunny day (the weather
gods apparently love Clojure too!), down to the tunnel under the central
station, and through it to arrive back at the Het Depot just in time for Daniel
Szmulewicz to show us how [John McCarthy stood on the shoulders of giants to
uncover the foundational ideas of
Lisp](https://2024.heartofclojure.eu/talks/the-shoulders-of-giants-or-uncovering-the-foundational-ideas-of-lisp/).

![Daniel Szmulewicz stands under his title slide][shoulders]
[shoulders]: assets/draft-heart-of-clojure-2024-shoulders.jpg "Kurt Gödel scoffs at your perfect palace of pure mathematics" width=800px

*Photo by Josh Glover, licenced under [CCO 1.0 Universal](https://creativecommons.org/publicdomain/zero/1.0/) (AKA public domain)*

Stuff stuff things.

## Photos that will be mixed into the post

![A hand-drawn illustration of day one of the conference with scenes of: a sofa and coffee table on the main stage; the screen on the main stage showing 'No internet connection. Reload.'; the front entrance to the Het Depot; screen printed t-shirts hanging on a clothesline; a person presenting a low key talk in an activity session in the Het Depot][day1]
[day1]: assets/draft-heart-of-clojure-2024-day1.jpg "Day 1 of the conference (typical off by one error; this is clearly day 0)" width=800px

*Drawing by Evgeniy Latukhin, licenced under the [CC BY
4.0](https://creativecommons.org/licenses/by/4.0/) licence.*
