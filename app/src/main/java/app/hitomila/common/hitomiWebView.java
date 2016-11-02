package app.hitomila.common;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.hitomila.common.hitomi.IndexData;
import app.hitomila.common.hitomi.hitomiData;

/**
 * Created by admin on 2016-11-01.
 */
public class HitomiWebView {
    private static HitomiWebView ourInstance;
    public static HitomiWebView getInstance() {
        return ourInstance;
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
    public void loadUrl(String url, WebViewLoadCompletedCallback _callback){
        webview.loadUrl(url);
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
        hitomiData resultData = null;
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
            Matcher matcher = Pattern.compile(indexCrawlRegex).matcher(html);


            while(matcher.find()){
                String type = matcher.group(1);
                String plainUrl = matcher.group(2);
                String thumbnailUrl = matcher.group(3);
                String title = matcher.group(4);
                String lang = matcher.group(5);

                result.add(title, type, lang, plainUrl, thumbnailUrl);
            }
        }
    }
}
