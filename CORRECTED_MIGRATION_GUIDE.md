# ⚠️ CORRECTED Migration Guide - Use This One!

## What Went Wrong?

The previous guide had an error in Pattern 2B. It told you to find & replace `buyerAppToken` everywhere, which accidentally replaced it in **method names** too, creating invalid syntax like:

```java
// WRONG - Method name got corrupted:
public static String getVariableManager.getBuyerAppToken()() {
```

**✅ I've already fixed this!** Compilation now succeeds.

---

## ✅ CORRECTED Patterns - Use These

### Step 1: BOMB Tests (~60 files)

**Scope:** `src/test/java/com/automation/tests/bomb/`

#### Replace 1.1 - Update Import  
- **Find:** `import com.automation.tests.bomb.Login.LoginApiTest;`
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
- **Find:** `import static com.automation.tests.buyerapp.Login.LoginTest.buyerAppToken;`
- **Replace:** `import com.automation.utils.VariableManager;`
- **Regex:** OFF

#### Replace 2.2 - Update Token Usage ⚠️ CORRECTED
- **Find (Regex ON):** `\bbuyerAppToken\b`
- **Replace:** `VariableManager.getBuyerAppToken()`
- **Regex:** ON (IMPORTANT!)

**Why Regex?** The `\b` means "word boundary" - it only matches `buyerAppToken` as a standalone word, NOT as part of method names.

---

## ✅ How to Execute

### In IntelliJ IDEA:

1. **Press Ctrl+Shift+R** (Find & Replace in Path)
2. **For each pattern:**
   - Copy "Find" text → paste in Find box
   - Copy "Replace" text → paste in Replace box
   - Enable/disable Regex as noted
   - Set correct scope directory
   - Click "Find" to preview
   - Click "Replace All"

---

## ⚡ Quick Execution Order:

```
1. BOMB - Pattern 1.1 (import)       → Regex: OFF → Scope: bomb/
2. BOMB - Pattern 1.2 (token check)  → Regex: ON  → Scope: bomb/
3. BOMB - Pattern 1.3 (else block)   → Regex: ON  → Scope: bomb/

4. Buyer App - Pattern 2.1 (import)  → Regex: OFF → Scope: buyerapp/
5. Buyer App - Pattern 2.2 (token)   → Regex: ON  → Scope: buyerapp/
```

---

## ✅ Verify

```bash
mvn clean compile -DskipTests
```

Expected: **BUILD SUCCESS** ✅

---

## 📌 Summary of Changes

- **Pattern 2.2 is now CORRECTED** to use regex with word boundaries
- This prevents corrupting method names
- All other patterns remain the same

---

**Ready to try again!** Follow this corrected guide and you should have no issues. 🚀
