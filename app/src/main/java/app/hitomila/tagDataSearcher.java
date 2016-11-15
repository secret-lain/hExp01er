package app.hitomila;

import com.crashlytics.android.Crashlytics;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.regex.Matcher;

import app.hitomila.common.exception.CrashlyticsLoggingException;
import app.hitomila.services.DownloadServiceDataParser;
import cz.msebera.android.httpclient.Header;

/**
 * Created by admin on 2016-11-15.
 * 얘가 하는 일은
 * hitomi.la/alltags- 다~양
 * hitomi.la/allartists- 다~양
 * hitomi.la/allcharatcers- 다~양
 * 처럼 태그별, 작가별, 캐릭터별 등등의 데이터들을 긁어 모아주는 역할을 한다.
 *
 * 쓰레드에 프로퍼티를 설정하고, 프로퍼티에 맞춰 위의 주소를 정제할 준비를 한다.
 * 주소가 준비가 되면 인덱스에 맞춰서 iterator로 진행.
 * onFailure의 경우는 상정하지 않고 보고만 하는 SyncedHttpClient를 사용해서 데이터를 파싱해 온다.
 * 하지만 앱 실행마다 모든 태그페이지 크롤링은 히토미 페이지에 부하를 줄 수 있으므로 한번 한 후
 * 클라이언트에 데이터를 저장하는 것으로 한다.
 *
 */

//TODO get Hashes 구현하기 + 테스트해보기 + 쓰레드 init 연결하기 + Data Write 구현하기
public class tagDataSearcher {
    private boolean isReady = false;
    private TreeMap<String, String> tags;
    private TreeMap<String, String> artists;
    private TreeMap<String, String> characters;
    private final LinkedList<String> index = new LinkedList<>(Arrays.asList("123","a","b","c","d","e",
            "f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"));
    private String[] language = {"korean","japanese","chinese","english"};

    public void init(){
        //TODO 살려주세요
    }

    private class initThread extends Thread{
        private String prefix;
        private int property;
        private final String suffix = ".html";
        private final LinkedList<String> index = new LinkedList<>(Arrays.asList("123","a","b","c","d","e",
                "f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"));

        //where == tags, artists, characters
        public initThread(String where){
            prefix = "https://hitomi.la/all" + where + "-";
            switch(where){
                case "tags":
                    property = 1;
                    break;
                case "artists":
                    property = 2;
                    break;
                case "characters":
                    property = 3;
                    break;
                default:
                    property = -1;
                    break;
            }
        }

        private void executeCrawl(){
            SyncHttpClient client = new SyncHttpClient();

            //일어나면 안되는데?
            //Property가 태그도, 아티스트도, 캐릭터도 아닌 경우 발생
            if(property == -1){
                Crashlytics.log(3,"tagCrawl","property is -1");
                Crashlytics.setString("prefix", prefix);
                return;
            }


            final int[] maxCountOut = {5};
            final boolean[] breaker = {false};
            for(String item : index){
                if(breaker[0])break;
                client.get(prefix + item + suffix, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        extractTags(new String(responseBody));
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        if(--maxCountOut[0] < 0){
                            breaker[0] = true;
                        }
                    }
                });
            }
            if( maxCountOut[0] <= 0 ){
                Crashlytics.log(4, "tagInitializing" , "tag index access countout is upper 5. check address");
                Crashlytics.logException(new CrashlyticsLoggingException("tag init Error"));
            }
        }

        private void extractTags(String responseBody){
            String firstRegex = "ul class=\"posts\">[\\r\\n]{1,3}(<li>[^\"]*\"[^\"]*\">[^<]*.*[\\r\\n]{1,3})*";
            Matcher matcher = DownloadServiceDataParser.getMatcher(firstRegex, responseBody);

            String secondResponseBody = null;
            if(matcher.find()){
                secondResponseBody = matcher.group(1);
            }

            if(secondResponseBody == null){
                Crashlytics.setString("cannotFind FirstResponseBody", responseBody);
                Crashlytics.logException(new CrashlyticsLoggingException("extractTags Error"));
            } else{
                String secondRegex = "href=\"([^\"]*)\">([^<]*)";
                matcher = DownloadServiceDataParser.getMatcher(secondRegex, secondResponseBody);
                while(matcher.find()){
                    switch(property){
                        case 1:
                            tags.put(matcher.group(2), matcher.group(1));
                            break;
                        case 2:
                            artists.put(matcher.group(2), matcher.group(1));
                            break;
                        case 3:
                            characters.put(matcher.group(2), matcher.group(1));
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        @Override
        public synchronized void start() {
            executeCrawl();
            super.start();
        }
    }
}
