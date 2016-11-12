package app.hitomila.services;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;

import java.util.Queue;

import app.hitomila.common.HitomiWebView;
import cz.msebera.android.httpclient.Header;

/**
 * Created by admin on 2016-11-02.
 */

public class ReaderDownloadClient extends AsyncHttpClient {
    private final String TAG = "ADownload::";
    private Context mContext;
    private final Queue<String> imageUrlList;
    private String[] allowedContentTypes = new String[]{"image/png", "image/jpeg", "image/gif"};
    private final int CONNECTION = 5;

    public ReaderDownloadClient(Context context, Queue<String> imageList) {
        mContext = context;
        imageUrlList = imageList;
        this.setTimeout(20000);
        this.setResponseTimeout(20000);
        this.setConnectTimeout(20000);

        this.setMaxConnections(CONNECTION);
    }

    //맨앞 한장만 받는다. 쓸일이 있을지는 잘
    public byte[] downloadFirst() {
        HitomiWebView webView = HitomiWebView.getInstance();
        final byte[][] result = new byte[1][];

        //TODO Notification이 없어도 될까?

        get(imageUrlList.peek(), new BinaryHttpResponseHandler(allowedContentTypes) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] binaryData) {
                Log.d(TAG + "first", this.getRequestURI().toString() + " download completed");

                result[0] = binaryData;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] binaryData, Throwable error) {
                Log.d(TAG + "::first", this.getRequestURI().toString() + " download FAILED\n" +
                        "statusCode : " + statusCode + ", error : " + error.getMessage());
            }
        });

        return result[0];
    }


    private boolean isInterrupted = false;
    public void interrupt(){
        isInterrupted = true;
    }
    public void downloadAll(final DownloadFileWriteCallback callback) {
        //쓰레드는 개인이 설정할 수 없다. 코드상에서만 구현한다.
        if(isInterrupted)
            isInterrupted = false;
        final int threadCount = CONNECTION;
        final int[] semaphore = {threadCount};
        for(int i = 0 ; i < threadCount ; i++){
            get(imageUrlList.poll(), new BinaryHttpResponseHandler(allowedContentTypes) {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] binaryData) {
                    Log.d(TAG + "download", this.getRequestURI().toString() + " download completed");
                    if (!isInterrupted)
                        callback.notifyPageDownloaded(
                                extractImageNameFromUrl(DownloadServiceDataParser.extractImageName(this.getRequestURI().toString())),binaryData);

                    //다운로드 받아야할 img src가 남아있으면 돌린다.
                    if(!imageUrlList.isEmpty()){
                        try{
                            Thread.sleep(200);
                            if(!isInterrupted)
                                get(imageUrlList.poll(), this);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else{
                        //마지막에는 5개가 동시에 데이터를 가져가므로,
                        //5개의 큐가 전부 isEmpty를 반환한다. 이중 가장 마지막에 도착한 큐만
                        //notify를 할 수 있게 만든다.
                        if(--semaphore[0] <= 0)
                            callback.notifyDownloadCompleted();
                    }
                }


                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] binaryData, Throwable error) {
                    Log.d(TAG+"::download", this.getRequestURI().toString() + " download FAILED\n" +
                            "statusCode : " + statusCode + ", error : " + error.getMessage());
                    interrupt();
                    callback.notifyDownloadFailed();

                    //TODO 다운로드가 실패했을 경우 중간에 잘라버리는 콜백함수를 구현하면 될듯
                    //TODO HitomiDownloadDataObject에 실패한 페이지 갯수를 적는 란을 만들어서 실패현황을 알려주는 코드도 구현하면 좋을 듯
                }
            });
        }
    }

    private String extractImageNameFromUrl(String url){
        return url;
    }
}
