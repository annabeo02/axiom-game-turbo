package com.axiom.turbo

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvRam: TextView
    private lateinit var tvGames: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvRam = findViewById(R.id.tvRam)
        tvGames = findViewById(R.id.tvGames)

        findViewById<Button>(R.id.btnBoost).setOnClickListener { doBoost("generic") }
        findViewById<Button>(R.id.btnOverlay).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                BoostEngine.openOverlaySettings(this)
            } else {
                startService(Intent(this, FloatingService::class.java))
                Toast.makeText(this, "Nut noi da bat", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.btnDnd).setOnClickListener { BoostEngine.openDndSettings(this) }
        findViewById<Button>(R.id.btnBattery).setOnClickListener { BoostEngine.openBatterySettings(this) }

        buildGameList()
        updateRam()
    }

    private fun buildGameList() {
        val pm = packageManager
        for (g in BoostEngine.GAMES) {
            val installed = try { pm.getPackageInfo(g.pkg, 0); true } catch (_: Exception) { false }
            if (!installed) continue
            val btn = Button(this).apply {
                text = "Boost ${g.name}"
                setOnClickListener { doBoost(g.profile) }
            }
            tvGames.addView(btn)
        }
        if (tvGames.childCount == 0) {
            tvGames.addView(TextView(this).apply { text = "Chua co game nao duoc cai." })
        }
    }

    private fun doBoost(profile: String) {
        tvStatus.text = "Dang boost..."
        thread {
            val result = BoostEngine.boostAll(this, profile)
            runOnUiThread {
                tvStatus.text = "Xong! Kill ${result.appsKilled} app, +${result.ramFreed}MB"
                updateRam()
            }
        }
    }

    private fun updateRam() {
        thread {
            val ram = BoostEngine.getAvailableRam(this)
            runOnUiThread { tvRam.text = "RAM trong: ${ram}MB" }
        }
    }
}
