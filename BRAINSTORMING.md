# Group 45 - AND101 Capstone Project

# Final Idea - Amazon What!?
The game start with a random object in an Amazon store. The system then randomly generate a goal ["Higher Price", "Lower Price", "More popular", etc.] that the user have to guess. The system will give the user 3 options to pick from, in which there can be multiple correct (but never none correct). The user gain points. The game end after 30 seconds.
  
  - **The price is right, but Amazon** and better (with more option!)
  - https://developer-docs.amazon.com/sp-api/reference/getpricing
  - https://developer-docs.amazon.com/sp-api/reference/getitemreviewtrends
    - Better b/c comparing more metrics
      - E.g. price, reviews, 
    - (Could be amazon package stealer themed lmao)

## Required Component - MVP
A scoring system. Start with one global timer.
1. The system randomly pick a goal: ["Higher Price", "Lower Price"]
2. The system first pick a random item as the target.
3. The system then pick additional 2 random item.
4. The system then pick additional 1 item that fulfilled the goal
5. The user then pick an item:
    a. If the goal is met, increase the point, else nothing
6. When the timer run out, the game end. Display the user score.

## Design:
1. Text: score
2. Text: timer
3. Image + Text: target item + its price
4. Image + Text + Button: option item
5. Text: goal

## Wireframe:
https://www.figma.com/design/3mMVU2BOiYj5gJ1xwtOmhT/Untitled?node-id=0-1&t=NbUTIEgn8o9KS59R-1

## Stretch Goal
- Instead of a global timer, have timer between guess and implement a health-based system instead
- Add more goal to that the system use

## Appendix
API List: https://github.com/public-apis/public-apis
Github: https://github.com/VicAlexSim/AmazonWhat

# Other Ideas
- Education
  - Dictionary word definition game app v1: The game start with a random word in a random Germanic language (so a, b, c, etc.). The game provide 3 word in another language to the user where one is the correct translation. Regardless if the user pick the correct option, the game pick the next word using the last character as starting character in a random language.

"Human" - Provided: 人間 -> ["Lovely", "Earth", "Human"] -> "Human" -> Shiratori into "Never" -> Translated into "niemals" in German...
Note: Could be same language throughout

- Dictionary word definition game app v2: The game start with a random word in a random Germanic language (so a, b, c, etc.). The game provide 3 word in another language to the user, which the user need to translate. One of the translated word will have the first syllable as the last syllable of the display word. That is the correct word
  - Shiritori-like game (chain words using the previous word's last syllabal)
    - Dictionary API
  - Another idea: The next word is the first link word in the Wikidefition article of the word
- Lifestyle
  - Furniture Curation or guesser
- Productivity
  - File management system ()
  - AI app (https://aimlapi.com/)
  - File Sharing app?
- Travel
  - Hotel or Airline Comparing app
- Health & Fitness
  - Food Nutrition Dictionary
- Social
  - Urban dictionary 2.0 (official app does not exist atm lmao)
- Entertainment
  - Riichi Mahjong App
    - Core features:
      - Tile efficiency/discard analysis
      - Simple UI of player's tiles
    - Optional features:
      - Shows yaku that'd be easiest to go for (optional)
      - UI of entire state of the game