package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb


import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.MissingMandatoryParameterException
import org.zstack.sdk.CreateCertificateAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_NAME

/**
 * Created by Qi Le on 2019-07-29
 */
class UploadServerCertificate extends BaseAPI {

    @Override
    Class getZStackAction() {
        return CreateCertificateAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {

                zstackNeedParam {
                    zstackParamName = ZSTACK_NAME

                    getZstackValue = { Map ecsParamMap, zstackParamMap ->
                        if (ecsParamMap.containsKey("ServerCertificateName")) {
                            return ecsParamMap.get("ServerCertificateName")
                        }
                        return "untitled_certificate"
                    }
                }

                complexConvert {
                    ecsParamName = "PrivateKey"
                    alterEcsParamName = "ServerCertificate"
                    zstackParamName = "certificate"

                    getZstackValue = { Map ecsParamMap, String ecsParamValue ->
                        if (!ecsParamMap.containsKey(ecsParamName) || !ecsParamMap.containsKey(alterEcsParamName)) {
                            throw new MissingMandatoryParameterException("${ecsParamName}/${alterEcsParamName}".toString())
                        }

                        String privateKey = ecsParamMap.get("PrivateKey")
                        String serverCertificate = ecsParamMap.get("ServerCertificate")

                        char newLine = '\n'
//                        if (privateKey.charAt(privateKey.length() - 1) != newLine && serverCertificate.charAt(0) != newLine)
                        return "${privateKey}\n${serverCertificate}".toString()
                    }
                }
            }

            convertAPIResponse {

                convertResponseAttribute {
                    ecsAttributeName = "ServerCertificateId"

                    getZstackAttributeValue = {
                        return zstackAPIRsp.value.inventory.uuid
                    }

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "ServerCertificateName"

                    getZstackAttributeValue = {
                        return zstackAPIRsp.value.inventory.uuid
                    }

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "CreateTime"

                    getZstackAttributeValue = {
                        return zstackAPIRsp.value.inventory.createDate
                    }

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "CreateTimeStamp"

                    getZstackAttributeValue = {
                        return zstackAPIRsp.value.inventory.createDate.getTime()
                    }

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "IsAliCloudCertificate"

                    getZstackAttributeValue = {
                        return 0
                    }

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }
            }
        }
    }
}
