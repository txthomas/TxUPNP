package de.txserver.slickupnp.activity

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log

import de.txserver.slickupnp.R

class SettingsActivity : AppCompatActivity() {

    private val TAG = SettingsActivity::class.java.simpleName

    private var actionBar: ActionBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val myToolbar = findViewById(R.id.myToolbar) as Toolbar
        setSupportActionBar(myToolbar)

        actionBar = supportActionBar
        if (actionBar != null) {

            actionBar!!.setDisplayHomeAsUpEnabled(true)
            actionBar!!.setHomeButtonEnabled(true)
        }

        // das Fragment anzeigen
        fragmentManager.beginTransaction()
                .replace(R.id.settingsContainer, SettingsFragment())
                .commit()

        setActivityTitle(null)
    }

    private fun setActivityTitle(customText: String?) {

        if (actionBar == null) {
            return
        }

        if (customText == null) {
            actionBar!!.title = resources.getString(R.string.menuItem_settings)
        } else {
            actionBar!!.title = customText
        }
    }
}
