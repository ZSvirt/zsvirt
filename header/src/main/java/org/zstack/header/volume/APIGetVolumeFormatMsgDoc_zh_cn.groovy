package org.zstack.header.volume

import org.zstack.header.volume.APIGetVolumeFormatReply

doc {
	title "GetVolumeFormat"

	category "volume"

	desc """获取云盘格式"""

	rest {
		request {
			url "GET /v1/volumes/formats"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVolumeFormatMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
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
			clz APIGetVolumeFormatReply.class
		}
	}
}