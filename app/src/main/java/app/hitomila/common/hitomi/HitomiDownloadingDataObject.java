package app.hitomila.common.hitomi;

import android.app.Notification;

/**
 * Created by admin on 2016-11-04.
 */

public class HitomiDownloadingDataObject implements HitomiData{
    private ReaderData readerData;

    public String title;
    public int maxPages;
    public int currentPage;
    public Notification.Builder notificationBuilder;
    public int galleryNumber;

    public HitomiDownloadingDataObject(ReaderData data, Notification.Builder notificationBuilder, int galleryNumber){
        readerData = data;
        title = data.title;
        maxPages = data.getImageCount();
        this.galleryNumber = galleryNumber;
        this.notificationBuilder = notificationBuilder;
        currentPage = 0;
    }
}
