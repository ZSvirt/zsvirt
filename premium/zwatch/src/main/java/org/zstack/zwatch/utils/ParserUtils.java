package org.zstack.zwatch.utils;

import org.apache.logging.log4j.util.Strings;
import org.zstack.zwatch.ruleengine.RuleEvaluationResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by kayo on 2018/10/9.
 */
public class ParserUtils {
    public static Map<String, String> parseIdentifyLabel(String identifyLabel) {
        String[] entries = identifyLabel.split("::");

        Map<String, String> map = new HashMap<>();

        for (String entry : entries) {
            List<String> keyPair = Arrays.stream(entry.split(":"))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            if (keyPair.size() != 2) continue;
            map.put(keyPair.get(0), keyPair.get(1));
        }

        return map;
    }

    public static String getResourceUuid(RuleEvaluationResult result, String identifyLabelName) {
        if (identifyLabelName == null) {
            return null;
        }

        if (!Strings.isNotEmpty(result.getIdentifyLabel())) {
            return null;
        }

        return ParserUtils.parseIdentifyLabel(result.getIdentifyLabel()).get(identifyLabelName);
    }
}
