package app.hitomila.common;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import app.hitomila.downloadService.DownloadServiceDataParser;

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
        webview.getSettings().setDatabaseEnabled(true);
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url)
            {
                //이 부분을 실행함으로서 자바스크립트 인터페이스를 실행한다.
                view.loadUrl("javascript:window.HTMLOUT.processHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");

            }
        });
    }

    public static void init(Context context){
        applicationContext = context;
        ourInstance = new HitomiWebView();
    }

    WebViewLoadCompletedCallback callback;
    public void loadUrl(final String url, WebViewLoadCompletedCallback _callback){
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
        @JavascriptInterface
        @SuppressWarnings("unused")
        public void processHTML(String html)
        {
            if(callback != null) {
                DownloadServiceDataParser.extractPrefixFromReaderPage(html);

                callback.onCompleted(DownloadServiceDataParser.prefix);
                callback = null;
            }
        }


    }
}
