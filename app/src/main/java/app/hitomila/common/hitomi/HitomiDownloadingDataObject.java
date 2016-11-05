package app.hitomila.common.hitomi;

import android.app.Notification;

/**
 * Created by admin on 2016-11-04.
 */

public class HitomiDownloadingDataObject {
    private ReaderData readerData;

    public String title;
    public int maxPages;
    public int currentPage;
    public Notification notification;
    public int galleryNumber;

    public HitomiDownloadingDataObject(ReaderData data, Notification notification, int galleryNumber){
        readerData = data;
        title = data.title;
        maxPages = data.getImageCount();
        this.galleryNumber = galleryNumber;
        this.notification = notification;
        currentPage = 0;
    }
}
