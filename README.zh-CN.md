# ZVirt 简介 [http://www.zstack.io](http://www.zstack.io)

ZVirt是一款产品化的开源IaaS（基础架构即服务）软件。它面向智能数据中心，通过完善的API统一管理包括计算、存储和网络在内的数据中心资源，提供简单快捷的环境搭建。 5分钟完成安装和部署单台Linux机器上的POC环境；30分钟完成安装和部署多管理节点生产环境（可扩展至数万台物理服务器）。

ZVirt通过完善的API管理数据中心资源，构建软件定义数据中心。用户可选择UI界面或命令行工具管理云平台，与ZVirt管理节点进行友好交互。 ZVirt提供统一配置、统一安装、统一高可用（High Availability）和统一第三方监控的云服务解决方案，使云平台的管理更加便捷、稳定、持久。

## 为什么选择ZVirt？

ZVirt解决了长期困扰开源IaaS软件用户的两大痛点：复杂性和稳定性。

复杂性：ZVirt始终秉承安全可控、全自动化、自管服务、硬件无锁、用户友好查询API的设计理念，为用户提供一款便捷配置、持久运行的云计算软件。

稳定性：ZVirt功能架构具备以下特点：

* 全插件系统：添加或移除功能不影响已有代码
* 工作流引擎：出现错误时，任何变更均可回滚
* 瀑布流架构：支持资源的级联操作
* 3套严密的自动化测试系统：确保每个功能的代码质量，并从架构设计层面解决稳定性问题

此外，ZVirt具有扩展性：单个管理节点可管理数万台物理服务器、数百万台虚拟机，处理数千条并发调用API请求，尤其适用于大型公有云平台的搭建。

> 在某些场合，ZVirt也被称为ZSphere

## 主要优势

#### 弹性

单个管理节点可管理**数万台**物理服务器、数**百万台**虚拟机，处理**数千万条**并发API请求。

#### 敏捷

各类操作**非常快速**。以创建虚拟机为例，详见以下性能数据。

<table class="table table-bordered home-table" style="margin-bottom: 0;">
  <tr>
    <th>虚拟机数量</td>
    <th>时长&nbsp;&nbsp;
        <i class='fa fa-info-circle' style='cursor:help' title="Limited by hardware, this data is from a mixed environment containing real VMs created on nested virtualization hypervisor and simulator VMs, which are created by 100 threads using only one management node. We are 100% sure the performance will get better in the real data center with decent hardware."></i>
    </td>
  </tr>
  <tr>
    <td>1</td>
    <td>0.51 秒</td>
  </tr>
  <tr>
    <td>10</td>
    <td>1.55 秒</td>
  </tr>
  <tr>
    <td>100</td>
    <td>11.33 秒</td>
  </tr>
  <tr>
    <td>1000</td>
    <td>103 秒</td>
  </tr>
  <tr>
    <td>10000</td>
    <td>23 分</td>
  </tr>
</table>

#### 网络功能虚拟化

ZVirt默认网络模型基于网络功能虚拟化（**NFV**），为每位租户提供云路由专有网络节点。整个网络模型独立自主、安全可控，用户无需购买特定设备，也无须在计算服务器上部署网络服务器。

#### 全API查询

ZVirt支持超过**400万**个查询条件，以及400万阶乘的组合查询条件。用户无须编写临时脚本或登录数据库，直接通过API即可任意查询资源。

     >> QueryVmInstance vmNics.eip.guestIp=16.16.16.16 zone.name=west-coast
     >> QueryHost fields=name,uuid,managementIp hypervisorType=KVM vmInstance.allVolumes.size>=549755813888000 vmInstance.state=Running start=0 limit=10

#### 便捷部署与升级

ZVirt安装升级如同安装一个**Java WAR**文件一样简单。用户只需执行一个Bootstrap脚本，即可在**5分钟**内搭建一套POC环境；**30分钟**内搭建一套多管理节点生产环境（包括研读文档时间）。

     >> [root@localhost ~]# curl http://download.zstack.org/install.sh |  bash -s -- -a

#### 全自动化

ZVirt云平台由**API统一管理**，全自动化、统一配置。此外，通过无缝透明集成Ansible，可在大规模硬件设备上全自动安装/配置/升级代理程序，全过程无需用户干预。

#### 通用插件系统

ZVirt采用与OSGi和Eclipse类似的插件系统方式作为核心架构基础，添加或移除功能不会对核心架构产生任何影响，满足了用户对于开源IaaS软件的鲁棒性需求。

#### 严密的测试系统

ZVirt提供**3套全自动化的严密测试系统**，确保每个功能的代码质量。

## 安装

安装ZVirt极为便捷。用户可按需选择不同的安装模式安装首套ZVirt环境。

* 如需快速尝试，请参阅[快速安装手册](http://en.zstack.io/installation/index.html)。
* 如需部署生产环境，请参阅[手动安装手册](http://en.zstack.io/installation/manual.html)。
* 如需部署多管理节点环境，请参阅[多管理节点安装手册](http://en.zstack.io/installation/multi-node.html)。

安装完成之后，可参考以下手册快速使用云平台：

* [快速使用云平台（快速安装）](http://en.zstack.io/documentation/getstart-quick.html)
* [快速使用云平台（手动安装）](http://en.zstack.io/documentation/getstart-manual.html)
* [快速使用云平台（多管理节点安装）](http://en.zstack.io/documentation/getstart-multi.html)

## 教程

对于首次使用All in One方式在单台Linux机器上搭建使用ZVirt云平台的用户，ZVirt提供以下6本教程可供参阅：

#### Amazon EC2经典弹性IP域：

* [UI界面](http://en.zstack.io/tutorials/ec2-ui.html)
* [命令行工具](http://en.zstack.io/tutorials/ec2-cli.html)

#### 扁平网络:

* [UI界面](http://en.zstack.io/tutorials/flat-network-ui.html)
* [命令行工具](http://en.zstack.io/tutorials/flat-network-cli.html)

#### 三层网络:

* [UI界面](http://en.zstack.io/tutorials/three-tiered-ui.html)
* [命令行工具](http://en.zstack.io/tutorials/three-tiered-cli.html)

#### 安全组:

* [UI界面](http://en.zstack.io/tutorials/security-group-ui.html)
* [命令行工具](http://en.zstack.io/tutorials/security-group-cli.html)

#### 弹性端口转发：

* [UI界面](http://en.zstack.io/tutorials/elastic-port-forwarding-ui.html)
* [命令行工具](http://en.zstack.io/tutorials/elastic-port-forwarding-cli.html)

#### 快照：

* [UI界面](http://en.zstack.io/tutorials/snapshot-ui.html)
* [命令行工具](http://en.zstack.io/tutorials/snapshot-cli.html)

#### 更多

关于ZVirt架构设计的更多解读，请参阅以下文章：

#### 伸缩性

* [ZVirt弹性架构揭秘 1：异构架构](http://en.zstack.io/blog/asynchronous-architecture.html)
* [ZVirt弹性架构揭秘 2：无状态服务架构](http://en.zstack.io/blog/stateless-clustering.html)
* [ZVirt弹性架构揭秘 3：无锁架构](http://en.zstack.io/blog/lock-free.html)

#### 插件架构

* [进程内微服务架构](http://en.zstack.io/blog/microservices.html)
* [通用插件系统](http://en.zstack.io/blog/plugin.html)
* [标签系统](http://en.zstack.io/blog/tag.html)
* [工作流引擎](http://en.zstack.io/blog/workflow.html)
* [瀑布流架构](http://en.zstack.io/blog/cascade.html)

#### 查询API:

* [查询API](http://en.zstack.io/blog/query.html)

#### 自动化:

* [全自动化Ansible部署](http://en.zstack.io/blog/ansible.html)

#### 存储与网络：

* [网络模型 1：二层网络和三层网络](http://en.zstack.io/blog/network-l2.html)
* [网络模型 2：云路由器网络服务提供商](http://en.zstack.io/blog/virtual-router.html)
* [存储模型：主存储与镜像服务器](http://en.zstack.io/blog/storage.html)
* [云主机迁移：场景、存储与常见误解](doc/zh-CN/vm-migration-scenarios.md)（与源码对照的说明）
* [KVM 热迁移 TLS 证书管理系统设计](doc/migration-encryption.md)
* [KVM 热迁移 TLS 加密框架方案（设计评审稿）](doc/zh-CN/kvm-live-migration-tls-framework-design.md)
* [KVM 热迁移 TLS（QEMU 证书）规范与扩展](doc/zh-CN/kvm-live-migration-tls.md)

#### 测试

* [自动化测试系统 1：综合测试](http://en.zstack.io/blog/integration-testing.html)
* [自动化测试系统2：系统测试](http://en.zstack.io/blog/system-testing.html)
* [自动化测试系统3：模型测试](http://en.zstack.io/blog/model-based-testing.html)

## 许可证

根据Apache许可证2.0版本（"许可证"）授权，为正常使用该服务，请确保许可证与本文件兼容。用户可通过以下链接获得许可证副本：

http://www.apache.org/licenses/LICENSE-2.0

除非适用法律要求或以书面形式约定，该许可证分发的软件将按“原样”提供，无任何明示或暗示的保证或条件。请参阅该许可证，通过特定语言了解具体权限和限制。
