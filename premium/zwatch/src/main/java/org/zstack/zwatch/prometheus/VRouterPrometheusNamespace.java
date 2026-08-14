package org.zstack.zwatch.prometheus;

import org.zstack.header.core.StaticInit;
import org.zstack.header.vpc.VpcConstants;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.VRouterNamespace;

public class VRouterPrometheusNamespace extends VmPrometheusNamespace {
    public static class VRouterCollector extends VmCollector {
        @Override
        protected String getNamespaceName() {
            return VRouterNamespace.NAME;
        }

        @Override
        protected String getApplianceVmType() {
            return VpcConstants.VPC_VROUTER_VM_TYPE;
        }

        @Override
        public String getCollectorName() {
            return VRouterCollector.class.getName();
        }
    }

    @StaticInit
    static void staticInit() {
        PrometheusNamespace.namespacesClasses.put(VRouterNamespace.class, VRouterPrometheusNamespace.class);
        PrometheusCollector.registerMetricCollector(new VRouterCollector());
    }

    public VRouterPrometheusNamespace(Namespace namespace) {
        super(namespace);
    }
}
