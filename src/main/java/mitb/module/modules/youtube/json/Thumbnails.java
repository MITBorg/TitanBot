package mitb.module.modules.youtube.json;

public final class Thumbnails {

    public Default _default;
    public Medium medium;
    public High high;
    public Standard standard;
    public Maxres maxres;

    public Default get_default() {
        return _default;
    }

    public Medium getMedium() {
        return medium;
    }

    public High getHigh() {
        return high;
    }

    public Standard getStandard() {
        return standard;
    }

    public Maxres getMaxres() {
        return maxres;
    }
}
