package app.hitomila.common.exception;

import com.crashlytics.android.Crashlytics;

import app.hitomila.common.hitomi.HitomiData;
import app.hitomila.common.hitomi.HitomiDownloadingDataObject;
import app.hitomila.common.hitomi.ReaderData;

/**
 * Created by admin on 2016-11-15.
 */

public class CrashlyticsLoggingException extends Exception{
    public CrashlyticsLoggingException(String msg){
        System.out.println("Custom Log : " + msg);
        printStackTrace();
    }

    public CrashlyticsLoggingException(HitomiData data){
        if(data instanceof ReaderData){
            ReaderData readerData = (ReaderData)data;

            Crashlytics.setString("title", readerData.title);
            Crashlytics.setInt("imageCount", readerData.getImageCount());
        }
        if(data instanceof HitomiDownloadingDataObject){
            HitomiDownloadingDataObject dlData = (HitomiDownloadingDataObject)data;

            Crashlytics.setString("title", dlData.title);
            Crashlytics.setInt("maxPages", dlData.maxPages);
            Crashlytics.setInt("currentPages", dlData.currentPage);
            Crashlytics.setInt("galleryNumber",dlData.galleryNumber);
        }
    }
}
