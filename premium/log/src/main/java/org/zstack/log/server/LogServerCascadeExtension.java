package org.zstack.log.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.log.LogConfigurationFactory;
import org.zstack.log.LogConfigurationManager;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LogServerCascadeExtension extends AbstractAsyncCascadeExtension {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private LogConfigurationManager manager;

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE) ||
                action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleCleanDeletion(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleCleanDeletion(CascadeAction action, Completion completion) {
        if (LogServerVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<LogServerInventory> logServerInventories = action.getParentIssuerContext();
            List<String> logServerUuids = logServerInventories.stream()
                    .map(LogServerInventory::getUuid)
                    .collect(Collectors.toList());
            logServerUuids.forEach(this::deleteLogServers);
        }

        completion.success();
    }

    private void deleteLogServers(String uuid) {
        LogServerVO vo = dbf.findByUuid(uuid, LogServerVO.class);
        if (vo == null) {
            return;
        }
        LogConfigurationFactory factory = manager.getLogConfigurationFactory(vo.getType().toString().toLowerCase());
        factory.deleteLogConfiguration(vo.getUuid());
        dbf.remove(vo);
    }

    @Override
    public List<String> getEdgeNames() {
        return Collections.emptyList();
    }

    @Override
    public String getCascadeResourceName() {
        return LogServerVO.class.getSimpleName();
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        return null;
    }
}
