package com.example.budgetapp

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import kotlin.math.min

class ExpenseAdapter(var context: Context, var arraylist: MutableList<Expense>): BaseAdapter() {
    override fun getCount(): Int {
        return arraylist.size
    }

    override fun getItem(p0: Int): Any {
        return arraylist[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        val view: View = View.inflate(context, R.layout.expense_item, null)
        var categories: TextView = view.findViewById(R.id.expense_categories)
        var percent: TextView = view.findViewById(R.id.expense_percent)
        var max: TextView = view.findViewById(R.id.expense_max)
        var rec: TextView = view.findViewById(R.id.expense_rec)
        var action: ImageButton = view.findViewById(R.id.action)

        var expense: Expense = arraylist[p0]

        categories.setText(expense.categories)
        percent.setText(expense.percentage.toInt().toString() + "%")
        max.setText("Max: $" + expense.max.toInt().toString())

        rec.setText("Rec: $" + min(expense.percentage/100 * 1000.0, expense.max).toString())

        action.setOnClickListener {
            // start new activity
            // calc budget
            remove(p0)
            notifyDataSetChanged()
            Toast.makeText(context, "Delete category successfully!", Toast.LENGTH_SHORT).show()
        }

        return view!!
    }

    fun remove(position: Int) {
        arraylist.remove(arraylist.get(position))
    }

}

