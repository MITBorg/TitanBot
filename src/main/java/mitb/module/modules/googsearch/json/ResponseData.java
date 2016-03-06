package mitb.module.modules.googsearch.json;

import java.util.ArrayList;
import java.util.List;

public final class ResponseData {

    private List<Result> results = new ArrayList<>();
    private Cursor cursor;

    public List<Result> getResults() {
        return results;
    }

    public Cursor getCursor() {
        return cursor;
    }
}
