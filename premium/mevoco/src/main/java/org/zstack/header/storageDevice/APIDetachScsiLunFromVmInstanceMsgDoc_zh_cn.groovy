package org.zstack.header.storageDevice

import org.zstack.header.storageDevice.APIDetachScsiLunFromVmInstanceEvent

doc {
	title "DetachScsiLunFromVmInstance"

	category "storage.device"

	desc """将SCSI Lun从虚拟机卸载"""

	rest {
		request {
			url "DELETE /v1/vm-instances/{vmInstanceUuid}/scsi-lun/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachScsiLunFromVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.1.0"
				}
			}
		}

		response {
			clz APIDetachScsiLunFromVmInstanceEvent.class
		}
	}
}