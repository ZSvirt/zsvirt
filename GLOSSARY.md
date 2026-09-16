# ZSvirt Translation Glossary

The shared terminology reference for every translated README. It exists so that
twenty-one languages describe the same product the same way, and so a term decided in
one language is not silently re-decided in the next.

`README.md` (English) is the source of truth for **content**. This file is the source of
truth for **terminology**.

## How to use it

1. Read [Do not translate](#do-not-translate) first. Most translation mistakes in this
   repository are terms that should have been left alone.
2. For everything else, use the definition in [Core terminology](#core-terminology) to
   choose the right word in your language.
3. If you make a binding choice that other contributors should follow — the standard word
   for *host*, whether acronyms are localized, how a term is inflected — record it under
   [Per-language decisions](#per-language-decisions). One row per decision.
4. Check [README section order](#readme-section-order) before submitting. Section order
   and link targets must match the English file.

If a core term is missing, open an issue instead of inventing a translation. A term
missing from this file is a gap in this file, not a licence to improvise — one language's
choice becomes every language's choice.

---

## Do not translate

These appear in every README and stay in their original form in every language.

### Product and company names

| Term | Note |
|---|---|
| `ZSvirt` | Project name. Never localized, never respelled, never given a local-script form. |
| `ZStack` | Company name. |
| `ZSphere` | ZStack's commercial virtualization engine, which ZSvirt inherits from. |
| `ZMigrate` | ZSvirt migration tooling. |
| `zsvirt.io` | Website domain. |

### Third-party products

| Term | Note |
|---|---|
| `VMware` | |
| `VMware vSphere` | |
| `Proxmox VE` | |
| `VirtualBox` | |
| `KVM` | |
| `Terraform` | |
| `GitHub` | Also `GitHub Discussions`. |
| `Go` / `Python` / `Java` | Also in the phrase "Go/Python/Java SDKs". |

### Commands, formats, code identifiers

| Term | Note |
|---|---|
| `qcow2` / `OVA` / `OVF` / `VMDK` | Image and disk formats. |
| `CLI` / `API` / `RESTful` / `SDK` | Acronyms. Do not expand or transliterate them. |
| `assets/zsvirt-architecture.svg` | Asset path. |

### UI labels

| Term | Note |
|---|---|
| `Demo Login` | Button label in the live demo. Keep exactly as written. |
| `DASHBOARD` / `INVENTORY` / `MIGRATION MANAGEMENT` | UI navigation section names inside the Product Tour. Keep the English label; you may translate the descriptive subtitle that follows the dash. The screenshots themselves are in English. |

### Repository files

Filenames and link targets must point at the same file in every language.

| Term |
|---|
| `README.md` · `README_zh.md` · and the other `README.<language>.md` files |
| `CONTRIBUTING.md` · `GOVERNANCE.md` · `SECURITY.md` · `LICENSE` · `NOTICE` |

### Badge images

Badge text ("Website", "Docs", "Live Demo", "Download") is baked into the shields.io
images and stays English. Translate only the `alt` attributes, which are visible to
screen readers.

---

## Core terminology

Definitions are written so you can pick the right word in your language without reading
the whole README. This is a description of what each term means in ZSvirt, not a
translation dictionary.

### Architecture and resources

| English | Definition |
|---|---|
| management node | The node running the ZSvirt management service, providing the web UI, API, and CLI. One per environment, or an HA pair. |
| host | A physical server that runs virtual machines. Hosts are attached to a cluster. |
| cluster | A group of hosts managed together and scheduled as one unit. |
| virtual machine (VM) | A guest running on a host. |
| image | A template used to create virtual machines. |
| primary storage | Storage holding VM disks and images. Attached at the cluster level. |
| backup storage | Storage holding backups, separate from primary storage. |
| volume | A virtual disk attached to a virtual machine. |
| snapshot | A point-in-time copy of a volume or virtual machine. |
| hypervisor | The layer that runs virtual machines on a host. ZSvirt uses KVM. |
| nested virtualization | Running the ZSvirt management node itself inside a VM, on VMware, VirtualBox, KVM, or a cloud host. |

### Networking

| English | Definition |
|---|---|
| virtual network | A software-defined network that VMs attach to. |
| security group | A set of network access rules applied to VMs. |
| network services | Network features layered on top of virtual networks. |

### Storage and virtualization domains

| English | Definition |
|---|---|
| compute virtualization | Host, cluster, VM, and image lifecycle management. |
| network virtualization | Virtual networks, network services, and security groups. |
| storage virtualization | Primary storage, backup storage, volumes, and snapshots. |
| resource | Any managed object: a host, VM, volume, network, and so on. |
| tagging | Attaching arbitrary attributes to resources for classification and querying. |
| lifecycle management | Creating, operating, and removing a resource over its lifetime. |

### Management plane

| English | Definition |
|---|---|
| management plane | The layer that controls the platform: API, permissions, events, and operations. |
| API framework | The framework the RESTful API is built on. |
| permission model | The rules governing which accounts may perform which operations. |
| event | A record of something that happened in the system. |
| alarm | A rule that raises a notification when a condition is met. |
| auditing | Recording who did what, for later review. |
| quota management | Limiting how much of a resource an account may consume. |
| access control | Deciding who may reach which resource. |
| extension services | Optional capabilities layered on the core platform: migration, DR, monitoring, and so on. |
| workflow engine | Executes the steps of a complex operation in order, with rollback on failure. |

### Software design

| English | Definition |
|---|---|
| asynchronous | An operation that returns before it finishes, reporting completion later. |
| stateless | A service whose handling of a request does not depend on other requests. |
| extensibility | The ability to add resource types and capabilities without changing the core. |
| plugin | A unit of extension loaded into the platform. |
| automation | Performing deployment, configuration, and operations without manual steps. |
| vendor lock-in | Being unable to leave a platform without disproportionate cost. ZSvirt is positioned against this. |

### Migration

| English | Definition |
|---|---|
| online migration | Moving a running VM with minimal downtime. |
| OVF import | Creating VMs from an OVF package exported from another platform. |
| VMDK upload | Uploading a VMware virtual disk directly into ZSvirt. |
| workload migration | Moving running workloads from one platform to another. |

### Project and community

| English | Definition |
|---|---|
| governance | How the project is maintained and how decisions are made. |
| maintainer | Someone with responsibility for reviewing and merging contributions. |
| contributor | Anyone who submits a change, report, translation, or answer. |
| pull request | A proposed change awaiting review. |
| issue | A reported bug, request, or question. |
| release notes | The description of what changed in a release. |
| best practice | A recommended way of doing something, drawn from real deployments. |
| live demo | The free hosted environment at `demo.zsvirt.io`, requiring no installation. |
| Quick Start | The named getting-started guide in the product documentation. Translate the phrase; keep the link target unchanged. |

---

## README section order

Every translated README keeps these blocks in this order. Section headings may be
translated; the order and the link targets may not change.

| # | Block | Notes |
|---|---|---|
| 1 | Language selector | Lists all 22 entries in the same order as `README.md`. The current language is plain text, the rest are links. |
| 2 | Tagline and badges | `<h1>` tagline, then the badge row. |
| 3 | "Try it now" blockquote | The qcow2/OVA nested-virtualization paragraph. |
| 4 | What is ZSvirt | |
| 5 | Product Tour | Three collapsible sections. The first is open by default. |
| 6 | Live Demo | |
| 7 | Architecture | Includes `./assets/zsvirt-architecture.svg`. |
| 8 | VMware Migration Guide | |
| 9 | Quick Start | |
| 10 | Best Practices | |
| 11 | Virtualization Platform Comparison | |
| 12 | Roadmap | |
| 13 | Governance | |
| 14 | Contributing | |
| 15 | Security | |
| 16 | License | |
| 17 | Resources | The two-column table of community links. |

---

## Per-language decisions

Add a row when you make a choice another contributor should follow. Keep it to one
decision per row so the table stays searchable.

| Language | English term | Choice | Decided by |
|---|---|---|---|
| _(example)_ | host | 主机 | @handle, 2026-09-15 |
| _(example)_ | cluster | 集群 | @handle, 2026-09-15 |
