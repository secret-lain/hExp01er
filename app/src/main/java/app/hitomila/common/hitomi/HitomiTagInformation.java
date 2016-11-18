package app.hitomila.common.hitomi;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by admin on 2016-11-18.
 */


public class HitomiTagInformation extends RealmObject {
    @PrimaryKey
    int pkey = 0;

    //long 으로 넣은 currentMilliSecond를 보여줘야할것같다
    private long updatedDate;
    private int tagCount;
    private int artistsCount;
    private int charactersCount;

    public int getCharactersCount() {
        return charactersCount;
    }

    public void setCharactersCount(int charactersCount) {
        this.charactersCount = charactersCount;
    }

    public int getTagCount() {
        return tagCount;
    }

    public void setTagCount(int tagCount) {
        this.tagCount = tagCount;
    }

    public int getArtistsCount() {
        return artistsCount;
    }

    public void setArtistsCount(int artistsCount) {
        this.artistsCount = artistsCount;
    }

    public long getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(long updatedDate) {
        this.updatedDate = updatedDate;
    }
}
