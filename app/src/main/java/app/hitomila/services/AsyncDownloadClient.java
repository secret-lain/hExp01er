package app.hitomila.services;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;

import java.util.Queue;

import app.hitomila.common.HitomiWebView;
import app.hitomila.common.WebViewLoadCompletedCallback;
import app.hitomila.common.hitomi.hitomiData;
import app.hitomila.common.hitomi.hitomiFileWriter;
import cz.msebera.android.httpclient.Header;

/**
 * Created by admin on 2016-11-02.
 */

public class AsyncDownloadClient extends AsyncHttpClient {
    private Context mContext;
    private String[] allowedContentTypes = new String[]{"image/png", "image/jpeg", "image/gif"};

    public AsyncDownloadClient(Context context){
        mContext = context;
    }

    /*public void download(String readerUrl){
        HitomiWebView webView = HitomiWebView.getInstance();
        final Queue<String> imageList = null;

        webView.loadUrl(readerUrl, new WebViewLoadCompletedCallback() {
            @Override
            public void onCompleted(hitomiData data) {
                imageList = DownloadServiceDataParser.extractImageList(responseBody)
            }

            @Override
            public void onStart() {

            }
        });

        final String mangaTitle = hitomiParser.parseTitleFromReader(responseBody);
        final hitomiFileWriter writer = new hitomiFileWriter(mContext, mangaTitle);
        if(isInterrupted == true)
            isInterrupted = false;

        callback.initNotification(mangaTitle, imageList.size(), notificationID);
        //최초에 maxConnection 만큼 get request를 일단 던진다. 그 이후에는 재귀적호출. (계속 max유지)
        final int[] completedSemaphore = {getMaxConnections()};
        for(int i = 0 ; i < getMaxConnections() ; i++){
            get(imageList.poll() , new BinaryHttpResponseHandler(allowedContentTypes) {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] binaryData) {
                    Log.d(TAG+"::download", this.getRequestURI().toString() + " download completed");
                    String fileName = hitomiParser.getImageNameFromRequestURI(this.getRequestURI().toString());
                    if(!isInterrupted && writer.writeImage(fileName,binaryData))
                        callback.notifyPageDownloaded(notificationID);

                    if(!imageList.isEmpty()){
                        try {
                            Thread.sleep(500);
                            if(!isInterrupted)
                                get(imageList.poll(), this);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else{
                        //3개의 커넥션 중 마지막 결과값만 notifyCompleted하기 위함
                        if(--completedSemaphore[0] <= 0)
                            callback.notifyDownloadCompleted(notificationID);
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] binaryData, Throwable error) {
                    Log.d(TAG+"::download", this.getRequestURI().toString() + " download FAILED\n" +
                            "statusCode : " + statusCode + ", error : " + error.getMessage());
                }
            });
        }
        //이건 변수 하나 지정해서 notify 하고 해당 notify에서 로그박는걸로.
        //Log.d("hitomiClient::download", "download process DONE.");
    }*/


}
