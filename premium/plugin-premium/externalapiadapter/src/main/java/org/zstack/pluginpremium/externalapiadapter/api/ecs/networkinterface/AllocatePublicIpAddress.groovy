package org.zstack.pluginpremium.externalapiadapter.api.ecs.networkinterface

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterGlobalProperty
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.AttachL3NetworkToVmAction
import org.zstack.sdk.VmNicInventory

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by lining on 2018/4/30.
 */
class AllocatePublicIpAddress extends BaseAPI{
    @Override
    Class getZStackAction() {
        return AttachL3NetworkToVmAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {

                simpleConvert {
                    ecsParamName = ECS_INSTANCE_ID
                    zstackParamName = ZSTACK_VM_INSTANCE_UUID
                }

               zstackNeedParam {
                   zstackParamName = "l3NetworkUuid"

                   getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                       return ExternalAPIAdapterGlobalProperty.PUBLICL3NETWORKUUID
                   }
               }
            }

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_NETWORK_IP_ADDRESS

                    getZstackAttributeValue = {
                        String l3NetworkUuid = zstackAPIParamMap.get("l3NetworkUuid")
                        List<VmNicInventory> vmNics = zstackAPIRsp.value.inventory.vmNics
                        return vmNics.find {n -> n.l3NetworkUuid == l3NetworkUuid}.ip
                    }

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }
            }
        }
    }
}
