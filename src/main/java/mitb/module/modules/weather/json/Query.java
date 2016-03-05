package mitb.module.modules.weather.json;

public final class Query {

    private long count;
    private String created;
    private String lang;
    private Results results;

    public long getCount() {
        return count;
    }
    public String getCreated() {
        return created;
    }
    public String getLang() {
        return lang;
    }
    public Results getResults() {
        return results;
    }
}
