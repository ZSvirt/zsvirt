# ZVirt Governance

This document describes how the ZVirt project is governed, maintained, and developed.

## Goals

The governance model is designed to:

- Keep the project open, transparent, and sustainable.
- Define clear maintainer responsibilities.
- Provide a predictable contribution and review process.
- Help users and contributors understand how decisions are made.
- Support future publication under the Alibaba GitHub organization.

## Project Scope

ZVirt is an open infrastructure project for private cloud, server virtualization, and enterprise cloud management.

The project may include the following repositories:

- `zsvirt`: core IaaS engine and cloud infrastructure foundation including enterprise operations and advanced scenario
- `zsvirt-utility`: installation, diagnostics and host/vm operations
- `zsvirt-ui`: UI architecture for virtualization and cloud

Repository names, module boundaries, and release processes may be adjusted before the official open-source publication.

## Roles

### Maintainers

Maintainers are responsible for the overall health and direction of the project.

Maintainer responsibilities include:

- Reviewing and merging pull requests
- Triage issues and discussions
- Maintaining project quality and compatibility
- Managing releases and roadmap updates
- Reviewing security-related changes
- Helping contributors understand project direction
- Ensuring that governance, contribution, and security processes are followed

### Reviewers

Reviewers help review issues, pull requests, documentation, tests, and design proposals.

Reviewer responsibilities include:

- Reviewing code or documentation changes
- Giving constructive feedback
- Helping validate bug reports and feature requests
- Recommending whether a pull request is ready to merge

Reviewers may not have merge permission unless they are also maintainers.

### Contributors

Contributors are individuals or organizations who participate in the project through issues, pull requests, discussions, documentation, tests, tools, or community support.

Contributors are expected to follow:

- [CONTRIBUTING.md](CONTRIBUTING.md)
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
- [SECURITY.md](SECURITY.md)

## Decision Making

ZVirt uses a maintainer-led decision-making model during the open-source preview stage.

Routine decisions may be made by maintainers through issues, pull requests, and discussions.

Examples of routine decisions include:

- Bug fixes
- Documentation updates
- Test improvements
- Small feature improvements
- Refactoring without behavior changes

Significant changes should be discussed before implementation.

Examples of significant changes include:

- Architecture changes
- Public API changes
- Repository or module boundary changes
- Compatibility-impacting changes
- Release process changes
- Security-sensitive changes
- Large feature additions

Significant changes should be proposed through GitHub Issues or Discussions before a pull request is submitted.

## Pull Request Review

Pull requests should follow the process described in [CONTRIBUTING.md](CONTRIBUTING.md).

A pull request generally requires maintainer review before it can be merged.

Maintainers may consider:

- Correctness
- Maintainability
- Compatibility
- Security impact
- Test coverage
- Documentation impact
- Alignment with project scope and roadmap

Large or risky changes may require additional review.

## Roadmap

The project roadmap is maintained in [ROADMAP.md](ROADMAP.md).

The roadmap may include:

- Planned features
- Documentation work
- Installation and deployment improvements
- Migration tooling
- CI and testing improvements
- Open-source release preparation
- Compatibility and upgrade work

Roadmap changes should be discussed through GitHub Issues, Discussions, or maintainer review.

## Releases

The release process will be defined as the project moves toward official open-source publication.

Release responsibilities may include:

- Version planning
- Release branch preparation
- Build and test verification
- Release notes
- Upgrade notes
- Security fixes
- Artifact publication
- Documentation updates

Release candidates and official releases should be approved by project maintainers.

## Security

Security vulnerabilities must not be reported through public GitHub issues.

Please follow [SECURITY.md](SECURITY.md) for vulnerability reporting and disclosure coordination.

Security-related changes may require maintainer review before disclosure or release.

## Community

Community collaboration may happen through:

- GitHub Issues
- GitHub Pull Requests
- GitHub Discussions
- Project documentation
- Community website

Community links:

[Discussions](https://github.com/orgs/zsvirt/discussions) |
[Community Website](https://zsvirt.io/) |
[Product Documentation](https://zsvirt.io/docs/)

## Code of Conduct

All participants are expected to follow [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

The project aims to provide a respectful, inclusive, and collaborative environment for users, contributors, maintainers, and partners.

## Contributor License Agreement

Contributors may be required to sign a Contributor License Agreement before their pull requests can be accepted.

If the project is published under the Alibaba GitHub organization, the CLA process may follow Alibaba open-source governance requirements.

Pull requests that require CLA verification may not be merged until the CLA check is completed.

## Governance Changes

This governance document may evolve as the project grows.

Changes to this document should be proposed through a pull request and reviewed by maintainers.

Major governance changes should be discussed publicly before being merged.
