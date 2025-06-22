package com.example.smartduino.bottomdialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import com.example.smartduino.ObjectBox.store
import com.example.smartduino.R
import com.example.smartduino.databinding.FragmentDeviceBinding
import com.example.smartduino.entities.Device
import com.example.smartduino.entities.Room
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.objectbox.kotlin.boxFor
import android.widget.ArrayAdapter
import com.example.smartduino.interfaces.OnDeviceChangeListener
import com.example.smartduino.interfaces.OnRoomAddedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeviceFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentDeviceBinding? = null
    private val binding get() = _binding!!
    private lateinit var device: Device
    private lateinit var rooms: List<Room>
    private lateinit var roomNames: List<String>
    private var selectedRoomPosition = 0

    companion object {
        private const val ARG_DEVICE_ID = "device_id"

        fun newInstance(deviceId: Long): DeviceFragment {
            return DeviceFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_DEVICE_ID, deviceId)
                }
            }
        }
    }
    private var deviceChangeListener: OnDeviceChangeListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Проверяем, что активность реализует наш интерфейс
        deviceChangeListener = context as? OnDeviceChangeListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.post {
            val parent = view.parent as View
            val params = parent.layoutParams
            params.height = resources.getDimensionPixelSize(R.dimen.bottom_sheet_height)
            parent.layoutParams = params
        }
        // Get device from DB
        val deviceId = arguments?.getLong(ARG_DEVICE_ID) ?: 0L
        device = store.boxFor<Device>().get(deviceId)
        rooms = store.boxFor<Room>().all
        roomNames = rooms.map { it.name }

        setupUI()
    }

    private fun setupUI() {
        // Set icon based on device type
        val iconRes = when (device.type) {
            "LIGHT" -> R.drawable.relay
            "THERMOSTAT" -> R.drawable.temperature_humid
            "CURTAIN" -> R.drawable.curtains
            else -> R.drawable.wifi
        }
        binding.deviceIcon.setImageResource(iconRes)

        binding.deviceName.text = device.name
        binding.deviceType.text = device.type

        // Setup room AutoCompleteTextView
        setupRoomSpinner()

        // Save button
        binding.saveButton.setOnClickListener {
            saveDeviceChanges()
            dismiss()
        }
        binding.deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun setupRoomSpinner() {
        val rooms = store.boxFor<Room>().all
        val roomNames = rooms.map { it.name }

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_menu_item,
            roomNames
        )

        binding.roomSpinner.setAdapter(adapter)

        // Установка текущего значения
        device.room.target?.let { currentRoom ->
            val position = rooms.indexOfFirst { it.id == currentRoom.id }
            if (position >= 0) {
                binding.roomSpinner.setText(roomNames[position], false)
            }
        }

        binding.roomSpinner.setOnItemClickListener { _, _, position, _ ->
            selectedRoomPosition = position
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun saveDeviceChanges() {
        if (selectedRoomPosition in rooms.indices) {
            val selectedRoom = rooms[selectedRoomPosition]
            if (device.room.target?.id != selectedRoom.id) {
                device.room.target = selectedRoom
                store.boxFor<Device>().put(device)
                deviceChangeListener?.onDeviceChanged()
            }
        }
    }
    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete device")
            .setMessage("Are you sure you want to delete this device?")
            .setPositiveButton("Delete") { _, _ ->
                // Логика удаления устройства
                deleteDevice()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteDevice() {
        // Удаление устройства из базы данных
        store.boxFor<Device>().remove(device.id)

        // Уведомление о удалении
        deviceChangeListener?.onDeviceChanged()

        // Закрытие BottomSheet
        dismiss()
    }
}