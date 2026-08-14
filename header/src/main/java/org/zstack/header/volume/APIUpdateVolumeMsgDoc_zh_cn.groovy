package org.zstack.header.volume

import org.zstack.header.volume.APIUpdateVolumeEvent

doc {
	title "修改云盘属性(UpdateVolume)"

	category "volume"

	desc """修改云盘属性"""

	rest {
		request {
			url "PUT /v1/volumes/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVolumeMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateVolume"
					desc "云盘的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "name"
					enclosedIn "updateVolume"
					desc "云盘名称"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "updateVolume"
					desc "云盘的详细描述"
					location "body"
					type "String"
					optional true
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
			clz APIUpdateVolumeEvent.class
		}
	}
}