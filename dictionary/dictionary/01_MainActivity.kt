package com.abhayattcc.dictionaryreader.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.findNavController
import com.abhayattcc.dictionaryreader.R
import com.abhayattcc.dictionaryreader.databinding.ActivityMainBinding
import com.abhayattcc.dictionaryreader.utils.FileUtils

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_settings -> {
                    // Open settings (to be implemented)
                    true
                }
                R.id.nav_share -> {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Check out Abhayattcc Dictionary Reader: [Your App URL]")
                        type = "text/plain"
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share App"))
                    true
                }
                R.id.nav_premium -> {
                    // Handle in-app purchase (to be implemented in BillingManager)
                    true
                }
                R.id.nav_privacy -> {
                    startActivity(Intent(this, PrivacyPolicyActivity::class.java))
                    true
                }
                else -> false
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        FileUtils.checkAndRequestPermissions(this) {
            FileUtils.downloadResources(this) {
                // Resources downloaded
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}