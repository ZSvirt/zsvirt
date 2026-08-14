package org.zstack.header.volume

import org.zstack.header.volume.APIGetVolumeCapabilitiesReply

doc {
	title "获取云盘支持的类型的能力(GetVolumeCapabilities)"

	category "volume"

	desc """获取云盘支持的类型的能力"""

	rest {
		request {
			url "GET /v1/volumes/{uuid}/capabilities"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVolumeCapabilitiesMsg.class

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
			clz APIGetVolumeCapabilitiesReply.class
		}
	}
}