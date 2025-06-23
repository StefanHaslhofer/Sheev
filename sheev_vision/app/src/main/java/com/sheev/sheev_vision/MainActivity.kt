package com.sheev.sheev_vision

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.sheev.sheev_vision.databinding.ActivityMainBinding

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        var arr: Array<String> = arrayOf("123", "TEST")
        val broadcastMsgAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, arr)

        val listView: ListView = findViewById(R.id.broadcast_msg_view)
        listView.adapter = broadcastMsgAdapter
    }
}