package app.hitomila.deprecated;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeMap;
import java.util.regex.Matcher;

import app.hitomila.common.exception.CrashlyticsLoggingException;
import app.hitomila.common.hitomiObjects.HitomiTagInformation;
import app.hitomila.downloadService.DownloadServiceDataParser;
import cz.msebera.android.httpclient.Header;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by admin on 2016-11-15.
 * 태그별검색, 키워드검색을 위한 모든 데이터를 준비하는 클래스.
 *
 * 태그별검색
 * AsyncTask를 통해 모든 필요한 모든 태그페이지에 접속하여 파싱을 시도하며,
 * 한번 파싱한 결과는 7일간 저장한다.(IndexActivity에서 진행) 이전까지는 Realm에 저장된 태그데이터를사용.
 *
 * 키워드검색
 * search.html은 특이하게 최초에 모든 망가데이터를 json으로 보낸 후, 클라이언트에서 검색결과를
 * 추출해서 처리하는 느낌이었다.
 */

     //TODO get Hashes 구현하기 + 테스트해보기 + 쓰레드 init 연결하기 + Data Write 구현하기
     public class deprecatedTagDataController {
     static private boolean _isReady = false;
     static private TreeMap<String, String> tags = new TreeMap<>();
     static private TreeMap<String, String> artists = new TreeMap<>();
     static private TreeMap<String, String> characters = new TreeMap<>();

    public static void init(final Context mContext){
        Realm realm = Realm.getDefaultInstance();
        HitomiTagInformation information = realm.where(HitomiTagInformation.class).findFirst();
        long DAY_IN_MS = 1000 * 60 * 60 * 24; // 하루를 ms 로 나타낸 초
        long TODAY = System.currentTimeMillis() - DAY_IN_MS;

        // 첫 구동이거나 갱신일이 지난경우, 갱신일을 오늘로 지정한 후 새 업데이트를 진행한다.
        if(information == null || information.getUpdatedDate() <= TODAY) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.deleteAll();
                }
            });

            information = new HitomiTagInformation();
            information.setUpdatedDate(System.currentTimeMillis());
            final HitomiTagInformation finalInformation = information;
            realm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.copyToRealmOrUpdate(finalInformation);
                }
            });
            updateFromHitomi(mContext);
        }
        else{
            //갱신일도 지나지 않았고, 첫 구동도 아닌 경우 데이터베이스에서 정보를 불러온다.
            updateFromRealm(mContext);
        }
        realm.close();
    }

     /**
     * 태그데이터를 준비한다. 데이터베이스에 데이터가 있다는 가정하에
     * 해시맵을 초기화한다. 이 메소드는 단 한번만 동작한다.
     */
    @SuppressWarnings("unchecked")
    public static void updateFromRealm(final Context mContext) {
        if (_isReady)
            return;

        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                final Realm realm = Realm.getDefaultInstance();
                final RealmResults<deprecatedHitomiTagData> database = realm.where(deprecatedHitomiTagData.class).findAll();

                for (deprecatedHitomiTagData data : database) {
                    //1 tags, 2 artists, 3 characters
                    switch (data.getTypeInteger()) {
                        case 1:
                            tags.put(data.getKeyword(), data.getAddress());
                            break;
                        case 2:
                            artists.put(data.getKeyword(), data.getAddress());
                            break;
                        case 3:
                            characters.put(data.getKeyword(), data.getAddress());
                            break;
                    }
                }

                realm.close();
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                tagCountInit();
                _isReady = true;
                Toast.makeText(mContext, "태그 초기화 완료", Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    /**
     * 태그데이터를 준비한다. 갱신일이 지났다는 가정하에
     * 데이터를 크롤링하고, 데이터베이스에 데이터를 추가한다.
     * 이 메소드는 한번만 실행된다.
     */
    @SuppressWarnings("unchecked")
    public static void updateFromHitomi(final Context mContext) {
        if (_isReady)
            return;

        new AsyncTask() {
            Queue<String> failedAddressList = new LinkedList<>();
            final String suffix = ".html";
            final LinkedList<String> prefixList = new LinkedList<>(Arrays.asList("https://hitomi.la/alltags-", "https://hitomi.la/allartists-", "https://hitomi.la/allcharacters-"));
            final LinkedList<String> indexList = new LinkedList<>(Arrays.asList("123", "a", "b", "c", "d", "e",
                    "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"));
            final LinkedList<String> indexListForTagPage = new LinkedList<>(Arrays.asList("f", "m"));
            SyncHttpClient client = new SyncHttpClient();

            @Override
            protected Object doInBackground(Object[] params) {
                client.setMaxConnections(26);
                for (int i = 0; i < 3; i++) {
                    final String prefix = prefixList.get(i);
                    final int type = i + 1;

                    //artists, characters는 모든 페이지를 검사하고, tags만 f, m을 검사한다.
                    //female: | male: 만 검사하기 위함
                    LinkedList<String> usingIndexList = indexList;
                    if(type == 1)
                        usingIndexList = indexListForTagPage;


                    for (final String index : usingIndexList) {
                        client.get(prefix + index + suffix, new AsyncHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                                parseTagList(new String(responseBody), type);
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                                failedAddressList.add(prefix + index + suffix);
                            }
                        });
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    //실패한 찌끄래기들 한번더 시도해본다.
                    //이미 데이터는 준비되었다고 가정.
                    while (!failedAddressList.isEmpty()) {
                        String failedAddress = failedAddressList.poll();
                        new AsyncHttpClient().get(failedAddress, new AsyncHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                                parseTagList(new String(responseBody), type);
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                                //어쩔수 없지 뭐
                                Crashlytics.log(1, "deprecatedTagDataController", "태그페이지 접속 두번실패. 주소 : " + getRequestURI());
                            }
                        });
                    }
                }
                return null;
            }

            /*
            * 좀 더 깔끔한 파싱을 위해 먼저 Raw ResponseBody에서 태그테이블만 추출한다.
            * */
            private synchronized void parseTagList(String responseBody, final int type) {

                String firstRegex = "ul class=\"posts\">[\\r\\n]{1,3}(<li>[^\"]*\"[^\"]*\">[^<]*.*[\\r\\n]{1,3})*";
                final Matcher matcher = DownloadServiceDataParser.getMatcher(firstRegex, responseBody);

                String secondResponseBody = null;
                if (matcher.find()) {
                    secondResponseBody = matcher.group();
                    extractTag(secondResponseBody, type);
                } else{
                    Crashlytics.setString("cannotFind FirstResponseBody", responseBody);
                    Crashlytics.logException(new CrashlyticsLoggingException("parseTagList Error"));
                }
            }

            /*
            * 태그를 추출한다. Tags의 경우 경량화를 위해 (female: | male:) 태그만 추출하기로 한다.
            * type = 1의 경우가 Tags의 경우로, regex를 변경해서 추출한다.
            * */
            private synchronized void extractTag(String listedBody, int type) {
                Realm realm = Realm.getDefaultInstance();
                String secondRegex = "href=\"([^\"]*)\">([^<]*)";
                if(type == 1)
                    secondRegex = "href=\"([^\"]*)\">(?:female:|male:)([^<]*)";

                final Matcher second_matcher = DownloadServiceDataParser.getMatcher(secondRegex, listedBody);


                while (second_matcher.find()) {
                    //태그를 찾은 경우.
                    //group1이 주소, group2가 태그키워드가 될 것이다.
                    //데이터베이스에 넣는 동시에 해시맵에도 추가한다.
                    final deprecatedHitomiTagData data = new deprecatedHitomiTagData(second_matcher.group(2), second_matcher.group(1), type);
                    switch (data.getTypeInteger()) {
                        case 1:
                            tags.put(data.getKeyword(), data.getAddress());
                            break;
                        case 2:
                            artists.put(data.getKeyword(), data.getAddress());
                            break;
                        case 3:
                            characters.put(data.getKeyword(), data.getAddress());
                            break;
                    }
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            realm.copyToRealmOrUpdate(data);
                        }
                    });
                }
                realm.close();
            }

            @Override
            protected void onPostExecute(Object o) {
                tagCountInit();
                _isReady = true;
                Toast.makeText(mContext, "태그 초기화 완료", Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }



    private static void tagCountInit() {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                HitomiTagInformation information = realm.where(HitomiTagInformation.class).findFirst();

                information.setTagCount(tags.size());
                information.setArtistsCount(artists.size());
                information.setCharactersCount(characters.size());
            }
        });
        realm.close();
    }



    public static boolean isReady() {
        return _isReady;
    }
    public static TreeMap<String, String> getCharacters() {
        return characters;
    }
    public static TreeMap<String, String> getTags() {
        return tags;
    }
    public static TreeMap<String, String> getArtists() {
        return artists;
    }

}
