# 🚀 Reporthole – GitHub Workflow Guide

This guide walks you through how to clone the Reporthole repositories, create a feature branch from `develop`, make changes, commit, and push. Follow these steps every time you start working on a new feature.

---

## 📋 Prerequisites

- Download and install [GitHub Desktop](https://desktop.github.com/)
- Sign in with your GitHub account by clicking **Sign in to GitHub.com**
- Make sure you have been added to the **UrbanEye-UJ** organisation on GitHub

![Step 1 – Sign in to GitHub Desktop](./images/step1.png)

---

## 🔁 Part 1 – Clone the Backend Repository (`reporthole-be`)

### Step 1: Open GitHub Desktop and clone the repo

1. Open GitHub Desktop — you will see your repositories listed on the left
2. Click **"Clone a Repository from the Internet..."** on the right panel

![Step 2 – Clone a Repository from the Internet](./images/step2.png)

3. In the search box, type `urban` — you will see the **UrbanEye-UJ** organisation appear
4. Select **`UrbanEye-UJ/reporthole-be`**
5. Choose a Local Path (e.g. `/Users/yourname/Documents/GitHub/reporthole-be`)
6. Click **Clone**

![Step 3 – Select reporthole-be and clone](./images/step3.png)

> 💡 You will see two repos under UrbanEye-UJ: `reporthole-be` (backend) and `reporthole-ui` (frontend). Start with `reporthole-be`.

---

### Step 2: Switch to the `develop` branch

After cloning, the repo will be on the `main` branch by default. **Never work directly on `main`.**

1. Click the **Current Branch** dropdown at the top
2. Under **Other Branches**, click **`origin/develop`**

![Step 4 – Switch to origin/develop](./images/step4.png)

You are now on the `develop` branch.

---

### Step 3: Create your feature branch from `develop`

> ⚠️ Always create your feature branch **from `develop`**, not from `main`.

1. Click the **Current Branch** dropdown again
2. Click **New Branch**
3. The "Create a Branch" dialog will appear — under **"Create branch based on..."**, make sure **`develop`** is selected ✅

![Step 5 – Create a Branch dialog with develop selected](./images/step5.png)

4. Enter a branch name using the format:
   ```
   feature/your-feature-name
   ```
   For example: `feature/civilian-home-screen`

![Step 6 – Name your branch and confirm develop as base](./images/step6.png)

5. Click **Create Branch**

Your new branch is now based on `develop` and ready for your work.

---

### Step 4: Open the project and make your changes

1. Click **Open in Visual Studio Code** (or your preferred editor)
2. Make your code changes inside the project

---

### Step 5: Commit your changes

Once you have made changes, go back to GitHub Desktop. You will see your changed files listed under the **Changes** tab with a diff preview on the right.

![Step 7 – Changed files appear in the Changes tab](./images/step7.png)

1. In the **Summary** field (bottom left), write a short, clear commit message, e.g.:
   ```
   Add civilian home screen layout
   ```
2. Optionally add a description for more detail
3. Click **Commit to feature/your-feature-name**

![Step 8 – Write a commit message and commit](./images/step8.png)

---

### Step 6: Publish and push your branch

After committing, you will see a **"Publish your branch"** prompt — this means your branch only exists locally and hasn't been pushed to GitHub yet.

1. Click **Publish branch** to push your branch to GitHub for the first time
2. On subsequent commits, this button will say **Push origin** — click it to sync

![Step 9 – Publish your branch to GitHub](./images/step9.png)

Your branch and commits are now visible on GitHub for the team to see and review.

---

## 🔁 Part 2 – Clone the Frontend Repository (`reporthole-ui`)

Once the backend is set up, repeat the same process for the frontend.

### Step 1: Clone `reporthole-ui`

1. In GitHub Desktop, click the **Current Repository** dropdown (top left)
2. Click **Add** → **Clone Repository...**

![Step 10 – Add → Clone Repository for the second repo](./images/step10.png)

3. Search `urban` again and select **`UrbanEye-UJ/reporthole-ui`**
4. Set your Local Path (e.g. `/Users/yourname/Documents/GitHub/reporthole-ui`)
5. Click **Clone**

![Step 11 – Select reporthole-ui and clone](./images/step11.png)

---

### Step 2: Switch to `develop` and create your feature branch

Repeat the exact same steps from Part 1:

1. Switch to **`origin/develop`** from the Current Branch dropdown
2. Create a new branch using the `feature/your-feature-name` naming convention
3. Select **`develop`** as the base branch ✅
4. Click **Create Branch**

---

### Step 3: Make changes, commit, and push

Same as Part 1 Steps 4–6. Make your changes, write a clear commit message, and publish/push your branch.

---

## 📐 Branch Naming Convention

Always use lowercase with hyphens. Follow this format:

| Type | Format | Example |
|------|--------|---------|
| New feature | `feature/description` | `feature/civilian-home-screen` |
| Bug fix | `fix/description` | `fix/login-crash` |
| Hotfix | `hotfix/description` | `hotfix/null-gps-coordinates` |

---

## 🔄 Summary: The Full Workflow

```
main
 └── develop                    ← always branch FROM here
       └── feature/your-work    ← work happens here
             └── commit & push  ← then open a Pull Request → develop
```

1. Clone the repo
2. Switch to `develop`
3. Create a `feature/` branch **from `develop`**
4. Write code → commit with a clear message
5. Publish/push your branch
6. Open a Pull Request on GitHub targeting `develop`

---

## ❗ Important Rules

- **Never commit directly to `main` or `develop`**
- Always create your branch **from `develop`**
- Keep commit messages short and descriptive
- Push regularly so the team can see your progress
- When your feature is done, open a **Pull Request** on GitHub from your branch → `develop`

---

*UrbanEye-UJ · Reporthole · Capstone Project*