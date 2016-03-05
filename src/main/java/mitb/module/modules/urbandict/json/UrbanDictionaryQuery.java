package mitb.module.modules.urbandict.json;

import java.util.ArrayList;

public class UrbanDictionaryQuery {

    public java.util.List<String> tags = new ArrayList<String>();
    public String resultType;
    public java.util.List<mitb.module.modules.urbandict.json.List> list = new ArrayList<mitb.module.modules.urbandict.json.List>();
    public java.util.List<String> sounds = new ArrayList<String>();

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
