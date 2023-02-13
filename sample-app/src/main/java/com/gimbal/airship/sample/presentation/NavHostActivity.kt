package com.gimbal.airship.sample.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gimbal.airship.sample.databinding.ActivityNavHostBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NavHostActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNavHostBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavHostBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}