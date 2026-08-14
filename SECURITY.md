## Reporting a Vulnerability

The ZSvirt project takes security vulnerabilities seriously.

If you believe you have found a security vulnerability in ZSvirt, please report it responsibly. Do not open a public GitHub issue for security vulnerabilities.

Please contact the security team by email:

- security@zsvirt.io

If the security mailbox is not available, please contact:

- zsvirt@zstack.io

We will review the report and follow up as soon as possible.

## What to Include

Please include as much information as possible to help us understand, reproduce, and assess the issue.

A useful security report should include:

- A clear description of the vulnerability
- Affected repository, component, module, or feature
- Affected version, branch, commit, or build, if known
- Deployment mode and environment details
- Steps to reproduce the issue
- Proof of concept, if available
- Potential impact and attack scenario
- Logs, error messages, screenshots, or traces
- Known mitigations or workarounds
- Whether the issue has been publicly disclosed elsewhere

Please avoid sharing secrets, private keys, passwords, customer data, or unrelated sensitive information in the report.

## Scope

This security policy applies to ZSvirt community repositories under the `ZSvirt` organization, including but not limited to:

- `zvirt`
- `zvirt-utility`
- `...`

Some repositories or components may have their own security policy. If a repository contains a separate `SECURITY.md`, please follow the policy in that repository.

## Out of Scope

The following issues are usually not considered security vulnerabilities unless they demonstrate a clear security impact:

- General bugs without a security impact
- Missing security headers in non-production documentation sites
- Social engineering attacks
- Denial-of-service reports without practical exploitability
- Vulnerabilities in unsupported versions
- Issues that require full administrative access without privilege escalation
- Reports from automated scanners without analysis or reproducible impact

If you are unsure whether an issue qualifies as a security vulnerability, please report it privately.

## Response Process

After receiving a security report, the project maintainers will try to:

1. Acknowledge receipt of the report.
2. Triage and validate the vulnerability.
3. Determine affected versions and components.
4. Work on a fix or mitigation.
5. Coordinate disclosure when appropriate.
6. Publish a security notice or advisory if needed.

Response timelines may vary depending on the severity, complexity, and affected components.

## Confidentiality

Please keep the vulnerability confidential until the project maintainers have completed triage and coordinated a fix or mitigation.

Information shared with the security team will be handled carefully to reduce the risk of misuse or accidental disclosure.

## Public Disclosure

Please do not publicly disclose a vulnerability before coordinated disclosure is completed.

Once a fix or mitigation is available, the project may publish a security notice, advisory, release note, or upgrade recommendation.

## Security Updates

Security fixes may be delivered through:

- Patch releases
- Hotfixes
- Upgrade instructions
- Configuration mitigations
- Security advisories
- Documentation updates

Users are encouraged to keep ZSphere components up to date and follow published security guidance.

## Third-Party Components

ZSvirt may include or depend on third-party open-source software.

Security vulnerabilities in third-party components may be handled by upgrading, patching, configuration changes, or other mitigations. Please include the affected third-party component and version in your report if known.

## Acknowledgements

We appreciate responsible disclosure and may acknowledge reporters who help improve the security of ZSvirt, unless they prefer to remain anonymous.
