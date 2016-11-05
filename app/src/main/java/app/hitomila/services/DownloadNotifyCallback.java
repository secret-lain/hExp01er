package app.hitomila.services;

/**
 * Created by admin on 2016-11-04.
 */

public interface DownloadNotifyCallback {
    void notifyPageDownloaded(int notificationID);
    void notifyDownloadCompleted(int notificationID);
}
