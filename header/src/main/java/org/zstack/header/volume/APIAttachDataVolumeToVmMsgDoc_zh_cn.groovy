package org.zstack.header.volume

import org.zstack.header.volume.APIAttachDataVolumeToVmEvent

doc {
	title "AttachDataVolumeToVm"

	category "volume"

	desc """挂载云盘到云主机上"""

	rest {
		request {
			url "POST /v1/volumes/{volumeUuid}/vm-instances/{vmInstanceUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachDataVolumeToVmMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "params"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "volumeUuid"
					enclosedIn "params"
					desc "云盘UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIAttachDataVolumeToVmEvent.class
		}
	}
}