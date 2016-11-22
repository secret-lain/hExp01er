package app.hitomila.downloadService;

/**
 * Created by admin on 2016-11-04.
 */

public interface DownloadNotifyCallback {
    void notifyPageDownloaded();
    void notifyDownloadCompleted();
    void notifyDownloadFailed();
}
