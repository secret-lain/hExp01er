package app.hitomila.common;

import android.app.Application;

/**
 * Created by admin on 2016-11-01.
 */

public class hitomiApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        hitomiWebView.init(this);
    }
}
