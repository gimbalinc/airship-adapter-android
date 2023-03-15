package com.gimbal.airship.sample.presentation.permission

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.gimbal.airship.sample.R
import com.gimbal.airship.sample.databinding.FragmentPermissionBinding
import com.gimbal.airship.sample.databinding.FragmentPermissionPageBinding
import com.gimbal.airship.sample.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class PermissionFragment : Fragment(R.layout.fragment_permission) {
    private val viewModel: PermissionViewModel by viewModels()
    private val binding by viewBinding(FragmentPermissionBinding::bind)
    private lateinit var adapter: PageAdapter
    private val args: PermissionFragmentArgs by navArgs()

    @SuppressLint("InlinedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pages = mutableListOf<PageAdapter.Page>()
        for (permission in args.permissionsToRequest) {
            when(permission) {
                ACCESS_FINE_LOCATION -> pages.add(
                    PageAdapter.Page(
                        getString(R.string.location_permission_title),
                        getString(R.string.location_permission_body),
                        listOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION)
                    )
                )
                ACCESS_BACKGROUND_LOCATION -> pages.add(
                    PageAdapter.Page(
                        getString(R.string.background_permission_title),
                        getString(R.string.background_permission_body,
                            context?.packageManager?.backgroundPermissionOptionLabel.toString()),
                        listOf(ACCESS_BACKGROUND_LOCATION)
                    )
                )
                BLUETOOTH_SCAN -> pages.add(
                    PageAdapter.Page(
                        getString(R.string.bluetooth_permission_title),
                        getString(R.string.bluetooth_permission_body),
                        listOf(BLUETOOTH_SCAN)
                    )
                )
                POST_NOTIFICATIONS -> pages.add(
                    PageAdapter.Page(
                        getString(R.string.notification_permission_title),
                        getString(R.string.notification_permission_body),
                        listOf(POST_NOTIFICATIONS)
                    )
                )
            }
        }

        adapter = PageAdapter(pages)
        binding.pager.adapter = adapter
        binding.button.setOnClickListener {
            val page = pages[binding.pager.currentItem]
            requestMultiplePermissionLauncher.launch(page.requiredPermissions.toTypedArray())
        }
    }

    private val requestMultiplePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                permissionGrantMap: Map<String, Boolean> ->
            if (!permissionGrantMap.all { grant -> grant.value }) {
                var warning = "Permission denied:"
                permissionGrantMap.forEach {grant -> warning += " ${grant.key}" }
                Timber.w(warning)
            }
            nextPageOrDone()
        }

    private fun nextPageOrDone() {
        if (binding.pager.currentItem == adapter.itemCount - 1) {
            viewModel.onRequestsComplete()
            findNavController().popBackStack()
        } else {
            binding.pager.setCurrentItem(binding.pager.currentItem + 1, true)
        }
    }

    class PageAdapter(private val pages: List<Page>) : RecyclerView.Adapter<PageViewHolder>() {
        data class Page(
            val title: String,
            val body: String,
            val requiredPermissions: List<String>
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
