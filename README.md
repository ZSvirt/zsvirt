<div align="center">
  <a href="https://zsvirt.io">
    <img
      src="https://raw.githubusercontent.com/zsvirt/.github/main/assets/zsvirt-logo.jpg"
      alt="ZSvirt Logo"
      width="180"
    >
  </a>
  <p align="center">
    <a
      href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
      target="_blank"
      rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/daily?language=Java"
        alt="ZSvirt/zsvirt Daily Java | Trendshift"
        width="250"
        height="55"
      /></a> <a href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
       target="_blank"
       rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/weekly?language=Java"
        alt="ZSvirt/zsvirt Weekly Java | Trendshift"
        width="250"
        height="55"
      /></a>&nbsp;&nbsp;
  </p>
  <h1 align="center">
    Open Source Virtualization
    <br>
    Enterprise Ready, Community Driven
  </h1>

  <p align="center">
    <a href="https://zsvirt.io">
      <img
        src="https://img.shields.io/badge/Website-0F62FE?style=flat-square&logo=googlechrome&logoColor=white"
        alt="ZSvirt Website"
      >
    </a>
    <a href="https://zsvirt.io/en/docs">
      <img
        src="https://img.shields.io/badge/Docs-7C3AED?style=flat-square&logo=readthedocs&logoColor=white"
        alt="Documentation"
      >
    </a>
    <a href="https://demo.zsvirt.io/">
      <img
        src="https://img.shields.io/badge/Live%20Demo-16A34A?style=flat-square&logo=internetcomputer&logoColor=white"
        alt="Live Demo"
      >
    </a>
    <a href="https://zsvirt.io/download">
      <img
        src="https://img.shields.io/badge/Download-F97316?style=flat-square&logo=data:image/svg%2Bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTUgMjBoMTR2LTJINXYyem0xNC05aC00VjNIOXY4SDVsNyA3IDctN3oiLz48L3N2Zz4%3D"
        alt="Download"
      >
    </a>
  </p>

  <p>
    <strong>English</strong>
    &nbsp;&middot;&nbsp;
    <a href="./README_zh.md">简体中文</a>
  </p>
</div>

> **Try it now — no physical hardware needed.** Grab the [qcow2 or OVA image](https://zsvirt.io/download), boot it as a VM on VMware, VirtualBox, KVM, or a cloud host (nested virtualization), set an IP, and the ZSvirt management node is ready. See [Run ZSvirt inside a VM](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node) or jump to the [Quick Start](https://zsvirt.io/en/docs/quick-start).

## What is ZSvirt
ZSvirt brings [ZStack](https://www.zstack-cloud.com/)'s enterprise-proven ZSphere virtualization engine into the open-source world. Backed by [ZStack](https://www.zstack-cloud.com/), a mature infrastructure leader, ZSvirt is a lightweight, scalable platform for running and managing virtual machines without vendor lock-in.

Install a management node, attach KVM-based hosts, and manage your whole environment — VMs, clusters, storage, and networks — through a web UI, a RESTful API (with Terraform and Go/Python/Java SDKs), or a CLI. VMware migration tooling — online migration, OVF import, and VMDK upload — is built in.

## Product Tour

<details open>
  <summary>
    <strong>📊 DASHBOARD — Unified Operations Overview</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png"
        alt="ZSvirt Unified Operations Dashboard"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🗂️ INVENTORY — Centralized Infrastructure Management</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png"
        alt="ZSvirt Centralized Infrastructure Inventory"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🔄 MIGRATION MANAGEMENT — Workload Migration</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png"
        alt="ZSvirt Migration Management"
        width="100%"
      >
    </a>
  </p>
</details>

## Live Demo

[ZSvirt Live Demo](https://demo.zsvirt.io/) is a free hosted environment for trying ZSvirt online — no installation or sign-up required. Open the link and click **Demo Login** to start exploring the platform right away.

## Architecture

ZSvirt uses a modular architecture built around virtualization resource
management, the management plane, extension services, and operational tooling.

Core capabilities include:

- **Compute virtualization**: host, cluster, virtual machine, image, and
  lifecycle management.
- **Network virtualization**: virtual networks, network services, security
  groups, and related capabilities.
- **Storage virtualization**: primary storage, backup storage, volumes,
  snapshots, and storage resource management.
- **Management plane**: API framework, permission model, events, alarms,
  auditing, and system operations.
- **Extension services**: capabilities for migration, disaster recovery,
  monitoring, quota management, access control, and enterprise operations.
- **Tools and integrations**: installation tools, diagnostics tools, migration
  tools, automation scripts, agents, CLI, and external system integrations.

At the software architecture level, ZSvirt emphasizes asynchrony, statelessness,
extensibility, and automation:

- **Asynchronous architecture**: supports asynchronous messages, asynchronous
  methods, and asynchronous HTTP calls to reduce blocking and improve system
  throughput.
- **Stateless services**: individual requests do not depend on state from other
  requests, making services easier to scale, recover, and operate.
- **Plugin-based extensibility**: supports horizontal extension of resource
  types, business capabilities, and integration capabilities through plugins.
- **Workflow engine**: manages the execution order of complex operations and
  supports rollback and recovery in failure scenarios.
- **Tagging and query capabilities**: supports resource attribute extension,
  resource classification, unified queries, and automation orchestration.
- **Automated deployment**: uses automation tools to handle deployment,
  configuration, and operations tasks, reducing deployment and maintenance
  complexity.

<p align="center">
  <img
    src="https://github.com/ZSvirt/.github/blob/main/assets/zsvirt-architecture.svg?raw=true"
    alt="ZSvirt Architecture"
    width="100%"
  >
</p>

## VMware Migration Guide

As enterprises reassess their virtualization strategies, migration from VMware to alternative platforms has become an important topic for organizations seeking cost control, infrastructure flexibility, and long-term operational stability.

ZSvirt provides migration-oriented capabilities and operational tools to help users evaluate, plan, and move workloads from existing VMware environments to ZSvirt-based virtualization infrastructure.

- [VMware Migration Guide](https://zsvirt.io/vmware-alternative/)

<p align="center">
  <a href="https://zsvirt.io/vmware-alternative/">
    <img
      src="https://github.com/ZSvirt/.github/blob/main/assets/zvirt-migrate.png?raw=true"
      alt="Migrate from VMware to ZSvirt"
      width="100%"
    >
  </a>
</p>

## Quick Start

The fastest way to evaluate ZSvirt is to follow the quick start guide in the product documentation. It walks you through preparing compute, network, and storage resources, initializing the management service, and creating your first virtual machine.

Prefer to try it in a VM first? Download the [qcow2 or OVA image](https://zsvirt.io/download) and run the management node inside any hypervisor — see [Running ZSvirt inside a VM](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node).

🚀 [Quick Start](https://zsvirt.io/en/docs/quick-start)<br>
▶️ [Video](https://youtu.be/LsSJlBRUvYw)

## Best Practices
Powered by the same enterprise engine as ZSphere, ZSvirt inherits proven success across global customers below.
<p align="center">
  <img
    src="https://github.com/ZSvirt/.github/blob/main/assets/zsvirt-partner-en.png?raw=true"
    alt="ZSvirt global customers and partners"
    width="100%"
  >
</p>

## Virtualization Platform Comparison

Proxmox VE vs VMware vSphere vs ZSvirt

<p align="center">
  <img
    src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-comparison.png"
    alt="Proxmox VE vs ZSvirt virtualization platform comparison"
    width="100%"
  >
</p>


## Governance

ZSvirt is governed by a lightweight open-source governance model that defines
how the project is maintained, how decisions are made, and how contributors
collaborate.

The [GOVERNANCE.md](GOVERNANCE.md) document is the starting point for learning
about project roles, maintainer responsibilities, decision-making processes,
release management, and community collaboration.

As the community grows, the governance model may evolve to include dedicated
maintainers, working groups, and more formal project processes.

## Contributing

We welcome and appreciate contributions from the community. Whether you are
fixing bugs, improving documentation, proposing features, adding tests, or
sharing deployment, migration, and operational practices, your contribution
helps make ZSvirt better.

If you are new to the project, you can start with documentation improvements,
issue reports, test verification, migration experience, or community
discussions. Developers are also welcome to contribute code improvements,
tooling enhancements, and integrations.

We may recognize active contributors through community acknowledgements,
release notes, contributor lists, or future community programs.

Before contributing, please read:

- [CONTRIBUTING.md](CONTRIBUTING.md)

## Security

The security process for reporting vulnerabilities is described in
[SECURITY.md](SECURITY.md).

Please do not report security vulnerabilities through public GitHub Issues or
Discussions.

## License

ZSvirt is licensed under the
[GNU General Public License v3.0](LICENSE).

Some repositories or components may include third-party open-source software
under different licenses. Please check the `LICENSE`, `NOTICE`, and related
files in each repository for details.

## Resources

<table>
  <tr>
    <td width="50%">
      <h3>🌐 Community Website</h3>
      <p>Explore ZSvirt features, use cases, news, and community resources.</p>
      <a href="https://zsvirt.io"><strong>Visit Website →</strong></a>
    </td>
    <td width="50%">
      <h3>▶️ Videos</h3>
      <p>Watch the ZSvirt product introduction and discover its core capabilities.</p>
      <a href="https://youtu.be/c6pYmlIoPIU"><strong>Watch Product Video →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>📝 Blog</h3>
      <p>Discover release updates, engineering stories, and virtualization insights.</p>
      <a href="https://zsvirt.io/blog"><strong>Read the Blog →</strong></a>
    </td>
    <td width="50%">
      <h3>💬 GitHub Discussions</h3>
      <p>Ask questions, share ideas, and connect with the ZSvirt community.</p>
      <a href="https://github.com/ZSvirt/zsvirt/discussions"><strong>Join the Discussion →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>▶️ YouTube</h3>
      <p>Follow the ZSvirt channel for demonstrations, tutorials, and product updates.</p>
      <a href="https://youtube.com/@ZSvirt"><strong>Follow on YouTube →</strong></a>
    </td>
    <td width="50%">
      <h3>💼 LinkedIn</h3>
      <p>Follow ZSvirt for project news, community highlights, and industry insights.</p>
      <a href="https://www.linkedin.com/in/zsvirt-community/"><strong>Follow on LinkedIn →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>𝕏 X</h3>
      <p>Get the latest ZSvirt announcements and community updates.</p>
      <a href="https://x.com/ZSvirt"><strong>Follow on X →</strong></a>
    </td>
    <td width="50%">
      <h3>🎮 Discord</h3>
      <p>Join the ZSvirt community to ask questions, share ideas, and connect with other users.</p>
      <a href="https://discord.com/invite/KHsw63z9xA"><strong>Join on Discord →</strong></a>
    </td>
  </tr>
</table>
