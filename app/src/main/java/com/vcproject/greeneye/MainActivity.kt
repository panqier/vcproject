package com.vcproject.greeneye

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vcproject.greeneye.ui.HomeFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpTransaction()
    }

    private fun setUpTransaction() {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, HomeFragment())
        ft.commit()
    }
}
