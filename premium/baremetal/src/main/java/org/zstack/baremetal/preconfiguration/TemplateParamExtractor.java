package org.zstack.baremetal.preconfiguration;

import org.zstack.header.baremetal.preconfiguration.PreconfigurationConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by GuoYi on 2018-12-28.
 */
public class TemplateParamExtractor {
    private static final CLogger logger = Utils.getLogger(TemplateParamExtractor.class);

    class Result {
        private boolean success = true;
        private Set<String> params;
        private String error;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public Set<String> getParams() {
            return params;
        }

        public void setParams(Set<String> params) {
            this.params = params;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    Result extractParams(String content) {
        Result result = new Result();
        Result commonResult = extractCommonParams(content);
        Result customResult = extractCustomParams(content);
        if (commonResult.success && customResult.success) {
            return result;
        }

        result.success = false;
        result.error = commonResult.success ? customResult.error : commonResult.error;
        return result;
    }

    // extract common parameters like `{{ REPO_URL }}` from template content
    Result extractCommonParams(String content) {
        Result result = new Result();
        Set<String> params = new HashSet<>();

        Pattern pattern = Pattern.compile("\\{\\{([^a-z\n]*?)\\}\\}");
        Matcher matcher = pattern.matcher(content);
        while(matcher.find()) {
            if (matcher.groupCount() == 0) continue;
            String param = matcher.group(1).trim();
            params.add(param);
        }

        if (!params.containsAll(PreconfigurationConstant.commonParameters)) {
            result.success = false;
            result.error = "common params missing in template content";
        }

        if (params.size() > PreconfigurationConstant.specialParameters.size() +
                PreconfigurationConstant.commonParameters.size()) {
            result.success = false;
            result.error = "unknown common params exist in template content";
        }

        if (result.success) {
            result.params = params;
            logger.debug(String.format("extracted %d common params from template content", params.size()));
        }
        return result;
    }

    // extract custom parameters like `{{ hostname }}` from template content
    Result extractCustomParams(String content) {
        Result result = new Result();
        Set<String> params = new HashSet<>();

        Pattern pattern = Pattern.compile("\\{\\{([^A-Z\n]*?)\\}\\}");
        Matcher matcher = pattern.matcher(content);
        while(matcher.find()) {
            if (matcher.groupCount() == 0) continue;
            String param = matcher.group(1).trim();
            if (param.length() > PreconfigurationConstant.customParamNameMaxLength) {
                result.success = false;
                result.error = String.format("custom param %s is too long", param);
            }
            params.add(param);
        }

        if (result.success) {
            result.params = params;
            logger.debug(String.format("extracted %d custom params from template content", params.size()));
        }

        return result;
    }
}
