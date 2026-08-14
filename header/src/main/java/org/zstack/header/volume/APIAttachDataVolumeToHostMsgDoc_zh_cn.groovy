package org.zstack.header.volume

import org.zstack.header.volume.APIAttachDataVolumeToHostEvent

doc {
	title "AttachDataVolumeToHost"

	category "volume"

	desc """加载数据云盘到物理机"""

	rest {
		request {
			url "POST /v1/volumes/{volumeUuid}/hosts/{hostUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachDataVolumeToHostMsg.class

			desc """加载数据云盘到物理机"""

			params {

				column {
					name "volumeUuid"
					enclosedIn "params"
					desc "云盘UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.15.0"
				}
				column {
					name "hostUuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.15.0"
				}
				column {
					name "mountPath"
					enclosedIn "params"
					desc "物理机上的挂载路径"
					location "body"
					type "String"
					optional false
					since "zsv 3.15.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.15.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.15.0"
				}
			}
		}

		response {
			clz APIAttachDataVolumeToHostEvent.class
		}
	}
}