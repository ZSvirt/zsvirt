package org.zstack.storage.device.iscsi

import org.zstack.storage.device.iscsi.APIAttachIscsiServerToClusterEvent

doc {
	title "AttachIscsiServerToCluster"

	category "storage.device"

	desc """将iSCSI服务器加载到集群"""

	rest {
		request {
			url "POST /v1/clusters/{clusterUuid}/storage-devices/iscsi/servers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachIscsiServerToClusterMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "iSCSI服务器的的UUID"
					location "url"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID"
					location "url"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.0.0"
				}
			}
		}

		response {
			clz APIAttachIscsiServerToClusterEvent.class
		}
	}
}