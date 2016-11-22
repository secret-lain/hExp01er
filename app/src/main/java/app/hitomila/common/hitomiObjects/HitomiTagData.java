package app.hitomila.common.hitomiObjects;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

/**
 * Created by admin on 2016-11-20.
 * 태그는 태그그룹명:[{태그정보},{태그정보}.....}] 로 이루어져있다.
 * 그렇기 때문에 {태그정보}를 하나의 클래스로 묶는 용도로 사용된다.
 * 사실상 count는 사용되지 않는다. 아마 나중에 줄세우기 할때 쓰지 않을까?
 */

@JsonObject
public class HitomiTagData {
    @JsonField(name = "t")
    int count;

    @JsonField(name = "s")
    String name;

    @Override
    public String toString() {
        return name;
    }
}
