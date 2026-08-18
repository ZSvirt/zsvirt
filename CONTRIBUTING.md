
# Contributing to ZSvirt

Thank you for your interest in contributing to ZSvirt.

ZSvirt is an open infrastructure project for private cloud, server virtualization, and enterprise cloud management. We welcome contributions from users, developers, partners, and infrastructure teams.

Contributions may include bug reports, feature proposals, documentation improvements, tests, tools, integrations, and code changes.

## Before You Start

Before contributing, please read:

- [Code of Conduct](CODE_OF_CONDUCT.md)
- [Governance](GOVERNANCE.md)
- [Security Policy](SECURITY.md)
- [License](LICENSE.txt)

By participating in this project, you are expected to follow the project Code of Conduct.

## Contributor License Agreement

All external contributors are required to sign the ZSvirt Contributor License Agreement (CLA) before their pull requests can be merged.

The CLA confirms that:

1. You have the right to submit the contribution, and the contribution is your original work or you have obtained all necessary rights to submit it.

2. To the best of your knowledge, your contribution does not knowingly violate any third-party copyright, patent, trade secret, or other right.

3. You grant the project maintainer and its affiliated organizations a perpetual, irrevocable, worldwide, non-exclusive, royalty-free, sublicensable copyright license to use, modify, reproduce, prepare derivative works of, distribute, sublicense, relicense, publicly perform, and publicly display your contribution as part of ZSvirt.

4. You grant the project maintainer a commercial relicensing right, permitting the project maintainer to relicense your contribution under any license terms of its choice. These terms may include proprietary or commercial licenses that differ from the open-source license applicable to the project.

   This right enables the project maintainer to offer ZSvirt under a dual-licensing model, providing:

   - An open-source release under GPLv3
   - A commercial release under a separate commercial license for enterprise users who prefer not to be bound by GPLv3 obligations

   You also grant the right to distribute your contributed content and derivative works under any license selected for the project.

5. You grant the project maintainer a perpetual, irrevocable, worldwide, non-exclusive, royalty-free patent license to make, use, sell, offer for sale, and import any contribution that embodies your patent claims, for the purpose of enabling the project maintainer to use, distribute, and commercially relicense the contribution.

   If a contributor initiates patent litigation alleging that the contribution or project infringes a patent, any patent license granted under this agreement may terminate as permitted by law.

6. This authorization is irrevocable and survives the termination of your participation in the project.

   Once granted, the rights above cannot be withdrawn. The project maintainer may continue to exercise them in all existing and future distributions of ZSvirt, including commercial distributions, enterprise editions, hosted services, and derivative works.

This authorization allows contributions to be used in:

- ZSvirt open-source releases
- Commercial distributions
- Enterprise editions
- Hosted services
- Derivative works
- Other project-maintained distributions

If you are contributing on behalf of your employer or another organization, your employer or organization may be required to sign a Corporate CLA that provides the same grants.

Pull requests from contributors who have not completed the required CLA check will not be merged.

The CLA process follows the ZSvirt open-source governance requirements and the CLA verification process configured for this repository.

## Ways to Contribute

You can contribute in many ways:

- Report bugs
- Request features
- Improve documentation
- Submit code changes
- Add or improve tests
- Improve installation or migration guides
- Share production experience and best practices
- Help review issues and pull requests

## Reporting Issues

Please use GitHub Issues to report bugs, request features, or ask project-related questions.

Before opening a new issue:

- Search existing issues to avoid duplicates.
- Use the appropriate issue template.
- Provide enough information for maintainers to understand and reproduce the problem.
- Include logs, screenshots, version information, and environment details when applicable.

Security vulnerabilities must not be reported through public issues. Please follow the instructions in [SECURITY.md](SECURITY.md).

## Issue Types

Common issue types include:

- Bug Report
- Feature Request
- Question
- Engineering Task
- Documentation

Please choose the issue type that best matches your request.

## Pull Request Workflow

ZSvirt uses GitHub pull requests to manage code and documentation contributions.

Recommended workflow:

1. Fork the repository.
2. Create a new branch from the latest `main` branch.
3. Make your changes in the new branch.
4. Add or update tests when applicable.
5. Update documentation when behavior changes.
6. Run local checks before submitting.
7. Push your branch to your fork.
8. Open a pull request against the upstream repository.
9. Respond to review comments and update the pull request as needed.

Please keep each pull request focused on a single topic. Large changes should be discussed in an issue before implementation.

## Branch Naming

Use clear branch names that describe the purpose of the change.

Examples:

- `fix/vm-start-timeout`
- `feature/vmware-migration-check`
- `docs/update-installation-guide`
- `test/add-storage-unit-tests`

## Commit Messages

Use clear and descriptive commit messages.

Recommended format:

```text
component: short description
```

Examples:

```text
vm: fix instance start timeout handling
docs: update quick start guide
storage: add validation for data storage capacity
```

Avoid vague messages such as:

```text
fix bug
update code
change files
```

## Pull Request Description

A pull request should describe:

- What changed
- Why the change is needed
- How the change was tested
- Related issues
- Compatibility or upgrade impact
- Screenshots for UI changes, if applicable

If the pull request fixes an issue, link it in the description.

Example:

```text
Fixes #123
```

## Tests

Please run all relevant tests before submitting a pull request.

Depending on the repository, tests may include:

- Unit tests
- Integration tests
- Build checks
- Documentation checks
- Static analysis
- Installation or upgrade verification

If tests cannot be run locally, explain why in the pull request description.

## Documentation

Documentation contributions are welcome.

Please update documentation when:

- User-facing behavior changes
- Installation steps change
- Configuration options are added, changed, or removed
- APIs are added, changed, or deprecated
- New troubleshooting procedures are discovered
- Migration or upgrade behavior changes

Documentation should be clear, accurate, and easy to follow.

## Code Review

Maintainers review pull requests for:

- Correctness
- Maintainability
- Compatibility
- Security
- Alignment with the project direction

A pull request may require changes before it can be merged.

Please keep discussions respectful and focused on the technical topic.

## Compatibility

ZSvirt is infrastructure software, so compatibility is important.

When making changes, consider:

- Upgrade compatibility
- API compatibility
- Configuration compatibility
- Data migration impact
- Deployment and rollback impact
- Integration impact on other components

Breaking changes must be clearly documented and discussed before implementation.

## Security

Do not include any of the following in issues, pull requests, commits, logs, or screenshots:

- Secrets
- Private keys
- Credentials
- Customer data
- Internal-only information

If you discover a security vulnerability, follow the instructions in [SECURITY.md](SECURITY.md).

## License

By contributing to this project, you agree that your contributions will be licensed under the project license (GPLv3) for the open-source release.

You also agree that you have granted the project maintainer the commercial relicensing rights described in the [Contributor License Agreement](#contributor-license-agreement) section above for the purpose of dual licensing.

This project is currently planned to use the GNU General Public License version 3.0.

Some repositories or components may include third-party open-source software under different licenses. Please check the `LICENSE`, `NOTICE`, and related files in each repository for details.

Thank you for helping improve ZSvirt.
