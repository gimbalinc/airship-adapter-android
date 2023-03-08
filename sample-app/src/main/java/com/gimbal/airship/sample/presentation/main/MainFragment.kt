package com.gimbal.airship.sample.presentation.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import com.urbanairship.UAirship
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

        if (!checkPermissions()) {
            findNavController().navigate(MainFragmentDirections.actionMainFragmentToPermissionFragment())
            return
        }

        UAirship.takeOff(requireActivity().application)
//        UAirship.shared().pushManager.userNotificationsEnabled = true

        val adapter = PlaceEventAdapter(listOf())
        binding.recycleView.adapter = adapter

        viewModel.onPermissionsGranted()
        viewModel.placeEvents.observe(viewLifecycleOwner) {
            it.map { placeEvent ->
                Timber.d(placeEvent.placeName)
            }
            adapter.updateItems(it)
        }
    }

    /**
     * Checks if the user has given the required permissions
     *
     * @return true if the user has given all permissions, false otherwise.
     */
    private fun checkPermissions(): Boolean {
        val hasFineLocationPermissions = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocationPermissions = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasNotificationPermissions = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        return hasFineLocationPermissions && hasCoarseLocationPermissions && hasNotificationPermissions
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
            binding.label.text = if (item.isArrival)
                "Arrived at ${item.placeName} at ${item.formattedTime}"
            else "Departed ${item.placeName} at ${item.formattedTime}"
        }
    }
}
