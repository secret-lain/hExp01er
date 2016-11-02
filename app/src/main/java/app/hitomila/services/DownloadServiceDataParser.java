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
            return matcher.group(1);
        }
        else
            return null;
    }

    //고정값에 대해서는 https://hitomi.la/reader.js
    public static Queue<String> extractImageList(String responseBody){
        //TODO response HTML 에서 프리로드쪽이나 현재이미지 쪽 파싱해서 가져오면 됨.
        throw new RuntimeException();
        //return null;
    }


    public static boolean checkGallery(String galleryNumber){
        String regex = "[0-9]*";
        if(galleryNumber.matches(regex))
            return true;
        else return false;
    }

    public static String getImageNameFromRequestURI(String requestURI){
        String extractImageNameRegex = "(?:galleries\\/\\d+\\/)(.*)";
        Pattern pattern = Pattern.compile(extractImageNameRegex);
        Matcher match = pattern.matcher(requestURI);

        if(match.find()){
            return match.group(1);
        }
        return null;
    }

    public static String parseTitleFromReader(String responseBody) {
        String extractImageNameRegex = "(?:<title>)([^|]*)";
        Pattern pattern = Pattern.compile(extractImageNameRegex);
        Matcher match = pattern.matcher(responseBody);

        if(match.find()){
            return match.group(1);
        }
        return null;
    }

    public static String parseFileNameToTwoDigits(String filename){
        if(isOneDigit(filename))
            return "0" + filename;
        return filename;
    }
    private static boolean isOneDigit(String filename){
        String extractImageNameRegex = "[^\\.]*";
        Pattern pattern = Pattern.compile(extractImageNameRegex);
        Matcher match = pattern.matcher(filename);

        if(match.find())
            if(match.group(0).length() == 1) return true;
        return false;
    }

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
