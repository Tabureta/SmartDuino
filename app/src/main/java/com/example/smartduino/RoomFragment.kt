package com.example.smartduino

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartduino.ObjectBox.store
import com.example.smartduino.adapters.DeviceListAdapter
import com.example.smartduino.bottomdialog.DeviceFragment
import com.example.smartduino.entities.Device
import com.example.smartduino.entities.Room
import io.objectbox.kotlin.boxFor

class RoomFragment : Fragment() {
    companion object {
        fun newInstance(roomId: Long): RoomFragment {
            return RoomFragment().apply {
                arguments = Bundle().apply {
                    putLong("ROOM_ID", roomId)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_room, container, false)
        val roomId = arguments?.getLong("ROOM_ID") ?: 0

        // Получаем комнату из базы данных
        val roomBox = store.boxFor<Room>()
        val room = roomBox.get(roomId)

        val devices = room.devices

        val recyclerView: RecyclerView = view.findViewById(R.id.device_list_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        recyclerView.adapter = DeviceListAdapter(devices) { device ->
            Log.d("DeviceClick", "Clicked device: ${device.name}, type: ${device.type}, id: ${device.id}")
            DeviceFragment.newInstance(device.id).show(
                parentFragmentManager,
                "device_fragment"
            )
        }

        return view
    }
}