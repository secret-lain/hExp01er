package app.hitomila.services;

import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;

import app.hitomila.R;
import app.hitomila.common.HitomiWebView;
import app.hitomila.common.WebViewLoadCompletedCallback;
import app.hitomila.common.exception.wrongHitomiDataException;
import app.hitomila.common.hitomi.HitomiDownloadingDataObject;
import app.hitomila.common.hitomi.ReaderData;
import app.hitomila.common.hitomi.HitomiData;
import app.hitomila.common.hitomi.HitomiFileWriter;

/**
 * Created by admin on 2016-11-02.
 */

public class DownloadService extends Service {
    //DataSet 은 GalleryNumber(for NotificationID), notification 을 가지며, 현재 진행중인 다운로드 알림들을 표시
    private HashMap<Integer, HitomiDownloadingDataObject> dataSet;
    private Notification.Builder mBuilder;
    private NotificationManager mNotificationManager;

    //한번만 실행된다. 그 이후는 바로 onStartCommand.
    @Override
    public void onCreate() {
        super.onCreate();
        dataSet = new HashMap<>();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        final String plainGalleryUrl = bundle.getString("galleryUrl");
        final String readerUrl = DownloadServiceDataParser.galleryUrlToReaderUrl(plainGalleryUrl);

        //백그라운드에서 다운로드를 진행한다.
        new AsyncTask<Void, Void, Boolean>() {

            int galleryNumber;

            @Override
            protected void onPreExecute() {
                galleryNumber = Integer.parseInt(DownloadServiceDataParser.extractGalleryNumberFromAddress(readerUrl));
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                HitomiWebView webview = HitomiWebView.getInstance();

                webview.loadUrl(readerUrl, new WebViewLoadCompletedCallback() {
                    @Override
                    public void onCompleted(HitomiData data) {
                        //TODO notification 실시간 동작을 위한 DataSet 추가
                        if (!(data instanceof ReaderData))
                            throw new wrongHitomiDataException("DownloadService", "reader 페이지 수신 완료 -> readerData가 아님");

                        ReaderData readerData = (ReaderData) data;

                        HitomiFileWriter fileWriter = new HitomiFileWriter(DownloadService.this, (ReaderData)data);
                        Notification currNotification = initNotification(galleryNumber, fileWriter.getFilePath(), readerData.title, readerData.getImageCount());
                        HitomiDownloadingDataObject item = new HitomiDownloadingDataObject((ReaderData)data, currNotification, galleryNumber);

                        //재활용을 위해 해시에 넣어둔다.
                        dataSet.put(galleryNumber, item);



                        //정상적으로 정보를 수신한 후에는 FileWriter에 정보를 넘긴다
                        //다운로드 큐를 돌리는것과 저장하는것은 FileWriter가 해준다.
                        fileWriter.downloadAll();

                    }

                    @Override
                    public void onStart() {
                        //Toast.makeText(DownloadService.this, "다운로드를 시작합니다", Toast.LENGTH_SHORT).show();
                        String a = "a";
                    }
                });
                return null;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
            }
        }.execute();


        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //해당 망가의 진행상황을 확인시켜줄 노티피케이션의 초기화
    //노티피케이션의 고유 ID는 해당 작품의 번호로 한다.(동일 작품을 두번 받지 못하게 하려고)
    private Notification initNotification(int galleryNumber, String directoryName, String mangaTitle, int maxPages){


        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        File root = new File(directoryName);
        Uri uri = Uri.fromFile(root);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "resource/folder");
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setAction(Intent.ACTION_GET_CONTENT);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(ListActivity.class);
        stackBuilder.addNextIntent(intent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(galleryNumber, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.hitomi_32x32)
                .setAutoCancel(false)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setPriority(Notification.PRIORITY_MAX)
                .setWhen(System.currentTimeMillis())
                .setOngoing(true)
                //.setStyle(new Notification.BigTextStyle().bigText("BigText"))
                .setContentIntent(resultPendingIntent)
                .setContentTitle(mangaTitle)
                .setContentText("0" + " / " + maxPages);

//        mNotificationManager.notify(item.notificationID, mBuilder.build());
        return mBuilder.build();
    }

    public interface notificationCallback {
        void notifyPageDownloaded(int notificationID);
        void notifyDownloadCompleted(int notificationID);
    }

   /* notificationCallback notificationCallback = new notificationCallback() {
        @Override
        public void notifyPageDownloaded(int notificationID) {
            mangaInformationData item = dataSet.get(notificationID);
            item.currDownloadedPages += 1;
            mBuilder
                    //.setContentIntent(item.fileLocationPendingIntent)
                    .setContentTitle(item.mangaTitle)
                    .setContentText(item.currDownloadedPages + " / " + item.maxPages);

            mNotificationManager.notify(item.notificationID, mBuilder.build());
        }

        @Override
        public void notifyDownloadCompleted(int notificationID) {
            mangaInformationData item = dataSet.get(notificationID);

            mBuilder
                    .setAutoCancel(false)
                    .setDefaults(Notification.DEFAULT_LIGHTS)
                    .setPriority(Notification.PRIORITY_LOW)
                    .setWhen(System.currentTimeMillis())
                    .setOngoing(false)
                    .setContentTitle(item.mangaTitle)
                    .setContentText("Download done - " + item.currDownloadedPages + " / " + item.maxPages);

            mNotificationManager.notify(item.notificationID, mBuilder.build());
        }
    };*/
}
