package app.hitomila.services;

import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by admin on 2016-11-02.
 */

public class DownloadServiceDataParser {
    public static final String domain = "https://hitomi.la/";
    public static final String galleryDomain = domain + "galleries/";
    public static final String readerDomain = domain +  "reader/";

    public static Matcher getMatcher(String regex, String target){
        return Pattern.compile(regex).matcher(target);
    }

    public static String galleryUrlToReaderUrl(String plainGalleryUrl){
        String regex = "[^\\d]*([\\d]*)";
        Matcher matcher = getMatcher(regex, plainGalleryUrl);

        if(matcher.find()){
            return readerDomain + matcher.group(1) + ".html";
        }
        else
            return null;
    }

    public static boolean checkInvalidGalleryNumber(String galleryNumber){
        String regex = "[0-9]*";
        if(galleryNumber.matches(regex))
            return true;
        else return false;
    }

    //가장 뒤의 갤러리 주소를 가져온다.
    //갤러리 혹은 리더화면에서 가능하다.
    public static String extractGalleryNumberFromAddress(String addr){
        if(addr == null || !addr.contains("hitomi"))
            return null;

        String extractAddrRegex = "([\\d+]{0,7})(?:.html)";
        Pattern pattern = Pattern.compile(extractAddrRegex);
        Matcher match = pattern.matcher(addr);
        if(match.find()){
            return match.group(1);
        }
        else
            return null;
    }
}
