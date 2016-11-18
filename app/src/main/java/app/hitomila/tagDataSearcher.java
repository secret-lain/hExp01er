package app.hitomila;

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
import app.hitomila.common.hitomi.HitomiTagData;
import app.hitomila.common.hitomi.HitomiTagInformation;
import app.hitomila.services.DownloadServiceDataParser;
import cz.msebera.android.httpclient.Header;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by admin on 2016-11-15.
 * 얘가 하는 일은
 * hitomi.la/alltags- 다~양
 * hitomi.la/allartists- 다~양
 * hitomi.la/allcharatcers- 다~양
 * 처럼 태그별, 작가별, 캐릭터별 등등의 데이터들을 긁어 모아주는 역할을 한다.
 * <p>
 * 쓰레드에 프로퍼티를 설정하고, 프로퍼티에 맞춰 위의 주소를 정제할 준비를 한다.
 * 주소가 준비가 되면 인덱스에 맞춰서 iterator로 진행.
 * onFailure의 경우는 상정하지 않고 보고만 하는 SyncedHttpClient를 사용해서 데이터를 파싱해 온다.
 * 하지만 앱 실행마다 모든 태그페이지 크롤링은 히토미 페이지에 부하를 줄 수 있으므로 한번 한 후
 * 클라이언트에 데이터를 저장하는 것으로 한다.
 */

//TODO get Hashes 구현하기 + 테스트해보기 + 쓰레드 init 연결하기 + Data Write 구현하기
public class TagDataSearcher {
    static private boolean _isReady = false;
    static private TreeMap<String, String> tags = new TreeMap<>();
    static private TreeMap<String, String> artists = new TreeMap<>();
    static private TreeMap<String, String> characters = new TreeMap<>();
    static Context mContext;
    //private String[] language = {"korean", "japanese", "chinese", "english"};

    /**
     * 태그데이터를 준비한다. 데이터베이스에 데이터가 있다는 가정하에
     * 해시맵을 초기화한다. 이 메소드는 단 한번만 동작한다.
     */
    @SuppressWarnings("unchecked")
    public static void init(final Context mContext) {
        if (_isReady)
            return;

        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                final Realm realm = Realm.getDefaultInstance();
                final RealmResults<HitomiTagData> database = realm.where(HitomiTagData.class).findAll();

                for (HitomiTagData data : database) {
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
    public static void update(final Context mContext) {
        if (_isReady)
            return;

        new AsyncTask() {
            Queue<String> failedAddressList = new LinkedList<>();
            final String suffix = ".html";
            final LinkedList<String> prefixList = new LinkedList<>(Arrays.asList("https://hitomi.la/alltags-", "https://hitomi.la/allartists-", "https://hitomi.la/allcharacters-"));
            final LinkedList<String> indexList = new LinkedList<>(Arrays.asList("123", "a", "b", "c", "d", "e",
                    "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"));
            SyncHttpClient client = new SyncHttpClient();

            @Override
            protected Object doInBackground(Object[] params) {
                client.setMaxConnections(10);
                for (int i = 0; i < 3; i++) {
                    final String prefix = prefixList.get(i);
                    final int type = i + 1;
                    for (final String index : indexList) {
                        client.get(prefix + index + suffix, new AsyncHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                                extractTags(new String(responseBody), type);
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
                                extractTags(new String(responseBody), type);
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                                //어쩔수 없지 뭐
                                Crashlytics.log(1, "TagDataSearcher", "태그페이지 접속 두번실패. 주소 : " + getRequestURI());
                            }
                        });
                    }
                }
                return null;
            }

            private synchronized void extractTags(String responseBody, final int type) {
                Realm realm = Realm.getDefaultInstance();
                String firstRegex = "ul class=\"posts\">[\\r\\n]{1,3}(<li>[^\"]*\"[^\"]*\">[^<]*.*[\\r\\n]{1,3})*";
                final Matcher matcher = DownloadServiceDataParser.getMatcher(firstRegex, responseBody);

                String secondResponseBody = null;
                if (matcher.find()) {
                    secondResponseBody = matcher.group();
                }

                if (secondResponseBody == null) {
                    Crashlytics.setString("cannotFind FirstResponseBody", responseBody);
                    Crashlytics.logException(new CrashlyticsLoggingException("extractTags Error"));
                } else {
                    String secondRegex = "href=\"([^\"]*)\">([^<]*)";
                    final Matcher second_matcher = DownloadServiceDataParser.getMatcher(secondRegex, secondResponseBody);


                    while (second_matcher.find()) {
                        //태그를 찾은 경우.
                        //group1이 주소, group2가 태그키워드가 될 것이다.
                        //데이터베이스에 넣는 동시에 해시맵에도 추가한다.
                        final HitomiTagData data = new HitomiTagData(second_matcher.group(2), second_matcher.group(1), type);
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
        /*new Thread(new Runnable() {
            Queue<String> failedAddressList = new LinkedList<>();
            final String suffix = ".html";
            final LinkedList<String> prefixList = new LinkedList<>(Arrays.asList("https://hitomi.la/alltags-", "https://hitomi.la/allartists-", "https://hitomi.la/allcharacters-"));
            final LinkedList<String> indexList = new LinkedList<>(Arrays.asList("123", "a", "b", "c", "d", "e",
                    "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"));
            SyncHttpClient client = new SyncHttpClient();

            @Override
            public void run() {
                client.setMaxConnections(10);
                for (int i = 0; i < 3; i++) {
                    final String prefix = prefixList.get(i);
                    final int type = i + 1;
                    for (final String index : indexList) {
                        client.get(prefix + index + suffix, new AsyncHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                                extractTags(new String(responseBody), type);
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
                                extractTags(new String(responseBody), type);
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                                //어쩔수 없지 뭐
                                Crashlytics.log(1, "TagDataSearcher", "태그페이지 접속 두번실패. 주소 : " + getRequestURI());
                            }
                        });
                    }
                }
            }

            private synchronized void extractTags(String responseBody, final int type) {
                Realm realm = Realm.getDefaultInstance();
                String firstRegex = "ul class=\"posts\">[\\r\\n]{1,3}(<li>[^\"]*\"[^\"]*\">[^<]*.*[\\r\\n]{1,3})*";
                final Matcher matcher = DownloadServiceDataParser.getMatcher(firstRegex, responseBody);

                String secondResponseBody = null;
                if (matcher.find()) {
                    secondResponseBody = matcher.group();
                }

                if (secondResponseBody == null) {
                    Crashlytics.setString("cannotFind FirstResponseBody", responseBody);
                    Crashlytics.logException(new CrashlyticsLoggingException("extractTags Error"));
                } else {
                    String secondRegex = "href=\"([^\"]*)\">([^<]*)";
                    final Matcher second_matcher = DownloadServiceDataParser.getMatcher(secondRegex, secondResponseBody);


                    while (second_matcher.find()) {
                        //태그를 찾은 경우.
                        //group1이 주소, group2가 태그키워드가 될 것이다.
                        //데이터베이스에 넣는 동시에 해시맵에도 추가한다.
                        final HitomiTagData data = new HitomiTagData(second_matcher.group(2), second_matcher.group(1), type);
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
                }
                realm.close();
            }
        }).start();*/
    }

    /**
     * 해당 태그페이지의 모든 태그데이터를 HitomiTagData화 시킨다. 데이터는 바로 Realm에 추가된다.
     */
    /*private static synchronized void extractTags(String responseBody, final int type) {
        Realm realm = Realm.getDefaultInstance();
        String firstRegex = "ul class=\"posts\">[\\r\\n]{1,3}(<li>[^\"]*\"[^\"]*\">[^<]*.*[\\r\\n]{1,3})*";
        final Matcher matcher = DownloadServiceDataParser.getMatcher(firstRegex, responseBody);

        String secondResponseBody = null;
        if (matcher.find()) {
            secondResponseBody = matcher.group();
        }

        if (secondResponseBody == null) {
            Crashlytics.setString("cannotFind FirstResponseBody", responseBody);
            Crashlytics.logException(new CrashlyticsLoggingException("extractTags Error"));
        } else {
            String secondRegex = "href=\"([^\"]*)\">([^<]*)";
            final Matcher second_matcher = DownloadServiceDataParser.getMatcher(secondRegex, secondResponseBody);


            while (second_matcher.find()) {
                //태그를 찾은 경우.
                //group1이 주소, group2가 태그키워드가 될 것이다.
                //데이터베이스에 넣는 동시에 해시맵에도 추가한다.
                final HitomiTagData data = new HitomiTagData(second_matcher.group(2), second_matcher.group(1), type);
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
        }
        realm.close();
    }*/
    private static void tagCountInit() {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
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
