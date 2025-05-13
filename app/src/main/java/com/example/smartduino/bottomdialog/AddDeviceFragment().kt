package com.example.smartduino.bottomdialog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

import com.example.smartduino.interfaces.OnRoomAddedListener
import com.example.smartduino.R
import com.example.smartduino.supactivities.ConnectionActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddDeviceFragment : BottomSheetDialogFragment() {

    private var roomAddedListener: OnRoomAddedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        roomAddedListener = context as? OnRoomAddedListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.add_device_fragment, container, false)
        val addButton = v.findViewById<Button>(R.id.add_device)

        addButton.setOnClickListener {
            roomAddedListener?.onRoomAdded()
            dismiss()
            startActivity(Intent(getActivity(), ConnectionActivity::class.java))


        }

        return v
    }

    override fun onDetach() {
        super.onDetach()
        roomAddedListener = null
    }
}