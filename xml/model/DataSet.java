package xml.model;

import java.util.List;

/**
 * Created by ekaterina on 04.07.2015.
 */
public class DataSet {
    private String dataSetId;
    private List<Feature> features;

    public String getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(String dataSetId) {
        this.dataSetId = dataSetId;
    }

    public List<Feature> getFeatures() {
        return features;
    }

    public void setFeatures(List<Feature> features) {
        this.features = features;
    }
}
