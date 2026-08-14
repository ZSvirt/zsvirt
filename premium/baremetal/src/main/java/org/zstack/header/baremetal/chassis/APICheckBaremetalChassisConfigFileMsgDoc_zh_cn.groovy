package org.zstack.header.baremetal.chassis

import org.zstack.header.baremetal.chassis.APICheckBaremetalChassisConfigFileReply

doc {
	title "CheckBaremetalChassisConfigFile"

	category "baremetal.chassis"

	desc """检查批量添加裸金属设备文件合法性"""

	rest {
		request {
			url "POST /v1/baremetal/chassis/from-file/check"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICheckBaremetalChassisConfigFileMsg.class

			desc """检查批量添加裸金属设备文件合法性"""

			params {

				column {
					name "baremetalChassisInfo"
					enclosedIn "params"
					desc "经过base64编码的裸金属设备信息"
					location "body"
					type "String"
					optional false
					since "3.1.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.1.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.1.1"
				}
			}
		}

		response {
			clz APICheckBaremetalChassisConfigFileReply.class
		}
	}
}