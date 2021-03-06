package de.txserver.txupnp.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.widget.DrawerLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBar
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView

import java.util.ArrayList

import de.txserver.txupnp.helper.DrawerListAdapter
import de.txserver.txupnp.helper.NavigationItem
import de.txserver.txupnp.upnp.ContentDirectoryBrowseCallbacks
import de.txserver.txupnp.R
import de.txserver.txupnp.app.TxUPnP
import de.txserver.txupnp.helper.CustomListAdapter
import de.txserver.txupnp.helper.CustomListItem
import de.txserver.txupnp.helper.DeviceModel
import de.txserver.txupnp.helper.ItemModel
import de.txserver.txupnp.upnp.ContentDirectoryBrowseHandler

class MainActivity : AppCompatActivity(), ContentDirectoryBrowseCallbacks, SharedPreferences.OnSharedPreferenceChangeListener {

    private val TAG = MainActivity::class.java.simpleName;

    private var actionBar: ActionBar? = null
    private var navigationItemArrayList: ArrayList<NavigationItem> = ArrayList()

    private var drawerLayout: DrawerLayout? = null
    private var drawerPane: RelativeLayout? = null
    private var drawerToggle: ActionBarDrawerToggle? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null

    private var listView: ListView? = null

    private lateinit var contentDirectoryBrowseHandler: ContentDirectoryBrowseHandler
    private var mDeviceList: ArrayList<CustomListItem>? = ArrayList()
    private lateinit var mDeviceListAdapter: ArrayAdapter<CustomListItem>
    private var mItemList: ArrayList<CustomListItem>? = ArrayList()
    private lateinit var mItemListAdapter: ArrayAdapter<CustomListItem>

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == WifiManager.WIFI_STATE_CHANGED_ACTION) {

                val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                        WifiManager.WIFI_STATE_UNKNOWN)

                val wifiWarning = findViewById(R.id.wifi_warning) as TextView

                when (state) {
                    WifiManager.WIFI_STATE_ENABLED -> {
                        wifiWarning.visibility = View.GONE

                        contentDirectoryBrowseHandler.let {
                            it.refreshDevices()
                            it.refreshCurrent()
                        }
                    }
                    WifiManager.WIFI_STATE_DISABLED -> {
                        wifiWarning.visibility = View.VISIBLE
                        mDeviceListAdapter.clear()
                        mItemListAdapter.clear()
                    }
                    WifiManager.WIFI_STATE_UNKNOWN -> wifiWarning.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val myToolbar = findViewById(R.id.myToolbar) as Toolbar
        setSupportActionBar(myToolbar)

        actionBar = supportActionBar

        actionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeButtonEnabled(true)
        }

        navigationItemArrayList.let {
            it.add(NavigationItem(resources.getString(R.string.menuItem_settings), resources.getString(R.string.menuItemText_settings), R.drawable.ic_settings_black_24px, R.layout.activity_settings))
            it.add(NavigationItem(resources.getString(R.string.menuItem_about), resources.getString(R.string.menuItemText_about), R.drawable.ic_info_outline_black_24px, R.layout.activity_about))
            it.add(NavigationItem(resources.getString(R.string.menuItem_exit), resources.getString(R.string.menuItemText_exit), R.drawable.ic_exit_to_app_black_24px, R.layout.activity_main))
        }

        // Populate the Navigtion Drawer with options
        drawerLayout = findViewById(R.id.drawerLayout) as DrawerLayout
        drawerPane = findViewById(R.id.drawerPane) as RelativeLayout

        val navigationListView = findViewById(R.id.navList) as ListView
        val drawerListAdapter = DrawerListAdapter(this, navigationItemArrayList)
        navigationListView.adapter = drawerListAdapter
        navigationListView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id -> selectItemFromDrawer(position) }

        drawerToggle = object : ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {

            override fun onDrawerOpened(drawerView: View?) {
                super.onDrawerOpened(drawerView)
                invalidateOptionsMenu()
            }

            override fun onDrawerClosed(drawerView: View?) {
                super.onDrawerClosed(drawerView)
                invalidateOptionsMenu()
            }
        }

        drawerLayout?.addDrawerListener(drawerToggle!!)

        swipeRefreshLayout = findViewById(R.id.swipeRefresh) as SwipeRefreshLayout

        swipeRefreshLayout?.let {
            it.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorPrimary))
            it.setOnRefreshListener { refreshList() }
        }

        mDeviceListAdapter = CustomListAdapter(this, mDeviceList)
        mItemListAdapter = CustomListAdapter(this, mItemList)

        listView = findViewById(R.id.list) as ListView
        listView?.adapter = mDeviceListAdapter

        listView?.onItemClickListener = AdapterView.OnItemClickListener { adapter, v, position, arg3 ->
            setShowRefreshing(true)
            contentDirectoryBrowseHandler.navigateTo(adapter.getItemAtPosition(position))
        }

        contentDirectoryBrowseHandler = ContentDirectoryBrowseHandler(this, this)

        if (contentDirectoryBrowseHandler.isServiceConnectionBound()) {
            contentDirectoryBrowseHandler.refreshDevices()
            contentDirectoryBrowseHandler.refreshCurrent()
        } else {
            contentDirectoryBrowseHandler.bindServiceConnection()
        }

        val prefs = TxUPnP.instance.getSharedPref()
        prefs.registerOnSharedPreferenceChangeListener(this)

        val filter = IntentFilter()
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED")
        registerReceiver(receiver, filter)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle?.onOptionsItemSelected(item) == true) {
            return true
        } else {
            when (item.itemId) {
                R.id.action_refresh -> refreshList()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {

        super.onPostCreate(savedInstanceState)

        drawerToggle?.syncState()
    }

    public override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        contentDirectoryBrowseHandler.unbindServiceConnection()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        refreshList()
    }
/*
    private fun setActivityTitle() {

        actionBar?.title = resources.getString(R.string.app_name)
    }
*/
    private fun selectItemFromDrawer(position: Int) {

        var intent: Intent? = null

        when (navigationItemArrayList[position].activity) {

            R.layout.activity_settings -> intent = Intent(this, SettingsActivity::class.java)
            R.layout.activity_about -> intent = Intent(this, AboutActivity::class.java)
            R.layout.activity_main -> this.finish()
        }

        if (intent != null) {

            startActivity(intent)
        }

        // Close the drawer
        drawerLayout?.closeDrawer(drawerPane)
    }

    private fun unsetListSelection() {
        listView?.setItemChecked(-1, true)
    }

    private fun refreshList() {
        setShowRefreshing(true)
        contentDirectoryBrowseHandler.refreshCurrent()
    }

    override fun setShowRefreshing(show: Boolean) {
        runOnUiThread { swipeRefreshLayout?.isRefreshing = show }
    }

    override fun onBackPressed() {
        if (contentDirectoryBrowseHandler.goBack())
            super.onBackPressed()
    }

    override fun onDisplayDevices() {
        runOnUiThread {
            unsetListSelection()
            mDeviceListAdapter.clear()
            listView?.adapter = mDeviceListAdapter
            setShowRefreshing(false)
        }
    }

    override fun onDisplayDirectories() {
        runOnUiThread {
            unsetListSelection()
            mItemList?.clear()
            mItemListAdapter.clear()
            listView?.adapter = mItemListAdapter
            setShowRefreshing(false)
        }
    }

    override fun onDisplayItems(items: ArrayList<ItemModel>) {
        runOnUiThread {
            unsetListSelection()
            mItemList?.clear()
            mItemList?.addAll(items)
            mItemListAdapter.notifyDataSetChanged()
        }
    }

    override fun onDisplayAddItems(items: ArrayList<ItemModel>) {
        runOnUiThread {
            mItemList?.addAll(items)
            mItemListAdapter.notifyDataSetChanged()
        }
    }

    override fun onDisplayItemsError(error: String) {
        runOnUiThread {
            mItemListAdapter.clear()
            mItemListAdapter.add(CustomListItem(
                    R.drawable.ic_alert_black_24px,
                    resources.getString(R.string.info_errorlist_folders),
                    error))
        }
    }

    override fun onDeviceAdded(device: DeviceModel) {
        runOnUiThread {
            val position = mDeviceListAdapter.getPosition(device)

            if (position >= 0) {
                mDeviceListAdapter.remove(device)
                mDeviceListAdapter.insert(device, position)
            } else {
                mDeviceListAdapter.add(device)
            }
        }
    }

    override fun onDeviceRemoved(device: DeviceModel) {
        runOnUiThread { mDeviceListAdapter.remove(device) }
    }
}