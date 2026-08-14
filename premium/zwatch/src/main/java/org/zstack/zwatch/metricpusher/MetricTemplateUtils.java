package org.zstack.zwatch.metricpusher;

import groovy.lang.Writable;
import groovy.text.GStringTemplateEngine;
import groovy.text.Template;
import org.apache.commons.collections.map.HashedMap;
import org.zstack.header.exception.CloudRuntimeException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lining on 2020/5/7.
 */
public class MetricTemplateUtils {
    private static GStringTemplateEngine engine = new GStringTemplateEngine();
    private static Map<Integer, Template> templateMap = new HashMap<>();

    private static Template getTemplate(String templateText) {
        Template temp = templateMap.get(templateText.hashCode());
        try {
            if (temp == null) {
                temp = engine.createTemplate(new StringReader(templateText));
                templateMap.put(templateText.hashCode(), temp);
            }

            return temp;
        } catch (ClassNotFoundException | IOException e) {
            throw new CloudRuntimeException(e);
        }
    }

    public static String render(String templateText, Map<String, Object> params) {
        Template temp = getTemplate(templateText);
        Writable writable = temp.make(params);
        return writable.toString();
    }

    public static List<String> render(String templateText, List<Map<String, Object>> params) {
        List<String> result = new ArrayList<>();

        Template temp = getTemplate(templateText);
        for (Map<String, Object> param : params) {
            Writable writable = temp.make(param);
            result.add(writable.toString());
        }

        return result;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        String templateText = "{" +
                "  \"metricName\":\"${METRIC_NAME}\",\n" +
                "  \"regionId\":\"cn-shanghai\",\n" +
                "  \"instanceId\":\"${RESOURCE_UUID}\",\n" +
                "  \"resource\":\"vm/${RESOURCE_NAME}\",\n" +
                "  \"dimensions\":{\n" +
                "      \"instanceId\":\"${METRIC_LABLES.get('VMUuid')}\",\n" +
                "      \"device\":\"${METRIC_LABLES.get('DiskDeviceLetter')}\"\n" +
                "  },\n" +
                "  \"value\":${METRIC_VALUE},\n" +
                "  \"ts\":${METRIC_TIME}\n" +
                "}";
        Map<String, Object> params = new HashedMap();
        params.put("METRIC_NAME", "DiskReadOps");
        params.put("RESOURCE_UUID", "95e5885772de4f2bb3e05eba98a43cd0");
        params.put("RESOURCE_NAME", "newVm");

        Map<String, String> labels = new HashedMap();
        labels.put("VMUuid", "95e5885772de4f2bb3e05eba98a43cd0");
        labels.put("DiskDeviceLetter", "/sda");
        params.put("METRIC_LABLES", labels);
        params.put("METRIC_VALUE", 50f);
        params.put("METRIC_TIME", System.currentTimeMillis());

        /*
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100000; i ++) {
            String result = render(templateText, params);
            //params.put("RESOURCE_UUID", String.format("95e5885772de4f2bb3e05eba%s", i));
            //params.put("METRIC_VALUE", 1.0f * i);
            //System.out.println(result);
        }
        long endTime = System.currentTimeMillis();
        System.out.println(endTime - startTime);
        */

        List<String> metricPart = new ArrayList<>();
        metricPart.add(render(templateText, params));
        metricPart.add(render(templateText, params));
        System.out.println(metricPart.toString());

        /*
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100000; i ++) {
            JSONObjectUtil.toObject(render(templateText, params), Map.class);
        }
        long endTime = System.currentTimeMillis();
        System.out.println(endTime - startTime);
        */
    }
}
