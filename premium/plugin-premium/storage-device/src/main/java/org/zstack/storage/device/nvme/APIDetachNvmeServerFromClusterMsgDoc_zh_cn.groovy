package org.zstack.storage.device.nvme

import org.zstack.storage.device.nvme.APIDetachNvmeServerFromClusterEvent

doc {
	title "DetachNvmeServerFromCluster"

	category "storageDevice"

	desc """从集群卸载NVMe服务器"""

	rest {
		request {
			url "DELETE /v1/clusters/{clusterUuid}/storage-devices/nvme/servers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachNvmeServerFromClusterMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "clusterUuid"
					enclosedIn ""
					desc "集群UUID"
					location "url"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.17.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.17.21"
				}
			}
		}

		response {
			clz APIDetachNvmeServerFromClusterEvent.class
		}
	}
}