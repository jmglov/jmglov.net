Title: Soundcljoud, or a young man's Soundcloud clonejure
Date: FIXME
Tags: clojure,babashka,scittle,clonejure,clojurescript
Description: In which I put Soundcloud out of business in 243 lines of Clojure
Discuss: FIXME
Image: assets/draft-soundcljoud-preview.jpg
Image-Alt: A stack of CDs. Photo by Brett Jordan on Unsplash.
Preview: true

![A stack of CDs. Photo by Brett Jordan on Unsplash.][preview]
[preview]: assets/draft-soundcljoud-preview.jpg "I should be allowed to glue my poster" width=800px

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
a real bummer. SOLUTION RIP CDS

## OMG finally stuff about Clojure

If you wisely clicked [the link at the
beginning](#OMG_finally_stuff_about_Clojure) to skip my [rambling
exposition](#Rambling_exposition), welcome to a discussion of how I solved a
serious problem caused by a certain [Country & Western super-duper
star](https://en.wikipedia.org/wiki/Garth_Brooks) (much like VA Beach legend
[Magoo](https://en.wikipedia.org/wiki/Timbaland_&_Magoo)—RIP—[on every CD, he
spits 48 bars](https://genius.com/Timbaland-and-magoo-cop-that-shit-lyrics))
wisely flicking the V at [Daniel Ek](https://en.wikipedia.org/wiki/Daniel_Ek)
and [Tim Cook](https://en.wikipedia.org/wiki/Tim_Cook) but [somehow being
A-OK](https://uproxx.com/indie/garth-brooks-spotify-amazon-streaming-apple-music/)
with [an even more repulsive
billionaire](https://en.wikipedia.org/wiki/Jeff_Bezos)'s streaming service,
welcome to some stuff about Clojure!
