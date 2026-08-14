package org.zstack.baremetal.instance;

import org.apache.commons.io.FileUtils;
import org.zstack.baremetal.BaremetalUtils;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.Component;
import org.zstack.header.baremetal.BaremetalConstant;
import org.zstack.header.baremetal.instance.BaremetalInstanceStaticConfigManager;
import org.zstack.header.baremetal.instance.BaremetalInstanceStatus;
import org.zstack.header.baremetal.instance.BaremetalInstanceVO;
import org.zstack.header.baremetal.instance.BaremetalInstanceVO_;
import org.zstack.header.core.BypassWhenUnitTest;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by GuoYi on 2018-10-17.
 */
public class BaremetalInstanceStaticConfigManagerImpl implements BaremetalInstanceStaticConfigManager, ManagementNodeReadyExtensionPoint, ManagementNodeChangeListener, Component {
    private static final CLogger logger = Utils.getLogger(BaremetalInstanceStaticConfigManagerImpl.class);

    @Override
    @BypassWhenUnitTest
    public void writeNoVNCProxy(String bmUuid) {
        if (!PathUtil.exists(BaremetalConstant.NGINX_CONF_PATH)) {
            new File(BaremetalConstant.NGINX_CONF_PATH).mkdirs();
        }

        String pxeServerUuid = getPxeServerIp(bmUuid);
        if (pxeServerUuid == null) {
            return;
        }

        File file = new File(PathUtil.join(BaremetalConstant.NGINX_CONF_PATH, bmUuid));
        String content = String.format(
                "location /%s { proxy_pass http://%s:%d/; proxy_http_version 1.1; proxy_set_header Upgrade $http_upgrade; proxy_set_header Connection $connection_upgrade; }",
                bmUuid, pxeServerUuid, BaremetalConstant.WEBSOCKIFY_PORT
        );

        try {
            FileUtils.write(file, content);
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }

        BaremetalUtils.reloadNginxService();
        logger.info(String.format("successfully created novnc proxy for baremetal instance[uuid:%s]", bmUuid));
    }

    @Override
    @BypassWhenUnitTest
    public void writeTerminalProxy(String bmUuid) {
        if (!PathUtil.exists(BaremetalConstant.NGINX_CONF_PATH)) {
            new File(BaremetalConstant.NGINX_CONF_PATH).mkdirs();
        }

        String pxeServerUuid = getPxeServerIp(bmUuid);
        if (pxeServerUuid == null) {
            return;
        }

        File file = new File(PathUtil.join(BaremetalConstant.NGINX_CONF_PATH, bmUuid));
        String content = String.format(
                "location /%s { proxy_pass http://%s:%d/%s/; }",
                bmUuid, pxeServerUuid, BaremetalConstant.NGINX_TERMINAL_PROXY_PORT, bmUuid
        );

        try {
            FileUtils.write(file, content);
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }

        BaremetalUtils.reloadNginxService();
        logger.info(String.format("successfully created web terminal proxy for bm instance[uuid:%s]", bmUuid));
    }

    @Override
    @BypassWhenUnitTest
    public void deleteNginxProxy(String bmUuid) {
        File file = new File(PathUtil.join(BaremetalConstant.NGINX_CONF_PATH, bmUuid));
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                BaremetalUtils.reloadNginxService();
                logger.info(String.format("successfully deleted nginx proxy for baremetal instance[uuid:%s]", bmUuid));
            } else {
                logger.error(String.format("failed to delete nginx proxy for baremetal instance[uuid:%s]", bmUuid));
            }
        }
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
        createNoVNCProxyForBaremetalInstance();
        createTerminalProxyForBaremetalInstance();
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        createNoVNCProxyForBaremetalInstance();
        createTerminalProxyForBaremetalInstance();
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {

    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {

    }

    @Override
    public void managementNodeReady() {
        cleanUpNginxProxyForBaremetalInstance();
    }

    private void cleanUpNginxProxyForBaremetalInstance() {
        File dir = new File(BaremetalConstant.NGINX_CONF_PATH);
        if (!dir.exists()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files != null) {
            new SQLBatch() {
                @Override
                protected void scripts() {
                    for (File f : files) {
                        String uuid = f.getName();
                        if (!StringDSL.isZStackUuid(uuid)) {
                            throw new CloudRuntimeException(String.format("%s is not a zstack uuid, %s", uuid, f.getAbsolutePath()));
                        }

                        if (!Q.New(BaremetalInstanceVO.class).eq(BaremetalInstanceVO_.uuid, uuid).isExists()) {
                            deleteNginxProxy(uuid);
                        }
                    }
                }
            }.execute();
        }
    }

    private void createNoVNCProxyForBaremetalInstance() {
        new SQLBatchWithReturn<List<Runnable>>() {
            @Override
            protected List<Runnable> scripts() {
                List<Runnable> runnables = new ArrayList<>();

                long count = q(BaremetalInstanceVO.class)
                        .eq(BaremetalInstanceVO_.status, BaremetalInstanceStatus.Provisioning)
                        .count();
                sql("select bm from BaremetalInstanceVO bm where bm.status = :status", BaremetalInstanceVO.class)
                        .param("status", BaremetalInstanceStatus.Provisioning)
                        .limit(1000)
                        .paginate(count, bms -> bms.forEach(bm -> {
                            BaremetalInstanceVO vo = (BaremetalInstanceVO) bm;
                            runnables.add(() -> writeNoVNCProxy(vo.getUuid()));
                        }));
                return runnables;
            }
        }.execute().forEach(Runnable::run);
    }

    private void createTerminalProxyForBaremetalInstance() {
        new SQLBatchWithReturn<List<Runnable>>() {
            @Override
            protected List<Runnable> scripts() {
                List<Runnable> runnables = new ArrayList<>();

                long count = q(BaremetalInstanceVO.class)
                        .eq(BaremetalInstanceVO_.status, BaremetalInstanceStatus.Provisioned)
                        .count();
                sql("select bm from BaremetalInstanceVO bm where bm.status = :status", BaremetalInstanceVO.class)
                        .param("status", BaremetalInstanceStatus.Provisioned)
                        .limit(1000)
                        .paginate(count, bms -> bms.forEach(bm -> {
                            BaremetalInstanceVO vo = (BaremetalInstanceVO) bm;
                            runnables.add(() -> writeTerminalProxy(vo.getUuid()));
                        }));
                return runnables;
            }
        }.execute().forEach(Runnable::run);
    }

    private String getPxeServerIp(String bmUuid) {
        return SQL.New("select pxe.hostname from BaremetalPxeServerVO pxe, BaremetalInstanceVO bm " +
                "where bm.uuid = :bmUuid and bm.pxeServerUuid = pxe.uuid")
                .param("bmUuid", bmUuid)
                .find();
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
