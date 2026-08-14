package org.zstack.header.baremetal.pxeserver

import org.zstack.header.baremetal.pxeserver.APIAttachBaremetalPxeServerToClusterEvent

doc {
	title "AttachBaremetalPxeServerToCluster"

	category "baremetal.pxeserver"

	desc """部署服务器挂载至裸金属集群"""

	rest {
		request {
			url "POST /v1/clusters/{clusterUuid}/pxeservers/{pxeServerUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachBaremetalPxeServerToClusterMsg.class

			desc """部署服务器挂载至裸金属集群"""

			params {

				column {
					name "pxeServerUuid"
					enclosedIn "params"
					desc "部署服务器UUID"
					location "url"
					type "String"
					optional false
					since "3.1.1"
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
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
					location "body"
					type "List"
					optional true
					since "3.1.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.1.1"
				}
			}
		}

		response {
			clz APIAttachBaremetalPxeServerToClusterEvent.class
		}
	}
}