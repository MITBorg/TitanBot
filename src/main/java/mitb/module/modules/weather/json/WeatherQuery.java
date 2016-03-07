package mitb.module.modules.weather.json;

import java.util.ArrayList;
import java.util.List;

public final class WeatherQuery {

    public Coord coord;
    public List<Weather> weather = new ArrayList<Weather>();
    public String base;
    public Main main;
    public Wind wind;
    public Clouds clouds;
    public long dt;
    public Sys sys;
    public long id;
    public String name;
    public long cod;

    public Coord getCoord() {
        return coord;
    }

    public List<Weather> getWeather() {
        return weather;
    }

    public String getBase() {
        return base;
    }

    public Main getMain() {
        return main;
    }

    public Wind getWind() {
        return wind;
    }

    public Clouds getClouds() {
        return clouds;
    }

    public long getDt() {
        return dt;
    }

    public Sys getSys() {
        return sys;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getCod() {
        return cod;
    }

}
