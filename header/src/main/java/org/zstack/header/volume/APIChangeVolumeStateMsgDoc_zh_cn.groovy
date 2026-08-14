package org.zstack.header.volume

import org.zstack.header.volume.APIChangeVolumeStateEvent

doc {
	title "ChangeVolumeState"

	category "volume"

	desc """开启或关闭云盘"""

	rest {
		request {
			url "PUT /v1/volumes/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeVolumeStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeVolumeState"
					desc "云盘资源Uuid"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "stateEvent"
					enclosedIn "changeVolumeState"
					desc "开启或关闭"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
					values ("enable","disable")
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
			clz APIChangeVolumeStateEvent.class
		}
	}
}