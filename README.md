# FlashDeck

A small Java command-line flashcard app with a local spaced-review schedule.

## Features

- Add and list question/answer cards.
- Study cards that are due.
- Rate each answer as again or good to schedule the next review.
- Stores cards in flashdeck.tsv on the current device.

## Requirements

JDK 17 or newer. No third-party packages.

## Build and run

~~~sh
javac FlashDeck.java
java FlashDeck add "What does HTTP stand for?" "Hypertext Transfer Protocol"
java FlashDeck list
java FlashDeck study
~~~

During study, press Enter to reveal the answer, then type again or good.
