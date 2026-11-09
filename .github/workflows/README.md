# Release and description synchronization

Configure these GitHub Actions secrets and variables before publishing:

| Kind | Name | Purpose |
| --- | --- | --- |
| Secret | `MODRINTH_TOKEN` | Modrinth token with version-publishing and `PROJECT_WRITE` permissions |
| Variable | `MODRINTH_ID` | Modrinth project ID or slug; defaults to `5tC6IJAL` |
| Secret | `CURSEFORGE_TOKEN` | CurseForge author API token with access to the project |
| Variable | `CURSEFORGE_ID` | Numeric CurseForge project ID |
| Variable | `CURSEFORGE_SLUG` | CurseForge project slug, required by Stonecraft's publisher configuration |

Pushing a `v*` tag publishes both loader artifacts to GitHub, Modrinth, and CurseForge. A push to `main` that changes `Modrinth.md` or `CurseForge.md` synchronizes the corresponding project description. Each description workflow can also be run manually from the Actions tab, and always reads the latest `main` content. Pull requests and other branches do not publish descriptions.

Modrinth's [project API](https://docs.modrinth.com/api/operations/modifyproject/) documents its description update. The CurseForge description workflow uses the `update-project` author endpoint used by community tooling; this endpoint is not listed in the [official upload API documentation](https://support.curseforge.com/support/solutions/articles/9000197321), so its availability and behavior are not guaranteed. Check the workflow result and the project page after synchronization. Failed requests fail the workflow; use the manual trigger to retry after fixing configuration or a temporary service issue.
