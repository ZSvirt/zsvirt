package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance.networkinterface

import org.zstack.compute.vm.VmSystemTags
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.DetachL3NetworkFromVmAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by lining on 2018/5/20.
 */
class DetachNetworkInterface extends BaseAPI {
    @Override
    Class getZStackAction() {
        return DetachL3NetworkFromVmAction.class
    }

    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = "NetworkInterfaceId"
                    zstackParamName = "vmNicUuid"
                }

                simpleConvert {
                    ecsParamName = ECS_INSTANCE_ID
                    zstackParamName = ZSTACK_VM_INSTANCE_UUID
                }

	            zstackNeedParam {
                    zstackParamName = ZSTACK_SYSTEMTAGS

                    getZstackValue = { ecsParamMap, zstackParamMap ->
                        return [VmSystemTags.RELEASE_NIC_AFTER_DETACH_NIC.instantiateTag([(VmSystemTags.RELEASE_NIC_AFTER_DETACH_NIC_TOKEN): false])]
                    }

                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        zstackParamMap.put(zstackParamName, zstackParamValue)
                    }
                }
            }

            convertAPIResponse {
            }
        }
    }
}
