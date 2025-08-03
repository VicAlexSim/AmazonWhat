package com.example.amazonwhat

import android.os.Bundle
import android.os.CountDownTimer
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
    private lateinit var tvTargetPrice: TextView

    private var score = 0
    private var timeLeft = 30_000L
    private lateinit var countDownTimer: CountDownTimer

    private lateinit var targetItem: Item
    private lateinit var goal: String

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
        tvTargetPrice = findViewById(R.id.tvTargetPrice)

        // Setup RecyclerView
        adapter = OptionAdapter(mutableListOf()) { selectedItem ->
            checkAnswer(selectedItem)
        }
        rvOptions.adapter = adapter
        rvOptions.layoutManager = LinearLayoutManager(this)

        // Start game
        startGame()
    }

    private fun startGame() {
        startTimer()
        loadNextRound()
    }

    private fun startTimer() {
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

    private fun loadNextRound() {
        goal = if (Random.nextBoolean()) "Higher Price" else "Lower Price"
        tvGoal.text = "Goal: $goal"

        // TODO: Replace with real API calls
        targetItem = getRandomItem()
        updateTargetUI(targetItem)

        val options = mutableListOf<Item>()
        options.add(getValidOption(targetItem, goal))
        options.add(getInvalidOption(targetItem, goal))
        options.add(getInvalidOption(targetItem, goal))

        options.shuffle()
        adapter.updateOptions(options)
    }

    private fun updateTargetUI(item: Item) {
        tvTargetBrand.text = item.brand
        tvTargetName.text = item.name
        tvTargetPrice.text = "$${String.format("%.2f", item.price)}"
        Glide.with(this).load(item.imageUrl).into(ivTarget)
    }

    private fun checkAnswer(selected: Item) {
        val isCorrect = if (goal == "Higher Price") {
            selected.price > targetItem.price
        } else {
            selected.price < targetItem.price
        }

        if (isCorrect) {
            score++

        }
        else {
            score--; // Prevent spam-clicking (Net Negative)
        }
        tvScore.text = "Score: $score"

        loadNextRound()
    }

    private fun endGame() {
        Toast.makeText(this, "Time's up! Final Score: $score", Toast.LENGTH_LONG).show()
        // TODO: Show final score screen or restart
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
}