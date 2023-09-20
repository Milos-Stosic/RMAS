package com.example.rmas

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ScoreboardFragment : Fragment() {

    private lateinit var database:FirebaseDatabase
    private lateinit var auth:FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view= inflater.inflate(R.layout.fragment_score_board, container, false)

        val context=requireContext()

        database= FirebaseDatabase.getInstance("https://projekat-rmas-default-rtdb.europe-west1.firebasedatabase.app/")
        auth= FirebaseAuth.getInstance()

        val tableLayout=view.findViewById<TableLayout>(R.id.tableLayout)

        val headerRow = TableRow(requireContext())
        val headerNameTextView = TextView(requireContext())
        headerNameTextView.text = "Username"
        headerNameTextView.gravity = Gravity.CENTER
        headerNameTextView.setTypeface(null, Typeface.BOLD)
        val headerScoreTextView = TextView(requireContext())
        headerScoreTextView.text = "Score"
        headerScoreTextView.gravity = Gravity.CENTER
        headerScoreTextView.setTypeface(null, Typeface.BOLD)
        headerRow.addView(headerNameTextView)
        headerRow.addView(headerScoreTextView)
        tableLayout.addView(headerRow)

        val usersRef=database.reference.child("users")
        usersRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val userList = mutableListOf<Pair<String, Int>>()

                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key
                    val userName = userSnapshot.child("userName").getValue(String::class.java)
                    val userScore = userSnapshot.child("brPoena").getValue(Int::class.java)

                    if (userId != null && userName != null && userScore != null) {
                        userList.add(Pair(userName, userScore))
                    }
                }


                userList.sortByDescending { it.second }


                tableLayout.removeViews(1, tableLayout.childCount - 1)


                for (userData in userList) {
                    val userName = userData.first
                    val userScore = userData.second

                    val userRow = TableRow(context)
                    userRow.gravity = Gravity.CENTER
                    userRow.setBackgroundResource(R.drawable.table_border)

                    val userNameTextView = TextView(context)
                    userNameTextView.text = userName
                    userNameTextView.gravity = Gravity.CENTER
                    userNameTextView.layoutParams = TableRow.LayoutParams(
                        0, TableRow.LayoutParams.WRAP_CONTENT, 1f
                    )
                    val userScoreTextView = TextView(context)
                    userScoreTextView.text = userScore.toString()
                    userScoreTextView.gravity = Gravity.CENTER
                    userScoreTextView.layoutParams = TableRow.LayoutParams(
                        0, TableRow.LayoutParams.WRAP_CONTENT, 1f
                    )
                    userRow.addView(userNameTextView)
                    userRow.addView(userScoreTextView)
                    tableLayout.addView(userRow)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("ERROR","$error")
            }
        })

        return view
    }

}
