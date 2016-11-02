package app.hitomila.common;

import android.app.Application;

/**
 * Created by admin on 2016-11-01.
 */

public class HitomiApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        HitomiWebView.init(this);
    }
}
