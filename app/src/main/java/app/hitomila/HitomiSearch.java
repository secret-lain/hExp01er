package app.hitomila;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.bluelinelabs.logansquare.LoganSquare;
import com.crashlytics.android.Crashlytics;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

import app.hitomila.common.hitomiObjects.HitomiSearchData;
import app.hitomila.common.hitomiObjects.HitomiTagList;
import app.hitomila.downloadService.DownloadServiceDataParser;
import cz.msebera.android.httpclient.Header;

/**
 * Created by admin on 2016-11-20.
 * 태그데이터 및 망가데이터 json파일을 받아 파싱하고, 저장한다.
 * 또한 다양한 검색기능을 제공하는 클래스이다.
 *
 * 순서는 이렇다.
 *  - 태그정보 받기
 *  https://ltn.hitomi.la/tags.json 에 접속한후의 Response를 받은뒤 파싱만 하면 그만이다.
 *
 *  - 망가데이터 받기
 *  먼저 https://ltn.hitomi.la/searchlib.js 로 접속 후 var number_of_gallery_jsons[^\d]*([\d]+); 정규화검색
 *  해당 값을 최대로한 for(int i = 0 ; i < number_of_gallery_jsons ; i++) 를 삽입
 *  https://ltn.hitomi.la/galleries + i + .json 이 주소이다.
 *  모든 망가데이터는 전부 파싱해서 한군데에 다 때려넣는다.
 */

public class HitomiSearch {
    private static HitomiTagList tagList = null;
    //private static List<HitomiSearchData> list = new LinkedList<>();
    private static List<String> mangaList = new LinkedList<>();
    private static int searchDataListSize = 0;
    private static boolean isTagReady = false;
    private static boolean isSearchDataReady = false;
    public static boolean isReady(){
        isSearchDataReady = true;
        return (isTagReady && isSearchDataReady);
    }

    /**
     * 2016-11-21 망가리스트는 메모리 적재가 불가능할정도로 크기 때문에 다른 방법을 물색해본다.
     * (지금상황으로는 json0 다운 -> 표현 -> 다음페이지 다운 -> 표현 순서로 20개의 페이지를 만들 생각인데.)
     *
     *
     * */
    public static void init(Context mContext){
        initTags(mContext);
        //initMangaList(mContext);
    }

    public static List<String> getTagList(String type){
        List<String> result = null;
        switch(type){
            case "tag":
                result = new ArrayList<>();
                result.addAll(tagList.getFemale());
                result.addAll(tagList.getMale());
                break;
            case "artist":
                result = tagList.getArtist();
                break;
            case "character":
                result = tagList.getCharacter();
                break;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void initTags(final Context mContext){
        new AsyncTask(){
            @Override
            protected Object doInBackground(Object[] params) {
                SyncHttpClient client = new SyncHttpClient();
                client.setLoggingEnabled(false);
                client.get("https://ltn.hitomi.la/tags.json", new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        try {
                            String responseString = new String(responseBody);
                            tagList = LoganSquare.parse(responseString, HitomiTagList.class);
                            Log.d("MangaTagUpdate", "Tag Initializing Success");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        Crashlytics.log(3, "TagJsonWarning", "tag Json data receive failed");
                    }
                });
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                if(tagList != null){
                    isTagReady = true;
                    Toast.makeText(mContext, "태그 정보 초기화 완료", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();


    }

    /**
     * 망가데이터를 초기화한다. 만약 잘못되는 경우, 에러로그를 남긴 후 최대페이지를 20으로 가정한다.
     * (마지막으로 확인했을 때가 20이었다.)
     * */
    @SuppressWarnings("unchecked")
    private static void initMangaList(final Context mContext){
//        AsyncHttpClient client = new AsyncHttpClient();
        new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] params) {

                SyncHttpClient client = new SyncHttpClient();
                client.setLoggingEnabled(false);
                client.get("https://ltn.hitomi.la/searchlib.js", new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        String regex = "var number_of_gallery_jsons[^\\d]*([\\d]+);";
                        Matcher matcher = DownloadServiceDataParser.getMatcher(regex, new String(responseBody));

                        if(matcher.find()){
                            try{
                                parseMangaListInJsonRecursive(Integer.parseInt(matcher.group(1)), mContext);
                            } catch(NumberFormatException e){
                                Crashlytics.log(3, "SearchLibJs_regexWarning", "number_of_gallery_jsons is not integer!? json file count will be 20 forcefully");
                                parseMangaListInJsonRecursive(20, mContext);
                            }
                        } else{
                            Crashlytics.log(3, "SearchLibJs_regexWarning", "number_of_gallery_jsons not found! json file count will be 20 forcefully");
                            parseMangaListInJsonRecursive(20, mContext);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        Crashlytics.log(3, "SearchLibJsWarning", "searchLib connection failed. json file count will be 20 forcefully");
                        parseMangaListInJsonRecursive(20, mContext);
                    }
                });
                return null;
            }
        }.execute();


    }

    // 1. 재귀형태의 스트림으로 엮어서 하나씩 받는다
    // 2. 비동기형태로 받되 synchronized 함수로 추가한다.
    // 그러나 순서가 중요한 부분으로 작용하기 때문에 1번을 하는것으로 한다.
    @SuppressWarnings("unchecked")
    private static void parseMangaListInJsonRecursive(final int maxPages, final Context mContext) {
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                String prefix = "https://ltn.hitomi.la/galleries";
                String suffix = ".json";
//        AsyncHttpClient client = new AsyncHttpClient();
                for (int i = 0; i < maxPages; i++) {
                    final int pageNumber = i;
                    //AsyncHttpClient client = new AsyncHttpClient();
                    SyncHttpClient client = new SyncHttpClient();
                    client.setLoggingEnabled(false);
                    client.setTimeout(1000 * 120); // TimeOut Limit 2 min

                    client.get(prefix + String.valueOf(pageNumber) + suffix, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            int currentPageNumber = pageNumber;
                            String responseString = new String(responseBody);

                            //List<HitomiSearchData> listForAdd = LoganSquare.parseList(responseString, HitomiSearchData.class);
                            //addMangaList(listForAdd);
                            addMangaList(responseString, currentPageNumber);
                            //listForAdd.clear();

                            if (mangaList.size() >= maxPages) {
                                isSearchDataReady = true;
                                System.gc();
                                Toast.makeText(mContext, "검색 정보 초기화 완료", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d("MangaListUpdated", "Current Page : " + currentPageNumber);
                                //parseMangaListInJsonRecursive(maxPages);
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            Crashlytics.log(2, "MangaListJsonWarning", "galleries" + pageNumber + ".json file connection failed");
                            onRetry(pageNumber);
                        }
                    });
                }
                return null;
            }
        }.execute();
    }

    private static synchronized void addMangaList(final String jsonString, final int index){
        mangaList.add(index, jsonString);
        Log.d("MangaListStringUpdated", "Current Size : " + mangaList.size());
    }

    private static synchronized void addMangaList(final List<HitomiSearchData> toList){
        //list.addAll(toList);
        /*Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                //realm.insertOrUpdate(toList);
                for(HitomiSearchData data : toList){
                    data.setPrimaryKey(searchDataListSize++);
                    realm.copyToRealmOrUpdate(data);
                }
            }
        });*/

//        Log.d("MangaListUpdated", "Current Size : " + (searchDataListSize-1));
        //Log.d("MangaListUpdated", "Current Size : " + list.size());
    }



}
