package work.icu007.broadcasttest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class AnotherBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "received broadcast in AnotherBroadcastReceiver", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "onReceive: ")
    }

    companion object{
        const val TAG = "AnotherBroadcastReceiver"
    }
}