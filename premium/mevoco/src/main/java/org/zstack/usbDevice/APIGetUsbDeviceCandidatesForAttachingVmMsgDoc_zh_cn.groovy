package org.zstack.usbDevice

import org.zstack.usbDevice.APIGetUsbDeviceCandidatesForAttachingVmReply

doc {
	title "GetUsbDeviceCandidatesForAttachingVm"

	category "usbDevice"

	desc """获取USB透传候选列表"""

	rest {
		request {
			url "GET /v1/vm-instances/{vmInstanceUuid}/candidate-usb-devices"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetUsbDeviceCandidatesForAttachingVmMsg.class

			desc """获取USB透传候选列表"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "2.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "2.2"
				}
				column {
					name "attachType"
					enclosedIn ""
					desc "加载方式"
					location "query"
					type "String"
					optional true
					since "3.5"
					values ("PassThrough","Redirect")
				}
			}
		}

		response {
			clz APIGetUsbDeviceCandidatesForAttachingVmReply.class
		}
	}
}