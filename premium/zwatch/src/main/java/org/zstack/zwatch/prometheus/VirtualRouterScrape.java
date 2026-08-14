package org.zstack.zwatch.prometheus;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.vyos.*;
import org.zstack.premium.externalservice.prometheus.PreparePrometheusConfigExtensionPoint;
import org.zstack.premium.externalservice.prometheus.Prometheus;
import org.zstack.premium.externalservice.prometheus.PrometheusConfig;
import org.zstack.utils.Bash;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.util.Map;

/**
 * Created by shixin on 2018/04/24.
 */
public class VirtualRouterScrape implements PreparePrometheusConfigExtensionPoint, VyosPostCreateFlowExtensionPoint,
        VyosPostRebootFlowExtensionPoint, VyosPostStartFlowExtensionPoint, VyosPostReconnectFlowExtensionPoint,
        VyosPostDestroyFlowExtensionPoint {

    private static final CLogger logger = Utils.getLogger(VirtualRouterScrape.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PrometheusStaticConfigManager pscMgr;

    private static final String VROUTER_JOB = "vrouter-exporter";
    private static final String DISCOVER_DIR = PathUtil.join(Prometheus.DISCORVERY_ROOT, "vrouter");
    private VirtualRouterScrapePrometheusConfig sconfig;

    public VirtualRouterScrape() {
        sconfig =  new VirtualRouterScrapePrometheusConfig();
        sconfig.setDiscoverDir(DISCOVER_DIR);
        sconfig.setJobName(VROUTER_JOB);

        new Bash() {
            @Override
            protected void scripts() {
                mkdirs(DISCOVER_DIR);
            }
        }.execute();
    }

    @Override
    public void prepareConfig(PrometheusConfig config) {
        sconfig.update(config);
    }

    @Override
    public Flow vyosPostCreateFlow() {
        return PrometheusVRSyncFlow();
    }

    @Override
    public Flow vyosPostRebootFlow() {
        return PrometheusVRSyncFlow();
    }

    @Override
    public Flow vyosPostReconnectFlow() {
        return PrometheusVRSyncFlow();
    }

    @Override
    public Flow vyosPostStartFlow() {
        return PrometheusVRSyncFlow();
    }

    @Override
    public Flow vyosPostDestroyFlow() {
        return prometheusStopFlow();
    }

    private Flow PrometheusVRSyncFlow() {
        return new Flow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (PrometheusHelper.isPrometheusDisabled()) {
                    trigger.next();
                    return;
                }

                VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
                String vrUuid;
                VmNicInventory mgmtNic;
                if (vr != null) {
                    mgmtNic = vr.getManagementNic();
                    vrUuid = vr.getUuid();
                } else {
                    final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                    vrUuid = spec.getVmInventory().getUuid();
                    ApplianceVmInventory applianceVm = ApplianceVmInventory.valueOf(dbf.findByUuid(vrUuid, ApplianceVmVO.class));
                    mgmtNic = applianceVm.getManagementNic();
                    DebugUtils.Assert(mgmtNic!=null, String.format("cannot find management nic for virtual router[uuid:%s, name:%s]", spec.getVmInventory().getUuid(), spec.getVmInventory().getName()));
                }
                String mngtIp = mgmtNic.getIp();

                pscMgr.writeVRConfig(vrUuid, mngtIp, PrometheusStaticConfigManager.createVirtualRouterConfig(vrUuid, mngtIp));
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
                String vrUuid;
                if (vr != null) {
                    vrUuid = vr.getUuid();
                } else {
                    final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                    vrUuid = spec.getVmInventory().getUuid();
                }

                File[] files = new File(DISCOVER_DIR).listFiles();
                if (files == null) {
                    trigger.rollback();
                    return;
                }

                for (File file : files) {
                    if (file.isFile() && file.getName().startsWith(vrUuid)) {
                        try {
                            FileUtils.forceDelete(file);
                            logger.info(String.format("delete file %s success after delete virtual router", file.getName()));
                        }catch (Exception e) {
                            logger.error(String.format("delete file %s fail after delete virtual router because %s", file.getName(), e.getMessage()));
                        }
                    }
                }

                trigger.rollback();
            }
        };
    }

    private Flow prometheusStopFlow() {
        return new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                VmInstanceSpec spec = (VmInstanceSpec)data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                VmInstanceInventory vm = spec.getVmInventory();

                ApplianceVmVO apvo = dbf.findByUuid(vm.getUuid(), ApplianceVmVO.class);
                if (apvo != null) {
                    pscMgr.deleteVRConfig(apvo.getUuid());
                }

                trigger.next();
            }
        };
    }
}
