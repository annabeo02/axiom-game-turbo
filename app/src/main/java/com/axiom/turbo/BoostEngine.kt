package com.axiom.turbo

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.io.DataOutputStream

object BoostEngine {

    private val BLOAT = listOf(
        "com.facebook.katana",
        "com.facebook.orca",
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.snapchat.android",
        "com.google.android.youtube",
        "com.android.chrome",
        "com.discord",
        "com.spotify.music",
        "com.netflix.mediaclient"
    )

    val GAMES = listOf(
        GameInfo("org.angelauramc.zalith", "Zalith Launcher", "mc"),
        GameInfo("com.mojang.minecraftpe", "Minecraft PE", "mc"),
        GameInfo("com.roblox.client", "Roblox", "generic"),
        GameInfo("com.garena.game.kgvn", "Lien Quan", "moba"),
        GameInfo("com.dts.freefireth", "Free Fire", "moba"),
        GameInfo("com.tencent.ig", "PUBG Mobile", "moba")
    )

    data class GameInfo(val pkg: String, val name: String, val profile: String)

    data class BoostResult(
        val appsKilled: Int,
        val ramFreed: Long,
        val ramAvailable: Long,
        val profile: String
    )

    fun killBloat(ctx: Context): Int {
        var killed = 0
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (pkg in BLOAT) {
            try {
                am.killBackgroundProcesses(pkg)
                killed++
            } catch (_: Exception) {}
        }
        return killed
    }

    fun getAvailableRam(ctx: Context): Long {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return mi.availMem / (1024 * 1024)
    }

    fun enableDnd(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }
    }

    fun disableDnd(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }

    fun disableAnimations(ctx: Context): Boolean {
        return try {
            Settings.Global.putFloat(ctx.contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, 0f)
            Settings.Global.putFloat(ctx.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
            Settings.Global.putFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
            true
        } catch (_: SecurityException) { false }
    }

    fun restoreAnimations(ctx: Context) {
        try {
            Settings.Global.putFloat(ctx.contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, 1f)
            Settings.Global.putFloat(ctx.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
            Settings.Global.putFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        } catch (_: SecurityException) {}
    }

    fun runShell(cmd: String) {
        try {
            val p = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(p.outputStream)
            os.writeBytes("$cmd\n")
            os.writeBytes("exit\n")
            os.flush()
            p.waitFor()
        } catch (_: Exception) {}
    }

    fun boostAll(ctx: Context, profile: String = "generic"): BoostResult {
        val killed = killBloat(ctx)
        val before = getAvailableRam(ctx)
        System.gc()
        val after = getAvailableRam(ctx)
        enableDnd(ctx)
        disableAnimations(ctx)

        when (profile) {
            "mc" -> runShell("echo performance > /sys/devices/system/cpu/cpufreq/policy0/scaling_governor")
            "moba" -> runShell("echo 1 > /proc/sys/net/ipv4/tcp_low_latency")
        }

        return BoostResult(
            appsKilled = killed,
            ramFreed = after - before,
            ramAvailable = after,
            profile = profile
        )
    }

    fun openOverlaySettings(ctx: Context) {
        ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openDndSettings(ctx: Context) {
        ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openBatterySettings(ctx: Context) {
        ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
