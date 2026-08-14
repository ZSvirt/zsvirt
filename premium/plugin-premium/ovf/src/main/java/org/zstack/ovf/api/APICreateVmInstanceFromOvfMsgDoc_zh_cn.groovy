package org.zstack.ovf.api

import org.zstack.ovf.api.APICreateVmInstanceFromOvfEvent

doc {
	title "CreateVmInstanceFromOvf"

	category "ovf"

	desc """从OVF模板导入云主机"""

	rest {
		request {
			url "POST /v1/ovf/create-vm-instance"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVmInstanceFromOvfMsg.class

			desc """"""

			params {

				column {
					name "xmlBase64"
					enclosedIn "params"
					desc "Base64编码的OVF文件内容"
					location "body"
					type "String"
					optional false
					since "3.14.6"
				}
				column {
					name "jsonImageInfos"
					enclosedIn "params"
					desc "描述OVF中disk ID与镜像文件对应关系的JSON字符串"
					location "body"
					type "String"
					optional false
					since "3.14.6"
				}
				column {
					name "backupStorageUuid"
					enclosedIn "params"
					desc "用于存储上传镜像文件的镜像存储UUID"
					location "body"
					type "String"
					optional false
					since "3.14.6"
				}
				column {
					name "jsonCreateVmParam"
					enclosedIn "params"
					desc "包含云主机创建参数的消息的JSON字符串"
					location "body"
					type "String"
					optional false
					since "3.14.6"
				}
				column {
					name "deleteImageAfterSuccess"
					enclosedIn "params"
					desc "部署完成后删除镜像文件"
					location "body"
					type "boolean"
					optional true
					since "3.14.6"
				}
				column {
					name "deleteImageOnFail"
					enclosedIn "params"
					desc "部署失败后删除镜像文件"
					location "body"
					type "boolean"
					optional true
					since "3.14.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.14.6"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.14.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.14.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.14.6"
				}
			}
		}

		response {
			clz APICreateVmInstanceFromOvfEvent.class
		}
	}
}