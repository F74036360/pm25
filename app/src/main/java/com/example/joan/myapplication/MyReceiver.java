package com.example.joan.myapplication;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Created by kathe on 2018/1/21.
 */

public class MyReceiver extends BroadcastReceiver {
    int MID = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent1 = new Intent(context, InfoActivity.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        String user_pm25 = intent.getStringExtra("pm2.5");
        String notificationMsg = null;
        Log.e("MyReceiver", user_pm25);
        double pm = Double.parseDouble(user_pm25);
        if (pm <= 50 ) {
            notificationMsg = "空氣良好，可以從事正常戶外活動。";
        } else if (pm > 50 && pm <= 100 ) {
            notificationMsg = "空氣普通，可以從事正常戶外活動。";
        } else if (pm > 100 && pm <= 150 ) {
            notificationMsg = "對敏感族群不健康，有心臟、呼吸道及心血管疾病的成人與孩童感受到癥狀時，應減少體力消耗及戶外活動。";
        } else if (pm > 150 && pm <= 200 ) {
            notificationMsg = "對所有族群不健康。若要外出請記得戴口罩，回家記得洗手洗臉清洗鼻腔。";
        } else if (pm > 201 && pm <= 300 ) {
            notificationMsg = "空氣非常不健康，請減少戶外活動。若要外出請記得戴口罩，回家記得洗手洗臉清洗鼻腔。";
        } else if (pm > 301 && pm <= 500 ) {
            notificationMsg = "空氣已達危害人體狀態，請勿戶外活動。若要外出請戴口罩，回家記得洗手洗臉清洗鼻腔。";
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 100, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context).
                setSmallIcon(R.drawable.common_ic_googleplayservices).
                setContentIntent(pendingIntent).
                setContentText(notificationMsg).
                setContentTitle("今日懸浮微粒指數："+ user_pm25).
//                setSound(alarmSound).
        setAutoCancel(true);
        notificationManager.notify(MID, builder.build());
        MID++;
    }
}
