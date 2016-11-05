package app.hitomila.common;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.hitomila.common.exception.htmlParsingException;
import app.hitomila.common.hitomi.IndexData;
import app.hitomila.common.hitomi.ReaderData;
import app.hitomila.common.hitomi.HitomiData;
import app.hitomila.services.DownloadServiceDataParser;

/**
 * Created by admin on 2016-11-01.
 */
public class HitomiWebView {
    private static HitomiWebView ourInstance;
    public static HitomiWebView getInstance() {
        return new HitomiWebView();
    }

    private static Context applicationContext;
    private WebView webview;

    private HitomiWebView() {
        webview = new WebView(applicationContext);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url)
            {
             /* This call inject JavaScript into the page which just finished loading. */
                String pageTypeRegex = ".la\\/([^\\/]*)";
                Matcher matcher = Pattern.compile(pageTypeRegex).matcher(url);
                int result = -1;
                if(matcher.find()){
                    // 0 = 인덱스(리스트), 1 = 갤러리(선택화면), 2 = 리더(실행중)
                    String str = matcher.group(1);
                    if(str.equals("galleries")) result = 1;
                    else if(str.equals("reader")) result = 2;
                    else result = 0;
                } else result = -1; // 알수없음

                webview.loadUrl("javascript:window.HTMLOUT.processHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>', " + result + ");");
            }
        });
    }

    public static void init(Context context){
        applicationContext = context;
        ourInstance = new HitomiWebView();
    }

    WebViewLoadCompletedCallback callback;
    public void loadUrl(final String url, WebViewLoadCompletedCallback _callback){
        clear();
        this.webview.post(new Runnable() {
            @Override
            public void run() {
                webview.loadUrl(url);
            }
        });

        callback = _callback;
        callback.onStart();
    }
    public void clear(){
        webview.clearCache(true);
        webview.clearFormData();
        webview.clearHistory();
        webview.clearMatches();
        webview.loadUrl("about:blank");
    }


    class MyJavaScriptInterface
    {
        HitomiData resultData = null;
        @JavascriptInterface
        @SuppressWarnings("unused")
        public void processHTML(String html, int result)
        {
            if(callback != null) {
                switch(result){
                    //index
                    case 0:
                        getIndexData(html);
                        break;
                    //gallery
                    case 1:
                        break;
                    //reader
                    case 2:
                        getReaderData(html);
                        break;
                }

                callback.onCompleted(resultData);
                callback = null;
            }
        }

        private void getIndexData(String html){
            resultData = new IndexData();
            IndexData result = (IndexData) resultData;

            String indexCrawlRegex =
                    "<div class=\"([^\"]*)" +
                    "[^\\w]*a href=\"([^\"]*)" +
                    "\"[^\\r\\n]*[\\r\\n].*src=\"([^\"]*)" +
                    "(?:[^\\r\\n]*[\\r\\n]){2,4}.*html\">([^<]*)" +
                    "(?:[^\\r\\n]*[\\r\\n]){1,50}.*Language<\\/td><td>([^\\r\\n]*)";
            Matcher matcher = DownloadServiceDataParser.getMatcher(indexCrawlRegex, html);


            while(matcher.find()){
                String type = matcher.group(1);
                String plainUrl = matcher.group(2);
                String thumbnailUrl = matcher.group(3);
                String title = matcher.group(4);
                String lang = matcher.group(5);

                result.add(title, type, lang, plainUrl, thumbnailUrl);
            }
        }

        //이미 Javascript가 전부 실행된 후의 데이터이기 때문에 이미지출력 부분도 포함
        private void getReaderData(String html){
            String titleRegex = "<title>([^|]*)";
            String imageRegex = "(?:<div class=\"img-url\">)(.*)(?:<\\/div>)";
            String getPrefixRegex = "comicImages[^>]*.*src=\"\\/\\/([^\"]*).hitomi.la";

            String title;
            String prefix;

            try{
                Matcher matcher = DownloadServiceDataParser.getMatcher(titleRegex, html);
                //작품 타이틀을 찾는다. 이전 결과에서 다시 찾아올 수 있지만
                //결합성을 낮추기 위해 이렇게 해보았다.
                if(matcher.find())
                    title = matcher.group(1);
                else throw new htmlParsingException("getReaderData", "titleMatching");
                title = title.trim();

                //접두사를 찾는다. javascript 적용이 끝난 뒤 변환된 preload, 현재 img src를 통해
                //찾는다. 편법이긴 하지만 HTML 특성상 소스에서 적용되지 않으면 화면에 표시되지 않는다.
                matcher = DownloadServiceDataParser.getMatcher(getPrefixRegex, html);
                if(matcher.find())
                    prefix = matcher.group(1);
                else throw new htmlParsingException("getReaderData", "prefixGetting");

                resultData = new ReaderData(title);
                ReaderData resultReaderData = (ReaderData) resultData;

                //이미지 데이터는 카운팅, 파싱, 접두사 적용을 한번에 실시
                matcher = DownloadServiceDataParser.getMatcher(imageRegex, html);
                while(matcher.find()){
                    String imageUrl = matcher.group(1).replaceFirst("(//[^\\.]*)","https://" + prefix);
                    resultReaderData.addImageUrl(imageUrl);
                }
                if(resultReaderData.getImageCount() == 0) throw new htmlParsingException("getReaderData", "imageCounting");

            }catch(htmlParsingException e){
                Toast.makeText(applicationContext, "리더데이터 생성중 문제발생", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
            //여기까지 거쳤으면 readerData의 정보가 전부 차게 된다.
            //정보는 title(디렉토리 생성용), Queue<String> imageList(다운로드 접근용) 이다.
        }
    }
}
