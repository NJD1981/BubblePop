package com.ally.bubblepop

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val unlockSequence = listOf("TL", "TR", "BR", "BL")
    private val currentSequence = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        enableKioskMode()
        disableBackButton()
    }

    private fun enableKioskMode() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
    }

    private fun disableBackButton() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Back button disabled for kiosk mode
            }
        })
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableKioskMode()
    }

    fun onCornerTouch(corner: String) {
        if (unlockSequence[currentSequence.size] == corner) {
            currentSequence.add(corner)
            if (currentSequence.size == unlockSequence.size) {
                currentSequence.clear()
                showPinDialog()
            }
        } else {
            currentSequence.clear()
        }
    }

    private fun showPinDialog() {
        // PIN dialog will go here
    }
}