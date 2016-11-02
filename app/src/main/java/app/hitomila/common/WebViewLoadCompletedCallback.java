package app.hitomila.common;

import app.hitomila.common.hitomi.hitomiData;

/**
 * Created by admin on 2016-11-01.
 */

public interface WebViewLoadCompletedCallback {
    void onCompleted(hitomiData data);
    void onStart();
}
