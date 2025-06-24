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
        return inflater.inflate(R.layout.add_room_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val addButton = view.findViewById<Button>(R.id.add_room)
        val editRoomName = view.findViewById<EditText>(R.id.edit_room_name)

        // Список готовых названий комнат и их кнопок
        val roomButtons = listOf(
            view.findViewById<Button>(R.id.btn_living_room),
            view.findViewById<Button>(R.id.btn_bedroom),
            view.findViewById<Button>(R.id.btn_kitchen),
            view.findViewById<Button>(R.id.btn_bathroom),
            view.findViewById<Button>(R.id.btn_kids_room),
            view.findViewById<Button>(R.id.btn_study),
            view.findViewById<Button>(R.id.btn_balcony),
            view.findViewById<Button>(R.id.btn_hallway),
            view.findViewById<Button>(R.id.btn_garage)
        )

        // Обработчики для готовых комнат
        roomButtons.forEach { button ->
            button.setOnClickListener {
                val roomName = button.text.toString()
                editRoomName.setText(roomName)
                editRoomName.setSelection(roomName.length) // Курсор в конец текста
            }
        }

        addButton.setOnClickListener {
            val roomName = editRoomName.text.toString().trim()

            if (roomName.isEmpty()) {
                editRoomName.error = "Введите название комнаты"
                return@setOnClickListener
            }

            addNewRoom(roomName)
        }

        // Устанавливаем фиксированную высоту
        view.post {
            val parent = view.parent as View
            parent.layoutParams = parent.layoutParams.apply {
                height = resources.getDimensionPixelSize(R.dimen.bottom_sheet_height)
            }
        }
    }

    private fun addNewRoom(roomName: String) {
        val newRoom = Room(name = roomName)
        store.boxFor<Room>().put(newRoom)
        roomAddedListener?.onRoomAdded()
        dismiss()
    }

    override fun onDetach() {
        super.onDetach()
        roomAddedListener = null
    }
}