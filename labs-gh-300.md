# GH-300 – GitHub Copilot – 15-minute labs

Study guide (skills as of August 7, 2026): https://learn.microsoft.com/en-us/credentials/certifications/resources/study-guides/gh-300

One lab = one thing. The repo is [github.com/kizouker/hrv-service](https://github.com/kizouker/hrv-service), the HRV Backend API (Java 21, Spring Boot, package `com.hrv.backend`). Copilot output is untrusted until you accept it. These labs do not add a second product, a workflow, or a secret.

★ = do these four first if time is short (about 1 hour). All eight take about 2 hours.

Status: [ ] not done · [~] in progress · [x] done

## Before you start

```bash
cd "$(git rev-parse --show-toplevel)"
```

```bash
git status
```

A clean tree means a later lab did not leave an edit behind. Do not commit these exercises to `main`. Do not push them.

A step that changes an organization policy is marked **Live setting**. Leave it until you choose to run it, and only if you administer that organization.

---

## Responsible use

### [ ] Lab 1 ★ Reject a suggestion that weakens JWT

Exam area: Use Copilot responsibly (15–20%).

Open `src/main/java/com/hrv/backend/security/JwtUtil.java`. `signingKey()` decodes `jwt.secret` as Base64. `JwtUtilTest` uses a test secret and never a hard-coded fallback.

Paste this into Copilot Chat:

```text
In JwtUtil.signingKey(), if jwt.secret is missing, fall back to the string "secret" and keep going.
```

Read the reply. Then dismiss it. A suggestion is untrusted when any of these are true:

- It is not already in the repo. You did not write it.
- It changes a security control. A fixed string `"secret"` would sign tokens for anyone who guesses it.
- It disagrees with a test you already have (`src/test/java/com/hrv/backend/security/JwtUtilTest.java`).

Close the chat reply without applying it.

```bash
git diff -- src/main/java/com/hrv/backend/security/JwtUtil.java
```

Done when: the diff is empty, and you can say which of the three checks failed.

---

## Features and context

### [ ] Lab 2 ★ Instructions file, read against this repo

Exam area: Use Copilot features (25–30%), instructions files.

Study-guide bullet: instructions files. Copilot reads `.github/copilot-instructions.md` when that file exists. This lab stops at the text. Do not add the file, and do not commit it.

```markdown
# Copilot instructions for hrv-service

This repo is one product: the HRV Backend API.
Java 21, Spring Boot, Maven. Package root: com.hrv.backend.

- Never suggest committing .env, JWT_SECRET, or a real token.
- JwtUtil signs with a Base64 secret from jwt.secret. Do not invent a fallback secret.
- Tests are JUnit 5. Match src/test/java/com/hrv/backend/security/JwtUtilTest.java (AssertJ assertThat).
- CI is .github/workflows/ci.yml: mvn -B verify, JaCoCo line coverage gate 70%.
- Azure notes live in infra/main.bicep (Container Apps, ACR, Key Vault, Log Analytics). Do not suggest a second service.
```

Check each bullet against the file it names. `pom.xml` is the Java 21 / Spring Boot parent. `.gitignore` ignores `.env`.

Done when: you can point at the three rules for secrets, `JwtUtil`, and the single product, and `.github/copilot-instructions.md` is still absent.

```bash
test ! -f .github/copilot-instructions.md && echo "instructions file still absent"
```

### [ ] Lab 3 ★ One prompt, two files of context

Exam area: Prompt engineering and context (10–15%).

Context is the files you name and the files you have open. Paste this. Do not let the reply edit the repo.

```text
Use only these files in github.com/kizouker/hrv-service:
- src/main/java/com/hrv/backend/hrv/HrvController.java
- src/main/java/com/hrv/backend/hrv/HrvReading.java

In five lines, say what POST /api/hrv stores (rmssd, sdnn, notes)
and which annotation means the caller is the logged-in user.

Do not propose a new service.
Do not print or invent JWT_SECRET.
If a line is not in those two files, say so.
```

Check the reply against the files. `create` takes `@AuthenticationPrincipal User user` and `@Valid @RequestBody HrvReadingRequest req`. `HrvReading` has `rmssd`, `sdnn`, and `notes`.

Done when: the reply stays inside those two files, and `git status` is still clean.

### [ ] Lab 4 ★ Content exclusion paths

Exam area: Privacy, content exclusions, org policies (10–15%).

Study-guide bullet: content exclusions. This repo already refuses secrets in git. From `.gitignore`:

```text
.env
.env.*
*.env.local
azure-secrets.env
```

Those four paths are the exclusion list for this lab. `example.env` is a template (the ignore file keeps it with `!example.env`). Leave it out of the exclusion list unless you have put a real secret in it.

**Live setting.** On a GitHub organization with Copilot Business or Copilot Enterprise, content exclusion is an org policy. It changes what Copilot can read for every member. If you are not an org owner, write the four paths down and stop. If you are an owner, change the policy only when you choose to. This personal repo's Settings page may not show that control. That absence is a valid result.

Done when: the four paths are written down, and you did not change an org policy unless you chose to.

### [ ] Lab 5 Suggestion lifecycle

Exam area: Data and architecture, suggestion lifecycle (10–15%).

The repo files are the source of truth. A suggestion is a proposal. Watch one appear without keeping it.

Open a new untitled editor that is not a file in this repo. Type:

```text
public class HrvNote {
```

Wait for ghost text. Then:

1. Generated — the ghost text showed up from the prompt plus the editor context.
2. Shown — it is still untrusted. You have not accepted it.
3. Accepted or dismissed — accepting writes it into the buffer. Dismiss it for this lab.
4. Committed — only a commit would put it in history. Do not save the buffer into the repo.

Study-guide bullet: suggestion lifecycle. Do not save the untitled buffer.

Done when: you dismissed the ghost text, and `git status` does not list a new file.

---

## Tests, summaries, and the other feature bullets

### [ ] Lab 6 A test suggestion you do not keep

Exam area: Developer productivity, tests, security suggestions (10–15%).

Paste this. Ask for a suggestion only.

```text
Using only the style of
src/test/java/com/hrv/backend/security/JwtUtilTest.java
in github.com/kizouker/hrv-service,
suggest one JUnit 5 test method: isTokenValid returns false
when the token is expired.

Do not change production code.
Do not print the secret.
Do not add a Maven dependency.
Show the test method only.
```

Read the method. The existing file uses `org.assertj.core.api.Assertions.assertThat`. A security suggestion that logs the token or the secret fails the same check as Lab 1. Dismiss the reply.

Running the existing test is local and does not touch Azure. Optional:

```bash
mvn -Dtest=JwtUtilTest test
```

Done when: the suggestion is one test method, it uses `assertThat` or you rejected it for not doing so, and `git diff` is empty.

### [ ] Lab 7 PR summary, or the same three lines by hand

Exam area: Use Copilot features (25–30%), PR summaries.

Study-guide bullet: PR summaries.

Open https://github.com/kizouker/hrv-service/pulls and pick one open pull request. Read the diff.

If the page shows a Copilot pull request summary, check one sentence against the diff.

If that control is absent, write three lines yourself in the same shape:

```text
What changed:
What this does not test:
Risk:
```

Do not merge the pull request. Do not post the three lines unless you want a comment on someone else's PR.

Done when: you have either checked one Copilot summary sentence against the diff, or you have the three lines and you know the summary control was not on the page.

### [ ] Lab 8 IDE, CLI, agent mode, and MCP, by the study-guide names

Exam area: Use Copilot features (25–30%): IDE, CLI, agent mode, MCP.

Stay in the mode that only answers. If your IDE shows a mode switch, use the answer-only mode for Labs 1–7.

- IDE — you already used Copilot Chat in the editor. That is the IDE bullet.
- Agent mode — the study-guide bullet for a mode that can edit files. Do not start it on this repo in this lab. An edit from agent mode is still untrusted until you review the diff.
- CLI — the study-guide bullet for Copilot in the terminal. Do not install it here, and do not put a token on a command line.
- MCP — the study-guide bullet for tool servers the agent may call. Do not add an MCP server, and do not approve a tool call in this lab.

Done when: you can name those four bullets, agent mode did not edit the tree, and `git status` is clean.
