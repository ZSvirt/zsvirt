package org.zstack.network.l2.vxlan.vxlanNetworkPool

import org.zstack.network.l2.vxlan.vxlanNetworkPool.APIDeleteVxlanPoolRemoteVtepEvent

doc {
	title "删除远端VXLAN隧道端点(DeleteVxlanPoolRemoteVtep)"

	category "network.l2"

	desc """删除远端VXLAN隧道端点"""

	rest {
		request {
			url "DELETE /v1/l2-networks/{l2NetworkUuid}/clusters/{clusterUuid}/delete/remote-vtep-ip"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVxlanPoolRemoteVtepMsg.class

			desc """"""

			params {

				column {
					name "l2NetworkUuid"
					enclosedIn ""
					desc "二层网络UUID"
					location "url"
					type "String"
					optional false
					since "3.17.11"
				}
				column {
					name "clusterUuid"
					enclosedIn ""
					desc "集群UUID"
					location "url"
					type "String"
					optional false
					since "3.17.11"
				}
				column {
					name "remoteVtepIp"
					enclosedIn ""
					desc "远端隧道端点IP地址"
					location "query"
					type "String"
					optional false
					since "3.17.11"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "3.17.11"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.17.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.17.11"
				}
			}
		}

		response {
			clz APIDeleteVxlanPoolRemoteVtepEvent.class
		}
	}
}