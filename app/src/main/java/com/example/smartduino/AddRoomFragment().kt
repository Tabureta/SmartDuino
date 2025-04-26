package com.example.smartduino

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.example.smartduino.ObjectBox.store
import com.example.smartduino.entities.Room
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.objectbox.kotlin.boxFor

class AddRoomFragment : BottomSheetDialogFragment() {

    interface OnRoomAddedListener {
        fun onRoomAdded()
    }

    private var roomAddedListener: OnRoomAddedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Проверяем, что активность реализует наш интерфейс
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

            // Сохраняем новую комнату
            val newRoom = Room(name = roomName)
            store.boxFor<Room>().put(newRoom)

            // Уведомляем активность о добавлении комнаты
            roomAddedListener?.onRoomAdded()

            // Закрываем BottomSheet
            dismiss()
        }

        return v
    }

    override fun onDetach() {
        super.onDetach()
        roomAddedListener = null
    }
}