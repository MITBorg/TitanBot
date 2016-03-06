package mitb.module.modules.googsearch.json;

public class Result {

    private String GsearchResultClass;
    private String unescapedUrl;
    private String url;
    private String visibleUrl;
    private String cacheUrl;
    private String title;
    private String titleNoFormatting;
    private String content;

    public String getContent() {
        return content;
    }

    public String getUnescapedUrl() {
        return unescapedUrl;
    }

    public String getUrl() {
        return url;
    }

    public String getVisibleUrl() {
        return visibleUrl;
    }

    public String getCacheUrl() {
        return cacheUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getTitleNoFormatting() {
        return titleNoFormatting;
    }

    public String getGsearchResultClass() {
        return GsearchResultClass;
    }
}
