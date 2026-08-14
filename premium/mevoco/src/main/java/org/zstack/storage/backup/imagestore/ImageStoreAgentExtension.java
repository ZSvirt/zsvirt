package org.zstack.storage.backup.imagestore;

import org.zstack.header.core.Completion;
import org.zstack.premium.externalservice.loki.LokiGlobalProperty;
import org.zstack.premium.externalservice.loki.PromtailFactory;

/**
 * Created by mingjian.deng on 2019/9/11.
 */
public class ImageStoreAgentExtension implements ImageStoreExtensionPoint {
    @Override
    public void addMoreAgentInBackupStorage(ImageStoreBackupStorageVO vo, Completion completion) {
        if (LokiGlobalProperty.isLokiOn()) {
            new PromtailFactory().deployPromtailService(vo.getUuid(), vo.getHostname(), vo.getUsername(), vo.getPassword(), vo.getSshPort(), completion);
        } else {
            completion.success();
        }
    }
}
