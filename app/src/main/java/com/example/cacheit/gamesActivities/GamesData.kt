package com.example.cacheit.gamesActivities

import java.util.ArrayList
import android.util.Log
import com.example.cacheit.AllGamesDataCallback
import com.example.cacheit.Firebase
import com.example.cacheit.MyCompletedGameplayDataCallback
import com.example.cacheit.MyGamesDataCallback
import com.example.cacheit.gameplayActivities.GameplayCard
import com.example.cacheit.gameplayActivities.GameplayData
import com.example.cacheit.mainActivities.MainActivity.Companion.currentLat
import com.example.cacheit.mainActivities.MainActivity.Companion.currentLon
import com.example.cacheit.shared.LocationActivites.Companion.distance
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class GamesData {

    companion object {
        var myGamesData = ArrayList<GameCard>()
        var allGamesData = ArrayList<GameCard>()

        fun fetchMyGamesData(myGamesDataCallback: MyGamesDataCallback) {
            Log.e("fetch my games data", "function called")

            myGamesData = ArrayList()
            Firebase.databaseGames!!.orderByChild("active")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                        Log.e("fetchMyGamesData", "Failed to read value. " + p0.message)
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        val children = p0.children
                        val userId = Firebase.auth!!.currentUser!!.uid
                        children.forEach {
                            if (it.child("ownerId").value.toString() == userId && !it.child("deleted").value.toString().toBoolean()) {
                                Log.e("if ownerId: ", "match found!")
                                myGamesData.add(
                                    GameCard (
                                        it.child("id").value.toString(),
                                        it.child("name").value.toString(),
                                        it.child("hint").value.toString(),
                                        it.child("ownerId").value.toString(),
                                        it.child("rating").value.toString(),
                                        it.child("ratingCount").value.toString(),
                                        it.child("locationHint").value.toString().toBoolean(),
                                        it.child("gameImg").value.toString(),
                                        it.child("difficulty").value.toString(),
                                        it.child("active").value.toString().toBoolean(),
                                        it.child("deleted").value.toString().toBoolean(),
                                        it.child("timesReported").value.toString(),
                                        it.child("lat").value.toString(),
                                        it.child("lon").value.toString(),
                                        it.child("timesFinished").value.toString(),
                                        it.child("timesClosed").value.toString(),
                                        it.child("gameMakerPoints").value.toString()
                                    )
                                )
                            }
                        }
                        myGamesData.reverse()
                        Log.i("returning value: ", myGamesData.toString())
                        myGamesDataCallback.onMyGamesDataCallback(myGamesData)
                    }
                })
        }

        fun fetchAllGamesData(allGamesDataCallback: AllGamesDataCallback) {
            Log.e("fetch my games data", "function called")

            GameplayData.myCompletedGameplays.clear()
            GameplayData.fetchMyCompletedGameplayData(object : MyCompletedGameplayDataCallback {
                override fun onMyCompletedGameplayDataCallback(MyCompletedGameplayData: ArrayList<GameplayCard>) {
                    var completedIds = ArrayList<String>()
                    MyCompletedGameplayData.forEach { it ->
                        completedIds.add(it.gameId)
                    }

                    allGamesData = ArrayList()
                    Firebase.databaseGames!!
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onCancelled(p0: DatabaseError) {
                                Log.e("fetchMyGamesData", "Failed to read value. " + p0.message)
                            }
                            override fun onDataChange(p0: DataSnapshot) {
                                val children = p0.children
                                val userId = Firebase.auth!!.currentUser!!.uid
                                children.forEach {
                                    if (it.child("ownerId").value.toString() != userId && !it.child("deleted").value.toString().toBoolean() && it.child("active").value.toString().toBoolean() && !completedIds.contains(it.child("id").value.toString())) {
                                        if (distance(currentLat!!, currentLon!!, it.child("lat").value.toString().toDouble(), it.child("lon").value.toString().toDouble()) > 10.0) return
                                        Log.e("if ownerId: ", "match found!")
                                        allGamesData.add(
                                            GameCard (
                                                it.child("id").value.toString(),
                                                it.child("name").value.toString(),
                                                it.child("hint").value.toString(),
                                                it.child("ownerId").value.toString(),
                                                it.child("rating").value.toString(),
                                                it.child("ratingCount").value.toString(),
                                                it.child("locationHint").value.toString().toBoolean(),
                                                it.child("gameImg").value.toString(),
                                                it.child("difficulty").value.toString(),
                                                it.child("active").value.toString().toBoolean(),
                                                it.child("deleted").value.toString().toBoolean(),
                                                it.child("timesReported").value.toString(),
                                                it.child("lat").value.toString(),
                                                it.child("lon").value.toString(),
                                                it.child("timesFinished").value.toString(),
                                                it.child("timesClosed").value.toString(),
                                                it.child("gameMakerPoints").value.toString()
                                            )
                                        )
                                    }
                                }
                                Log.i("returning value: ", allGamesData.toString())
                                allGamesDataCallback.onAllGamesDataCallback(allGamesData)
                            }
                        })
                }
            })
        }
    }
}