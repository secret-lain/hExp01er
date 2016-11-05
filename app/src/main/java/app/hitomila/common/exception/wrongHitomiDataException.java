package app.hitomila.common.exception;

import android.util.Log;

/**
 * Created by admin on 2016-11-03.
 */

//잘못된 위치에서 데이터를 쓸때 발생한다. 개발자가 빡대가리인 것이 잘못.
public class wrongHitomiDataException extends RuntimeException{
    public wrongHitomiDataException(String where, String happen){
        Log.e(where, happen);
    }
}
