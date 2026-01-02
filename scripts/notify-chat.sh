#!/bin/bash
#
# Simple notification script using curl for Google Chat
# Can be used standalone or as part of Jenkins pipeline
#

# Configuration
PROJECT_NAME="API Automation Framework"
TESTNG_RESULTS="target/surefire-reports/testng-results.xml"
GOOGLE_CHAT_WEBHOOK="${GOOGLE_CHAT_WEBHOOK_URL}"

# Extract test statistics from TestNG XML
extract_stats() {
    if [ ! -f "$TESTNG_RESULTS" ]; then
        echo "Error: TestNG results file not found: $TESTNG_RESULTS"
        exit 1
    fi
    
    # Parse XML attributes using grep and sed
    TOTAL=$(grep -oP '(?<=<testng-results).*?(?=>)' "$TESTNG_RESULTS" | grep -oP '(?<=total=")[^"]*')
    PASSED=$(grep -oP '(?<=<testng-results).*?(?=>)' "$TESTNG_RESULTS" | grep -oP '(?<=passed=")[^"]*')
    FAILED=$(grep -oP '(?<=<testng-results).*?(?=>)' "$TESTNG_RESULTS" | grep -oP '(?<=failed=")[^"]*')
    SKIPPED=$(grep -oP '(?<=<testng-results).*?(?=>)' "$TESTNG_RESULTS" | grep -oP '(?<=skipped=")[^"]*')
}

# Send Google Chat notification
send_google_chat() {
    local status_emoji="✅"
    local status_text="PASSED"
    
    if [ "$FAILED" -gt 0 ]; then
        status_emoji="❌"
        status_text="FAILED"
    fi
    
    local message=$(cat <<EOF
{
  "text": "*${status_emoji} Test Report: ${PROJECT_NAME}*\n\n*------${PROJECT_NAME}------*\n\n*Total Tests:* ${TOTAL}\n*Test Pass:* ✅ ${PASSED}\n*Test Fail:* ❌ ${FAILED}\n*Test Skipped:* ⏭️ ${SKIPPED}\n\n*Links:*\n• Build: <${BUILD_URL}|Jenkins Build>\n• Report: <${REPORT_URL}|Allure Report>\n• Logs: <${LOGS_URL}|View Logs>"
}
EOF
    )
    
    curl -X POST \
        -H 'Content-Type: application/json' \
        -d "$message" \
        "$GOOGLE_CHAT_WEBHOOK"
}

# Main execution
extract_stats
send_google_chat

echo "Notification sent successfully!"
