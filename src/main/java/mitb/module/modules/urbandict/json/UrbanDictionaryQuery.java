package mitb.module.modules.urbandict.json;

import java.util.ArrayList;

public final class UrbanDictionaryQuery {

    private final java.util.List<String> tags = new ArrayList<>();
    private String resultType;
    private final java.util.List<mitb.module.modules.urbandict.json.List> list = new ArrayList<>();
    private final java.util.List<String> sounds = new ArrayList<>();

    public java.util.List<String> getTags() {
        return tags;
    }

    public String getResultType() {
        return resultType;
    }

    public java.util.List<List> getList() {
        return list;
    }

    public java.util.List<String> getSounds() {
        return sounds;
    }
}
