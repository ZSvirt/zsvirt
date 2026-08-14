package org.zstack.header.volume

import org.zstack.header.volume.APISetVolumeIoThreadPinEvent

doc {
	title "SetVolumeIoThreadPin"

	category "mevoco"

	desc """设置云盘IO线程绑定"""

	rest {
		request {
			url "PUT /v1/volumes/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVolumeIoThreadPinMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "setVolumeIoThreadPin"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.16.31"
				}
				column {
					name "vmUuid"
					enclosedIn "setVolumeIoThreadPin"
					desc "云盘加载目标云主机的UUID"
					location "body"
					type "String"
					optional false
					since "3.16.31"
				}
				column {
					name "pin"
					enclosedIn "setVolumeIoThreadPin"
					desc "绑定的CPU范围"
					location "body"
					type "String"
					optional false
					since "3.16.31"
				}
				column {
					name "ioThreadId"
					enclosedIn "setVolumeIoThreadPin"
					desc "IO线程的id"
					location "body"
					type "int"
					optional false
					since "3.16.31"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.16.31"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.16.31"
				}
			}
		}

		response {
			clz APISetVolumeIoThreadPinEvent.class
		}
	}
}