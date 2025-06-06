package com.example.smartduino.bottomdialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.example.smartduino.R
import com.example.smartduino.supactivities.ConnectionActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

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

        connect_hub_button.setOnClickListener {
            Toast.makeText(
                activity,
                "First Button Clicked", Toast.LENGTH_SHORT
            ).show()
            dismiss()
            val intent = Intent(getActivity(), ConnectionActivity::class.java)
            intent.putExtra(ConnectionActivity.EXTRA_DEVICE_TYPE, ConnectionActivity.DEVICE_TYPE_HUB)
            startActivity(intent)
        }

        connect_devices_button.setOnClickListener {
            Toast.makeText(
                activity,
                "Second Button Clicked", Toast.LENGTH_SHORT
            ).show()
            dismiss()
            val intent = Intent(getActivity(), ConnectionActivity::class.java)
            intent.putExtra(ConnectionActivity.EXTRA_DEVICE_TYPE, ConnectionActivity.DEVICE_TYPE_HUB)
            startActivity(intent)
        }

        create_room_button.setOnClickListener {
            Toast.makeText(
                activity,
                "Third Button Clicked", Toast.LENGTH_SHORT
            ).show()
            val addRoomFragment = AddRoomFragment()
            addRoomFragment.show(parentFragmentManager, "ModalBottomSheet")
            dismiss()
        }
        return v
    }
}
