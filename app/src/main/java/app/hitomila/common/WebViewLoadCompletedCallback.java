package app.hitomila.common;

import app.hitomila.common.hitomi.HitomiData;

/**
 * Created by admin on 2016-11-01.
 */

public interface WebViewLoadCompletedCallback {
    void onCompleted(HitomiData data);
    void onStart();
}
