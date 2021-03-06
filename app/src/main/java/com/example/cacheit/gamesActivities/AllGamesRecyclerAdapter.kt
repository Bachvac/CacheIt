package com.example.cacheit.gamesActivities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cacheit.*
import com.example.cacheit.Firebase.Companion.databaseUsers
import com.example.cacheit.MailingConfig.GMailSender
import com.example.cacheit.MailingConfig.SendEmail
import com.example.cacheit.gameplayActivities.GameplayCard
import com.example.cacheit.gameplayActivities.GameplayData
import com.example.cacheit.gameplayActivities.MyGameplayActivity
import com.example.cacheit.gamesActivities.GameCard
import com.example.cacheit.mainActivities.MainActivity
import com.example.cacheit.mainActivities.MainActivity.Companion.activeGameplay
import com.example.cacheit.mainActivities.MainActivity.Companion.currentLat
import com.example.cacheit.mainActivities.MainActivity.Companion.currentLon
import com.example.cacheit.mainActivities.MainActivity.Companion.mainContext
import kotlinx.android.synthetic.main.activity_main.navigationView
import com.example.cacheit.myGamesActivities.MyGamesActivity
import com.example.cacheit.shared.LocationActivites.Companion.distance
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.layout_game_card_item.view.*
import kotlinx.android.synthetic.main.layout_playable_game_card_item.view.img_game_card
import kotlinx.android.synthetic.main.layout_playable_game_card_item.view.tv_game_card_name
import kotlinx.android.synthetic.main.layout_playable_game_card_item.view.*
import kotlinx.coroutines.*
import net.glxn.qrgen.android.QRCode
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlinx.android.synthetic.main.activity_main.navigationView
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import kotlin.concurrent.thread


class AllGamesRecyclerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<GameCard> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyGamesViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.layout_playable_game_card_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder) {
            is MyGamesViewHolder -> {
                holder.bind(items[position])
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun submitList(GroupCardList: List<GameCard>) {
        items = GroupCardList
        notifyDataSetChanged()
    }

    class MyGamesViewHolder constructor(
        itemView: View
    ): RecyclerView.ViewHolder(itemView) {
        private val gameImage = itemView.img_game_card
        private val gameName = itemView.tv_game_card_name
        private val btnStartGame = itemView.btn_startGame
        private val ratingStars = itemView.rb_game_rating
        val difficulty = itemView.tv_game_card_difficulty

        fun bind(GameCard: GameCard) {
            gameName.text = GameCard.name
            ratingStars.rating = GameCard.rating.toFloat()
            difficulty.text = GameCard.difficulty

            btnStartGame!!.setOnClickListener { v ->
                var mDatabase: FirebaseDatabase? = null
                var mAuth: FirebaseAuth? = null
                var email: String? = null

                mDatabase = FirebaseDatabase.getInstance()
                mAuth = FirebaseAuth.getInstance()

                val userRef = mDatabase.getReference("/Users/" + mAuth.uid)
                val ref = mDatabase.getReference("/Games/" + GameCard.id)

                val dialog = Dialog(itemView.context)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setCancelable(false)
                dialog.setContentView(R.layout.layout_game_details)

                val name = dialog.findViewById(R.id.tv_game_details_name) as TextView
                val hintTitle = dialog.findViewById(R.id.tv_hint_title) as TextView
                val hintTxt = dialog.findViewById(R.id.tv_hint_txt) as TextView
                val distanceTitle = dialog.findViewById(R.id.tv_distance_title) as TextView
                val distanceTxt = dialog.findViewById(R.id.tv_distance_txt) as TextView
                val btnCloseDetails = dialog.findViewById(R.id.btn_close_details) as ImageButton

                val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
                val currentDate = sdf.format(Date())

                var flagExistsGameplay = false;
                GameplayData.fetchMySavedGameplayData(object : MySavedGameplayDataCallback {
                    override fun onMySavedGameplayDataCallback(MySavedGameplayData: java.util.ArrayList<GameplayCard>) {
                        if (GameplayData.mySavedGameplay.size > 0) {
                            for (savedGame in GameplayData.mySavedGameplay) {
                                Log.e("saved game id", savedGame.gameId)
                                Log.e("current game id", GameCard.id)
                                if (savedGame.gameId == GameCard.id) {
                                    val ref = mDatabase!!.getReference("/Gameplays/${savedGame.gameplayId}")
                                    ref?.child("active")?.setValue(true)
                                    ref?.child("dateStarted")?.setValue(currentDate)
                                    GameplayData.mySavedGameplay.clear()
                                    GameplayData.myActiveGameplay
                                    GameplayData.myActiveGameplay = savedGame
                                    flagExistsGameplay = true
                                    Log.e("game", "added")
                                    break
                                }
                            }
                        } else {
                            val gameplayId = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT)
                            val dbGame = Firebase.databaseGameplays?.child(gameplayId)
                            dbGame?.push()
                            dbGame?.child("gameplayId")?.setValue(gameplayId)
                            dbGame?.child("gameId")?.setValue(GameCard.id)
                            dbGame?.child("playerId")?.setValue(mAuth!!.uid)
                            dbGame?.child("totalTime")?.setValue(0)
                            dbGame?.child("completed")?.setValue(false)
                            dbGame?.child("active")?.setValue(true)
                            dbGame?.child("points")?.setValue(0)
                            dbGame?.child("dateStarted")?.setValue(currentDate)
                            dbGame?.child("lat")?.setValue(GameCard.lat)
                            dbGame?.child("lon")?.setValue(GameCard.lon)
                            dbGame?.child("name")?.setValue(GameCard.name)
                            dbGame?.child("hint")?.setValue(GameCard.hint)
                            dbGame?.child("gameMakerId")?.setValue(GameCard.ownerId)
                            dbGame?.child("difficulty")?.setValue(GameCard.difficulty)
                            dbGame?.child("initialDistance")?.setValue(GameCard.difficulty)
                            dbGame?.child("gameImg")?.setValue(GameCard.gameImg)
                            dbGame?.child("reported")?.setValue(false)
                            flagExistsGameplay = true
                        }
                        if (!flagExistsGameplay) {
                            val gameplayId = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT)
                            val dbGame = Firebase.databaseGameplays?.child(gameplayId)
                            dbGame?.push()
                            dbGame?.child("gameplayId")?.setValue(gameplayId)
                            dbGame?.child("gameId")?.setValue(GameCard.id)
                            dbGame?.child("playerId")?.setValue(mAuth!!.uid)
                            dbGame?.child("totalTime")?.setValue(0)
                            dbGame?.child("completed")?.setValue(false)
                            dbGame?.child("active")?.setValue(true)
                            dbGame?.child("points")?.setValue(0)
                            dbGame?.child("dateStarted")?.setValue(currentDate)
                            dbGame?.child("lat")?.setValue(GameCard.lat)
                            dbGame?.child("lon")?.setValue(GameCard.lon)
                            dbGame?.child("name")?.setValue(GameCard.name)
                            dbGame?.child("hint")?.setValue(GameCard.hint)
                            dbGame?.child("gameMakerId")?.setValue(GameCard.ownerId)
                            dbGame?.child("difficulty")?.setValue(GameCard.difficulty)
                            dbGame?.child("initialDistance")?.setValue(GameCard.difficulty)
                            dbGame?.child("gameImg")?.setValue(GameCard.gameImg)
                            dbGame?.child("reported")?.setValue(false)
                        }
                    }
                })

                btnCloseDetails!!.setOnClickListener { v ->

                    GameplayData.fetchMyActiveGameplayData(object : MyActiveGameplayDataCallback {
                        @SuppressLint("RestrictedApi")
                        override fun onMyActiveGameplayDataCallback(myGameplayData: GameplayCard) {
                            Log.e("lala", "switching activity from rec adapter")
                            val intent = Intent(itemView.context, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            itemView.context.startActivity(intent)
                            dialog.dismiss()
                        }
                    })
                }

                name.text = GameCard.name

                if (GameCard.hint.isNotEmpty()) {
                    hintTitle.visibility = View.VISIBLE
                    hintTxt.visibility = View.VISIBLE
                    hintTxt.text = GameCard.hint
                }

                if (GameCard.locationHint) {
                    var distanceApprox = distance(currentLat!!, currentLon!!, GameCard.lat.toDouble(), GameCard.lon.toDouble()).toInt()
                    distanceTitle.visibility = View.VISIBLE
                    distanceTxt.visibility = View.VISIBLE
                    distanceTxt.text = distanceApprox.toString()
                }

                dialog.show()
            }

            /*val requestOptions = RequestOptions()
                .placeholder(drawable.ic_launcher_background)
                .error(drawable.ic_launcher_background)
*/
            Glide.with(itemView.context)
                //.applyDefaultRequestOptions(requestOptions)
                .load(GameCard.gameImg)
                .into(gameImage)
        }
    }

}


