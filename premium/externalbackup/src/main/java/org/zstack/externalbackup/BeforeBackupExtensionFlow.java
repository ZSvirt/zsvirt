package org.zstack.externalbackup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCodeList;

import java.util.Map;

/**
 * Created by MaJin on 2020/8/19.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BeforeBackupExtensionFlow implements Flow {
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public boolean skip(Map data) {
        ExternalBackupSpec spec = (ExternalBackupSpec) data.get(ExternalBackupConstants.EXTERNAL_BACKUP_SPEC);
        return spec.isDryRun();
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        ExternalBackupSpec spec = (ExternalBackupSpec) data.get(ExternalBackupConstants.EXTERNAL_BACKUP_SPEC);
        new While<>(pluginRgty.getExtensionList(ExternalBackupExtensionPoint.class)).each((ext, compl) -> {
            ext.beforeBackup(spec, new NoErrorCompletion() {
                @Override
                public void done() {
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                trigger.next();
            }
        });
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        ExternalBackupSpec spec = (ExternalBackupSpec) data.get(ExternalBackupConstants.EXTERNAL_BACKUP_SPEC);
        new While<>(pluginRgty.getExtensionList(ExternalBackupExtensionPoint.class)).each((ext, compl) -> {
            ext.failToBackup(spec, new NoErrorCompletion() {
                @Override
                public void done() {
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                trigger.rollback();
            }
        });
    }
}
