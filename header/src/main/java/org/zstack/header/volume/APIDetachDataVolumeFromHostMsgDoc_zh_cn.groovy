package org.zstack.header.volume

import org.zstack.header.volume.APIDetachDataVolumeFromHostEvent

doc {
	title "DetachDataVolumeFromHost"

	category "volume"

	desc """从物理机卸载数据云盘"""

	rest {
		request {
			url "DELETE /v1/volumes/{volumeUuid}/hosts"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachDataVolumeFromHostMsg.class

			desc """从物理机卸载数据云盘"""

			params {

				column {
					name "volumeUuid"
					enclosedIn ""
					desc "云盘UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.15.0"
				}
				column {
					name "hostUuid"
					enclosedIn ""
					desc "物理机UUID"
					location "query"
					type "String"
					optional true
					since "zsv 3.15.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.15.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.15.0"
				}
			}
		}

		response {
			clz APIDetachDataVolumeFromHostEvent.class
		}
	}
}