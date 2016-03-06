package mitb.module.modules.googsearch.json;

import java.util.ArrayList;
import java.util.List;

public final class Cursor {

    private String resultCount;
    private List<Page> pages = new ArrayList<>();
    private String estimatedResultCount;
    private long currentPageIndex;
    private String moreResultsUrl;
    private String searchResultTime;

    public String getResultCount() {
        return resultCount;
    }

    public List<Page> getPages() {
        return pages;
    }

    public String getEstimatedResultCount() {
        return estimatedResultCount;
    }

    public long getCurrentPageIndex() {
        return currentPageIndex;
    }

    public String getMoreResultsUrl() {
        return moreResultsUrl;
    }

    public String getSearchResultTime() {
        return searchResultTime;
    }
}
