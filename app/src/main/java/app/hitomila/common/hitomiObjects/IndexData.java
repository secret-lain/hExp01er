package app.hitomila.common.hitomiObjects;


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
            title = _title.trim();
            type = parseTypeString(_type);
            mangaLangugae  = parseLanguageString(_mangaLangugae);
            plainUrl = "https://hitomi.la" +  _plainUrl;
            thumbnailUrl =  "https:" + _thumbnailUrl;
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
