package org.zstack.pluginpremium.externalapiadapter.api.ecs.others


import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by Qi Le on 2020/3/20
 */
//mock api
class DescribeCommonBandwidthPackages extends BaseAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {}

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_QUERY_API_PAGENUMBER_KEY
                    ecsAttributeValue = ECS_QUERY_API_PAGENUMBER_DEFAULT_VALUE.toString()

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp[ecsAttributeName] = ecsAttributeValue
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_QUERY_API_PAGESIZE_KEY
                    ecsAttributeValue = ECS_QUERY_API_PAGESIZE_DEFAULT_VALUE.toString()

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp[ecsAttributeName] = ecsAttributeValue
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_TOTAL_COUNT
                    ecsAttributeValue = "0"
                }

                convertResponseAttribute {
                    ecsAttributeName = "CommonBandwidthPackages"
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp[ecsAttributeName] = ecsAttributeValue
                    }

                    convertResponseAttribute {
                        ecsAttributeName = "CommonBandwidthPackage"
                        ecsAttributeValue = new HashMap<>()

                        addEcsValueToFather = { Map parentMap ->
                            parentMap[ecsAttributeName] = ecsAttributeValue
                        }

                        convertResponseAttribute {
                            ecsAttributeName = "Name"
                            ecsAttributeValue = "abc"

                            addEcsValueToFather = { Map parentMap ->
                                parentMap[ecsAttributeName] = ecsAttributeValue
                            }
                        }

                        convertResponseAttribute {
                            ecsAttributeName = ECS_API_STATUS_KEY
                            ecsAttributeValue = "Available"

                            addEcsValueToFather = { Map parentMap ->
                                parentMap[ecsAttributeName] = ecsAttributeValue
                            }
                        }

                        convertResponseAttribute {
                            ecsAttributeName = "BandwidthPackageId"
                            ecsAttributeValue = "cbwp-bp1vevu8h3ieh5xkcdhdy"

                            addEcsValueToFather = { Map parentMap ->
                                parentMap[ecsAttributeName] = ecsAttributeValue
                            }
                        }

                        convertResponseAttribute {
                            ecsAttributeName = "BusinessStatus"
                            ecsAttributeValue = "Normal"

                            addEcsValueToFather = { Map parentMap ->
                                parentMap[ecsAttributeName] = ecsAttributeValue
                            }
                        }

                        convertResponseAttribute {
                            ecsAttributeName = "Ratio"
                            ecsAttributeValue = "100"

                            addEcsValueToFather = { Map parentMap ->
                                parentMap[ecsAttributeName] = ecsAttributeValue
                            }
                        }

                        convertResponseAttribute {
                            ecsAttributeName = ECS_API_REGIONID_KEY
                            ecsAttributeValue = "cn-hangzhou"

                            addEcsValueToFather = { Map parentMap ->
                                parentMap[ecsAttributeName] = ecsAttributeValue
                            }
                        }

                        convertResponseAttribute {
                            ecsAttributeName = ECS_API_ZONEID_KEY
                            ecsAttributeValue = "cn-hanghzou-d"

                            addEcsValueToFather = { Map parentMap ->
                                parentMap[ecsAttributeName] = ecsAttributeValue
                            }
                        }

                        convertResponseAttribute {
                            ecsAttributeName = "InstanceChargeType"
                            ecsAttributeValue = "PostPaid"

                            addEcsValueToFather = { Map parentMap ->
                                parentMap[ecsAttributeName] = ecsAttributeValue
                            }
                        }

                        convertResponseAttribute {
                            ecsAttributeName = "InternetChargeType"
                            ecsAttributeValue = "PayByBandwidth"

                            addEcsValueToFather = { Map parentMap ->
                                parentMap[ecsAttributeName] = ecsAttributeValue
                            }
                        }

                        convertResponseAttribute {
                            ecsAttributeName = ECS_NETWORK_BANDWIDTH
                            ecsAttributeValue = "20"

                            addEcsValueToFather = { Map parentMap ->
                                parentMap[ecsAttributeName] = ecsAttributeValue
                            }
                        }

                        convertResponseAttribute {
                            ecsAttributeName = ECS_API_CREATION_TIME
                            ecsAttributeValue = "2017-06-28T06:39:20Z"

                            addEcsValueToFather = { Map parentMap ->
                                parentMap[ecsAttributeName] = ecsAttributeValue
                            }
                        }

                        convertResponseAttribute {
                            ecsAttributeName = "PublicIpAddresses"
                            ecsAttributeValue = new HashMap<>()

                            addEcsValueToFather = { Map parentMap ->
                                parentMap[ecsAttributeName] = ecsAttributeValue
                            }

                            convertResponseAttribute {
                                ecsAttributeName = "PublicIpAddresse"
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { Map parentMap ->
                                    parentMap[ecsAttributeValue] = ecsAttributeValue
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_NETWORK_IP_ADDRESS
                                    ecsAttributeValue = "116.62.129.250"

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap[ecsAttributeName] = ecsAttributeValue
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_EIP_ALLOCATION_ID
                                    ecsAttributeValue = "eip-bp13e9i2qst4g6jzi35tc"

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap[ecsAttributeName] = ecsAttributeValue
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
    Class getZStackAction() {
        return null
    }

    @Override
    Object callZStackAction() {
        return null
    }
}
