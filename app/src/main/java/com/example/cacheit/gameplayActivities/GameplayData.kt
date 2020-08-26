package com.example.cacheit.gameplayActivities

import android.util.Log
import com.example.cacheit.Firebase
import com.example.cacheit.MyActiveGameplayDataCallback
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class GameplayData {

    companion object {

        var myActiveGameplay = GameplayCard()
        fun fetchMyActiveGameplayData(myActiveGameplayDataCallback: MyActiveGameplayDataCallback) {
            Log.e("fetch my games data", "function called")
            Firebase.databaseGameplays!!
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                        Log.e("fetchMyGamesData", "Failed to read value. " + p0.message)
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        val children = p0.children
                        val userId = Firebase.auth!!.currentUser!!.uid
                        children.forEach {
                            if (it.child("playerId").value.toString() == userId && it.child("active").value.toString().toBoolean()) {
                                Log.e("if ownerId: ", "match found!")
                                myActiveGameplay =
                                    GameplayCard(
                                        it.child("gameplayId").value.toString(),
                                        it.child("gameId").value.toString(),
                                        it.child("playerId").value.toString(),
                                        it.child("totalTime").value.toString(),
                                        it.child("completed").value.toString().toBoolean(),
                                        it.child("active").value.toString().toBoolean(),
                                        it.child("points").value.toString(),
                                        it.child("dateStarted").value.toString(),
                                        it.child("lat").value.toString(),
                                        it.child("lon").value.toString()
                                    )
                            }
                        }
                        Log.i("returning value: ", myActiveGameplay.toString())
                        myActiveGameplayDataCallback.onMyActiveGameplayDataCallback(myActiveGameplay)
                    }
                })
        }
    }
}