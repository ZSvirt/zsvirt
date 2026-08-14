package org.zstack.header.volume

import org.zstack.header.volume.APIExpungeDataVolumeEvent

doc {
	title "ExpungeDataVolume"

	category "volume"

	desc """彻底删除云盘"""

	rest {
		request {
			url "PUT /v1/volumes/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIExpungeDataVolumeMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "expungeDataVolume"
					desc "云盘的UUID，唯一标示该资源"
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
			clz APIExpungeDataVolumeEvent.class
		}
	}
}