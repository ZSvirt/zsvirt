package org.zstack.softwarePackage.header

import org.zstack.softwarePackage.header.APIUploadSoftwarePackageToVmEvent

doc {
	title "UploadSoftwarePackageToVm"

	category "softwarePackage"

	desc """上传软件包到单台运行中的KVM虚拟机"""

	rest {
		request {
			url "POST /v1/software-packages/uploadtovm"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUploadSoftwarePackageToVmMsg.class

			desc """"""

			params {

				column {
					name "type"
					enclosedIn "params"
					desc "软件包业务类型"
					location "body"
					type "String"
					optional false
					since "5.1.0"
				}
				column {
					name "url"
					enclosedIn "params"
					desc "支持upload、http和https协议的软件包地址，地址中必须包含文件名"
					location "body"
					type "String"
					optional false
					since "5.1.0"
				}
				column {
					name "vmInstanceUuid"
					enclosedIn "params"
					desc "接收软件包的虚拟机UUID，虚拟机必须处于运行状态"
					location "body"
					type "String"
					optional false
					since "5.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.1.0"
				}
			}
		}

		response {
			clz APIUploadSoftwarePackageToVmEvent.class
		}
	}
}
