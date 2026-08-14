package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI

/**
 * Created by Qi Le on 2019/10/15
 */
class DescribeVServerGroups extends BaseAPI{
    @Override
    Class getZStackAction() {
        return null
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {}
            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "VServerGroups"
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = {Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "VServerGroup"
                        ecsAttributeValue = new ArrayList<>()

                        addEcsValueToFather = { Map parentMap ->
                            parentMap.put(ecsAttributeName, ecsAttributeValue)
                        }
                    }
                }
            }
        }
    }

    @Override
    Object callZStackAction() {
        return null
    }
}