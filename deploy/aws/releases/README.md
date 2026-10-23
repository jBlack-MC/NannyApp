# AWS Android download directory

For the AWS compose deployment, put the tested signed native APK at `NannyApp-latest.apk` here. The production stack mounts it privately for the website download handler; replacing this file changes the download without rebuilding the server image. Keep the previous signed APK outside Git for rollback.

This is the AWS filesystem alternative, not the hosted Vercel/GitHub Release path. Local development uses [shared releases](../../../NannyApp.Shared/releases/README.md). See [CI/CD](../../../docs/CI_CD.md) for signed publishing and [AWS deployment](../../../docs/AWS_DEPLOYMENT.md) for mount configuration.

Verify the embedded HTTPS API URL, actual Android version code, signature and SHA-256 before replacement. Keep the signing key stable for upgrades. APKs and signing secrets must stay out of Git.
