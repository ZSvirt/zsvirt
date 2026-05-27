package org.zstack.log.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.jsonlabel.JsonLabelVO;
import org.zstack.core.jsonlabel.JsonLabelVO_;
import org.zstack.header.Component;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.log.LogConfigurationStruct;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;


public class LogServerUpgradeExtension implements Component {
    private static final CLogger logger = Utils.getLogger(LogServerUpgradeExtension.class);

    @Override
    public boolean start() {
        if (LogServerGlobalProperty.UPGRADE_LOG_LABEL_TO_LOG_SERVER) {
            upgradeJsonLabelToLogServer();
        }
        return true;
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private void upgradeJsonLabelToLogServer() {
        List<JsonLabelVO> labelVOS = Q.New(JsonLabelVO.class)
                .like(JsonLabelVO_.labelKey, "log4j2-%")
                .list();
        List<LogServerVO> logServerVOS = new ArrayList<>();
        List<AccountResourceRefVO> resourceRefs = new ArrayList<>();
        List<JsonLabelVO> migratedLabels = new ArrayList<>();
        for (JsonLabelVO label : labelVOS) {
            LogConfigurationStruct struct = JSONObjectUtil.toObject(label.getLabelValue(), LogConfigurationStruct.class);
            if (Q.New(LogServerVO.class)
                    .eq(LogServerVO_.uuid, struct.getUuid())
                    .isExists()) {
                migratedLabels.add(label);
                continue;
            }
            LogServerVO logServerVO = new LogServerVO();
            logServerVO.setUuid(struct.getUuid());
            logServerVO.setName(struct.getName());
            logServerVO.setDescription(struct.getDescription());
            logServerVO.setCategory(LogCategory.ManagementNodeLog);
            logServerVO.setType(LogType.Log4j2);
            if (StringUtils.isNotEmpty(struct.getLevel())) {
                logServerVO.setLevel(LogLevel.valueOf(struct.getLevel()));
            }
            logServerVO.setConfiguration(renameAppenderType(struct.getConfiguration()));
            logServerVO.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
            logServerVOS.add(logServerVO);
            resourceRefs.add(AccountResourceRefVO.forOwner(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, struct.getUuid(), LogServerVO.class));
            migratedLabels.add(label);
        }
        new SQLBatch() {
            @Override
            protected void scripts() {
                logServerVOS.forEach(this::persist);
                resourceRefs.forEach(this::persist);
                migratedLabels.forEach(this::remove);
            }
        }.execute();
    }

    private String renameAppenderType(String configuration) {
        try {
            ObjectNode node = (ObjectNode) MAPPER.readTree(configuration);
            if (node.has("appenderType")) {
                node.set("type", node.get("appenderType"));
                node.remove("appenderType");
            }
            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            logger.warn("Failed to rename appenderType in configuration, using original", e);
            return configuration;
        }
    }

    @Override
    public boolean stop() {
        return true;
    }
}
