package org.zstack.header.volume

import org.zstack.header.volume.APIDetachDataVolumeFromVmEvent

doc {
	title "DetachDataVolumeFromVm"

	category "volume"

	desc """从云主机上卸载云盘"""

	rest {
		request {
			url "DELETE /v1/volumes/{uuid}/vm-instances"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachDataVolumeFromVmMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "云盘的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "vmUuid"
					enclosedIn ""
					desc "云主机的UUID"
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIDetachDataVolumeFromVmEvent.class
		}
	}
}