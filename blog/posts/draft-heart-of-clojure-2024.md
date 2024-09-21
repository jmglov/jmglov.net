Title: I Heart Heart of Clojure
Date: FIXME
Tags: clojure,conference debrief
Description: In which I am reminded that what I love about Clojure isn't parens, it's people
Discuss: FIXME
Image: assets/draft-heart-of-clojure-2024-preview.png
Image-Alt: FIXME
Preview: true


![FIXME ALT TEXT GOES HERE][preview]
[preview]: assets/draft-heart-of-clojure-2024-preview.png "FIXME HOVER TEXT GOES HERE" width=800px

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
crack about us being on "Belgian time" (whatever **that** mean, you Dutchbag!)
and then laughed and I blamed it on Laddo and was then politely told to bloody
well keep it down because the door was open and Arne was saying stuff that
people probably wanted to hear more than they wanted to hear my stupid bullshit.
