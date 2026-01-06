# CI/CD Setup Guide

This guide shows you how to run your API tests automatically using Jenkins.

---

## What This Does

When you set up Jenkins with this project, it will:
- ✅ Run your API tests automatically
- ✅ Generate beautiful test reports (Allure)
- ✅ Upload reports to AWS S3
- ✅ Send notifications to Email and Google Chat
- ✅ Let you choose which tests to run (BOMB, Buyer App, or both)

---

## Files You Need

All these files are already created and ready to use:

| File | What It Does |
|------|--------------|
| **Jenkinsfile** | Main pipeline that runs everything |
| **scripts/extract-test-summary.py** | Gets test results from TestNG |
| **scripts/send-notification.py** | Sends Email + Google Chat notifications |
| **scripts/notify-chat.sh** | Simple script for Google Chat only |
| **scripts/notification-config.example.json** | Template for notification settings |

---

## Quick Setup Steps

### Step 1: Create Notification Config File

```bash
# Copy the example file
cp scripts/notification-config.example.json scripts/notification-config.json

# Edit with your details
nano scripts/notification-config.json
```

### Step 2: Add to .gitignore

Make sure your config file is not uploaded to Git:

```bash
echo "scripts/notification-config.json" >> .gitignore
```

### Step 3: Setup Jenkins

1. **Create a new Jenkins job**
   - Job type: Pipeline
   - Pipeline from SCM (Git)
   - Point to your repository

2. **Add these credentials in Jenkins**:
   - Google Chat Webhook: ID = `google-chat-webhook-dev`
   - SMTP Credentials: ID = `smtp-credentials`
   - AWS Credentials (for S3 upload)

3. **Choose test profile when running**:
   - `all` - Runs both BOMB and Buyer App tests
   - `bomb` - Runs only BOMB API tests
   - `buyerapp` - Runs only Buyer App tests

### Step 4: Run the Pipeline

Click "Build with Parameters" in Jenkins and select your test profile!

---

## How It Works

### Pipeline Flow

```
1. Checkout Code from Git
   ↓
2. Run Tests (Maven)
   ↓
3. Generate Allure Report
   ↓
4. Upload to S3 (with timestamp)
   ↓
5. Send Notifications (Email + Chat)
```

### Test Profiles

You can choose which tests to run:

```bash
# Run all tests
mvn clean test

# Run only BOMB API tests
mvn clean test -Pbomb

# Run only Buyer App tests
mvn clean test -Pbuyerapp
```

Jenkins uses the same profiles!

---

## Notification Setup

### Email Notifications

Emails are sent automatically when:
- ✅ All tests pass (SUCCESS)
- ❌ Tests fail (FAILURE)
- ⚠️ Some tests fail (UNSTABLE)

**Recipient**: `shubham.bizup@gmail.com` (change in Jenkinsfile if needed)

### Google Chat Notifications

Messages include:
- Test summary (Total, Pass, Fail, Skip)
- Links to reports and logs
- Build status
- Execution time

### Notification Config File

Edit `scripts/notification-config.json`:

```json
{
  "project_name": "RestAssured API Automation",
  "enable_email": true,
  "enable_google_chat": true,
  "email_to": ["your-email@example.com"],
  "smtp_host": "smtp.gmail.com",
  "smtp_port": 587,
  "smtp_username": "your-email@gmail.com",
  "smtp_password": "your-app-password",
  "google_chat_webhook": "your-webhook-url"
}
```

---

## AWS S3 Reports

Reports are uploaded with timestamps for easy tracking:

### Report Structure

```
s3://bizup-builds/
├── RestAssured API Report/
│   ├── reports/
│   │   └── allure_2026-01-06_17_30/     ← Timestamped
│   ├── logs/
│   │   └── logs_2026-01-06_17_30/       ← Timestamped
│   └── results/
│       └── testng-results-42.xml        ← Build number
```

### Accessing Reports

After each build, you get these links:
- **Allure Report**: `https://bizup-builds.s3.ap-south-1.amazonaws.com/RestAssured API Report/reports/allure_YYYY-MM-DD_HH_MM/index.html`
- **Logs**: `https://bizup-builds.s3.ap-south-1.amazonaws.com/RestAssured API Report/logs/logs_YYYY-MM-DD_HH_MM/automation.log`

Jenkins prints these links in the console!

---

## Python Scripts Explained

### 1. extract-test-summary.py

**What it does**: Reads TestNG results and creates a simple summary

**Input**: `testng-results.xml`

**Output**: 
```json
{
  "total": 56,
  "passed": 54,
  "failed": 0,
  "skipped": 2
}
```

### 2. send-notification.py

**What it does**: Sends test results to Email and Google Chat

**Features**:
- Beautiful HTML emails
- Google Chat cards with test details
- Individual test case breakdown
- Status icons and color coding

### 3. notify-chat.sh

**What it does**: Simple bash script for Google Chat only

**Use when**: You only need Google Chat notifications (no email)

---

## Running Tests Locally

### Command Line

```bash
# All tests
mvn clean test

# BOMB API only
mvn clean test -Pbomb

# Buyer App only  
mvn clean test -Pbuyerapp

# Generate Allure report
mvn allure:serve
```

### Via Jenkins

1. Go to your Jenkins job
2. Click "Build with Parameters"
3. Select profile: `all`, `bomb`, or `buyerapp`
4. Click "Build"

Watch the pipeline run and get notifications!

---

## Troubleshooting

### Allure Report Not Generated

**Problem**: Report generation fails

**Solution**:
```bash
# Install Allure CLI
wget https://github.com/allure-framework/allure2/releases/download/2.25.0/allure-2.25.0.tgz
sudo tar -zxf allure-2.25.0.tgz -C /opt/
sudo ln -s /opt/allure-2.25.0/bin/allure /usr/local/bin/allure
```

### S3 Upload Fails

**Problem**: Reports not uploading to S3

**Solution**:
- Check AWS credentials in Jenkins
- Verify S3 bucket permissions
- Check bucket name: `bizup-builds`
- Check region: `ap-south-1`

### Notifications Not Sent

**Problem**: No email or chat messages

**Solution**:
- Verify credentials in Jenkins
- Check webhook URL is correct
- Test email settings (SMTP port 587)
- Enable "Less secure app access" for Gmail

### No Test Results

**Problem**: Tests don't run

**Solution**:
```bash
# Check Maven is installed
mvn --version

# Check Java version
java -version  # Need Java 17+

# Run tests manually first
mvn clean test
```

---

## Configuration Reference

### Jenkins Environment Variables

Set these in Jenkins job configuration:

| Variable | Value | Description |
|----------|-------|-------------|
| `PROJECT_NAME` | RestAssured API Automation | Project display name |
| `S3_BUCKET` | bizup-builds | S3 bucket name |
| `S3_REGION` | ap-south-1 | AWS region |
| `EMAIL_RECIPIENTS` | shubham.bizup@gmail.com | Who gets emails |

### Required Jenkins Plugins

- Pipeline
- Git
- Email Extension
- Allure
- S3 Publisher (or AWS CLI)

### Required Credentials

Add these in Jenkins → Credentials:

1. **google-chat-webhook-dev** (Secret text)
   - Your Google Chat webhook URL

2. **smtp-credentials** (Username/Password)
   - Username: Your email
   - Password: App password

3. **AWS credentials** (for S3)
   - Access Key ID
   - Secret Access Key

---

## What You Get

### After Each Build

1. **Email** with:
   - Build status (Success/Failed/Unstable)
   - Test counts
   - Links to reports

2. **Google Chat** message with:
   - Test summary
   - Individual test results
   - Quick links

3. **S3 Reports**:
   - Allure HTML report
   - Test logs
   - TestNG XML results

4. **Jenkins Artifacts**:
   - Test summary JSON
   - S3 links file

---

## Example Notification

### Email Subject
```
✅ SUCCESS: RestAssured API Automation - Build #42
```

### Google Chat Message
```
✅ Test Report: RestAssured API Automation
━━━━━━━━━━━━━━━━━━━━━━━

📊 Test Summary:
• Total Tests: 56
• Passed: ✅ 54
• Failed: ❌ 0  
• Skipped: ⏭️ 2

🔗 Quick Links:
• Jenkins Build
• Allure Report
• Automation Logs

⏱️ Duration: 45.2s
🏗️ Build: #42
```

---

## Next Steps

1. ✅ Set up Jenkins job
2. ✅ Add credentials
3. ✅ Configure notifications
4. ✅ Run first build
5. ✅ Check reports in S3
6. ✅ Verify notifications received

**You're all set!** 🎉

For more details, see the main [README.md](README.md)
