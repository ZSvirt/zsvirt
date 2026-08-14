<div align="center">
  <a href="https://zsvirt.io">
    <img
      src="https://raw.githubusercontent.com/zsvirt/.github/main/assets/zsvirt-logo.jpg"
      alt="ZSvirt 标志"
      width="180"
    >
  </a>

  <h1 align="center">
    开源虚拟化平台
    <br>
    企业级能力，社区共同驱动
  </h1>

  <p align="center">
    <a href="https://zsvirt.io">
      <img
        src="https://img.shields.io/badge/官方网站-0F62FE?style=flat-square&logo=googlechrome&logoColor=white"
        alt="ZSvirt 官方网站"
      >
    </a>
    <a href="https://zsvirt.io/docs">
      <img
        src="https://img.shields.io/badge/产品文档-2563EB?style=flat-square&logo=readthedocs&logoColor=white"
        alt="产品文档"
      >
    </a>
    <a href="https://demo.zsvirt.io/">
      <img
        src="https://img.shields.io/badge/在线体验-16A34A?style=flat-square&logo=internetcomputer&logoColor=white"
        alt="在线体验"
      >
    </a>
    <a href="https://zsvirt.io/download">
      <img
        src="https://img.shields.io/badge/下载-F97316?style=flat-square&logo=download&logoColor=white"
        alt="下载"
      >
    </a>
  </p>

  <p>
    <a href="./README.md">English</a>
    &nbsp;&middot;&nbsp;
    <strong>简体中文</strong>
  </p>
</div>

## ZSvirt介绍

ZSvirt 将 [ZStack](https://www.zstack.io/) 经企业级实践验证的 ZSphere 虚拟化引擎带入开源世界。

依托成熟的基础设施厂商 [ZStack](https://www.zstack.io/)，ZSvirt 提供轻量、可扩展的虚拟化平台。从高性能家庭实验室到超大规模基础设施，ZSvirt 致力于为用户提供不受厂商锁定限制的自由选择。

## 产品导览

<details open>
  <summary>
    <strong>📊 仪表盘 — 统一运维总览</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png"
        alt="ZSvirt 统一运维仪表盘"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🗂️ 资源清单 — 基础设施集中管理</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png"
        alt="ZSvirt 基础设施资源清单"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🔄 迁移管理 — 工作负载迁移</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png"
        alt="ZSvirt 迁移管理"
        width="100%"
      >
    </a>
  </p>
</details>

## 在线体验

[ZSvirt 在线体验环境](https://demo.zsvirt.io/)是一个可免费体验 ZSvirt 的托管环境，无需在本地安装。完成注册并登录后，即可开始探索 ZSvirt 的各项功能。

## 系统架构

ZSvirt 采用模块化架构，围绕虚拟化资源管理、管理平面、扩展服务和运维工具构建。

核心能力包括：

- **计算虚拟化**：提供物理机、集群、虚拟机、镜像及其生命周期管理能力。
- **网络虚拟化**：提供虚拟网络、网络服务、安全组及相关网络能力。
- **存储虚拟化**：提供主存储、备份存储、云盘、快照及存储资源管理能力。
- **管理平面**：提供 API 框架、权限模型、事件、告警、审计和系统运维能力。
- **扩展服务**：提供迁移、灾备、监控、配额管理、访问控制和企业运维等能力。
- **工具与集成**：提供安装工具、诊断工具、迁移工具、自动化脚本、Agent、CLI 及外部系统集成能力。

在软件架构层面，ZSvirt 强调异步、无状态、可扩展和自动化：

- **异步架构**：支持异步消息、异步方法及异步 HTTP 调用，减少阻塞并提高系统吞吐量。
- **无状态服务**：单个请求不依赖其他请求的状态，使服务更易于扩展、恢复和运维。
- **插件化扩展**：通过插件横向扩展资源类型、业务能力和系统集成能力。
- **工作流引擎**：管理复杂操作的执行顺序，并支持失败场景下的回滚与恢复。
- **标签与查询能力**：支持资源属性扩展、资源分类、统一查询和自动化编排。
- **自动化部署**：通过自动化工具完成部署、配置和运维任务，降低部署及维护复杂度。

<p align="center">
  <img
    src="https://github.com/ZSvirt/.github/blob/main/assets/zsvirt-architecture.svg?raw=true"
    alt="ZSvirt Architecture"
    width="100%"
  >
</p>

## VMware 迁移指南

随着企业重新评估虚拟化战略，从 VMware 迁移到其他平台已经成为关注成本控制、基础设施灵活性和长期稳定运营的组织所面临的重要课题。

ZSvirt 提供面向迁移的功能和运维工具，帮助用户评估、规划并将工作负载从现有 VMware 环境迁移到基于 ZSvirt 的虚拟化基础设施。

🔄 [查看 VMware 迁移指南](https://zsvirt.io/vmware-alternative/)

<p align="center">
  <a href="https://zsvirt.io/vmware-alternative/">
    <img
      src="https://github.com/ZSvirt/.github/blob/main/assets/zvirt-migrate.png?raw=true"
      alt="从 VMware 迁移到 ZSvirt"
      width="100%"
    >
  </a>
</p>

## 快速开始

体验 ZSvirt 最快捷的方式是阅读产品文档中的快速开始指南。该指南将引导你准备计算、网络和存储资源，初始化管理服务，并创建第一台虚拟机。

🚀 [快速开始](https://zsvirt.io/docs/quick-start)<br>
▶️ [视频链接](https://youtu.be/LsSJlBRUvYw)

## 最佳实践

ZSvirt 与 ZSphere 采用相同的企业级虚拟化引擎，继承了经过全球客户实践验证的技术能力。

<p align="center">
  <img
    src="https://github.com/ZSvirt/.github/blob/main/assets/zsvirt-partner-en.png?raw=true"
    alt="ZSvirt 全球客户与合作伙伴"
    width="100%"
  >
</p>

## 社区治理

ZSvirt 采用轻量级开源治理模式，用于明确项目如何维护、如何制定决策，以及贡献者之间如何协作。

如需了解项目角色、维护者职责、决策流程、版本发布管理和社区协作方式，请从 [GOVERNANCE.md](GOVERNANCE.md) 开始阅读。

随着社区不断发展，ZSvirt 的治理模式也可能逐步引入专门的维护者、工作组以及更加规范的项目流程。

## 参与贡献

我们欢迎并感谢来自社区的每一项贡献。无论是修复缺陷、完善文档、提出功能建议、增加测试，还是分享部署、迁移和运维实践，你的贡献都能帮助 ZSvirt 变得更好。

如果你刚开始接触本项目，可以从文档改进、问题反馈、测试验证、迁移经验或社区讨论入手。我们同样欢迎开发者贡献代码改进、工具增强和系统集成。

活跃贡献者可能会通过社区致谢、发行说明、贡献者名单或未来的社区计划获得认可。

参与贡献前，请阅读：

- [CONTRIBUTING.md](CONTRIBUTING.md)

## 安全

安全漏洞报告流程请参阅 [SECURITY.md](SECURITY.md)。

请勿通过公开的 GitHub Issues 或 Discussions 报告安全漏洞。

## 开源许可证

ZSvirt 基于 [GNU General Public License v3.0](LICENSE) 许可证发布。

部分代码仓库或组件可能包含采用其他许可证的第三方开源软件。具体信息请查看各代码仓库中的 `LICENSE`、`NOTICE` 及相关文件。

## 相关资源

<table>
  <tr>
    <td width="50%">
      <h3>🌐 社区网站</h3>
      <p>了解 ZSvirt 的产品功能、使用场景、项目动态和社区资源。</p>
      <a href="https://zsvirt.io/en"><strong>访问网站 →</strong></a>
    </td>
    <td width="50%">
      <h3>▶️ 产品视频</h3>
      <p>观看 ZSvirt 产品介绍，了解其核心功能与技术能力。</p>
      <a href="https://youtu.be/c6pYmlIoPIU"><strong>观看产品视频 →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>📝 博客</h3>
      <p>阅读版本动态、工程实践和虚拟化技术相关文章。</p>
      <a href="https://zsvirt.io/en/blog"><strong>阅读博客 →</strong></a>
    </td>
    <td width="50%">
      <h3>💬 GitHub Discussions</h3>
      <p>提出问题、分享想法，并与 ZSvirt 社区成员交流。</p>
      <a href="https://github.com/ZSvirt/zsvirt/discussions"><strong>参与讨论 →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>▶️ YouTube</h3>
      <p>关注 ZSvirt 频道，观看产品演示、技术教程和项目动态。</p>
      <a href="https://youtube.com/@ZSvirt"><strong>关注 YouTube →</strong></a>
    </td>
    <td width="50%">
      <h3>💼 LinkedIn</h3>
      <p>关注 ZSvirt 的项目新闻、社区动态和行业观点。</p>
      <a href="https://www.linkedin.com/in/zsvirt-community/"><strong>关注 LinkedIn →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>𝕏 X</h3>
      <p>及时获取 ZSvirt 的最新公告和社区动态。</p>
      <a href="https://x.com/ZSvirt"><strong>关注 X →</strong></a>
    </td>
    <td width="50%">
      <h3>🟠 Reddit</h3>
      <p>加入社区讨论，分享你的 ZSvirt 使用经验。</p>
      <a href="https://www.reddit.com/r/ZSvirt/"><strong>加入 Reddit 社区 →</strong></a>
    </td>
  </tr>
</table>
