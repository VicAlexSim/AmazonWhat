// MainActivity.kt
package com.example.amazonwhat

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
// Your Item and Adapter imports
// e.g., import com.example.amazonwhat.model.Item
// e.g., import com.example.amazonwhat.adapter.OptionAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var rvOptions: RecyclerView
    private lateinit var adapter: OptionAdapter // Or ItemAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTimer: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvGoalDisplay: TextView // Assuming you have a TextView to display the goal

    private lateinit var ivTarget: ImageView
    private lateinit var tvTargetBrand: TextView
    private lateinit var tvTargetName: TextView
    private lateinit var tvTargetPrice: TextView

    private var score = 0
    private val roundDurationMillis = 30_000L
    private var timeLeftInMillis = roundDurationMillis
    private lateinit var gameTimer: CountDownTimer

    private lateinit var targetItem: Item
    private lateinit var currentGoal: String

    private val mainActivityJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + mainActivityJob)

    private lateinit var sharedPreferences: SharedPreferences
    private val highScoreKey = "amazonWhatHighScore"

    private var gameHasEnded = false
    private val MIN_API_ITEMS_FOR_QUESTION = 3 // Increased slightly to prefer API data more

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)

        rvOptions = findViewById(R.id.rvOptions)
        progressBar = findViewById(R.id.progressBar)
        tvTimer = findViewById(R.id.tvTimer)
        tvScore = findViewById(R.id.tvScore)
        tvGoalDisplay = findViewById(R.id.tvGoal) // Make sure you have this ID in your XML

        ivTarget = findViewById(R.id.ivTarget)
        tvTargetBrand = findViewById(R.id.tvTargetBrand)
        tvTargetName = findViewById(R.id.tvTargetName)
        tvTargetPrice = findViewById(R.id.tvTargetPrice)

        adapter = OptionAdapter(mutableListOf()) { selectedOption ->
            if (!gameHasEnded) {
                checkAnswer(selectedOption)
            }
        }
        rvOptions.adapter = adapter
        rvOptions.layoutManager = LinearLayoutManager(this)

        startGame()
    }

    private fun startGame() {
        gameHasEnded = false
        score = 0
        timeLeftInMillis = roundDurationMillis
        updateScoreDisplay()
        startOverallGameTimer()
        uiScope.launch {
            loadNextQuestion()
        }
    }

    private fun startOverallGameTimer() {
        if (::gameTimer.isInitialized) {
            gameTimer.cancel()
        }
        progressBar.max = (roundDurationMillis / 1000).toInt()
        progressBar.progress = (roundDurationMillis / 1000).toInt()

        gameTimer = object : CountDownTimer(roundDurationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                tvTimer.text = "${millisUntilFinished / 1000}s"
                progressBar.progress = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                timeLeftInMillis = 0
                tvTimer.text = "0s"
                progressBar.progress = 0
                if (!gameHasEnded) {
                    endGame()
                }
            }
        }.start()
        progressBar.visibility = View.VISIBLE
    }

    private fun updateScoreDisplay() {
        tvScore.text = "Score: $score"
    }

    private suspend fun loadNextQuestion() {
        if (gameHasEnded) return

        currentGoal = if (Random.nextBoolean()) "Higher Price" else "Lower Price"
        Log.i("LoadQuestion", "Current Goal: $currentGoal") // Log the current goal
        withContext(Dispatchers.Main) {
            tvGoalDisplay.text = "Goal: $currentGoal" // Update UI with current goal
        }
        var foundSuitableItemsFromCache = false

        val cachedProducts = ProductCacheManager.getRandomProductsFromCache(12) // Fetch more items
        val itemsFromApi = cachedProducts.mapNotNull { convertCachedToItem(it) }
            .filter { it.krogerProductId != null && it.price > 0 }
            .distinctBy { it.krogerProductId } // Ensure unique items by ID

        Log.d("LoadQuestion", "Fetched ${itemsFromApi.size} unique valid items from API/cache.")

        if (itemsFromApi.size >= MIN_API_ITEMS_FOR_QUESTION) {
            val shuffledApiItems = itemsFromApi.shuffled()

            for (potentialTarget in shuffledApiItems) {
                if (potentialTarget.krogerProductId == null) continue

                val potentialOptions = shuffledApiItems.filter {
                    it.krogerProductId != null && it.krogerProductId != potentialTarget.krogerProductId && it.price != potentialTarget.price // Options must have different prices
                }

                if (potentialOptions.size < 2) continue // Need at least 2 other items to form 1 correct, 1 incorrect

                val correctOption = potentialOptions.find {
                    isOptionCorrect(it, potentialTarget, currentGoal)
                }

                if (correctOption != null && correctOption.krogerProductId != null) {
                    targetItem = potentialTarget
                    val finalOptions = mutableListOf(correctOption)

                    val incorrectApiOptions = potentialOptions.filter {
                        it.krogerProductId != null &&
                                it.krogerProductId != correctOption.krogerProductId &&
                                !isOptionCorrect(it, targetItem, currentGoal)
                        // Price difference already handled by initial potentialOptions filter
                    }.shuffled()

                    finalOptions.addAll(incorrectApiOptions.take(2))

                    if (finalOptions.size == 3 && finalOptions.all { it.krogerProductId != null } && finalOptions.distinctBy { it.krogerProductId }.size == 3) {
                        withContext(Dispatchers.Main) {
                            adapter.updateOptions(finalOptions.shuffled())
                            updateTargetUI(targetItem)
                        }
                        foundSuitableItemsFromCache = true
                        Log.d("LoadQuestion", "Successfully formed question from API data. Target: ${targetItem.name}, Price: ${targetItem.price}, Goal: $currentGoal. Options: ${finalOptions.joinToString { it.name + "($" + it.price + ")" }}")
                        break
                    } else {
                        Log.d("LoadQuestion", "Could not form 3 distinct options from API data for target: ${potentialTarget.name}. Have ${finalOptions.size} options.")
                    }
                }
            }
        }

        if (!foundSuitableItemsFromCache) {
            Log.w("LoadQuestion", "Could not form valid question purely from API data (need ${MIN_API_ITEMS_FOR_QUESTION} items with distinct prices, got ${itemsFromApi.size}, or couldn't find distinct options). Falling back to mock data for goal: $currentGoal")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Using fallback data for this question.", Toast.LENGTH_SHORT).show()
            }
            loadMockQuestionSet("API data insufficient or no valid question pair found")
        }
    }


    private fun convertCachedToItem(cachedProduct: CachedProduct?): Item? {
        if (cachedProduct == null || cachedProduct.productId.isBlank()) return null
        if (cachedProduct.price <= 0) {
            Log.w("ConvertItem", "CachedProduct ${cachedProduct.productId} has invalid price: ${cachedProduct.price} or missing image: ${cachedProduct.imageUrl}")
            return null
        }
        return Item(
            imageUrl = cachedProduct.imageUrl ?: "https://via.placeholder.com/150",
            brand = cachedProduct.category ?: "N/A",
            name = cachedProduct.name ?: "Unknown Product",
            price = cachedProduct.price,
            krogerProductId = cachedProduct.productId
        )
    }

    private fun updateTargetUI(item: Item) {
        this.targetItem = item
        tvTargetBrand.text = item.brand
        tvTargetName.text = item.name
        tvTargetPrice.text = "$${String.format("%.2f", item.price)}"
        Glide.with(this)
            .load(item.imageUrl)
            .into(ivTarget)
    }

    private fun checkAnswer(selectedOption: Item?) {
        if (gameHasEnded) return

        val correct: Boolean = if (selectedOption == null) {
            false
        } else {
            isOptionCorrect(selectedOption, targetItem, currentGoal)
        }

        if (correct) {
            score++
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
        } else {
            score = (score - 1).coerceAtLeast(0)
            Toast.makeText(this, "Incorrect. Goal was $currentGoal", Toast.LENGTH_SHORT).show()
        }
        updateScoreDisplay()

        if (timeLeftInMillis > 0) {
            uiScope.launch {
                loadNextQuestion()
            }
        } else if (!gameHasEnded) {
            endGame()
        }
    }

    private fun isOptionCorrect(option: Item, currentTarget: Item, goal: String): Boolean {
        if (option.price == currentTarget.price) return false
        return when (goal) {
            "Higher Price" -> option.price > currentTarget.price
            "Lower Price" -> option.price < currentTarget.price
            else -> false
        }
    }

    private fun loadMockQuestionSet(reason: String) {
        Log.w("MockData", "Loading full mock question set for goal '$currentGoal': $reason")

        targetItem = getRandomMockItem()
        if (targetItem.krogerProductId == null) {
            Log.e("MockData", "getRandomMockItem returned item with null ID for target.")
            runOnUiThread {
                Toast.makeText(this, "Error generating mock target. Please restart.", Toast.LENGTH_LONG).show()
                endGame(navigateToMenu = true)
            }
            return
        }

        // currentGoal is already set in loadNextQuestion, no need to reset here unless it's uninitialized
        // if (!::currentGoal.isInitialized || currentGoal.isBlank()) {
        // currentGoal = if (Random.nextBoolean()) "Higher Price" else "Lower Price"
        // }
        // Log.i("MockData", "Mock Question Goal: $currentGoal")


        val mockOptions = mutableListOf<Item>()
        var correctMock: Item? = null
        var attempts = 0
        while (correctMock == null && attempts < 50) { // Increased attempts for correct mock
            val exclusionsForCorrect = mutableListOf<String>()
            targetItem.krogerProductId?.let { exclusionsForCorrect.add(it) }

            val potentialCorrectMock = getMockItem(targetItem, currentGoal, true, exclusionsForCorrect)
            if (potentialCorrectMock.krogerProductId != null &&
                potentialCorrectMock.krogerProductId != targetItem.krogerProductId && // Must be different from target
                potentialCorrectMock.price != targetItem.price && // Price must be different
                isOptionCorrect(potentialCorrectMock, targetItem, currentGoal)
            ) {
                correctMock = potentialCorrectMock
            }
            attempts++
        }

        if (correctMock == null) {
            Log.e("MockData", "Failed to generate a valid correct mock option after $attempts attempts for goal $currentGoal and target price ${targetItem.price}.")
            runOnUiThread {
                Toast.makeText(this, "Error generating mock question. Trying again.", Toast.LENGTH_SHORT).show()
                // Potentially try to load another question or end game more gracefully
                uiScope.launch { loadNextQuestion() } // Try to load a completely new question
                // endGame(navigateToMenu = true)
            }
            return
        }
        mockOptions.add(correctMock)

        attempts = 0
        while (mockOptions.size < 3 && attempts < 50) { // Increased attempts for incorrect mocks
            val currentExclusionIds = mockOptions.mapNotNull { it.krogerProductId }.toMutableList()
            targetItem.krogerProductId?.let { currentExclusionIds.add(it) }

            val incorrectMock = getMockItem(targetItem, currentGoal, false, currentExclusionIds.distinct())
            if (incorrectMock.krogerProductId != null &&
                mockOptions.none { it.krogerProductId == incorrectMock.krogerProductId } &&
                incorrectMock.price != targetItem.price && // Ensure price is different
                !isOptionCorrect(incorrectMock, targetItem, currentGoal)
            ) {
                mockOptions.add(incorrectMock)
            }
            attempts++
        }

        if (mockOptions.size < 3) {
            Log.e("MockData", "Could not generate 3 distinct mock options after many attempts. Generated: ${mockOptions.size}. Forcing with random unique items.")
            // Fallback: fill remaining with purely random (but valid and distinct) items if still not enough
            var emergencyAttempts = 0
            while (mockOptions.size < 3 && emergencyAttempts < 20) {
                val exclusionList = mockOptions.mapNotNull { it.krogerProductId }.toMutableList()
                targetItem.krogerProductId?.let { exclusionList.add(it) }
                val randomFallbackMock = getRandomMockItem(exclusionList.distinct())

                if (randomFallbackMock.krogerProductId != null &&
                    mockOptions.none { it.krogerProductId == randomFallbackMock.krogerProductId } &&
                    randomFallbackMock.krogerProductId != targetItem.krogerProductId &&
                    randomFallbackMock.price != targetItem.price) { // Don't strictly check correctness here, just uniqueness
                    mockOptions.add(randomFallbackMock)
                }
                emergencyAttempts++
            }
        }


        if (mockOptions.size < 3 || mockOptions.any { it.krogerProductId == null } || mockOptions.distinctBy { it.krogerProductId }.size < mockOptions.size) {
            Log.e("MockData", "loadMockQuestionSet resulted in insufficient, invalid, or non-distinct options! Options count: ${mockOptions.size}, Distinct: ${mockOptions.distinctBy { it.krogerProductId }.size}")
            runOnUiThread {
                Toast.makeText(this, "Error generating complete mock options. Please restart.", Toast.LENGTH_LONG).show()
                endGame(navigateToMenu = true)
            }
            return
        }

        runOnUiThread {
            updateTargetUI(targetItem)
            adapter.updateOptions(mockOptions.shuffled().take(3))
            Log.d("MockData", "Successfully formed mock question. Target: ${targetItem.name}, Price: ${targetItem.price}, Goal: $currentGoal. Options: ${mockOptions.joinToString { it.name + "($" + it.price + ")" }}")
        }
    }

    private fun getMockItem(
        relativeToTarget: Item,
        goalForCorrectness: String, // This is the goal for the CORRECT option
        shouldBeCorrect: Boolean,   // Is the item we are generating the CORRECT one?
        excludeIds: List<String>
    ): Item {
        val basePrice = relativeToTarget.price
        var generatedPrice: Double
        var attempts = 0
        val maxAttempts = 50 // Increased max attempts for price generation

        var newProductId = "mock_item_${System.nanoTime()}_${Random.nextInt(10000)}"
        var idAttempts = 0
        while (excludeIds.contains(newProductId) && idAttempts < 10) {
            newProductId = "mock_item_${System.nanoTime()}_${Random.nextInt(10000)}_${idAttempts}"
            idAttempts++
        }

        do {
            val priceOffset = Random.nextDouble(0.25, 5.0).coerceAtLeast(0.01) // Ensure some difference, min 0.25

            if (shouldBeCorrect) {
                // Generating the CORRECT option based on goalForCorrectness
                generatedPrice = if (goalForCorrectness == "Higher Price") {
                    basePrice + priceOffset
                } else { // Lower Price
                    (basePrice - priceOffset)
                }
            } else {
                // Generating an INCORRECT option relative to goalForCorrectness
                // If goal is "Higher Price", an incorrect option is <= basePrice
                // If goal is "Lower Price", an incorrect option is >= basePrice
                generatedPrice = if (goalForCorrectness == "Higher Price") { // Correct is Higher, so Incorrect is Lower or Equal
                    basePrice - priceOffset
                } else { // Correct is Lower, so Incorrect is Higher or Equal
                    basePrice + priceOffset
                }
            }
            generatedPrice = String.format("%.2f", generatedPrice.coerceAtLeast(0.01)).toDouble()
            attempts++

            // Check if the generated price is valid for its role (correct/incorrect) and different from basePrice
            val isValid: Boolean = if (shouldBeCorrect) {
                generatedPrice != basePrice && isPriceConditionMet(generatedPrice, basePrice, goalForCorrectness)
            } else { // Should be incorrect
                generatedPrice != basePrice && !isPriceConditionMet(generatedPrice, basePrice, goalForCorrectness)
            }
        } while ((!isValid || generatedPrice <= 0) && attempts < maxAttempts)


        // Fallback if loop failed to produce a good price satisfying all conditions
        if (generatedPrice <= 0 || generatedPrice == basePrice || (shouldBeCorrect && !isPriceConditionMet(generatedPrice, basePrice, goalForCorrectness)) || (!shouldBeCorrect && isPriceConditionMet(generatedPrice, basePrice, goalForCorrectness))) {
            Log.w("GetMockItem", "Fallback price generation after $attempts attempts for ${if (shouldBeCorrect) "correct" else "incorrect"} item. Goal: $goalForCorrectness, Base: $basePrice")
            var fallbackAttempts = 0
            do {
                val randomFactor = Random.nextDouble(1.0, 5.0) * (if(Random.nextBoolean()) 1 else -1)
                generatedPrice = (basePrice + randomFactor).coerceAtLeast(0.01)
                generatedPrice = String.format("%.2f", generatedPrice).toDouble()
                fallbackAttempts++
            } while (generatedPrice == basePrice && fallbackAttempts < 10)

            if (generatedPrice == basePrice) { // Still same, force a small change
                generatedPrice = (basePrice + if (Random.nextBoolean()) 0.5 else -0.5).coerceAtLeast(0.01)
                generatedPrice = String.format("%.2f", generatedPrice).toDouble()
            }
            Log.d("GetMockItem", "Fallback price: $generatedPrice")
        }


        val brands = listOf("MockBrand", "Test Goods", "Sample Foods", "ValueMock")
        val itemNamesList = listOf("Mock Item", "Test Product", "Sample Stuff", "Placeholder Unit")
        return Item(
            imageUrl = "https://via.placeholder.com/150/${if (shouldBeCorrect) "4CAF50" else "F44336"}/FFFFFF?Text=${if (shouldBeCorrect) "C" else "W"}-${"%.2f".format(generatedPrice)}",
            brand = brands.random(),
            name = itemNamesList.random() + " " + (Random.nextInt(90) + 10),
            price = generatedPrice,
            krogerProductId = newProductId
        )
    }

    private fun isPriceConditionMet(optionPrice: Double, targetPrice: Double, goal: String): Boolean {
        // Condition met if the optionPrice correctly satisfies the goal relative to targetPrice
        if (optionPrice == targetPrice) return false // Equal price is never a correct condition
        return when (goal) {
            "Higher Price" -> optionPrice > targetPrice
            "Lower Price" -> optionPrice < targetPrice
            else -> false
        }
    }

    private fun getRandomMockItem(excludeIds: List<String> = emptyList()): Item {
        val brands = listOf("Budget Brand", "Everyday Essentials", "Basics Co", "Generic Goods")
        val itemNames = listOf("Random Item", "Assorted Product", "General Unit", "Misc Stuff")
        val price = String.format("%.2f", Random.nextDouble(0.5, 20.0).coerceAtLeast(0.01)).toDouble()

        var productId = "mock_standalone_${System.nanoTime()}_${Random.nextInt(1000)}"
        var attempts = 0
        while(excludeIds.contains(productId) && attempts < 10) {
            productId = "mock_standalone_${System.nanoTime()}_${Random.nextInt(1000)}_${attempts+1}" // ensure different attempt hash
            attempts++
        }

        return Item(
            imageUrl = "https://via.placeholder.com/150/9E9E9E/FFFFFF?Text=MockTarget",
            brand = brands.random(),
            name = itemNames.random() + " " + (Random.nextInt(900) + 100),
            price = price,
            krogerProductId = productId
        )
    }

    private fun endGame(navigateToMenu: Boolean = true) {
        if (gameHasEnded) return
        gameHasEnded = true
        Log.i("MainActivity", "Game ended. Final Score: $score")

        if (::gameTimer.isInitialized) {
            gameTimer.cancel()
        }
        runOnUiThread {
            rvOptions.visibility = View.GONE
        }

        val currentHighScore = sharedPreferences.getInt(highScoreKey, 0)
        if (score > currentHighScore) {
            sharedPreferences.edit().putInt(highScoreKey, score).apply()
            Toast.makeText(this, "New High Score: $score!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Game Over! Final Score: $score", Toast.LENGTH_LONG).show()
        }

        if (navigateToMenu) {
            val intent = Intent(this, MenuActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainActivityJob.cancel()
        if (::gameTimer.isInitialized) {
            gameTimer.cancel()
        }
    }
}
