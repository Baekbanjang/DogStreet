package com.example.firebase;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("GeofenceReceiver", "Geofence event received!");

        // Intent에서 GeofencingEvent를 추출
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) {
            Log.d("GeofenceReceiver", "GeofencingEvent is null");
            return;
        }

        // Geofence 전환 상태를 가져옴
        int geofenceTransition = GeofencingEvent.fromIntent(intent).getGeofenceTransition();
        Intent localIntent = new Intent();

        // Geofence에 진입했을 때 알림을 발생
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            localIntent.setAction("GeofenceEnterEvent");  // 진입 이벤트를 나타내는 Action을 설정
            int notificationId = 0; // 알림의 고유 식별자를 선언

            // 알림을 생성. CHANNEL_ID는 알림 채널의 ID
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "channelId")
                    .setSmallIcon(R.drawable.danger_marker)
                    .setContentTitle("Road Fix 알림 시스템")
                    .setContentText("도로 파손 지역 접근")
                    .setPriority(NotificationCompat.PRIORITY_MAX);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            // 알림 전송
            //noinspection MissingPermission
            notificationManager.notify(notificationId, builder.build());
            Log.d("GeofenceReceiver", "Notification sent!");
        }

        // Geofence에서 나갔을 때 알림을 발생
        else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            localIntent.setAction("GeofenceExitEvent");
        }

        // LocalBroadcastManager를 사용하여 알림을 전송
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
    }
}