package org.zstack.header.storageDevice

import org.zstack.header.storageDevice.APICheckScsiLunClusterStatusReply

doc {
	title "CheckScsiLunClusterStatus"

	category "storageDevice"

	desc """检查SCSI Lun与集群连接关系"""

	rest {
		request {
			url "PUT /v1/storage-devices/scsi-lun/{uuid}/cluster/{clusterUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICheckScsiLunClusterStatusMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "checkScsiLunClusterStatus"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "clusterUuid"
					enclosedIn "checkScsiLunClusterStatus"
					desc "集群UUID"
					location "url"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
			}
		}

		response {
			clz APICheckScsiLunClusterStatusReply.class
		}
	}
}