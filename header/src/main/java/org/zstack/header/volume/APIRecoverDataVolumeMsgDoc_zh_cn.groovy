package org.zstack.header.volume

import org.zstack.header.volume.APIRecoverDataVolumeEvent

doc {
	title "恢复云盘(RecoverDataVolume)"

	category "volume"

	desc """恢复云盘"""

	rest {
		request {
			url "PUT /v1/volumes/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRecoverDataVolumeMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "recoverDataVolume"
					desc "云盘的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
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
			clz APIRecoverDataVolumeEvent.class
		}
	}
}