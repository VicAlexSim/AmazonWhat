package com.example.amazonwhat

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button // If you have a start button, otherwise remove
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlin.random.Random
import android.util.Log
import android.view.View
//import androidx.compose.ui.semantics.text
//import androidx.glance.visibility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var rvOptions: RecyclerView
    private lateinit var adapter: OptionAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTimer: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvGoal: TextView

    private lateinit var ivTarget: ImageView
    private lateinit var tvTargetBrand: TextView
    private lateinit var tvTargetName: TextView
    private lateinit var ivTargetImage: ImageView
    private lateinit var tvTargetPrice: TextView

    private var score = 0
    private var timeLeft = 30_000L
    private lateinit var countDownTimer: CountDownTimer

    private lateinit var targetItem: Item
    private lateinit var goal: String

    // CoroutineScope for UI-related coroutines
    private val mainActivityJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + mainActivityJob)

    private var currentAccessToken: String? = null
    private var tokenExpiryTimeMillis: Long = 0L // Store when the token expires in milliseconds since epoch
    private val tokenJob = Job()
    private val tokenScope = CoroutineScope(Dispatchers.IO + tokenJob) // Scope for token operations

    // Example search terms - diversify these for better variety
    private val commonSearchTerms = listOf(
        "milk", "bread", "cheese", "apple", "banana", "orange juice", "soda", "water",
        "chips", "cookies", "yogurt", "eggs", "pasta", "rice", "cereal", "coffee", "tea",
        "frozen pizza", "ice cream", "laundry detergent", "shampoo", "toothpaste"
    )
    // Example location ID. CRITICAL: Find a valid one for your testing for prices to appear.
    // Pricing is location-specific. An invalid/irrelevant locationId will likely result in no prices.
    private val KROGER_LOCATION_ID = "01400943" // Example: Kroger store in Cincinnati, OH. REPLACE IF NEEDED.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind views
        rvOptions = findViewById(R.id.rvOptions)
        progressBar = findViewById(R.id.progressBar)
        tvTimer = findViewById(R.id.tvTimer)
        tvScore = findViewById(R.id.tvScore)
        tvGoal = findViewById(R.id.tvGoal)

        ivTarget = findViewById(R.id.ivTarget)
        tvTargetBrand = findViewById(R.id.tvTargetBrand)
        tvTargetName = findViewById(R.id.tvTargetName)
        //ivTargetImage = findViewById(R.id.ivTargetImage)
        tvTargetPrice = findViewById(R.id.tvTargetPrice)

        // Setup RecyclerView
        adapter = OptionAdapter(mutableListOf()) { selectedOption ->
            checkAnswer(selectedOption)
        }
        rvOptions.adapter = adapter
        rvOptions.layoutManager = LinearLayoutManager(this)

        // Start game
        startGame()
    }

    private fun startGame() {
        score = 0
        updateScoreDisplay()
        startTimer()
        loadNextRound()
    }

    private fun startTimer() {
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
        countDownTimer = object : CountDownTimer(timeLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = millisUntilFinished
                tvTimer.text = "${timeLeft / 1000}s"
                progressBar.progress = ((timeLeft / 300).toInt())
            }

            override fun onFinish() {
                tvTimer.text = "0s"
                endGame()
            }
        }
        countDownTimer.start()
    }

    private fun updateScoreDisplay() {
        tvScore.text = "Score: $score"
    }

    private fun loadNextRound() {
        goal = if (Random.nextBoolean()) "Higher Price" else "Lower Price"
        tvGoal.text = "Goal: $goal"

        uiScope.launch {
            val tokenAcquired = withContext(Dispatchers.IO) { ensureValidToken() }

            if (!tokenAcquired || currentAccessToken == null) {
                Log.e("LoadNextRound", "Failed to acquire access token. Loading mock data.")
                runOnUiThread { Toast.makeText(this@MainActivity, "API Error. Using fallback data.", Toast.LENGTH_LONG).show() }
                loadMockDataForRound("Token Error")
                progressBar.visibility = View.GONE
                rvOptions.visibility = View.VISIBLE
                return@launch
            }

            // --- Fetch Target Item ---
            var fetchedTargetItem: Item? = null
            var attempts = 0
            while (fetchedTargetItem == null && attempts < 5) { // Try a few times to get a target
                fetchedTargetItem = fetchRandomKrogerProduct(currentAccessToken!!, KROGER_LOCATION_ID)
                attempts++
            }

            if (fetchedTargetItem == null) {
                Log.e("LoadNextRound", "Could not fetch a suitable target item from Kroger API after $attempts attempts. Loading mock data.")
                loadMockDataForRound("No Target API Item")
                progressBar.visibility = View.GONE
                rvOptions.visibility = View.VISIBLE
                return@launch
            }
            updateTargetUI(fetchedTargetItem) // This sets the global targetItem

            // --- Fetch Option Items ---
            val options = mutableListOf<Item>()
            var fetchedCorrectOption: Item? = null
            var correctOptionAttempts = 0

            // 1. Try to fetch one CORRECT option
            while (fetchedCorrectOption == null && correctOptionAttempts < 10) {
                val potentialOption = fetchRandomKrogerProduct(currentAccessToken!!, KROGER_LOCATION_ID, excludeProductId = targetItem.krogerProductId)
                if (potentialOption != null && isOptionCorrect(potentialOption, targetItem, goal)) {
                    fetchedCorrectOption = potentialOption
                }
                correctOptionAttempts++
            }

            if (fetchedCorrectOption != null) {
                options.add(fetchedCorrectOption)
                Log.i("LoadNextRound", "Successfully fetched a CORRECT API option: ${fetchedCorrectOption.name} Price: ${fetchedCorrectOption.price}")
            } else {
                Log.w("LoadNextRound", "Could not fetch a CORRECT API option. Will use mock for correct.")
            }

            // 2. Try to fetch two INCORRECT options
            var incorrectOptionsFetched = 0
            var incorrectOptionAttempts = 0
            while (incorrectOptionsFetched < 2 && incorrectOptionAttempts < 20) { // More attempts for incorrect
                val potentialOption = fetchRandomKrogerProduct(currentAccessToken!!, KROGER_LOCATION_ID, excludeProductId = targetItem.krogerProductId)
                if (potentialOption != null &&
                    !isOptionCorrect(potentialOption, targetItem, goal) &&
                    options.none { it.krogerProductId == potentialOption.krogerProductId } && // Avoid duplicates
                    potentialOption.krogerProductId != fetchedCorrectOption?.krogerProductId) { // Avoid duplicating the correct one if it was fetched
                    options.add(potentialOption)
                    incorrectOptionsFetched++
                    Log.i("LoadNextRound", "Fetched an INCORRECT API option: ${potentialOption.name} Price: ${potentialOption.price}")
                }
                incorrectOptionAttempts++
            }
            Log.d("LoadNextRound", "API options fetched: ${options.size}. Correct: ${if(fetchedCorrectOption != null) 1 else 0}, Incorrect: $incorrectOptionsFetched")


            // 3. Fill with mock data if necessary to reach 3 options
            if (options.isEmpty() || fetchedCorrectOption == null) { // If no correct option from API, ensure one from mock
                Log.w("LoadNextRound", "Ensuring at least one correct option using mock data.")
                options.removeAll { isOptionCorrect(it, targetItem, goal) } // Remove any accidental correct from previous incorrect fetches
                val mockCorrect = getMockItem(targetItem, goal, true)
                if (options.none { it.name == mockCorrect.name }) options.add(0, mockCorrect) // Add at start if unique
            }

            var currentOptionIndex = 0
            while (options.size < 3 && currentOptionIndex < 10) { // Limit total mock additions
                val isCorrectNeeded = options.none { isOptionCorrect(it, targetItem, goal) }
                val mockItem = getMockItem(targetItem, goal, if (isCorrectNeeded) true else false)

                // Avoid adding exact duplicate mock items by name (simple check)
                if (options.none { it.name == mockItem.name && it.brand == mockItem.brand }) {
                    options.add(mockItem)
                }
                currentOptionIndex++
            }


            // Ensure exactly 3 options, trim if more, log if less (should be rare with fallbacks)
            val finalOptions = options.distinctBy { it.krogerProductId ?: it.name }.shuffled().take(3).toMutableList()

            // If somehow still less than 3, fill with generic random mocks (last resort)
            while(finalOptions.size < 3) {
                val randomMock = getRandomMockItem()
                if(finalOptions.none { it.name == randomMock.name }) {
                    finalOptions.add(randomMock)
                }
            }


            Log.i("LoadNextRound", "Final options count: ${finalOptions.size}. Displaying to user.")
            adapter.updateOptions(finalOptions)
            progressBar.visibility = View.GONE
            rvOptions.visibility = View.VISIBLE
        }
    }

    /**
     * Fetches a random product from Kroger API with valid price and image.
     * @param token The access token.
     * @param locationId The location ID for pricing.
     * @param currentSearchTerm Optional specific search term.
     * @param excludeProductId Optional product ID to exclude from results.
     * @param maxAttempts Max attempts to find a suitable product.
     * @return An Item object or null if no suitable product found.
     */
    private suspend fun fetchRandomKrogerProduct(
        token: String,
        locationId: String,
        currentSearchTerm: String? = null,
        excludeProductId: String? = null,
        maxAttempts: Int = 3 // Internal attempts for different terms if initial fails
    ): Item? {
        var attempts = 0
        var searchTermToUse = currentSearchTerm ?: commonSearchTerms.random()

        while (attempts < maxAttempts) {
            Log.d("KrogerAPI", "Attempt ${attempts + 1}/$maxAttempts: Searching for '$searchTermToUse' at location '$locationId'")
            try {
                val response = KrogerApiClient.krogerApiService.searchProducts(
                    authToken = "Bearer $token",
                    term = searchTermToUse,
                    locationId = locationId,
                    limit = 20 // Fetch a decent number to pick from
                )

                if (response.isSuccessful && response.body() != null) {
                    val products = response.body()?.data
                    val validProducts = products?.filterNotNull()?.filter { product ->
                        product.productId != excludeProductId && // Exclude specific product if needed
                                product.items?.any { item ->
                                    item.price?.regular != null && item.price.regular > 0.0 && item.price.regular < 200.0 // Price between $0 and $200
                                } == true &&
                                product.images?.any {
                                    it.perspective == "front" && it.sizes?.any { size -> size.url.isNotBlank() } == true
                                } == true
                    }?.shuffled() // Shuffle to get different items on retry

                    if (!validProducts.isNullOrEmpty()) {
                        val chosenProduct = validProducts.first() // Take the first from shuffled valid list
                        val firstItemEntry = chosenProduct.items!!.first { it.price?.regular != null && it.price.regular > 0.0 }
                        val price = firstItemEntry.price!!.regular!!
                        val frontImage = chosenProduct.images!!
                            .find { it.perspective == "front" }
                            ?.sizes?.find { it.size == "large" || it.size == "xlarge" || it.size == "medium" }?.url // Prefer larger images
                            ?: chosenProduct.images.firstOrNull { it.perspective == "front"}?.sizes?.firstOrNull { it.url.isNotBlank() }?.url
                            ?: chosenProduct.images.firstOrNull()?.sizes?.firstOrNull { it.url.isNotBlank() }?.url // Fallback to any image
                            ?: "https://via.placeholder.com/150/CCCCCC/FFFFFF?Text=No+Img" // Ultimate fallback

                        Log.i("KrogerAPI", "Fetched product: ${chosenProduct.description}, Price: $price, ID: ${chosenProduct.productId}")
                        return Item(
                            imageUrl = frontImage,
                            brand = chosenProduct.brand ?: "Unknown Brand",
                            name = chosenProduct.description ?: "Unknown Product",
                            price = price,
                            krogerProductId = chosenProduct.productId
                        )
                    } else {
                        Log.w("KrogerAPI", "No valid products found for '$searchTermToUse' with specified criteria.")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("KrogerAPI", "Product search API call failed for '$searchTermToUse'. Code: ${response.code()}, Error: $errorBody")
                    // If 401 or 403, token might be an issue, though ensureValidToken should catch this.
                    // If 400 with location error, KROGER_LOCATION_ID is likely problematic.
                    if (response.code() == 400 && errorBody?.contains("locationId",ignoreCase = true)==true) {
                        runOnUiThread{ Toast.makeText(this,"Invalid Location ID for Kroger API. Check KROGER_LOCATION_ID.", Toast.LENGTH_LONG).show()}
                    }

                }
            } catch (e: Exception) {
                Log.e("KrogerAPI", "Exception during product search for '$searchTermToUse': ${e.message}", e)
            }
            attempts++
            if (currentSearchTerm == null) searchTermToUse = commonSearchTerms.random() // Try a different term if auto-picking
        }
        Log.w("KrogerAPI", "Failed to fetch a suitable product after $maxAttempts attempts for search term pattern.")
        return null
    }

    /**
     * Checks if a given option item is "correct" based on the target item and current goal.
     */
    private fun isOptionCorrect(option: Item, currentTarget: Item, currentGoal: String): Boolean {
        if (option.price == currentTarget.price) return false // Cannot be correct if prices are equal
        return when (currentGoal) {
            "Higher Price" -> option.price > currentTarget.price
            "Lower Price" -> option.price < currentTarget.price
            else -> false
        }
    }

    /**
     * Loads a round with mock data if API calls fail.
     * @param reason A string explaining why mock data is being loaded.
     */
    private fun loadMockDataForRound(reason: String) {
        Log.w("MockData", "Loading mock data for round because: $reason")
        runOnUiThread { Toast.makeText(this, "Using Fallback Data: $reason", Toast.LENGTH_SHORT).show() }

        val mockTarget = getRandomMockItem()
        updateTargetUI(mockTarget) // This sets the global targetItem to the mock one

        val mockOptions = mutableListOf<Item>()
        mockOptions.add(getMockItem(targetItem, goal, true))  // One correct mock option
        mockOptions.add(getMockItem(targetItem, goal, false)) // Two incorrect mock options
        mockOptions.add(getMockItem(targetItem, goal, false))
        mockOptions.shuffle()

        adapter.updateOptions(mockOptions.take(3)) // Ensure only 3 options
        progressBar.visibility = View.GONE
        rvOptions.visibility = View.VISIBLE
    }

    /**
     * Generates a single mock item, trying to make it fit the "correct" or "incorrect" criteria.
     */
    private fun getMockItem(currentTarget: Item, currentGoal: String, shouldBeCorrect: Boolean): Item {
        val basePrice = currentTarget.price
        val priceVariation = Random.nextDouble(1.0, 5.0)
        var newPrice: Double

        if (shouldBeCorrect) {
            newPrice = if (currentGoal == "Higher Price") {
                basePrice + priceVariation
            } else { // Lower Price
                (basePrice - priceVariation).coerceAtLeast(0.50) // Ensure price is not negative or too low
            }
        } else { // Should be incorrect
            newPrice = if (currentGoal == "Higher Price") {
                (basePrice - priceVariation).coerceAtLeast(0.50)
            } else { // Lower Price
                basePrice + priceVariation
            }
        }
        // Ensure newPrice is not equal to basePrice if it's meant to be different
        if (newPrice == basePrice) newPrice += 0.5


        val brands = listOf("MockMart", "TestBrand", "SampleGoods", "Placeholder Inc.")
        val names = listOf("Mock Product Alpha", "Test Item Beta", "Sample Unit Gamma", "Placeholder Object Delta")
        return Item(
            imageUrl = "https://via.placeholder.com/150/${if (shouldBeCorrect) "28a745" else "dc3545"}/FFFFFF?Text=Mock",
            brand = brands.random(),
            name = names.random(),
            price = String.format("%.2f", newPrice).toDouble(),
            krogerProductId = "mock_${Random.nextInt(100000)}"
        )
    }

    /**
     * Generates a completely random mock item.
     */
    private fun getRandomMockItem(): Item {
        val brands = listOf("Great Value", "Market Pantry", "Signature Select", "Simple Truth", "Everyday Faves", "BestChoice")
        val names = listOf("Milk", "Bread", "Eggs", "Cheese", "Apples", "Bananas", "Juice", "Soda", "Chips", "Cookies")
        return Item(
            imageUrl = "https://via.placeholder.com/150/007bff/FFFFFF?Text=RandMock", // Generic placeholder
            brand = brands.random(),
            name = names.random(),
            price = Random.nextDouble(0.50, 20.0), // Wider price range for random mocks
            krogerProductId = "mock_random_${Random.nextInt(10000)}"
        )
    }


    override fun onDestroy() {
        super.onDestroy()
        mainActivityJob.cancel() // Cancel all coroutines started by uiScope
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
    }

    private fun updateTargetUI(item: Item) {
        targetItem = item // Store the fetched target item
        tvTargetBrand.text = item.brand
        tvTargetName.text = item.name
        tvTargetPrice.text = "$${String.format("%.2f", item.price)}"
        Glide.with(this).load(item.imageUrl).into(ivTarget)
        // add this onto the end of load() if u want a place holder
        // or error image if an image fetched doesnt load
        // .placeholder(R.drawable.ic_placeholder_image) // Make sure you have this
        // .error(R.drawable.ic_error_image)
    }

    private fun checkAnswer(selected: Item) {
        countDownTimer.cancel()

        if (selected == null) { // Time ran out
            Toast.makeText(this, "Time's up!", Toast.LENGTH_SHORT).show()
        } else {
            val isCorrect = when (goal) {
                "Higher Price" -> selected.price > targetItem.price
                "Lower Price" -> selected.price < targetItem.price
                else -> false // Should not happen
            }

            if (isCorrect) {
                score++
                Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
            } else {
                score--
                Toast.makeText(this, "Wrong! Option was $${String.format("%.2f", selected.price)}", Toast.LENGTH_LONG).show()
            }
        }
        updateScoreDisplay()

        // Add logic for game progression (e.g., number of rounds)
        if (score < 10) { // Example: game ends after 10 points or some number of rounds
            loadNextRound()
            startTimer()
        } else {
            endGame()
        }
    }

    private fun endGame() {
        Toast.makeText(this, "Time's up! Final Score: $score", Toast.LENGTH_LONG).show()
        // TODO: Show final score screen or restart
        val intent = Intent(this, MenuActivity::class.java)
        intent.putExtra("finalScore", score)
        intent.putExtra("isStart", false) // false means button should show "Restart"
        startActivity(intent)
        // Close the game screen
        finish()
    }

    // Mock functions
    private fun getRandomItem(): Item {
        return Item(
            "https://amandascookin.com/wp-content/uploads/2020/06/Fruit-Salad-SQ.jpg",
            "Brand",
            "Product ${Random.nextInt(100)}",
            Random.nextDouble(5.0, 100.0)
        )
    }

    private fun getRandomItems(count: Int): List<Item> {
        return List(count) { getRandomItem() }
    }

    private fun getValidOption(target: Item, goal: String): Item {
        return if (goal == "Higher Price") {
            getRandomItem().copy(price = target.price + Random.nextDouble(1.0, 50.0))
        } else {
            getRandomItem().copy(price = target.price - Random.nextDouble(1.0, target.price - 1.0))
        }
    }
    private fun getInvalidOption(target: Item, goal: String): Item {
        return if (goal == "Lower Price") {
            getRandomItem().copy(price = target.price + Random.nextDouble(1.0, 50.0))
        } else {
            getRandomItem().copy(price = target.price - Random.nextDouble(1.0, target.price - 1.0))
        }

    }


    /**
     * Fetches a Kroger access token if needed (i.e., if no valid token exists).
     * This function should be called from a coroutine.
     * @return True if a valid token is available (either pre-existing or newly fetched), false otherwise.
     */
    private suspend fun ensureValidToken(): Boolean {
        // Check if current token is valid and not about to expire (e.g., within next 60 seconds)
        if (currentAccessToken != null && System.currentTimeMillis() < (tokenExpiryTimeMillis - 60_000)) {
            Log.d("KrogerAuth", "Using existing valid token.")
            return true
        }

        Log.d("KrogerAuth", "Fetching new Kroger access token...")
        // Check if client ID/secret are actually loaded from BuildConfig
        if (BuildConfig.KROGER_CLIENT_ID.isEmpty() || BuildConfig.KROGER_CLIENT_SECRET.isEmpty()) {
            Log.e("KrogerAuth", "Kroger Client ID or Secret is empty in BuildConfig. Check local.properties and gradle sync.")
            runOnUiThread { Toast.makeText(this, "API Key Error. Check config.", Toast.LENGTH_LONG).show() }
            return false
        }

        return try {
            val authHeader = KrogerAuthClient.getBasicAuthHeaderValue()
            val response = KrogerAuthClient.authService.getAccessToken(authorization = authHeader)

            if (response.isSuccessful) {
                val tokenResponse = response.body()
                if (tokenResponse != null) {
                    currentAccessToken = tokenResponse.accessToken
                    // Calculate expiry time: current time + (expires_in seconds * 1000)
                    tokenExpiryTimeMillis = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000L)
                    Log.i("KrogerAuth", "Successfully fetched new token. Expires in: ${tokenResponse.expiresIn}s")
                    true
                } else {
                    Log.e("KrogerAuth", "Token response body is null.")
                    currentAccessToken = null
                    false
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("KrogerAuth", "Failed to get access token. Code: ${response.code()}, Error: $errorBody")
                currentAccessToken = null
                false
            }
        } catch (e: Exception) {
            Log.e("KrogerAuth", "Exception while fetching access token", e)
            currentAccessToken = null
            false
        }
    }

// Remember to cancel the tokenJob when your Activity/ViewModel is destroyed
// override fun onDestroy() {
//     super.onDestroy()
//     tokenJob.cancel()
// }

}