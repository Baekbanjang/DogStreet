package com.example.road_fix_project;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("GeofenceReceiver", "Geofence event received!");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) {
            Log.d("GeofenceReceiver", "GeofencingEvent is null");
            return;
        }

        int geofenceTransition = GeofencingEvent.fromIntent(intent).getGeofenceTransition();
        Intent localIntent = new Intent();

        // Geofence에 진입했을 때 알림을 발생시킵니다.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            localIntent.setAction("GeofenceEnterEvent");  // 진입 이벤트를 나타내는 Action을 설정합니다.
            int notificationId = 0; // 알림의 고유 식별자를 선언합니다.

            // 알림을 생성합니다. CHANNEL_ID는 알림 채널의 ID입니다.
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "channelId")
                    .setSmallIcon(R.drawable.danger_marker)
                    .setContentTitle("Road Fix 알림 시스템")
                    .setContentText("도로 파손 지역 접근")
                    .setPriority(NotificationCompat.PRIORITY_MAX);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            //Intent localIntent = new Intent("GeofenceEvent");
            //LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);

            // 알림을 발생시킵니다. notificationId는 알림의 ID입니다.
            //noinspection MissingPermission
            notificationManager.notify(notificationId, builder.build());
            Log.d("GeofenceReceiver", "Notification sent!");
        }

        else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            localIntent.setAction("GeofenceExitEvent");
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
    }
}
