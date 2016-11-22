package app.hitomila.common.hitomiObjects;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import java.util.List;

/**
 * Created by admin on 2016-11-20.
 *
 * List<HitomiSearchData> map =  LoganSquare.parseList(responseString, HitomiSearchData.class);
 *
 * HitomiSearchData는 하나의 망가데이터를 가지고 있다.
 * LoganSquare로 아예 리스트로 파싱한다. JsonArray와 같음.
 *
 * 이 리스트에 태그를 넣어서 검색하는 용도로 사용된다. 히토미라 전체 망가데이터가 json에 분할
 */

@JsonObject
public class HitomiSearchData{
    @JsonField(name = "a")
    public List<String> artists;

    @JsonField(name = "id")
    public String galleryNumber;

    @JsonField(name = "g")
    public List<String> groups;

    @JsonField(name = "n")
    public String title;

    @JsonField(name = "l")
    public String language;

    @JsonField(name = "t")
    public List<String> tags;

    @JsonField
    public String type;

}
