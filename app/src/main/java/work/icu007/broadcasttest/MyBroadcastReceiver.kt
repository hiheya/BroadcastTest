package work.icu007.broadcasttest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class MyBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "received in MyBroadcastReceiver", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "onReceive: ")
        abortBroadcast()
    }

    companion object{
        const val TAG = "MyBroadcastReceiver"
    }
}