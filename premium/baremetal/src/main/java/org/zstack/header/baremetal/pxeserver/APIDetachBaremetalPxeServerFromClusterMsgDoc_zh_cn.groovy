package org.zstack.header.baremetal.pxeserver

import org.zstack.header.baremetal.pxeserver.APIDetachBaremetalPxeServerFromClusterEvent

doc {
	title "DetachBaremetalPxeServerFromCluster"

	category "baremetal.pxeserver"

	desc """从裸金属集群卸载部署服务器"""

	rest {
		request {
			url "DELETE /v1/clusters/{clusterUuid}/pxeservers/{pxeServerUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachBaremetalPxeServerFromClusterMsg.class

			desc """从裸金属集群卸载部署服务器"""

			params {

				column {
					name "pxeServerUuid"
					enclosedIn ""
					desc "部署服务器UUID"
					location "url"
					type "String"
					optional false
					since "3.1.1"
				}
				column {
					name "clusterUuid"
					enclosedIn ""
					desc "裸金属集群UUID"
					location "url"
					type "String"
					optional false
					since "3.1.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.1.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.1.1"
				}
			}
		}

		response {
			clz APIDetachBaremetalPxeServerFromClusterEvent.class
		}
	}
}