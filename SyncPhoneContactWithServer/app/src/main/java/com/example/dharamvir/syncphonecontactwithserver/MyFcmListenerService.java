package com.example.dharamvir.syncphonecontactwithserver;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Created by dharamvir on 16/06/17.
 */
public class MyFcmListenerService extends FirebaseMessagingService {

    String mSessionID;
    String mToken;
    String mAPIKEY;
    String mFrom;
    String multi;

    @Override
    public void onMessageReceived(RemoteMessage message){
        String from = message.getFrom();
        Map data = message.getData();

        mSessionID = (String)data.get("SessionID");
        mToken = (String)data.get("Token");
        mFrom = (String)data.get("Name");
        mAPIKEY = (String)data.get("API_KEY");
        multi = (String)data.get("multi");


        Notification();
        Log.d("message received", data.toString());
    }

    public void Notification() {
        // Set Notification Title
      //  String strtitle = getString(R.string.notificationtitle);
        // Set Notification Text
     //   String strtext = getString(R.string.notificationtext);

        // Open NotificationView Class on Notification Click
        Intent in = new Intent(this, OngoingCallActivity.class);
        in.putExtra("SESSION_ID", mSessionID);
        in.putExtra("API_KEY", mAPIKEY);
        in.putExtra("TOKEN", mToken);
        in.putExtra("From", mFrom);
        in.putExtra("multi", multi);
        // isMultiParty = getIntent().getBooleanExtra("multi", false);

       // startActivity(in);
        // Open NotificationView.java Activity
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, in,
                PendingIntent.FLAG_UPDATE_CURRENT);

        //Create Notification using NotificationCompat.Builder
        NotificationCompat.Builder builder = (android.support.v7.app.NotificationCompat.Builder) new NotificationCompat.Builder(this)
                // Set Icon
                .setSmallIcon(R.drawable.video_call)
                // Set Ticker Message
                .setTicker("Incoming call from " + mFrom)
                // Set Title
                .setContentTitle(mFrom + " calling..")
                // Set Text
                .setContentText("Tap to pick the call")
                // Add an Action Button below Notification
                // Set PendingIntent into Notification
                .setContentIntent(pIntent)
                // Dismiss Notification
                .setAutoCancel(true);

        // Create Notification Manager
        NotificationManager notificationmanager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Build Notification with Notification Manager
        notificationmanager.notify(0, builder.build());

    }


}

