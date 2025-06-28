package com.sheev.sheev_vision

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.sheev.sheev_vision.databinding.ActivityMainBinding
import com.sheev.sheev_vision.udp.UdpSocketListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var udpListener: UdpSocketListener
    private lateinit var broadcastMsgAdapter: ArrayAdapter<String>
    private val messages = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        broadcastMsgAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, messages)
        val listView: ListView = findViewById(R.id.broadcast_msg_view)
        listView.adapter = broadcastMsgAdapter

        startListening()
    }

    private fun startListening() {
        udpListener = UdpSocketListener(58266)

        // Create coroutine to handle network task
        lifecycleScope.launch {
            while (true) {
                val m = withContext(Dispatchers.IO) {
                    udpListener.receive()
                }

                // Automatically back on main thread
                if (m != null) {
                    messages.add(m)
                }
                broadcastMsgAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        udpListener.stopListening()
    }
}