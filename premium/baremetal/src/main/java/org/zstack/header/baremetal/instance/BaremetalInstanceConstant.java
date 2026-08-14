package org.zstack.header.baremetal.instance;

/**
 * Created by GuoYi on 7/4/18.
 *
 * Constants used by Bare Metal in ZStack.
 */
public interface BaremetalInstanceConstant {
    String SERVICE_ID = "baremetal.instance";
    String ACTION_CATEGORY = "baremetal.instance";
    String SYNC_SIGNATURE_OF_BAREMETAL_INSTANCE = "sync-signature-of-baremetal-instance-";

    String NOTIFY_DEPLOY_BEGIN = "/baremetal/instance/deploybegin";
    String NOTIFY_DEPLOY_COMPLETE = "/baremetal/instance/deploycomplete";
    String NOTIFY_OS_RUNNING = "/baremetal/instance/osrunning";
}
