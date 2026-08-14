package org.zstack.storage.device.nvme

import org.zstack.storage.device.nvme.APIAttachNvmeServerToClusterEvent

doc {
	title "AttachNvmeServerToCluster"

	category "storageDevice"

	desc """NVMe服务器加载到集群"""

	rest {
		request {
			url "POST /v1/clusters/{clusterUuid}/storage-devices/nvme/servers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachNvmeServerToClusterMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
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
					location "body"
					type "List"
					optional true
					since "3.17.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.21"
				}
			}
		}

		response {
			clz APIAttachNvmeServerToClusterEvent.class
		}
	}
}