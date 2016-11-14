package app.hitomila.common.hitomi;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by admin on 2016-11-02.
 */

public class ReaderData implements HitomiData {
    public String title;
    //public String plainUrl;
    public Queue<String> images;

    public ReaderData(String _title){//, String _plainUrl
        try {
            title = StringEscapeUtils.unescapeHtml4(URLDecoder.decode(_title, "UTF-8"));
            // plainUrl = _plainUrl;
            images = new LinkedList<>();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

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
