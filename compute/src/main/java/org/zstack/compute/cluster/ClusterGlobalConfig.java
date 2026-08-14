package org.zstack.compute.cluster;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.resourceconfig.BindResourceConfig;

/**
 * Created by GuoYi on 3/13/18
 */
@GlobalConfigDefinition
public class ClusterGlobalConfig {
    public static final String CATEGORY = "cluster";

    @GlobalConfigValidation
    public static GlobalConfig CLUSTER_UPDATE_OS_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "update.os.parallelismDegree");

    @BindResourceConfig(value = {ClusterVO.class})
    @GlobalConfigValidation(validValues = {"true", "false"})
    @GlobalConfigDef(defaultValue = "false", type = Boolean.class, description = "enable zstack experimental repo")
    public static GlobalConfig ZSTACK_EXPERIMENTAL_REPO = new GlobalConfig(CATEGORY, "zstack.experimental.repo");

    @BindResourceConfig(value = {ClusterVO.class})
    @GlobalConfigDef(defaultValue = "", description = "dependencies need to be updated from experimental repo")
    public static GlobalConfig ZSTACK_EXPERIMENTAL_UPDATE_DEPENDENCY= new GlobalConfig(CATEGORY, "zstack.experimental.update.dependency");

    @BindResourceConfig(value = {ClusterVO.class})
    @GlobalConfigDef(defaultValue = "", description = "dependencies should not be updated from experimental repo")
    public static GlobalConfig ZSTACK_EXPERIMENTAL_EXCLUDE_DEPENDENCY= new GlobalConfig(CATEGORY, "zstack.experimental.exclude.dependency");
}
