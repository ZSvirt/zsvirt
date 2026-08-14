package org.zstack.premium.externalservice.grafana.api;

import org.zstack.premium.externalservice.grafana.GridPos;

import java.util.List;
import java.util.Map;

/**
 * Created by mingjian.deng on 2019/8/21.
 */
public class GrafanaData {
    public class TemplateCurrent {
        public String text;
        public String value;
    }
    public class TemplateOption {
        public boolean selected;
        public String text;
        public String value;
    }
    public class Template {
        public int hide;
        public String label;
        public String name;
        public String query;
        public boolean skipUrlSync;
        public String type;
        public TemplateCurrent current;
        public List<TemplateOption> options;
    }
    public class SingleAnnotation {
        public int builtIn;
        public String datasource;
        public boolean enable;
        public boolean hide;
        public String iconColor;
        public String name;
        public String type;
    }
    public class Annotation {
        public List<SingleAnnotation> list;
    }
    public class GrafanaDataTime {
        public String from;
        public String to;
    }
    public class TimePicker {
        public boolean collapse;
        public boolean enable;
        public boolean notice;
        public boolean now;
        public String status;
        public String type;
        public List<String> refresh_intervals;
        public List<String> time_options;
    }
    public class Legend {
        boolean avg;
        boolean current;
        boolean max;
        boolean min;
        boolean show;
        boolean total;
        boolean values;
    }
    public class Target {
        public String expr;    // need modify
        public String format;
        public int intervalFactor;
        public String refId;
    }
    public class Tooltip {
        public boolean shared;
        public int sort;
        public String value_type;
    }
    public class Xaxis {
        public String buckets;   // null
        public String mode;
        public String name;     // null
        public boolean show;
        public List<Object> values;
    }
    public class Yaxes {
        public String format;
        public String label;    // null
        public int logBase;
        public String max;      // null
        public String min;      // null
        public boolean show;
    }
    public class Yaxis {
        public boolean align;
        public String alignLevel;   // null
    }
    public class SinglePanel {
        public Object aliasColors;
        public boolean bars;
        public int dashLength;
        public boolean dashes;
        public String datasource;
        public int fill;
        public GridPos gridPos;    // need modify
        public int id;             // need modify
        public Legend legend;
        public boolean lines;
        public int linewidth;
        public List<Object> links;
        public String nullPointMode;
        public Object options;
        public boolean percentage;
        public int pointradius;
        public boolean points;
        public String renderer;
        public List<Object> seriesOverrides;
        public int spaceLength;
        public boolean stack;
        public boolean steppedLine;
        public List<Object> thresholds;
        public String timeFrom;      // null
        public List<Object> timeRegions;
        public String timeShift;    // null
        public String title;     // need modify
        public List<Target> targets;
        public String type;      // 'row' / 'graph'
        public Tooltip tooltip;
        public Xaxis xaxis;
        public List<Yaxes> yaxes;
        public Yaxis yaxis;
        public List<SinglePanel> panels;
    }

    public Annotation annotations;
    public String description;
    public boolean editable;
    public String gnetId;      // null
    public int graphTooltip;
    public int id;
    public List<Object> links;
    public int schemaVersion;
    public String style;
    public List<String> tags;
    public Map<String, List<Template>> templating;
    public GrafanaDataTime time;
    public String timezone;
    public String title;
    public String uid;
    public int version;
    public TimePicker timepicker;
    public List<SinglePanel> panels;
}
