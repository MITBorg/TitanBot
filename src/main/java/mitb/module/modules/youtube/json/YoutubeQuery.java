package mitb.module.modules.youtube.json;

import java.util.ArrayList;
import java.util.List;

public final class YoutubeQuery {

    public String kind;
    public String etag;
    public PageInfo pageInfo;
    public List<Item> items = new ArrayList<Item>();

    public String getKind() {
        return kind;
    }

    public String getEtag() {
        return etag;
    }

    public PageInfo getPageInfo() {
        return pageInfo;
    }

    public List<Item> getItems() {
        return items;
    }
}
