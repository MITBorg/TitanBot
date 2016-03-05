
package mitb.module.modules.weather.json;

import javax.annotation.Generated;

@Generated("org.jsonschema2pojo")
public class Atmosphere {

    private String humidity;
    private String pressure;
    private String rising;
    private String visibility;

    /**
     * 
     * @return
     *     The humidity
     */
    public String getHumidity() {
        return humidity;
    }

    /**
     * 
     * @param humidity
     *     The humidity
     */
    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    /**
     * 
     * @return
     *     The pressure
     */
    public String getPressure() {
        return pressure;
    }

    /**
     * 
     * @param pressure
     *     The pressure
     */
    public void setPressure(String pressure) {
        this.pressure = pressure;
    }

    /**
     * 
     * @return
     *     The rising
     */
    public String getRising() {
        return rising;
    }

    /**
     * 
     * @param rising
     *     The rising
     */
    public void setRising(String rising) {
        this.rising = rising;
    }

    /**
     * 
     * @return
     *     The visibility
     */
    public String getVisibility() {
        return visibility;
    }

    /**
     * 
     * @param visibility
     *     The visibility
     */
    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

}
