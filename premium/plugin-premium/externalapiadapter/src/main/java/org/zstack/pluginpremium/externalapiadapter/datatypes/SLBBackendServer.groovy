package org.zstack.pluginpremium.externalapiadapter.datatypes

import com.google.gson.annotations.SerializedName

/**
 * Created by Qi Le on 2019/12/31
 */
class SLBBackendServer {
    @SerializedName(value = "ServerId", alternate = ["serverId"])
    String serverId
    @SerializedName(value = "Type", alternate = ["type"])
    String type
    @SerializedName(value = "Weight", alternate = ["weight"])
    int weight
    @SerializedName(value = "ServerIp", alternate = ["serverIp"])
    String serverIp
    @SerializedName(value = "Description", alternate = ["description"])
    String description

    String vmInstanceId
    String vmNicId
}
