package io.euphoria.xkcd.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.View
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import io.euphoria.xkcd.app.MainActivity
import io.euphoria.xkcd.app.impl.ui.UIUtils

class MainActivity : Activity() {
    private var enterBtn: Button? = null
    private var roomField: AutoCompleteTextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Showing the title myself
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)
        enterBtn = findViewById<View>(R.id.join_room) as Button?
        roomField = findViewById<View>(R.id.room_name) as AutoCompleteTextView?

        // Filter out definitely invalid characters
        val inputFilter: InputFilter =
            InputFilter { source, start, end, dest, dstart, dend ->
                val corrected: StringBuilder = StringBuilder()
                var changed: Boolean = false
                for (i in start until end) {
                    if (URLs.isValidRoomNameFragment(source[i].toString())) {
                        corrected.append(source[i])
                    } else {
                        changed = true
                    }
                }
                if (changed) corrected else null
            }
        roomField!!.filters = arrayOf(inputFilter)

        // Go to the selected room when done
        UIUtils.setEnterKeyListener(
            roomField,
            EditorInfo.IME_ACTION_GO,
            Runnable { showRoom(roomField!!.text.toString()) })
        enterBtn!!.setOnClickListener { showRoom(roomField!!.text.toString()) }
    }

    private fun showRoom(roomName: String) {
        if (!URLs.isValidRoomName(roomName)) {
            Toast.makeText(this@MainActivity, "Please enter a valid room name", Toast.LENGTH_SHORT)
                .show()
            return
        }
        val roomIntent: Intent = Intent(this, RoomActivity::class.java)
        val roomURI: Uri =
            Uri.parse("https://euphoria.io/room/$roomName/")
        roomIntent.data = roomURI
        roomIntent.action = Intent.ACTION_VIEW
        startActivity(roomIntent)
    }
}
