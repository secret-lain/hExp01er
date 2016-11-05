package app.hitomila.common.exception;

import android.util.Log;

/**
 * Created by admin on 2016-11-03.
 */

public class htmlParsingException extends RuntimeException {
    public htmlParsingException(String where, String happen){
        Log.e(where, happen);
    }
}
