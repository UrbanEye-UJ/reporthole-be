# Reporthole Backend — Git Guide

This guide covers everything you need to work with Git on this project. Follow these conventions so the team stays in sync and the codebase stays clean.

---

## First time setup

Before you do anything, make sure Git knows who you are. This is how your name appears on commits.

```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

Clone the repo:

```bash
git clone <your-repo-url>
cd reporthole/reporthole-be
```

---

## Branch strategy

We use a simple feature branch workflow. **Never commit directly to `main`.**

| Branch | Purpose |
|--------|---------|
| `main` | Stable, production-ready code only |
| `develop` | Integration branch — merge your features here first |
| `feature/your-feature-name` | Your working branch for a new feature |
| `fix/your-fix-name` | Your working branch for a bug fix |

### Branch naming examples

```
feature/user-authentication
feature/manual-report-submission
feature/dashcam-duplicate-detection
fix/jwt-expiry-bug
fix/pothole-gps-capture
```

---

## Daily workflow

### 1. Before you start working — sync with the team

Always pull the latest changes before starting anything new:

```bash
git checkout develop
git pull origin develop
```

### 2. Create your feature branch

```bash
git checkout -b feature/your-feature-name
```

### 3. Check what files you've changed

```bash
git status
```

### 4. Stage your changes

Stage specific files (recommended):

```bash
git add src/main/java/za/co/urbaneye/reporthole/controller/ReportController.java
```

Stage everything at once:

```bash
git add .
```

### 5. Commit your changes

```bash
git commit -m "feat: add manual report submission endpoint"
```

See the [commit message guide](#commit-messages) below for how to write good commit messages.

### 6. Push your branch to GitHub

First push (creates the branch remotely):

```bash
git push -u origin feature/your-feature-name
```

Subsequent pushes:

```bash
git push
```

### 7. Open a Pull Request

Go to GitHub, open a Pull Request from your branch into `develop`, and ask a teammate to review it. Do not merge your own PR.

---

## Commit messages

We follow a simple convention so the commit history is readable.

**Format:**
```
<type>: <short description>
```

**Types:**

| Type | When to use |
|------|------------|
| `feat` | Adding a new feature |
| `fix` | Fixing a bug |
| `chore` | Config changes, dependency updates, build changes |
| `docs` | Documentation only |
| `refactor` | Code restructure with no behaviour change |
| `test` | Adding or updating tests |
| `style` | Formatting, whitespace (no logic changes) |

**Examples:**

```bash
git commit -m "feat: add JWT authentication filter"
git commit -m "fix: resolve null pointer in report duplicate check"
git commit -m "chore: update spring boot to 3.5.9"
git commit -m "docs: update local setup guide"
git commit -m "refactor: extract image compression into service layer"
git commit -m "test: add unit tests for report service"
```

---

## Keeping your branch up to date

If `develop` has moved on while you were working, bring those changes into your branch:

```bash
git checkout develop
git pull origin develop
git checkout feature/your-feature-name
git merge develop
```

If there are merge conflicts, see the [resolving conflicts](#resolving-merge-conflicts) section below.

---

## Resolving merge conflicts

When Git can't automatically merge changes, it will mark the conflicting sections in the file like this:

```
<<<<<<< HEAD
your version of the code
=======
teammate's version of the code
>>>>>>> develop
```

To resolve:

1. Open the conflicting file in IntelliJ
2. IntelliJ shows a visual merge tool — click **Resolve** next to the file in the Git panel
3. Choose which version to keep (or combine both manually)
4. Save the file
5. Stage and commit the resolution:

```bash
git add .
git commit -m "fix: resolve merge conflict in ReportService"
```

> If you're unsure which version to keep, ask the teammate who wrote the other version before deciding.

---

## Undoing mistakes

**Undo changes to a file before staging (go back to last commit):**

```bash
git checkout -- src/main/java/za/co/urbaneye/reporthole/service/ReportService.java
```

**Unstage a file you accidentally staged:**

```bash
git restore --staged src/main/java/za/co/urbaneye/reporthole/service/ReportService.java
```

**Undo your last commit but keep the changes (soft reset):**

```bash
git reset --soft HEAD~1
```

**Undo your last commit and discard the changes (hard reset — careful):**

```bash
git reset --hard HEAD~1
```

> ⚠️ Never use `git reset --hard` on commits that have already been pushed to GitHub. This rewrites history and causes problems for the whole team.

---

## Useful day-to-day commands

**See your full commit history:**

```bash
git log --oneline
```

**See what changed in a specific commit:**

```bash
git show <commit-hash>
```

**See all branches (local and remote):**

```bash
git branch -a
```

**Switch to an existing branch:**

```bash
git checkout feature/some-other-branch
```

**Delete a branch after it's been merged:**

```bash
# Delete locally
git branch -d feature/your-feature-name

# Delete on GitHub
git push origin --delete feature/your-feature-name
```

**Stash work in progress (save without committing):**

```bash
# Stash your current changes
git stash

# Get them back later
git stash pop
```

This is useful when you need to quickly switch branches without committing half-finished work.

---

## What not to commit

These should never be committed to Git. They are already listed in `.gitignore` but be aware of them:

- `.env` — contains your `JASYPT_ENCRYPTOR_PASSWORD`
- `target/` — Maven build output
- `.idea/` — IntelliJ project files
- Any file containing plain-text passwords, API keys, or secrets

If you accidentally commit a secret, tell the team immediately so the key can be rotated.

---

## Quick reference card

```bash
# Start of day
git checkout develop && git pull origin develop

# New feature
git checkout -b feature/your-feature-name

# Save your work
git add .
git commit -m "feat: description of what you did"
git push

# Sync with develop mid-feature
git merge develop

# End of feature — push and open PR on GitHub
git push -u origin feature/your-feature-name
```