package mitb.module.modules.weather.json;

public final class Weather {

    public long id;
    public String main;
    public String description;
    public String icon;

    public long getId() {
        return id;
    }

    public String getMain() {
        return main;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }
}
