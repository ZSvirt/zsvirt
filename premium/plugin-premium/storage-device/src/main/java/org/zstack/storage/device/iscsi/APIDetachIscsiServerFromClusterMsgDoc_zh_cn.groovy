package org.zstack.storage.device.iscsi

import org.zstack.storage.device.iscsi.APIDetachIscsiServerFromClusterEvent

doc {
	title "DetachIscsiServerFromCluster"

	category "storage.device"

	desc """将iSCSI服务器从集群卸载"""

	rest {
		request {
			url "DELETE /v1/clusters/{clusterUuid}/storage-devices/iscsi/servers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachIscsiServerFromClusterMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "iSCSI服务器的UUID"
					location "url"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "clusterUuid"
					enclosedIn ""
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
					location "query"
					type "List"
					optional true
					since "3.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.0.0"
				}
			}
		}

		response {
			clz APIDetachIscsiServerFromClusterEvent.class
		}
	}
}