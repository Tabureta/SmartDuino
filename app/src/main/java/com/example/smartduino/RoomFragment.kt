package com.example.smartduino


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

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

        val recyclerView: RecyclerView = view.findViewById(R.id.device_list_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        val data = arrayOf("Комната 1", "Комната 2", "Комната 3", "Комната 4", roomId.toString())

        recyclerView.adapter = DeviceListAdapter(data)

        return view
    }
}