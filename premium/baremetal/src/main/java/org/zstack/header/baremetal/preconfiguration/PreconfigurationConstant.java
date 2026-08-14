package org.zstack.header.baremetal.preconfiguration;

import java.util.Arrays;
import java.util.List;

/**
 * Created by GuoYi on 2018-12-26.
 */
public interface PreconfigurationConstant {
    String SERVICE_ID = "baremetal.preconfiguration";
    String ACTION_CATEGORY = "baremetal.preconfiguration";

    String predefinedTemplateFolder = "preconfigurationTemplates";

    int contentMaxLength = 16777215;            // max 16MB template content
    int customParamNameMaxLength  = 255;        // max 255B custom param name
    int customParamValueMaxLength = 65535;      // max 64KB custom param value

    // special parameters for some of the pre-configurations
    List<String> specialParameters = Arrays.asList(
            "IMAGE_UUID"
    );

    // common parameters for all of the pre-configurations
    List<String> commonParameters = Arrays.asList(
            "REPO_URL",
            "USERNAME",
            "PASSWORD",
            "NETWORK_CFGS",
            "FORCE_INSTALL",
            "PRE_SCRIPTS",
            "POST_SCRIPTS"
    );
}