package app.hitomila.common.hitomi;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by admin on 2016-11-02.
 */

public class ReaderData implements hitomiData {
    public String title;
    //public String plainUrl;
    public Queue<String> images;

    public ReaderData(String _title){//, String _plainUrl
        title = _title;
       // plainUrl = _plainUrl;
        images = new LinkedList<>();
    }

    public void addImageUrl(String imageUrl){
        images.add(imageUrl);
    }

    public Queue<String> getImages(){
        return images;
    }

    public int getImageCount(){
        return images.size();
    }
}
