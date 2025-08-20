package com.abhayattcc.dictionaryreader.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.abhayattcc.dictionaryreader.databinding.ActivityPrivacyPolicyBinding

class PrivacyPolicyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrivacyPolicyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.webView.loadUrl("file:///android_asset/privacy_policy.html")
    }
}