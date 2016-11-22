package app.hitomila.main;

/**
 * Created by admin on 2016-11-21.
 */

public class ConnectUrlBuilder {
    private static final String prefix = "https://hitomi.la/";
    private static String currLocation = "index";
    private static String currLanguage = "all";
    private static int currIndex = 1;
    private static final String suffix = ".html";

    public static String getCurrLocation() {
        return currLocation;
    }

    public static String getCurrLanguage() {
        return currLanguage;
    }

    public static int getCurrIndex() {
        return currIndex;
    }

    public ConnectUrlBuilder setCurrLangauage(String language){
        currLanguage = language;
        return this;
    }

    public ConnectUrlBuilder setCurrLocation(String tag){
        currLocation = tag;
        return this;
    }

    public ConnectUrlBuilder setCurrIndex(int customIndex){
        currIndex = customIndex;
        return this;
    }

    public ConnectUrlBuilder increaseCurrIndex(){
        currIndex += 1;
        return this;
    }

    public ConnectUrlBuilder decreaseCurrIndex(){
        currIndex -= 1;
        return this;
    }

    public String build(){
        return prefix + currLocation + "-" + currLanguage + "-" + currIndex + suffix;
    }

    //build의 static판. 귀찮을때 쓴다.
    public static String getCurrUrl(){
        return prefix + currLocation + "-" + currLanguage + "-" + currIndex + suffix;
    }

}
