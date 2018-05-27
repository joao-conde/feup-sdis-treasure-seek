package sdis.util;

import android.app.Notification;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import sdis.treasureseek.R;

public class Utils {

    public static class Pair<K,V> {

        public K key;
        public V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

    }

    public static void showNotification(Context context, String title, String text) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, "c")
                .setSmallIcon(R.drawable.common_google_signin_btn_text_dark)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Notification not = mBuilder.build();
        NotificationManagerCompat man = NotificationManagerCompat.from(context);
        man.notify(1,not);
    }

}
