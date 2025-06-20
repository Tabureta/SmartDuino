package com.example.smartduino

import android.os.Bundle
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
import io.objectbox.kotlin.boxFor

class AllDevicesFragment : Fragment() {
    companion object {
        fun newInstance() = AllDevicesFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_room, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем все устройства из базы данных
        val deviceBox = store.boxFor<Device>()
        val allDevices = deviceBox.query().build().find()

        val recyclerView: RecyclerView = view.findViewById(R.id.device_list_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        recyclerView.adapter = DeviceListAdapter(allDevices) { device ->
            DeviceFragment.newInstance(device.id).show(
                parentFragmentManager,
                "device_fragment"
            )
        }
    }
}