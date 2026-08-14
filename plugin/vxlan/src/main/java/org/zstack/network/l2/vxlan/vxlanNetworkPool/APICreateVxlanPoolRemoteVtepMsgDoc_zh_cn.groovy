package org.zstack.network.l2.vxlan.vxlanNetworkPool

import org.zstack.network.l2.vxlan.vxlanNetworkPool.APICreateVxlanPoolRemoteVtepEvent

doc {
	title "创建远端VXLAN隧道端点(CreateVxlanPoolRemoteVtep)"

	category "network.l2"

	desc """创建远端VXLAN隧道端点"""

	rest {
		request {
			url "POST /v1/l2-networks/{l2NetworkUuid}/clusters/{clusterUuid}/remote-vtep-ip"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVxlanPoolRemoteVtepMsg.class

			desc """"""

			params {

				column {
					name "l2NetworkUuid"
					enclosedIn "params"
					desc "二层网络UUID"
					location "url"
					type "String"
					optional false
					since "3.17.11"
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID"
					location "url"
					type "String"
					optional false
					since "3.17.11"
				}
				column {
					name "remoteVtepIp"
					enclosedIn "params"
					desc "远端隧道端点IP地址"
					location "body"
					type "String"
					optional false
					since "3.17.11"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.17.11"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
			}
		}

		response {
			clz APICreateVxlanPoolRemoteVtepEvent.class
		}
	}
}