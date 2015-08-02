package xml.model;

import java.io.Serializable;

/**
 * Created by ekaterina on 04.07.2015.
 */
public class Feature implements Serializable {
    private String name;
    private double value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getValue() {
        return value;
    }

    public double getValueWithoutNaN() {
        return Double.isNaN(value) ? 0.0 : value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
