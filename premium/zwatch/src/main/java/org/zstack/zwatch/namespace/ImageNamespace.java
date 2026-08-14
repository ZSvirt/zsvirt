package org.zstack.zwatch.namespace;

import org.zstack.header.image.ImageVO;
import org.zstack.zwatch.datatype.metric.CountMetric;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.metric.PercentMetric;
import org.zstack.zwatch.driver.DatabaseDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ImageNamespace extends AbstractNamespace {
    public static final String NAME = "Image";

    private static final List<Metric> metrics = new ArrayList<>();
    protected static final List<String> disableMetrics = getDisableMetrics(NAME);

    public static final Metric TotalImageCount = new CountMetric("TotalImageCount", metrics, false);
    public static final Metric ReadyImageCount = new CountMetric("ReadyImageCount", metrics, false);
    public static final Metric ReadyImageInPercent = new PercentMetric("ReadyImageInPercent", metrics, false);
    public static final Metric RootVolumeTemplateCount = new CountMetric("RootVolumeTemplateCount", metrics, false);
    public static final Metric RootVolumeTemplateInPercent = new PercentMetric("RootVolumeTemplateInPercent", metrics,
            false
    );
    public static final Metric DataVolumeTemplateCount = new CountMetric("DataVolumeTemplateCount", metrics, false);
    public static final Metric DataVolumeTemplateInPercent = new PercentMetric("DataVolumeTemplateInPercent", metrics,
            false
    );
    public static final Metric ISOCount = new CountMetric("ISOCount", metrics, false);
    public static final Metric ISOInPercent = new PercentMetric("ISOInPercent", metrics, false);

    public ImageNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public ImageNamespace() {
        super();
    }

    @Override
    protected String getSubNamespaceName() {
        return NAME;
    }

    @Override
    public List<Metric> getMetrics() {
        return metrics.stream().filter(m ->!disableMetrics.contains(m.getName())).collect(Collectors.toList());
    }

    @Override
    public List<EventFamily> getEvents() {
        return null;
    }

    @Override
    public String getResourceType() {
        return ImageVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return null;
    }
}
