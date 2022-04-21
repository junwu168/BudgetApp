package com.example.budgetapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlin.math.pow
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var addEntryButton: ImageButton
    private lateinit var adjustExpenseButton: ImageButton
    private lateinit var viewHistoryButton: ImageButton
    private lateinit var upcomingBillButton: ImageButton

    private lateinit var experienceBar: ProgressBar
    private lateinit var earningBar: ProgressBar
    private lateinit var spendingBar: ProgressBar

    private lateinit var totalAmount: TextView
    private lateinit var goalListview: ListView
    private var goalAdapter: GoalAdapter? = null

    private lateinit var addGoalButton: Button
    //create database object
    private val context = this
    private val db = EntriesDB(context)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addEntryButton = findViewById(R.id.add_entry_button)
        adjustExpenseButton = findViewById(R.id.adjust_expense_button)
        viewHistoryButton = findViewById(R.id.view_history_button)
        upcomingBillButton = findViewById(R.id.upcoming_bill_button)

        experienceBar = findViewById(R.id.experienceBar)
        earningBar = findViewById(R.id.earningBar)
        spendingBar = findViewById(R.id.spendingBar)

        addGoalButton = findViewById(R.id.add_goal)

        totalAmount = findViewById(R.id.total_amount)
        val totalMoney = db.addPaycheckAmount() - db.addExpenseAmount()

        goalListview = findViewById(R.id.goal_listview)
        totalAmount.text = "Total Amount: $$totalMoney"

        addGoalButton.setOnClickListener {
            val intent = Intent(this@MainActivity, AddGoals::class.java)
            startActivity(intent)
        }

        adjustExpenseButton.setOnClickListener {
            // start new activity
            val intent = Intent(this@MainActivity, AdjustExpense::class.java) //
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(intent)
        }

        viewHistoryButton.setOnClickListener {
            // start new activity
            val intent = Intent(this@MainActivity, ExpenseViewer::class.java) //
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(intent)
        }

        goalAdapter = GoalAdapter(applicationContext)
        goalListview.adapter = goalAdapter


        upcomingBillButton.setOnClickListener {
            val intent = Intent(this@MainActivity, RecurringViewer::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(intent)
        }

        val today = Calendar.getInstance()
        //Check to see if any of the recurring bills have passed their deadlines
        val recurringBills = db.getAll_Recurring()
        CheckDate@for(bill in recurringBills) {
            var dueDate = getDate(bill)

            if(today.after(dueDate)) {
                //Need to create a new entry and update the dueDate of this
                val entry = Entry(
                    id = null,
                    title = bill.title,
                    date = bill.date,
                    amount = bill.amount,
                    categories = bill.categories
                )

                db.insertData(entry)
                val newDate = getNewDate(dueDate, bill.frequency)
                val sdf: SimpleDateFormat = SimpleDateFormat("MM/dd/yyyy")
                bill.last_paid = bill.date
                bill.date = sdf.format(newDate.time)
                db.updateRow_Recurring(bill.id, bill)
                if(today.after(newDate)) break@CheckDate
            }
        }
    }

    private fun getDate(expense: RecurringExpense): Calendar {
        var dueDate = Calendar.getInstance()
        var dateString = expense.date
        val tokens = dateString.split("/")

        dueDate.set(tokens.get(2).toInt(), tokens.get(0).toInt()-1, tokens.get(1).toInt())
        //Minus 1s offset from 1-12 to 0-11 for the month

        return dueDate
    }

    private fun getNewDate(oldDate: Calendar, frequency: String): Calendar {
        val newDate = oldDate

        if(frequency == "Weekly") {
            newDate.add(Calendar.DATE, 7)
        }
        else if(frequency == "Monthly") {
            newDate.add(Calendar.MONTH, 1)
        }
        else if(frequency == "Annually") {
            newDate.add(Calendar.YEAR, 1)
        }

        return newDate
    }
}

/*
* An adapter to inflate the task bubbles and let the user add their
* accomplishments, see the progress of each bubbles and edit/delete
* the content of each bubble
*/
class GoalAdapter(var context: Context): BaseAdapter() {
    private val db = EntriesDB(context)
    private var arraylist: MutableList<Goal> = db.getAllGoals()
    // parameter to calculate level's required experience points
    private val X: Double = 0.3
    private val Y: Double = 2.0

    override fun getCount(): Int {
        return arraylist.size
    }

    override fun getItem(p0: Int): Any {
        return arraylist[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    @SuppressLint("SetTextI18n", "ViewHolder")
    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        val view: View = View.inflate(context, R.layout.activity_game_bubble, null)
        val title: Button = view.findViewById(R.id.goal_button)
        val progressBar: ProgressBar = view.findViewById(R.id.level_progress)
        val level: TextView = view.findViewById(R.id.level)
        val plusButton: ImageButton = view.findViewById(R.id.plus)

        val goal: Goal = arraylist[p0]

        title.text = goal.title
        level.text = "Level ${goal.level}"

        //calculating the progress bar percentage
        val currentLevelProgress = goal.plus * 20 - findLevelExp(goal.level)
        val levelDifference = findLevelExp(goal.level + 1) - findLevelExp(goal.level)
        val progress = (currentLevelProgress / levelDifference * 100).toInt()
        progressBar.progress = progress

        //getting and setting the color of the bubble based on the level
        val goalColor = findProgressColor(goal.level)
        progressBar.progressTintList = ColorStateList.valueOf(Color.parseColor(goalColor))
        plusButton.setBackgroundColor(Color.parseColor(goalColor))
        plusButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(goalColor))

        //when click on the title, the user can edit/delete the content of the goal
        title.setOnClickListener {
            val intent = Intent(context, EditGoals::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            intent.putExtra("id", arraylist[p0].id)
            intent.putExtra("title", arraylist[p0].title)
            intent.putExtra("level", arraylist[p0].level)
            intent.putExtra("plus", arraylist[p0].plus)

            context.startActivity(intent)
        }

        //when press the plus button, update the database and the progress bar
        plusButton.setOnClickListener {
            goal.plus += 1
            goal.level = calculateLevel(goal.plus)
            db.editGoal(goal.id, goal)
            this.notifyDataSetChanged()
        }

        //updating the bubble and the level when the user press the plus icon
        return view
    }

    /* Given the level of the task, calculate the amount of experience points
    * needed to reach that level. Formula: (level / X)^Y */
    private fun findLevelExp(level: Int): Double{
        return (level / X).pow(Y)
    }

    /* Given the level of the task, find the color to display the task bubble in */
    private fun findProgressColor(level: Int): String{
        //list of colors available for the bubbles
        val colorList = arrayOf("#8144EF", "#EA7D5B", "#ED4981", "#A9EC5C", "#5DD5E4")

        return when {
            level <= 3 -> {colorList[0]}    //beginner's levels
            level <= 7 -> {colorList[1]}    //intermediate's levels
            level <= 12 -> {colorList[2]}   //advance's levels
            level <= 20 -> {colorList[3]}   //expert's levels
            else -> colorList[4]
        }
    }

    /* Given plus (the number of time the person accomplish a task, calculate the level */
    private fun calculateLevel(plus: Int): Int{
        var currentLevel = 0
        val currentExp = plus * 20
        while ((currentLevel / X).pow(Y) < currentExp){
            currentLevel += 1
        }
        return currentLevel - 1
    }
}

/*
* A PopUp Window for User to add their goals.
* Included all the flags, including non-empty flags and the 20-character limit flags.
* Once created, automatically set the level and plus = 0.
*/
class AddGoals: AppCompatActivity() {
    private lateinit var title: EditText
    private lateinit var addButton: Button
    private lateinit var cancelButton: Button

    //create database object
    private val context = this
    private val db = EntriesDB(context)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_goal)

        title = findViewById(R.id.title)
        addButton = findViewById(R.id.add)
        cancelButton = findViewById(R.id.cancel)

        addButton.setOnClickListener {
            when {
                title.text.toString() == "" -> {
                    val toast = Toast.makeText(this, "Title cannot be empty. Try again!", Toast.LENGTH_SHORT)
                    toast.setGravity(Gravity.TOP or Gravity.CENTER, 0, 200)
                    toast.show()
                }
                title.text.toString().length > 30 -> {
                    val toast = Toast.makeText(this, "Title must not exceed 30 characters. Try again!", Toast.LENGTH_SHORT)
                    toast.setGravity(Gravity.TOP or Gravity.CENTER, 0, 200)
                    toast.show()
                }
                else -> {
                    val newGoal = Goal(
                        null,
                        title.text.toString(),
                        0,
                        0
                    )
                    db.insertGoal(newGoal)
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        // if the user does not want to add anything, let them return to the homepage
        cancelButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

    }
}

class EditGoals: AppCompatActivity() {
    private lateinit var title: EditText
    private lateinit var editButton: Button
    private lateinit var deleteButton: Button
    private lateinit var cancelButton: Button

    //create database object
    private val context = this
    private val db = EntriesDB(context)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_goal)

        title = findViewById(R.id.title)
        editButton = findViewById(R.id.edit)
        cancelButton = findViewById(R.id.cancel)
        deleteButton = findViewById(R.id.delete)

        //get the information of the goal
        val extras = intent.extras
        val id = extras!!.getInt("id")
        val titleInfo = extras.getString("title")
        val levelInfo = extras.getInt("level")
        val plusInfo = extras.getInt("plus")

        title.setText(titleInfo)

        editButton.setOnClickListener {
            when {
                title.text.toString() == "" -> {
                    val toast = Toast.makeText(this, "Title cannot be empty. Try again!", Toast.LENGTH_SHORT)
                    toast.setGravity(Gravity.TOP or Gravity.CENTER, 0, 200)
                    toast.show()
                }
                title.text.toString().length > 30 -> {
                    val toast = Toast.makeText(this, "Title must not exceed 30 characters. Try again!", Toast.LENGTH_SHORT)
                    toast.setGravity(Gravity.TOP or Gravity.CENTER, 0, 200)
                    toast.show()
                }
                else -> {
                    val updatedGoal = Goal(
                        null,
                        title.text.toString(),
                        levelInfo,
                        plusInfo
                    )
                    db.editGoal(id, updatedGoal)
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        deleteButton.setOnClickListener {
            //delete the goal form the database
            val intent = Intent(this, MainActivity::class.java)
            db.deleteGoal(id)
            startActivity(intent)
        }

        // if the user does not want to add anything, let them return to the homepage
        cancelButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

    }
}