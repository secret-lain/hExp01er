package app.hitomila.common;

import android.app.Activity;
import android.content.Context;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import app.hitomila.MainActivity;

/**
 * Created by admin on 2016-11-01.
 */
public class hitomiWebView {
    private static hitomiWebView ourInstance;
    public static hitomiWebView getInstance() {
        return ourInstance;
    }

    private static Context applicationContext;
    private WebView webview;

    private hitomiWebView() {
        webview = new WebView(applicationContext);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url)
            {
             /* This call inject JavaScript into the page which just finished loading. */
             webview.loadUrl("javascript:window.HTMLOUT.processHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
            }
        });
    }

    public static void init(Context context){
        applicationContext = context;
        ourInstance = new hitomiWebView();
    }

    public void loadUrl(String url){
        webview.loadUrl(url);
    }

    class MyJavaScriptInterface
    {
        @JavascriptInterface
        @SuppressWarnings("unused")
        public void processHTML(String html)
        {
            System.out.println(html);
            // process the html as needed by the app
        }
    }
}
