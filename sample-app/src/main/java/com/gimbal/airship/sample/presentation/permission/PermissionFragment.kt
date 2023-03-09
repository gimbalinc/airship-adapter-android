package com.gimbal.airship.sample.presentation.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.gimbal.airship.sample.R
import com.gimbal.airship.sample.databinding.FragmentPermissionBinding
import com.gimbal.airship.sample.databinding.FragmentPermissionPageBinding
import com.gimbal.airship.sample.viewBinding
import timber.log.Timber

class PermissionFragment : Fragment(R.layout.fragment_permission) {
    private val binding by viewBinding(FragmentPermissionBinding::bind)
    private lateinit var adapter: PageAdapter
    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionGrantMap: Map<String, Boolean> ->
        if (permissionGrantMap.all { entry -> entry.value }) {
            binding.pager.setCurrentItem(binding.pager.currentItem + 1, true)
        } else {
            Timber.w("Permission denied")
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isPermissionGranted ->
        if (isPermissionGranted) {
            findNavController().popBackStack()
        } else {
            Timber.w("Permission denied")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pages = mutableListOf<PageAdapter.Page>()

        pages.add(
            PageAdapter.Page(
                getString(R.string.location_permission_title),
                getString(R.string.location_permission_body)
            )
        )
        if (!checkNotificationPermission()) {
            pages.add(
                PageAdapter.Page(
                    getString(R.string.notification_permission_title),
                    getString(R.string.notification_permission_body)
                )
            )
        }
        adapter = PageAdapter(pages)
        binding.pager.adapter = adapter
        binding.button.setOnClickListener {
            if (binding.pager.currentItem == 0) {
                requestLocationPermissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ))
            } else if (Build.VERSION.SDK_INT >= 33) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        val hasFineLocationPermissions = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocationPermissions = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return hasFineLocationPermissions && hasCoarseLocationPermissions
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    class PageAdapter(private val pages: List<Page>) : RecyclerView.Adapter<PageViewHolder>() {
        data class Page(
            val title: String,
            val body: String
        )

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val binding = FragmentPermissionPageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return PageViewHolder(binding)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            holder.bind(
                pages.getOrNull(position)?.title ?: "",
                pages.getOrNull(position)?.body ?: ""
            )
        }

        override fun getItemCount() = pages.count()
    }

    class PageViewHolder internal constructor(
        private val binding: FragmentPermissionPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        internal fun bind(title: String, body: String) {
            binding.title.text = title
            binding.body.text = body
        }
    }
}
