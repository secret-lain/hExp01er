package app.hitomila.downloadService;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;

import java.util.LinkedList;
import java.util.Queue;

import cz.msebera.android.httpclient.Header;

/**
 * Created by admin on 2016-11-02.
 * ReaderPage 에서 추출된 imageList를 가진채 생성되는 클래스.
 * imageList에 포함된 이미지주소에 전부 접근하여 다운로드한다.
 *
 * HitomiFileWriter와 종속성이 있다.
 */

public class ReaderDownloadClient extends AsyncHttpClient {
    private final String TAG = "ADownload::";
    private Context mContext;
    private Queue<String> imageUrlList = new LinkedList<>();
    private String[] allowedContentTypes = new String[]{"image/png", "image/jpeg", "image/gif"};
    private final int CONNECTION = 10;

    /*
    * 2016-11-14 업데이트.
    * setLoggingEnabled = false를 통해 Download Progress Log를 없앤다.
    * (너무 많이떠)
    * */
    public ReaderDownloadClient(Context context, Queue<String> imageList) {
        mContext = context;
        imageUrlList = imageList;
        this.setTimeout(60000);
        this.setMaxRetriesAndTimeout(3, 60000);
        this.setLoggingEnabled(false);
        this.setMaxConnections(CONNECTION);
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
                        if(isInterrupted)
                            callback.notifyDownloadFailed();
                        //TODO 필요한 코드인진 잘 모르겠다.
                        else if(--semaphore[0] <= 0)
                            callback.notifyDownloadCompleted();
                    }
                }


                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] binaryData, Throwable error) {
                    Log.d(TAG+"::download", this.getRequestURI().toString() + " download FAILED\n" +
                            "statusCode : " + statusCode + ", error : " + error.getMessage());
                    interrupt();
                    imageUrlList.clear();
                    callback.notifyDownloadFailed();

                    //TODO 다운로드가 실패했을 경우 중간에 잘라버리는 콜백함수를 구현하면 될듯
                }
            });
        }
    }

    private String extractImageNameFromUrl(String url){
        return url;
    }
}
