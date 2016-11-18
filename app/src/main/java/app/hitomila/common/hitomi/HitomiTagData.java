package app.hitomila.common.hitomi;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

/**
 * Created by admin on 2016-11-18.
 * type 1 = tag
 * type 2 = artists
 * type 3 = characters
 */

public class HitomiTagData extends RealmObject {
    @Ignore
    private String domain = "https://hitomi.la";

    private int type;
    private String address;

    @Index
    @PrimaryKey
    private String keyword;

    public HitomiTagData(){}
    public HitomiTagData(String keyword, String address, int type){
        this.keyword = keyword;
        this.address = address;
        this.type = type;
    }

    public String getType() {
        switch(type) {
            case 1:
                return "tags";
            case 2:
                return "artists";
            case 3:
                return "characters";
            default:
                return null;
        }
    }

    public int getTypeInteger(){
        return type;
    }

    public String getAddress() {
        return domain + address;
    }

    public String getKeyword() {
        return keyword;
    }

}
