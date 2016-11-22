package app.hitomila.downloadService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by admin on 2016-11-02.
 */

public class DownloadServiceDataParser {
    public static final String domain = "https://hitomi.la/";
    public static final String galleryDomain = domain + "galleries/";
    public static final String readerDomain = domain +  "reader/";
    public static String prefix = "";

    public static Matcher getMatcher(String regex, String target){
        return Pattern.compile(regex).matcher(target);
    }

    //갤러리 url을 통해 reader url을 만들어낸다.
    public static String galleryUrlToReaderUrl(String plainGalleryUrl){
        String regex = "[^\\d]*([\\d]*)";
        Matcher matcher = getMatcher(regex, plainGalleryUrl);

        if(matcher.find()){
            return readerDomain + matcher.group(1) + ".html";
        }
        else
            return null;
    }

    /*
    * 2016-11-14 업데이트.
    * hitomi.la/galleries/GALLERYNUMBER/파일명인데
    * 파일명을 짤라옴.
    * */
    public static String extractImageName(String imageUrl){
        //String regex = "([\\d]+.[\\w]+)$";
        String regex = "([^\\/]+.[\\w]+)$";
        Matcher matcher = getMatcher(regex, imageUrl);

        if(matcher.find()){
            return matcher.group(1);
        }
        else throw new RuntimeException("extractImageName throw Error T-T");
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

    public static boolean extractPrefixFromReaderPage(String html){
        String getPrefixRegex = "comicImages[^>]*.*src=\"\\/\\/([^\"]*).hitomi.la";

        Matcher matcher = DownloadServiceDataParser.getMatcher(getPrefixRegex, html);
        if(matcher.find()){
            prefix = matcher.group(1);
            return true;
        }
        else
            return false;
    }
}
