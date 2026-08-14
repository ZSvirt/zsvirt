package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb


import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterGlobalProperty
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.SLB_QUOTA_CONST

class DescribeSlbQuotas extends BaseAPI {
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
                    ecsAttributeName = "Quotas"
                    ecsAttributeValue = new HashMap<>()
                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "Quota"
                        ecsAttributeValue = new ArrayList<>()

                        getZstackAttributeValue = {
                            List zstackValue = new ArrayList<>()
                            zstackValue.add("MockValue")
                            return zstackValue
                        }

                        addEcsValueToFather = { Map parentMap ->
                            parentMap.put(ecsAttributeName, ecsAttributeValue)
                        }

                        addListElement = { elementZstackValue ->
                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.QUOTA_NAME
                                    zstackAttributeValue = SLB_QUOTA_CONST.SLB_PER_USER

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.MAX
                                    getZstackAttributeValue = {
                                        Integer res
                                        try {
                                            res = Integer.valueOf(ExternalAPIAdapterGlobalProperty.SLB_PER_USER)
                                        } catch (NumberFormatException ignored) {
                                            res = Integer.valueOf(SLB_QUOTA_CONST.SLB_PER_USER_DEF)
                                        }
                                        return res
                                    }

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.COMMENT
                                    zstackAttributeValue = SLB_QUOTA_CONST.SLB_PER_USER_COM

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }
                            }

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.QUOTA_NAME
                                    zstackAttributeValue = SLB_QUOTA_CONST.SERVER_CA_PER_REGION

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.MAX
                                    getZstackAttributeValue = {
                                        Integer res
                                        try {
                                            res = Integer.valueOf(ExternalAPIAdapterGlobalProperty.SERVER_CA_PER_REGION)
                                        } catch (NumberFormatException ignored) {
                                            res = Integer.valueOf(SLB_QUOTA_CONST.SERVER_CA_PER_REGION_DEF)
                                        }
                                        return res
                                    }

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.COMMENT
                                    zstackAttributeValue = SLB_QUOTA_CONST.SERVER_CA_PER_REGION_COM

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }
                            }

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.QUOTA_NAME
                                    zstackAttributeValue = SLB_QUOTA_CONST.CLIENT_CA_PER_REGION

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.MAX
                                    getZstackAttributeValue = {
                                        Integer res
                                        try {
                                            res = Integer.valueOf(ExternalAPIAdapterGlobalProperty.CLIENT_CA_PER_REGION)
                                        } catch (NumberFormatException ignored) {
                                            res = Integer.valueOf(SLB_QUOTA_CONST.CLIENT_CA_PER_REGION_DEF)
                                        }
                                        return res
                                    }

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.COMMENT
                                    zstackAttributeValue = SLB_QUOTA_CONST.CLIENT_CA_PER_REGION_COM

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }
                            }

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.QUOTA_NAME
                                    zstackAttributeValue = SLB_QUOTA_CONST.SLB_PER_SERVER

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.MAX
                                    getZstackAttributeValue = {
                                        Integer res
                                        try {
                                            res = Integer.valueOf(ExternalAPIAdapterGlobalProperty.SLB_PER_SERVER)
                                        } catch (NumberFormatException ignored) {
                                            res = Integer.valueOf(SLB_QUOTA_CONST.SLB_PER_SERVER_DEF)
                                        }
                                        return res
                                    }

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.COMMENT
                                    zstackAttributeValue = SLB_QUOTA_CONST.SLB_PER_SERVER_COM

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }
                            }

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.QUOTA_NAME
                                    zstackAttributeValue = SLB_QUOTA_CONST.SERVER_PER_SLB

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.MAX
                                    getZstackAttributeValue = {
                                        Integer res
                                        try {
                                            res = Integer.valueOf(ExternalAPIAdapterGlobalProperty.SERVER_PER_SLB)
                                        } catch (NumberFormatException ignored) {
                                            res = Integer.valueOf(SLB_QUOTA_CONST.SERVER_PER_SLB_DEF)
                                        }
                                        return res
                                    }

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.COMMENT
                                    zstackAttributeValue = SLB_QUOTA_CONST.SERVER_PER_SLB_COM

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }
                            }

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.QUOTA_NAME
                                    zstackAttributeValue = SLB_QUOTA_CONST.LISTENER_PER_SLB

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.MAX
                                    getZstackAttributeValue = {
                                        Integer res
                                        try {
                                            res = Integer.valueOf(ExternalAPIAdapterGlobalProperty.LISTENER_PER_SLB)
                                        } catch (NumberFormatException ignored) {
                                            res = Integer.valueOf(SLB_QUOTA_CONST.LISTENER_PER_SLB_DEF)
                                        }
                                        return res
                                    }

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.COMMENT
                                    zstackAttributeValue = SLB_QUOTA_CONST.LISTENER_PER_SLB_COM

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }
                            }

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.QUOTA_NAME
                                    zstackAttributeValue = SLB_QUOTA_CONST.RULE_PER_LISTENER

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.MAX
                                    getZstackAttributeValue = {
                                        Integer res
                                        try {
                                            res = Integer.valueOf(ExternalAPIAdapterGlobalProperty.RULE_PER_LISTENER)
                                        } catch (NumberFormatException ignored) {
                                            res = Integer.valueOf(SLB_QUOTA_CONST.RULE_PER_LISTENER_DEF)
                                        }
                                        return res
                                    }

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.COMMENT
                                    zstackAttributeValue = SLB_QUOTA_CONST.RULE_PER_LISTENER_COM

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }
                            }

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.QUOTA_NAME
                                    zstackAttributeValue = SLB_QUOTA_CONST.DOMAIN_EXT_PER_LISTENER

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.MAX
                                    getZstackAttributeValue = {
                                        Integer res
                                        try {
                                            res = Integer.valueOf(ExternalAPIAdapterGlobalProperty.DOMAIN_EXT_PER_LISTENER)
                                        } catch (NumberFormatException ignored) {
                                            res = Integer.valueOf(SLB_QUOTA_CONST.DOMAIN_EXT_PER_LISTENER_DEF)
                                        }
                                        return res
                                    }

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.COMMENT
                                    zstackAttributeValue = SLB_QUOTA_CONST.DOMAIN_EXT_PER_LISTENER_COM

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }
                            }

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.QUOTA_NAME
                                    zstackAttributeValue = SLB_QUOTA_CONST.ACL_PER_REGION

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.MAX
                                    getZstackAttributeValue = {
                                        Integer res
                                        try {
                                            res = Integer.valueOf(ExternalAPIAdapterGlobalProperty.ACL_PER_REGION)
                                        } catch (NumberFormatException ignored) {
                                            res = Integer.valueOf(SLB_QUOTA_CONST.ACL_PER_REGION_DEF)
                                        }
                                        return res
                                    }

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.COMMENT
                                    zstackAttributeValue = SLB_QUOTA_CONST.ACL_PER_REGION_COM

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }
                            }

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.QUOTA_NAME
                                    zstackAttributeValue = SLB_QUOTA_CONST.LISTENER_PER_ACL

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.MAX
                                    getZstackAttributeValue = {
                                        Integer res
                                        try {
                                            res = Integer.valueOf(ExternalAPIAdapterGlobalProperty.LISTENER_PER_ACL)
                                        } catch (NumberFormatException ignored) {
                                            res = Integer.valueOf(SLB_QUOTA_CONST.LISTENER_PER_ACL_DEF)
                                        }
                                        return res
                                    }

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.COMMENT
                                    zstackAttributeValue = SLB_QUOTA_CONST.LISTENER_PER_ACL_COM

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }
                            }

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.QUOTA_NAME
                                    zstackAttributeValue = SLB_QUOTA_CONST.ENTRY_PER_ACL

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.MAX
                                    getZstackAttributeValue = {
                                        Integer res
                                        try {
                                            res = Integer.valueOf(ExternalAPIAdapterGlobalProperty.ENTRY_PER_ACL)
                                        } catch (NumberFormatException ignored) {
                                            res = Integer.valueOf(SLB_QUOTA_CONST.ENTRY_PER_ACL_DEF)
                                        }
                                        return res
                                    }

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = SLB_QUOTA_CONST.COMMENT
                                    zstackAttributeValue = SLB_QUOTA_CONST.ENTRY_PER_ACL_COM

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }
                            }
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
