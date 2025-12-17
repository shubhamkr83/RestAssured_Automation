# IDE Find & Replace Guide - VariableManager Migration

## Quick Reference - Complete All Steps Below

**Estimated Time:** 10-15 minutes  
**IDE:** IntelliJ IDEA / VSCode / Eclipse (instructions for all)

---

## 🎯 Step 1: BOMB Token Migration (60+ files)

### Pattern 1A: Remove LoginApiTest Import

**Find (with Regex DISABLED):**
```
import com.automation.utils.VariableManager;
```

**Replace with:**
```
import com.automation.utils.VariableManager;
```

**Scope:** `src/test/java/com/automation/tests/bomb/`  
**Expected Matches:** ~60 files

---

### Pattern 1B: Update @BeforeClass Token Check (Part 1)

**Find (with Regex ENABLED):**
```
if \(LoginApiTest\.bombToken != null\) \{\s+authToken = LoginApiTest\.bombToken;\s+logger\.info\("Using BOMB token from LoginApiTest"\);
```

**Replace with:**
```
authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
```

**Scope:** `src/test/java/com/automation/tests/bomb/`  
**Expected Matches:** ~60 files

---

### Pattern 1C: Update @BeforeClass Token Check (Part 2 - cleanup else block)

**Find (with Regex ENABLED):**
```
\s+\} else \{\s+throw new RuntimeException\("Login token not available\. Please run LoginApiTest first\."\);\s+\}
```

**Replace with:**
```

```
(Empty - just delete this block)

**Scope:** `src/test/java/com/automation/tests/bomb/`  
**Expected Matches:** ~60 files

---

## 🛍️ Step 2: Buyer App Token Migration (29 files)

### Pattern 2A: Remove Buyer App Static Import

**Find (with Regex DISABLED):**
```
import com.automation.utils.VariableManager;
```

**Replace with:**
```
import com.automation.utils.VariableManager;
```

**Scope:** `src/test/java/com/automation/tests/buyerapp/`  
**Expected Matches:** 29 files

---

### Pattern 2B: Replace VariableManager.getBuyerAppToken() Usage

**Find (with Regex DISABLED):**
```
VariableManager.getBuyerAppToken()
```

**Replace with:**
```
VariableManager.getVariableManager.getBuyerAppToken()()
```

**Scope:** `src/test/java/com/automation/tests/buyerapp/`  
**Expected Matches:** 100+ occurrences across 29 files

⚠️ **IMPORTANT:** This will replace ALL instances. If any file has a variable named `VariableManager.getBuyerAppToken()`, skip that file manually.

---

## ✅ Step 3: Verify Changes

After completing all find & replace operations:

### 3.1: Check for Errors in IDE
- Look for any red squiggly lines
- Fix any compilation errors

### 3.2: Run Compilation
```bash
mvn clean compile -DskipTests
```

### 3.3: Expected Result
```
[INFO] BUILD SUCCESS
[INFO] Total time: 12-15 s
```

---

## 📱 IDE-Specific Instructions

### IntelliJ IDEA

1. **Open Find in Path:** `Ctrl+Shift+F` (Windows) / `Cmd+Shift+F` (Mac)
2. **Open Replace in Path:** `Ctrl+Shift+R` / `Cmd+Shift+R`
3. **Enable Regex:** Click `.*` button
4. **Set Scope:** Click "Scope" → "Custom" → Select directory path
5. **Preview:** Click "Find" first to preview matches
6. **Replace:** Click "Replace All"

### VSCode

1. **Open Find & Replace:** `Ctrl+Shift+H` / `Cmd+Shift+H`
2. **Enable Regex:** Click `.*` button
3. **Filter Files:** Use "files to include" field with glob pattern
   - BOMB: `**/bomb/**/*.java`
   - Buyer App: `**/buyerapp/**/*.java`
4. **Replace:** Click "Replace All"

### Eclipse

1. **Open Search:** `Ctrl+H` → "File Search" tab
2. **Enable Regex:** Check "Regular expression"
3. **Set Scope:** Choose "Enclosing projects" or specific folder
4. **Find:** Search first
5. **Replace:** Use "Replace" button in search results

---

## 🔍 Verification Checklist

After all replacements:

- [ ] All BOMB tests: No `LoginApiTest.bombToken` references remain
- [ ] All Buyer App tests: No `VariableManager.getBuyerAppToken()` static imports remain
- [ ] All files: `VariableManager` import added
- [ ] Compilation: ✅ SUCCESS
- [ ] No unexpected changes to other code

---

## 🚨 Common Issues & Fixes

### Issue 1: "VariableManager cannot be resolved"

**Fix:** Import was not added. Manually add:
```java
import com.automation.utils.VariableManager;
```

### Issue 2: Duplicate imports after replacement

**Fix:** Use IDE's "Optimize Imports" feature:
- IntelliJ: `Ctrl+Alt+O` / `Cmd+Option+O`
- Eclipse: `Ctrl+Shift+O`
- VSCode: Right-click → "Organize Imports"

### Issue 3: Some files still have compilation errors

**Fix:** These files likely have custom logic. Update manually using Phase 1 examples.

---

## ⚡ Quick Execution (Copy-Paste Ready)

### For IntelliJ Users (Fastest Method):

1. Press `Ctrl+Shift+R`
2. Copy each pattern below, paste in "Find" box
3. Copy corresponding replacement, paste in "Replace" box
4. Set directory scope
5. Click "Replace All"
6. Move to next pattern

**Pattern Set 1 (BOMB - do these 3 in sequence):**

```
Find 1: import com.automation.utils.VariableManager;
Replace 1: import com.automation.utils.VariableManager;

Find 2 (regex): if \(LoginApiTest\.bombToken != null\) \{\s+authToken = LoginApiTest\.bombToken;\s+logger\.info\("Using BOMB token from LoginApiTest"\);
Replace 2: authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");

Find 3 (regex): \s+\} else \{\s+throw new RuntimeException\("Login token not available\. Please run LoginApiTest first\."\);\s+\}
Replace 3: [leave empty]
```

**Pattern Set 2 (Buyer App - do these 2 in sequence):**

```
Find 1: import com.automation.utils.VariableManager;
Replace 1: import com.automation.utils.VariableManager;

Find 2: VariableManager.getBuyerAppToken()
Replace 2: VariableManager.getVariableManager.getBuyerAppToken()()
```

---

## 📊 Expected Results

After completion:

| Category | Files Updated | LOC Changed |
|----------|---------------|-------------|
| BOMB Tests | ~60 | ~180 |
| Buyer App Tests | 29 | ~87 |
| **Total** | **~89** | **~267** |

---

## ✅ Final Step: Compile & Test

```bash
# Compile
mvn clean compile -DskipTests

# Optional: Run a quick smoke test
mvn test -Dtest=LoginApiTest#testStoreAccessToken

# Optional: Run full test suite
mvn test
```

---

**Done!** All tests now use thread-safe `VariableManager` 🎉

**Questions?** Check walkthrough.md for examples of migrated files.
