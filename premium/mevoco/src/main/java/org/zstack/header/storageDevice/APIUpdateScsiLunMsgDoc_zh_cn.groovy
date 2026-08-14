package org.zstack.header.storageDevice

import org.zstack.header.storageDevice.APIUpdateScsiLunEvent

doc {
	title "UpdateScsiLun"

	category "storage.device"

	desc """更新SCSI Lun"""

	rest {
		request {
			url "PUT /v1/storage-devices/scsi-lun/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateScsiLunMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateScsiLun"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "name"
					enclosedIn "updateScsiLun"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "state"
					enclosedIn "updateScsiLun"
					desc "启用状态"
					location "body"
					type "String"
					optional true
					since "3.1.0"
					values ("Enabled","Disabled")
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
			clz APIUpdateScsiLunEvent.class
		}
	}
}