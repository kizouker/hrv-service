# GH-900 – GitHub Foundations – 15-minute labs

Study guide (skills as of January 2026): https://learn.microsoft.com/en-us/credentials/certifications/resources/study-guides/gh-900

One lab = one thing. The repo is [github.com/kizouker/hrv-service](https://github.com/kizouker/hrv-service). These labs look, and one of them makes a local commit you delete. They do not push, they do not force-push, and they do not change `main`.

★ = do these four first if time is short (about 1 hour). All seven take about 2 hours.

Status: [ ] not done · [~] in progress · [x] done

## Before you start

```bash
cd "$(git rev-parse --show-toplevel)"
```

```bash
git status
```

Stop if the tree is dirty. A step that spends money or changes a live GitHub setting is marked **Live setting**.

---

## Git and the repo files

### [ ] Lab 1 ★ GitHub Flow, and the Markdown a PR body uses

Exam area: Git and GitHub basics (25–30%), including GitHub Flow and Markdown.

GitHub Flow is: branch, commit, open a pull request, review, merge. This sitting is the branch and the commit. Lab 4 is where you look at a pull request that already exists. Do not open a pull request for this empty commit.

`README.md` is the Markdown sample. Find one heading (`# HRV Backend API`), one table (the API overview), and one fenced code block (the Swift sample or the `mvn verify` block). A pull request body is the same kind of Markdown.

Write down the current commit. You will compare it at the end.

```bash
git rev-parse --short HEAD
```

```bash
git switch -c lab-gh900-flow
```

```bash
git commit --allow-empty -m "Lab: practice GitHub Flow"
```

```bash
git log -1 --oneline
```

Return to the branch you started on and delete the practice branch. `--allow-empty` made a commit with no file change. `git branch -D` drops that local commit. It does not touch the remote.

```bash
git switch -
```

```bash
git branch -D lab-gh900-flow
```

```bash
git rev-parse --short HEAD
```

The short hash matches the one you wrote down. Do not run `git push`. Do not run `git push --force`.

Done when: `lab-gh900-flow` is gone, the hash matches, and you can name one heading and one table in `README.md`.

### [ ] Lab 2 ★ README, and the four repo files this tree does not have

Exam area: Repositories (10–15%): README, LICENSE, CONTRIBUTING, CODEOWNERS, SECURITY.

```bash
for f in README.md LICENSE CONTRIBUTING.md CODEOWNERS .github/CODEOWNERS SECURITY.md .github/SECURITY.md; do
  if [ -f "$f" ]; then echo "present  $f"; else echo "absent   $f"; fi
done
```

Read the first screen of `README.md` (title, quick start, API table). The other names are the exam list. On this commit of hrv-service the README is present and the rest are absent.

One line each, from the study guide:

- `LICENSE` — the terms for reusing the code
- `CONTRIBUTING` — how someone proposes a change
- `CODEOWNERS` — who is asked to review which paths
- `SECURITY` — how to report a vulnerability

Do not add those files in this lab.

Done when: the loop prints `present` for `README.md` and `absent` for the other six paths, and you can say what each missing file is for.

### [ ] Lab 3 ★ Actions in ci.yml, Codespaces, and Copilot plans

Exam area: Modern development (10–15%): Actions, Copilot plans, Codespaces vs github.dev.

Open `.github/workflows/ci.yml`.

- The workflow `name` is `CI`.
- `on` is `push` to `main` and `pull_request` to `main`.
- Job `build-and-test` runs on `ubuntu-24.04`, uses `actions/checkout@v7` and `actions/setup-java@v6`, then `mvn -B verify`.
- Job `docker-build` needs `build-and-test` and runs `docker build -t hrv-backend:ci .`.

Two editors:

- github.dev is the browser editor. On the repo page, the `.` key opens it. There is no virtual machine.
- A codespace is a cloud virtual machine for the same repo.

**Live setting.** Creating a codespace can bill. Do not create one until you choose to.

Copilot plans are a study-guide bullet on this same skill. Open your own Copilot account page and read the plan name. Do not start a paid plan or a trial until you choose to.

Done when: you can name the CI trigger and both jobs, and you can say how github.dev differs from a codespace, without having created one.

---

## Working with people

### [ ] Lab 4 Issues, pull requests, and discussions

Exam area: Collaboration (10–15%): issues, pull requests, discussions, linking PRs to issues.

Open https://github.com/kizouker/hrv-service/issues and https://github.com/kizouker/hrv-service/pulls. Read only.

A pull request body line links the PR to an issue. When that PR merges into the default branch, GitHub closes the issue:

```text
Fixes #12
```

Use an issue number you can see, or write `Fixes #0` if the list is empty. Keep the line in your notes. Do not create an issue. Do not comment on one.

Open the Discussions tab on the same repo. If discussions are off, leave them off. Turning them on changes the repo.

Done when: the `Fixes #N` line is in your notes, and you did not create an issue or enable discussions.

### [ ] Lab 5 Projects

Exam area: Projects (5–10%).

Open https://github.com/kizouker/hrv-service/projects

A project is a board. The cards are issues and pull requests, arranged in columns such as Todo and Done. An empty list means this repo has no project board.

Do not create a project. A new board is a live addition on the account.

Done when: you know whether a board is listed, and you did not add one.

### [ ] Lab 6 ★ 2FA, roles, and branch protection

Exam area: Privacy, security, administration (10–15%): 2FA, roles, branch protection.

Sign-in security for your user: https://github.com/settings/security

Look at two-factor authentication. Leave the switch where it is. Turning 2FA off removes a sign-in check.

Repository roles, from the study guide: Read, Triage, Write, Maintain, Admin. On this repo that page is Settings → Collaborators and teams. Do not invite anyone, and do not change a role.

Branch protection and rulesets: https://github.com/kizouker/hrv-service/settings/rules

**Live setting.** A ruleset or a classic branch protection rule can block pushes to `main`, including yours. Do not create a ruleset. Do not add a branch protection rule. Do not save that page.

Done when: you can list the five roles, and the rules page is unchanged.

---

## Community

### [ ] Lab 7 Forks, templates, InnerSource, Sponsors, Marketplace

Exam area: Community (5–10%): forks, templates, InnerSource, Sponsors, Marketplace.

Five words, this repo:

- Fork — the Fork button would copy kizouker/hrv-service to your account. Do not click it.
- Template — a template repo offers "Use this template" on the home page. This home page is an ordinary repository. Do not turn the template setting on.
- InnerSource — the study-guide name for sharing a private repo inside one company. This public learning repo is not that program. There is no switch to flip.
- Sponsors — https://github.com/sponsors is the sponsorship page. Do not create a sponsor profile, and do not add `FUNDING.yml`.
- Marketplace — `.github/workflows/ci.yml` already uses two Marketplace actions: `actions/checkout@v7` and `actions/setup-java@v6`. Open one action's page. Do not publish an action.

Done when: you can match each of the five words to a fact above, and the repo settings are unchanged.
