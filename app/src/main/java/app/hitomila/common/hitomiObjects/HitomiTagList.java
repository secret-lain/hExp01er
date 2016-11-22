package app.hitomila.common.hitomiObjects;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by admin on 2016-11-20.
 *
 * HttpClient().get("https://ltn.hitomi.la/tags.json) then
 * HitomiTagList list = LoganSquare.parse(responseString, HitomiTagList.class);
 *
 * 태그는 태그그룹명:[{태그정보},{태그정보}.....}] 로 이루어져있다.
 * 그룹명 단위로 리스트를 가지게 될 것이며, 실제로 사용될때는
 * 필드명:태그명 으로 사용되는것이 일반적이다. 하지만 검색알고리즘은 내가 알아서 짜야 한다.
 */

@JsonObject
public class HitomiTagList {
    public List<String> getGroup() {
        List<String> titleList = new ArrayList<>();
        for(HitomiTagData item : group){
            titleList.add(item.toString());
        }
        return titleList;
    }

    public List<String> getMale() {
        List<String> titleList = new ArrayList<>();
        for(HitomiTagData item : male){
            titleList.add("male: " + item.toString());
        }
        return titleList;
    }

    public List<String> getSeries() {
        List<String> titleList = new ArrayList<>();
        for(HitomiTagData item : series){
            titleList.add(item.toString());
        }
        return titleList;
    }

    public List<String> getTag() {
        List<String> titleList = new ArrayList<>();
        for(HitomiTagData item : tag){
            titleList.add(item.toString());
        }
        return titleList;
    }

    public List<String> getCharacter() {
        List<String> titleList = new ArrayList<>();
        for(HitomiTagData item : character){
            titleList.add(item.toString());
        }
        return titleList;
    }

    public List<String> getLanguage() {
        List<String> titleList = new ArrayList<>();
        for(HitomiTagData item : language){
            titleList.add(item.toString());
        }
        return titleList;
    }

    public List<String> getArtist() {
        List<String> titleList = new ArrayList<>();
        for(HitomiTagData item : artist){
            titleList.add(item.toString());
        }
        return titleList;
    }

    public List<String> getFemale() {
        List<String> titleList = new ArrayList<>();
        for(HitomiTagData item : female){
            titleList.add("female: " + item.toString());
        }
        return titleList;
    }

    @JsonField
    List<HitomiTagData> group;

    @JsonField
    List<HitomiTagData> male;

    @JsonField
    List<HitomiTagData> series;

    @JsonField
    List<HitomiTagData> tag;

    @JsonField
    List<HitomiTagData> character;

    @JsonField
    List<HitomiTagData> language;

    @JsonField
    List<HitomiTagData> artist;

    @JsonField
    List<HitomiTagData> female;
}
