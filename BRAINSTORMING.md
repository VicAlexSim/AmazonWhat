# Group 45 - AND101 Capstone Project

# Final Idea - Kroger What!?
The game start with a random object in a Kroger store. The system then randomly generate a goal ["Higher Price", "Lower Price", "More popular", etc.] that the user have to guess. The system will give the user 3 options to pick from, in which there can be multiple correct (but never none correct). The user gain points. The game end after 30 seconds.
  
  - **The price is right, but Kroger** and better (with more option!)
  - https://api.kroger.com/v1/products?filter.term=shampoo&filter.locationId=01400943&filter.limit=20
    - Better b/c comparing more metrics
      - E.g. price, reviews, 

## Evaluation
- **Category:** Entertainment
- **Mobile:** While our app may be played on the web, many users would appreciate the simplicity a mobile interface provides. Its elegant button usage allows the user to easily tap away at any choice they so desire. Additionally, users have the added pleasure of bragging to their friends when they hit a new top score.
- **Story:** Historically, the price is right has done very well. So tacking on a NEW Kroger edition™️ to it will make it all the rage!
- **Market:** This app serves double purpose for Kroger fans; they get to learn the latest grocery prices AND have some nice family fun.
- **Habit:** Within just a few games, a user can quickly find themselves addicted to guessing Kroger prices. It's like gambling, but without having to spend real money.
- **Scope:** The main difficulty with creating this app will be in figuring out how to work with the API to get a random selection of products to compare their prices to.

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
- Dictionary word definition game app v1: The game start with a random word in a random Germanic language (so a, b, c, etc.). The game provide 3 word in another language to the user where one is the correct translation. Regardless if the user pick the correct option, the game pick the next word using the last character as starting character in a random language.
- Dictionary word definition game app v2: The game start with a random word in a random Germanic language (so a, b, c, etc.). The game provide 3 word in another language to the user, which the user need to translate. One of the translated word will have the first syllable as the last syllable of the display word, of which is the correct word.
  - **Category:** Education/Entertainment
  - **Mobile:** While our app may be played on the web, many users would appreciate the simplicity a mobile interface provides. Its elegant button usage allows the user to easily tap away at any choice they so desire. Additionally, users have the added pleasure of bragging to their friends when they hit a new top score.
  - **Story:** This app can be thought of as a strange mix between Duolingo, Wordle, and Shiritori. The app is designed to make its users enjoy learning different languages by gamifying the process.
  - **Market:** This app is targeted towards people who enjoy word-play, as well as multi-linguilists. Even those who are monolingual could appreciate learning how different words translate into different languages.
  - **Habit:** Within just a few games, a user can quickly find themselves addicted. It's like gambling, but without having to spend real money!
  - **Scope:** The main difficulty with creating this app will be in figuring out how to work with the Dictionary API (or any other equivalent) such that the app can chain into the next relevant words.
    - Example: "Human" - Provided: 人間 -> ["Lovely", "Earth", "Human"] -> "Human" -> Shiratori into "Never" -> Translated into "niemals" in German...
      - Note: Could be same language throughout
    - Shiritori-like game (chain words using the previous word's last syllabal)
    - The next word could also be the first link word in the Wikidefition article of the word (sort of like Wikiguesser)
    

- Productivity
  - File management system (feels saturated, as GDrive, Office, WinRAR all exist)
  - AI app (https://aimlapi.com/)
    - A hot topic, but is paywalled. Building off an open-source LLM would be very difficult as well, w/o top-end GPUs.

- Riichi Mahjong App
  - **Category:** Entertainment
  - **Mobile:** While our app may be played on the web, many users would appreciate the simplicity a mobile interface provides. Its elegant button usage allows the user to easily tap away at any choice they so desire; almost as if they're playing mahjong!
  - **Story:** This app is designed to be an efficiency trainer, of which there is only one that is reknowned.
  - **Market:** This app is targeted towards people who enjoy Riichi Mahjong, which is a growing niche, thanks to apps like Mahjong Soul and Riichi City.
  - **Habit:** Within just a few games, a user can quickly find themselves addicted. It's like gambling, but without having to spend real money!
  - **Scope:** This will be a very math-heavy project, since the app will always need to know the best possible tile a user can discard. There're 136 tiles in the game, of which 14 are in the player's hand. In addition to having to be mindful of 3 other player's discards, the app will need to take into account tiles that are still in the wall, if any tiles are out of play entirely (e.g. in the dead wall or all 4 copies visible), as well as if the player can make any valid yaku with their hand. Because we only have a week, this project is highly unfeasible. 
    - Core features:
      - Tile efficiency/discard analysis
      - Simple UI of player's tiles
    - Optional features:
      - Shows yaku that'd be easiest to go for (optional)
      - UI of entire state of the game