package org.zstack.header.volume

import org.zstack.header.volume.APIGetVolumeIoThreadPinReply

doc {
	title "GetVolumeIoThreadPin"

	category "mevoco"

	desc """获取云盘IO线程绑定信息"""

	rest {
		request {
			url "GET /v1/volumes/{uuid}/io-thread-pin"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVolumeIoThreadPinMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.16.31"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.16.31"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.16.31"
				}
			}
		}

		response {
			clz APIGetVolumeIoThreadPinReply.class
		}
	}
}