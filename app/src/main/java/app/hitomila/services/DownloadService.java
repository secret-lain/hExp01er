package app.hitomila.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.HashMap;

import app.hitomila.common.HitomiWebView;

/**
 * Created by admin on 2016-11-02.
 */

public class DownloadService extends Service {
    private HitomiWebView webview;
    private HashMap<Integer, String> dataSet;
    private Notification.Builder mBuilder;
    private NotificationManager mNotificationManager;

    //한번만 실행된다. 그 이후는 바로 onStartCommand.
    @Override
    public void onCreate() {
        super.onCreate();
        dataSet = new HashMap<>();
        webview = HitomiWebView.getInstance();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        String plainGalleryUrl = bundle.getString("galleryUrl");
        String readerUrl = DownloadServiceDataParser.galleryUrlToReaderUrl(plainGalleryUrl);

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
