package com.example.smartduino.bottomdialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.example.smartduino.ObjectBox.store
import com.example.smartduino.R
import com.example.smartduino.entities.Room
import com.example.smartduino.interfaces.OnRoomAddedListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.objectbox.kotlin.boxFor

class AddRoomFragment : BottomSheetDialogFragment() {

    private var roomAddedListener: OnRoomAddedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Устанавливаем стиль для BottomSheet
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        roomAddedListener = context as? OnRoomAddedListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.add_room_fragment, container, false)
        val addButton = v.findViewById<Button>(R.id.add_room)
        val editRoomName = v.findViewById<EditText>(R.id.edit_room_name)

        addButton.setOnClickListener {
            val roomName = editRoomName.text.toString().trim()

            if (roomName.isEmpty()) {
                editRoomName.error = "Введите название комнаты"
                return@setOnClickListener
            }

            val newRoom = Room(name = roomName)
            store.boxFor<Room>().put(newRoom)
            roomAddedListener?.onRoomAdded()
            dismiss()
        }

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Устанавливаем фиксированную высоту после создания view
        view.post {
            val parent = view.parent as View
            val params = parent.layoutParams
            params.height = resources.getDimensionPixelSize(R.dimen.bottom_sheet_height)
            parent.layoutParams = params
        }
    }

    override fun onDetach() {
        super.onDetach()
        roomAddedListener = null
    }
}