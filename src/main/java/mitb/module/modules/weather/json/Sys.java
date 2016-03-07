package mitb.module.modules.weather.json;

public final class Sys {

    public double message;
    public String country;
    public long sunrise;
    public long sunset;

    public double getMessage() {
        return message;
    }

    public String getCountry() {
        return country;
    }

    public long getSunrise() {
        return sunrise;
    }

    public long getSunset() {
        return sunset;
    }
}
