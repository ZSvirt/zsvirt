package org.zstack.header.storageDevice

import org.zstack.header.storageDevice.APIDetachScsiLunFromHostEvent

doc {
	title "DetachScsiLunFromHost"

	category "storageDevice"

	desc """将 LUN 设备从 Host 上卸载"""

	rest {
		request {
			url "PUT /v1/storage-devices/scsi-lun/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachScsiLunFromHostMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "detachScsiLunFromHost"
					desc "SCSI LUN UUID"
					location "url"
					type "String"
					optional false
					since "3.11.6"
				}
				column {
					name "hostUuid"
					enclosedIn "detachScsiLunFromHost"
					desc "物理机UUID"
					location "body"
					type "String"
					optional true
					since "3.11.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.11.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.11.6"
				}
			}
		}

		response {
			clz APIDetachScsiLunFromHostEvent.class
		}
	}
}