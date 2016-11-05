package app.hitomila.services;

/**
 * Created by admin on 2016-11-04.
 */

public interface DownloadFileWriteCallback {
    void notifyPageDownloaded(String imageFileName,byte[] binaryImageData);
    void notifyDownloadCompleted();
}
