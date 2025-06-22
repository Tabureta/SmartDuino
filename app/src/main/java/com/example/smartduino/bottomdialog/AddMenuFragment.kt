package com.example.smartduino.bottomdialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.example.smartduino.ObjectBox.store
import com.example.smartduino.R
import com.example.smartduino.entities.Device
import com.example.smartduino.supactivities.ConnectionActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.objectbox.kotlin.boxFor

class AddMenuFragment : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v: View = inflater.inflate(
            R.layout.add_menu_fragment,
            container, false
        )

        val connect_hub_button = v.findViewById<Button>(R.id.connect_hub)
        val connect_devices_button = v.findViewById<Button>(R.id.connect_devices)
        val create_room_button = v.findViewById<Button>(R.id.create_room)

        // Проверяем наличие хабов при создании фрагмента
        checkHubAvailability(connect_devices_button)

        connect_hub_button.setOnClickListener {
            Toast.makeText(
                activity,
                "Подключение хаба", Toast.LENGTH_SHORT
            ).show()
            dismiss()
            val intent = Intent(requireActivity(), ConnectionActivity::class.java)
            intent.putExtra(ConnectionActivity.EXTRA_DEVICE_TYPE, ConnectionActivity.DEVICE_TYPE_HUB)
            startActivity(intent)
        }

        connect_devices_button.setOnClickListener {
            if (hasAtLeastOneHub()) {
                Toast.makeText(
                    activity,
                    "Подключение устройства", Toast.LENGTH_SHORT
                ).show()
                dismiss()
                val intent = Intent(requireActivity(), ConnectionActivity::class.java)
                intent.putExtra(ConnectionActivity.EXTRA_DEVICE_TYPE, ConnectionActivity.DEVICE_TYPE_NODE)
                startActivity(intent)
            } else {
                Toast.makeText(
                    activity,
                    "Сначала добавьте хаб", Toast.LENGTH_LONG
                ).show()
            }
        }

        create_room_button.setOnClickListener {
            Toast.makeText(
                activity,
                "Создание комнаты", Toast.LENGTH_SHORT
            ).show()
            val addRoomFragment = AddRoomFragment()
            addRoomFragment.show(parentFragmentManager, "ModalBottomSheet")
            dismiss()
        }

        return v
    }

    private fun checkHubAvailability(devicesButton: Button) {
        if (!hasAtLeastOneHub()) {
            devicesButton.alpha = 0.5f // Делаем кнопку полупрозрачной
            devicesButton.isEnabled = false // Отключаем кнопку
        } else {
            devicesButton.alpha = 1f
            devicesButton.isEnabled = true
        }
    }

    private fun hasAtLeastOneHub(): Boolean {
        // Получаем список устройств и проверяем есть ли среди них хаб
        val devices = store.boxFor<Device>().all
        return devices.any { it.type == ConnectionActivity.DEVICE_TYPE_HUB }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.post {
            val parent = view.parent as View
            val params = parent.layoutParams
            params.height = resources.getDimensionPixelSize(R.dimen.bottom_sheet_height)
            parent.layoutParams = params
        }
    }
}
