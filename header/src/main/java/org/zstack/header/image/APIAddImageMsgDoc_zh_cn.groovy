package org.zstack.header.image

import org.zstack.header.image.APIAddImageEvent

doc {
	title "添加镜像(AddImage)"

	category "image"

	desc """向镜像服务器添加镜像"""

	rest {
		request {
			url "POST /v1/images"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddImageMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "镜像名称"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "镜像的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "url"
					enclosedIn "params"
					desc "被添加镜像的URL地址"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "mediaType"
					enclosedIn "params"
					desc "镜像的类型"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					values ("RootVolumeTemplate","ISO","DataVolumeTemplate")
				}
				column {
					name "guestOsType"
					enclosedIn "params"
					desc "镜像对应客户机操作系统的类型"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "system"
					enclosedIn "params"
					desc "是否系统镜像（如，虚拟路由镜像）"
					location "body"
					type "boolean"
					optional true
					since "zsv 0.6"
				}
				column {
					name "format"
					enclosedIn "params"
					desc "镜像的格式，比如：raw"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "platform"
					enclosedIn "params"
					desc "镜像的系统平台"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					values ("Linux","Windows","Other","Paravirtualization","WindowsVirtio")
				}
				column {
					name "backupStorageUuids"
					enclosedIn "params"
					desc "指定添加镜像的镜像服务器UUID列表"
					location "body"
					type "List"
					optional false
					since "zsv 0.6"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "内部使用字段"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID。若指定，镜像会使用该字段值作为UUID。"
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
				column {
					name "architecture"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 3.10.0"
					values ("x86_64","aarch64","mips64el","loongarch64")
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 3.4.0"
				}
				column {
					name "virtio"
					enclosedIn "params"
					desc ""
					location "body"
					type "boolean"
					optional true
					since "zsv 3.11.2"
				}
			}
		}

		response {
			clz APIAddImageEvent.class
		}
	}
}