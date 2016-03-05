package mitb.module.modules.urbandict.json;

import java.util.ArrayList;

public class UrbanDictionaryQuery {

    public final java.util.List<String> tags = new ArrayList<>();
    public String resultType;
    public final java.util.List<mitb.module.modules.urbandict.json.List> list = new ArrayList<>();
    public final java.util.List<String> sounds = new ArrayList<>();

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
