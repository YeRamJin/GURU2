package com.example.guru2

import android.app.Activity
import android.content.Intent
import android.os.Bundle


class SplashActivity : Activity() {
    override fun onCreate(savedlnstanceState: Bundle?) {
        super.onCreate(savedlnstanceState)
        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}


