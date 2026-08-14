package org.zstack.header.storage.primary;

import java.util.Date;
import java.util.List;

public class UsedPhysicalCapacityForecastsWithDates {
    List<Long> forecastList;
    List<Date> forecastFutureDateList;

    public UsedPhysicalCapacityForecastsWithDates(List<Long> forecastList, List<Date> forecastFutureDateList) {
        this.forecastList = forecastList;
        this.forecastFutureDateList = forecastFutureDateList;
    }

    public List<Long> getForecastList() {
        return forecastList;
    }

    public void setForecastList(List<Long> forecastList) {
        this.forecastList = forecastList;
    }

    public List<Date> getForecastFutureDateList() {
        return forecastFutureDateList;
    }

    public void setForecastFutureDateList(List<Date> forecastFutureDateList) {
        this.forecastFutureDateList = forecastFutureDateList;
    }
}
