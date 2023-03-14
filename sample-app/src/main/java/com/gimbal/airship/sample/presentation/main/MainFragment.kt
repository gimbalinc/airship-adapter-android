package com.gimbal.airship.sample.presentation.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.gimbal.airship.sample.R
import com.gimbal.airship.sample.databinding.FragmentMainBinding
import com.gimbal.airship.sample.databinding.ItemPlaceEventBinding
import com.gimbal.airship.sample.domain.PlaceEventDomainModel
import com.gimbal.airship.sample.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainFragment : Fragment(R.layout.fragment_main) {
    private val binding by viewBinding(FragmentMainBinding::bind)
    private val viewModel: MainViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.place_events_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                if (menuItem.itemId == R.id.delete) {

                    val builder = AlertDialog.Builder(requireContext())

                    builder.setMessage("Are you sure you want to delete all notifications?")
                        .setTitle("Delete")
                        .setPositiveButton("Yes") { dialog: DialogInterface, _: Int ->
                            viewModel.onDeleteClick()
                            dialog.dismiss()
                        }
                        .setNegativeButton(
                            "No"
                        ) { dialog: DialogInterface, _: Int -> dialog.cancel() }

                    val dialog = builder.create()
                    dialog.show()
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val permissionsToRequestWithoutRationale = permissionsToRequest(false)
        if (permissionsToRequestWithoutRationale.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequestWithoutRationale.toTypedArray())
        } else {
            requestPermissionsWithRationale()
        }

        val adapter = PlaceEventAdapter(listOf())
        binding.recycleView.adapter = adapter

        viewModel.placeEvents.observe(viewLifecycleOwner) {
            Timber.d("Refreshing transcript")
            it.map { placeEvent ->
                Timber.d(placeEvent.formattedTime + " " +
                        (if (placeEvent.isArrival) "ARRIVED  " else "DEPARTED ") +
                        placeEvent.placeName)
            }
            adapter.updateItems(it)
        }
    }

    @SuppressLint("InlinedApi")
    private fun permissionsToRequest(shouldProvideRationale: Boolean): List<String> {
        val desiredPermissions: List<Pair<String, Int?>> = listOf(
            Pair(Manifest.permission.ACCESS_FINE_LOCATION, null),
            Pair(Manifest.permission.BLUETOOTH_SCAN, 31),
            Pair(Manifest.permission.POST_NOTIFICATIONS, 33)
        )

        return desiredPermissions.filter { permission ->
            if (!hasPermission(permission.first, permission.second)) {
                shouldShowRequestPermissionRationale(permission.first) == shouldProvideRationale
            } else false
        }.map {
            it.first
        }
    }

    private fun hasPermission(permission: String, minApi: Int? = null): Boolean {
        return if (minApi == null || Build.VERSION.SDK_INT >= minApi) {
            ContextCompat.checkSelfPermission(requireContext(), permission) ==
                    PackageManager.PERMISSION_GRANTED
        } else true
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {
            permissionGrantMap: Map<String, Boolean> ->
                if (permissionGrantMap.all { requestResult -> requestResult.value }) {
                    viewModel.permissionsGranted()
                } else {
                    requestPermissionsWithRationale()
                }
        }

    private fun requestPermissionsWithRationale() {
        val permissionsWithRationale = permissionsToRequest(true)
        if (permissionsWithRationale.isNotEmpty()) {
            findNavController()
                .navigate(MainFragmentDirections.actionMainFragmentToPermissionFragment(
                    permissionsWithRationale.toTypedArray()
                ))
        }
    }

    class PlaceEventAdapter(
        private var items: List<PlaceEventDomainModel>
    ) : RecyclerView.Adapter<PlaceEventViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceEventViewHolder {
            val binding = ItemPlaceEventBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return PlaceEventViewHolder(binding)
        }

        override fun onBindViewHolder(holder: PlaceEventViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.count()

        @SuppressLint("NotifyDataSetChanged")
        fun updateItems(items: List<PlaceEventDomainModel>) {
            this.items = items
            this.notifyDataSetChanged()
        }
    }

    class PlaceEventViewHolder internal constructor(
        private val binding: ItemPlaceEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        internal fun bind(item: PlaceEventDomainModel) {
            binding.label.text = binding.label.resources.getString(R.string.place_event_item,
                item.formattedTime, if (item.isArrival) "ARRIVED" else "DEPARTED", item.placeName
            )
        }
    }
}
