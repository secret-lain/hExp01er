package app.hitomila.common.hitomi;


import org.apache.commons.lang3.StringEscapeUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by admin on 2016-11-01.
 */

public class IndexData implements HitomiData {
    public class node{
        public String title;
        public String type;
        public String plainUrl;
        public String thumbnailUrl;
        public String mangaLangugae;

        public node(String _title, String _type, String _mangaLangugae, String _plainUrl, String _thumbnailUrl){
            try {
                title = StringEscapeUtils.unescapeHtml4(URLDecoder.decode(_title.trim(),"UTF-8"));
                type = parseTypeString(_type);
                mangaLangugae  = parseLanguageString(_mangaLangugae);
                plainUrl = "https://hitomi.la" +  _plainUrl;
                thumbnailUrl =  "https:" + _thumbnailUrl;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    LinkedList<node> nodes;

    public IndexData(){
        nodes = new LinkedList<>();
    }

    public void add(String _title, String _type, String _mangaLangugae, String _plainUrl, String _thumbnailUrl){
        nodes.add(new node(_title, _type, _mangaLangugae, _plainUrl, _thumbnailUrl));
    }

    public node[] getDatas(){
        return nodes.toArray(new node[nodes.size()]);
    }

    private String parseTypeString(String type){
        switch(type){
            case "dj":
                return "동인지";
            case "acg":
                return "아티스트CG";
            case "cg":
                return "게임CG";
            case "manga":
                return "망가";
            default:
                return type;
        }
    }

    private String parseLanguageString(String Language){
        if(Language.contains("N/A")) return "N/A";
        else{
            String regex = ".html\">([^<]*)";
            Matcher matcher = Pattern.compile(regex).matcher(Language);

            if(matcher.find())
                return matcher.group(1);
            else
                return Language;
        }
    }
}
