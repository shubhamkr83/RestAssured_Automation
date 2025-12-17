# Quick Start: IDE Find & Replace Migration

## 🎯 What You Need to Do

Use your IDE's Find & Replace feature to update ~89 test files in **10-15 minutes**.

---

## 📋 Exact Steps

### Step 1: BOMB Tests (~60 files)

**Scope:** `src/test/java/com/automation/tests/bomb/`

#### Replace 1.1 - Update Import
- **Find:** `import com.automation.utils.VariableManager;`
- **Replace:** `import com.automation.utils.VariableManager;`
- **Regex:** OFF

#### Replace 1.2 - Update Token Retrieval  
- **Find (Regex ON):** 
```
if \(LoginApiTest\.bombToken != null\) \{\s+authToken = LoginApiTest\.bombToken;\s+logger\.info\("Using BOMB token from LoginApiTest"\);
```
- **Replace:**
```
authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
```

#### Replace 1.3 - Remove Else Block
- **Find (Regex ON):** `\s+\} else \{\s+throw new RuntimeException\("Login token not available\. Please run LoginApiTest first\."\);\s+\}`
- **Replace:** (empty - delete it)

---

### Step 2: Buyer App Tests (29 files)

**Scope:** `src/test/java/com/automation/tests/buyerapp/`

#### Replace 2.1 - Update Import
- **Find:** `import com.automation.utils.VariableManager;`
- **Replace:** `import com.automation.utils.VariableManager;`
- **Regex:** OFF

#### Replace 2.2 - Update Token Usage
- **Find:** `VariableManager.getBuyerAppToken()`
- **Replace:** `VariableManager.getVariableManager.getBuyerAppToken()()`
- **Regex:** OFF

---

## ✅ Verify

After all replacements:

```bash
mvn clean compile -DskipTests
```

Expected: **BUILD SUCCESS** ✅

---

## 📖 Full Details

See `MIGRATION_FIND_REPLACE_GUIDE.md` for:
- IDE-specific instructions (IntelliJ/VSCode/Eclipse)
- Troubleshooting tips
- Verification checklist

---

## 🏁 You're Done!

Once compilation succeeds, all **89 test files** will be using thread-safe `VariableManager`!
