package org.zstack.header.storageDevice

import org.zstack.header.storageDevice.APIAttachScsiLunToVmInstanceEvent

doc {
	title "AttachScsiLunToVmInstance"

	category "storage.device"

	desc """将SCSI Lun加载到虚拟机"""

	rest {
		request {
			url "POST /v1/vm-instances/{vmInstanceUuid}/scsi-lun/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachScsiLunToVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "Scsi Lun的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "vmInstanceUuid"
					enclosedIn "params"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "disableMultiPathAttach"
					enclosedIn "params"
					desc "关闭自动加载多路径设备"
					location "body"
					type "boolean"
					optional true
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
			clz APIAttachScsiLunToVmInstanceEvent.class
		}
	}
}