pipeline {
    agent any
    
    tools {
        maven 'Maven 3.9.9'
    }
    
    environment {
        // Project Configuration
        PROJECT_NAME = 'RestAssured API Automation'
        
        // Date/Time for report filenames
        DATE_STR = """${new Date().format('yyyy-MM-dd_HH_mm')}"""
        
        // S3 Configuration
        S3_BUCKET = 'bizup-builds'
        S3_BASE_FOLDER = 'RestAssured API Report'
        S3_REGION = 'ap-south-1'
        
        // Report paths with timestamps
        ALLURE_REPORT_NAME = "allure_${DATE_STR}"
        LOGS_REPORT_NAME = "logs_${DATE_STR}"
        
        // S3 full paths
        S3_ALLURE_PATH = "${S3_BASE_FOLDER}/reports/${ALLURE_REPORT_NAME}"
        S3_LOGS_PATH = "${S3_BASE_FOLDER}/logs/${LOGS_REPORT_NAME}"
        S3_RESULTS_PATH = "${S3_BASE_FOLDER}/results/testng-results-${env.BUILD_NUMBER}.xml"
        
        // Notification Configuration
        NOTIFICATION_CONFIG = 'scripts/notification-config.json'
        
        // Email Configuration
        EMAIL_RECIPIENTS = 'shubham.bizup@gmail.com' // Update with actual recipient emails (comma-separated)
    }
    
    parameters {
        choice(
            name: 'TEST_PROFILE',
            choices: ['all', 'bomb', 'buyerapp'],
            description: 'Select test profile to run (Default: all - runs both BOMB and Buyer App tests)'
        )
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Run Tests') {
            steps {
                script {
                    def mavenProfile = params.TEST_PROFILE == 'all' ? '' : "-P${params.TEST_PROFILE}"
                    
                    // Use catchError to continue pipeline even if tests fail
                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                        sh """
                            mvn clean test ${mavenProfile} \
                            -Dallure.results.directory=target/allure-results
                        """
                    }
                }
            }
        }
        
        stage('Setup Allure') {
            steps {
                script {
                    echo "Checking for Allure CLI installation..."
                    sh '''
                        # Check if allure command exists in PATH
                        if command -v allure >/dev/null 2>&1; then
                            echo "✅ Allure CLI already installed and working"
                            allure --version
                        else
                            echo "⚙️ Allure CLI not found or not working. Installing..."
                            
                            # Clean up any broken installation
                            sudo rm -f /usr/local/bin/allure 2>/dev/null || true
                            sudo rm -rf /opt/allure 2>/dev/null || true
                            
                            # Create directory for Allure
                            sudo mkdir -p /opt/allure
                            
                            # Download Allure
                            echo "Downloading Allure 2.25.0..."
                            cd /tmp
                            wget -q https://github.com/allure-framework/allure2/releases/download/2.25.0/allure-2.25.0.tgz
                            
                            # Extract to /opt
                            echo "Extracting Allure..."
                            sudo tar -zxf allure-2.25.0.tgz -C /opt/allure --strip-components=1
                            
                            # Create symlink
                            echo "Creating symlink..."
                            sudo ln -sf /opt/allure/bin/allure /usr/local/bin/allure
                            
                            # Make it executable
                            sudo chmod +x /opt/allure/bin/allure
                            
                            # Cleanup
                            rm -f allure-2.25.0.tgz
                            
                            # Verify installation
                            if command -v allure >/dev/null 2>&1 && allure --version; then
                                echo "✅ Allure CLI installed successfully"
                            else
                                echo "❌ Allure CLI installation failed"
                                exit 1
                            fi
                        fi
                    '''
                }
            }
        }
        
        stage('Generate Allure Report') {
            steps {
                script {
                    echo "Generating Allure report from test results..."
                    
                    // First verify test results exist
                    sh """
                        echo "=== Checking for test results ==="
                        if [ -d "target/allure-results" ]; then
                            echo "✅ allure-results directory exists"
                            ls -la target/allure-results/ | head -10
                            echo "Number of result files:"
                            find target/allure-results/ -type f | wc -l
                        else
                            echo "❌ ERROR: target/allure-results/ directory NOT FOUND!"
                            echo "Tests may not have generated Allure results"
                            exit 1
                        fi
                    """
                    
                    // Generate the report - use Allure Commandline instead of Maven plugin
                    sh """
                        echo "=== Generating Allure Report with Allure Commandline ==="
                        
                        # Check if allure command is available
                        if command -v allure &> /dev/null; then
                            echo "✅ Allure CLI found"
                            allure --version
                            
                            # Generate report using Allure CLI
                            allure generate target/allure-results --clean -o target/site/allure-maven-plugin
                        else
                            echo "⚠️ Allure CLI not found, trying Maven plugin..."
                            
                            # Fallback to Maven with different goals
                            mvn allure:report || \
                            mvn io.qameta.allure:allure-maven:2.14.0:report || \
                            mvn site -Dallure.report.directory=target/site/allure-maven-plugin
                        fi
                        
                        echo "Report generation complete"
                    """
                    
                    // Verify report was generated
                    echo "Verifying Allure report generation..."
                    sh """
                        echo "=== Checking Allure report directory ==="
                        ls -la target/site/allure-maven-plugin/ || echo "Report directory not found!"
                        
                        echo "=== Listing ALL files in report directory ==="
                        find target/site/allure-maven-plugin/ -type f -exec ls -lh {} \\;
                        
                        echo "=== Counting files in report ==="
                        find target/site/allure-maven-plugin/ -type f | wc -l
                        
                        echo "=== Checking alternative locations ==="
                        find target/site -name "index.html" 2>/dev/null || echo "No index.html found in target/site"
                        find target -name "allure-*" -type d 2>/dev/null || echo "No allure directories found"
                        
                        echo "=== Directory structure of target/site ==="
                        ls -laR target/site/ | head -50
                        
                        echo "=== Checking for critical files ==="
                        test -f target/site/allure-maven-plugin/index.html && echo "✅ index.html found" || echo "❌ index.html MISSING"
                        test -f target/site/allure-maven-plugin/app.js && echo "✅ app.js found" || echo "❌ app.js MISSING"
                        test -f target/site/allure-maven-plugin/styles.css && echo "✅ styles.css found" || echo "❌ styles.css MISSING"
                        test -d target/site/allure-maven-plugin/data && echo "✅ data/ directory found" || echo "❌ data/ directory MISSING"
                        
                        # Temporarily disable build failure to see full output
                        # if [ ! -f "target/site/allure-maven-plugin/index.html" ]; then
                        #     echo "❌ FATAL: Allure report generation failed!"
                        #     exit 1
                        # fi
                    """
                }
            }
        }
        
        stage('Upload to S3') {
            steps {
                script {
                    echo "Uploading reports to S3 with timestamp: ${DATE_STR}"
                    
                    // Upload Allure report with timestamped folder name
                    echo "=== Uploading Allure Report to S3 ==="
                    sh """
                        echo "Source: target/site/allure-maven-plugin/"
                        echo "Destination: s3://${S3_BUCKET}/${S3_ALLURE_PATH}/"
                        echo "Files to upload:"
                        find target/site/allure-maven-plugin/ -type f | head -20
                        
                        aws s3 sync target/site/allure-maven-plugin/ \
                        "s3://${S3_BUCKET}/${S3_ALLURE_PATH}/" \
                        --region ${S3_REGION} \
                        --debug 2>&1 | grep -E "(upload|delete):" || true
                        
                        echo "Upload complete. Verifying S3..."
                        aws s3 ls "s3://${S3_BUCKET}/${S3_ALLURE_PATH}/" --recursive | head -20
                    """
                    
                    // Upload logs with timestamped folder name
                    sh """
                        aws s3 sync target/logs/ \
                        "s3://${S3_BUCKET}/${S3_LOGS_PATH}/" \
                        --region ${S3_REGION}
                    """
                    
                    // Upload TestNG results
                    sh """
                        aws s3 cp target/surefire-reports/testng-results.xml \
                        "s3://${S3_BUCKET}/${S3_RESULTS_PATH}" \
                        --region ${S3_REGION}
                    """
                    
                    // Print S3 URLs for reference
                    echo "✅ Allure Report: https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_ALLURE_PATH}/index.html"
                    echo "✅ Logs: https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_LOGS_PATH}/"
                }
            }
        }
        
        stage('Extract Test Summary') {
            steps {
                script {
                    sh """
                        python3 scripts/extract-test-summary.py \
                        target/surefire-reports/testng-results.xml \
                        > test-summary.json
                    """
                    
                    // Read and display summary
                    def summary = readJSON file: 'test-summary.json'
                    echo "Test Summary: ${summary}"
                }
            }
        }
        
        stage('Send Notifications') {
            steps {
                script {
                    // Generate S3 URLs with timestamps
                    def reportUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_ALLURE_PATH}/index.html"
                    def logsUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_LOGS_PATH}/automation.log"
                    
                    // Securely handle credentials for webhook and SMTP
                    withCredentials([
                        string(credentialsId: 'google-chat-webhook-dev', variable: 'WEBHOOK_URL'),
                        usernamePassword(credentialsId: 'smtp-credentials', usernameVariable: 'SMTP_USERNAME', passwordVariable: 'SMTP_PASSWORD')
                    ]) {
                        // Create notification config using shell script to avoid exposing credentials
                        sh '''
                            cat > scripts/notification-config.json.tmp << 'EOF_CONFIG'
{
  "project_name": "''' + PROJECT_NAME + '''",
  "enable_email": true,
  "enable_google_chat": true,
  "email_from": "jenkins@bizup.app",
  "email_to": ["''' + EMAIL_RECIPIENTS + '''"],
  "smtp_host": "smtp.gmail.com",
  "smtp_port": 587,
  "smtp_use_tls": true,
  "smtp_username": "SMTP_USERNAME_PLACEHOLDER",
  "smtp_password": "SMTP_PASSWORD_PLACEHOLDER",
  "google_chat_webhook": "WEBHOOK_URL_PLACEHOLDER",
  "build_url": "''' + BUILD_URL + '''",
  "build_number": "''' + env.BUILD_NUMBER + '''",
  "timestamp": "''' + DATE_STR + '''",
  "report_url": "''' + reportUrl + '''",
  "logs_url": "''' + logsUrl + '''"
}
EOF_CONFIG
                            # Replace placeholders with actual credentials using sed
                            sed -i "s|SMTP_USERNAME_PLACEHOLDER|${SMTP_USERNAME}|g" scripts/notification-config.json.tmp
                            sed -i "s|SMTP_PASSWORD_PLACEHOLDER|${SMTP_PASSWORD}|g" scripts/notification-config.json.tmp
                            sed -i "s|WEBHOOK_URL_PLACEHOLDER|${WEBHOOK_URL}|g" scripts/notification-config.json.tmp
                        '''
                        
                        // Send notifications
                        sh """
                            python3 scripts/send-notification.py \\
                            test-summary.json \\
                            ${NOTIFICATION_CONFIG}.tmp
                        """
                    }
                    
                    echo "✅ Notification sent to Google Chat"
                }
            }
        }
        
        stage('Archive S3 Links') {
            steps {
                script {
                    // Generate S3 URLs with timestamps
                    def reportUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_ALLURE_PATH}/index.html"
                    def logsUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_LOGS_PATH}/automation.log"
                    def resultsUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_RESULTS_PATH}"
                    
                    // Create S3 links file
                    writeFile file: 's3-links.txt', text: """
==============================================
  S3 REPORT LINKS - Build #${env.BUILD_NUMBER}
==============================================

[ALLURE REPORT]
${reportUrl}

[AUTOMATION LOGS]
${logsUrl}

[TESTNG RESULTS XML]
${resultsUrl}

>> Tip: Click the links above to view reports in S3
==============================================
"""
                    
                    // Print S3 links to console
                    echo "=============================================="
                    echo "  📊 S3 REPORT LINKS - Build #${env.BUILD_NUMBER}"
                    echo "=============================================="
                    echo ""
                    echo "📊 Allure Report:"
                    echo "   ${reportUrl}"
                    echo ""
                    echo "📋 Automation Logs:"
                    echo "   ${logsUrl}"
                    echo ""
                    echo "📄 TestNG Results XML:"
                    echo "   ${resultsUrl}"
                    echo ""
                    echo "✅ S3 links saved to s3-links.txt artifact"
                    echo "=============================================="
                }
            }
        }
    }
    
    post {
        always {
            // Archive artifacts (only summary and S3 links)
            archiveArtifacts artifacts: 'test-summary.json, s3-links.txt', 
                             allowEmptyArchive: true
            
            // Publish Allure report in Jenkins (optional)
            allure([
                includeProperties: false,
                jdk: '',
                properties: [],
                reportBuildPolicy: 'ALWAYS',
                results: [[path: 'target/allure-results']]
            ])
        }
        
        failure {
            echo 'Tests failed! Check the reports for details.'
            
            script {
                def reportUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_ALLURE_PATH}/index.html"
                def logsUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_LOGS_PATH}/automation.log"
                
                emailext (
                    subject: "❌ FAILED: ${PROJECT_NAME} - Build #${env.BUILD_NUMBER}",
                    body: """<html>
                        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                                <div style="background: linear-gradient(135deg, #dc2626 0%, #991b1b 100%); padding: 20px; border-radius: 8px 8px 0 0;">
                                    <h2 style="color: white; margin: 0;">❌ Build Failed</h2>
                                </div>
                                
                                <div style="background: #f9fafb; padding: 20px; border: 1px solid #e5e7eb; border-top: none; border-radius: 0 0 8px 8px;">
                                    <h3 style="color: #1f2937; margin-top: 0;">${PROJECT_NAME}</h3>
                                    
                                    <table style="width: 100%; border-collapse: collapse; margin: 20px 0;">
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold; width: 120px;">Build #</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${env.BUILD_NUMBER}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">Status</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;"><span style="color: #dc2626; font-weight: bold;">FAILED</span></td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">Profile</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${params.TEST_PROFILE}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">Duration</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${currentBuild.durationString.replace(' and counting', '')}</td>
                                        </tr>
                                    </table>
                                    
                                    <div style="margin: 20px 0;">
                                        <h4 style="color: #1f2937; margin-bottom: 10px;">📊 Quick Links</h4>
                                        <p style="margin: 8px 0;">
                                            <a href="${BUILD_URL}" style="color: #2563eb; text-decoration: none;">🔗 Jenkins Build</a>
                                        </p>
                                        <p style="margin: 8px 0;">
                                            <a href="${reportUrl}" style="color: #2563eb; text-decoration: none;">📈 Allure Report</a>
                                        </p>
                                        <p style="margin: 8px 0;">
                                            <a href="${logsUrl}" style="color: #2563eb; text-decoration: none;">📋 Automation Logs</a>
                                        </p>
                                    </div>
                                    
                                    <p style="color: #6b7280; font-size: 12px; margin-top: 20px; border-top: 1px solid #e5e7eb; padding-top: 15px;">
                                        This is an automated notification from Jenkins CI/CD Pipeline<br>
                                        Timestamp: ${new Date().format('yyyy-MM-dd HH:mm:ss')}
                                    </p>
                                </div>
                            </div>
                        </body>
                    </html>""",
                    to: "${EMAIL_RECIPIENTS}",
                    mimeType: 'text/html',
                    attachLog: false
                )
            }
        }
        
        success {
            echo 'All tests passed successfully!'
            
            script {
                def reportUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_ALLURE_PATH}/index.html"
                def logsUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_LOGS_PATH}/automation.log"
                
                emailext (
                    subject: "✅ SUCCESS: ${PROJECT_NAME} - Build #${env.BUILD_NUMBER}",
                    body: """<html>
                        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                                <div style="background: linear-gradient(135deg, #059669 0%, #047857 100%); padding: 20px; border-radius: 8px 8px 0 0;">
                                    <h2 style="color: white; margin: 0;">✅ Build Successful</h2>
                                </div>
                                
                                <div style="background: #f9fafb; padding: 20px; border: 1px solid #e5e7eb; border-top: none; border-radius: 0 0 8px 8px;">
                                    <h3 style="color: #1f2937; margin-top: 0;">${PROJECT_NAME}</h3>
                                    
                                    <table style="width: 100%; border-collapse: collapse; margin: 20px 0;">
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold; width: 120px;">Build #</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${env.BUILD_NUMBER}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">Status</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;"><span style="color: #059669; font-weight: bold;">SUCCESS</span></td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">Profile</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${params.TEST_PROFILE}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">Duration</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${currentBuild.durationString.replace(' and counting', '')}</td>
                                        </tr>
                                    </table>
                                    
                                    <div style="margin: 20px 0;">
                                        <h4 style="color: #1f2937; margin-bottom: 10px;">📊 Quick Links</h4>
                                        <p style="margin: 8px 0;">
                                            <a href="${BUILD_URL}" style="color: #2563eb; text-decoration: none;">🔗 Jenkins Build</a>
                                        </p>
                                        <p style="margin: 8px 0;">
                                            <a href="${reportUrl}" style="color: #2563eb; text-decoration: none;">📈 Allure Report</a>
                                        </p>
                                        <p style="margin: 8px 0;">
                                            <a href="${logsUrl}" style="color: #2563eb; text-decoration: none;">📋 Automation Logs</a>
                                        </p>
                                    </div>
                                    
                                    <p style="color: #6b7280; font-size: 12px; margin-top: 20px; border-top: 1px solid #e5e7eb; padding-top: 15px;">
                                        This is an automated notification from Jenkins CI/CD Pipeline<br>
                                        Timestamp: ${new Date().format('yyyy-MM-dd HH:mm:ss')}
                                    </p>
                                </div>
                            </div>
                        </body>
                    </html>""",
                    to: "${EMAIL_RECIPIENTS}",
                    mimeType: 'text/html',
                    attachLog: false
                )
            }
        }
        
        unstable {
            echo 'Build is unstable - some tests may have failed.'
            
            script {
                def reportUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_ALLURE_PATH}/index.html"
                def logsUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_LOGS_PATH}/automation.log"
                
                emailext (
                    subject: "⚠️ UNSTABLE: ${PROJECT_NAME} - Build #${env.BUILD_NUMBER}",
                    body: """<html>
                        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                                <div style="background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); padding: 20px; border-radius: 8px 8px 0 0;">
                                    <h2 style="color: white; margin: 0;">⚠️ Build Unstable</h2>
                                </div>
                                
                                <div style="background: #f9fafb; padding: 20px; border: 1px solid #e5e7eb; border-top: none; border-radius: 0 0 8px 8px;">
                                    <h3 style="color: #1f2937; margin-top: 0;">${PROJECT_NAME}</h3>
                                    
                                    <table style="width: 100%; border-collapse: collapse; margin: 20px 0;">
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold; width: 120px;">Build #</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${env.BUILD_NUMBER}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">Status</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;"><span style="color: #f59e0b; font-weight: bold;">UNSTABLE</span></td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">Profile</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${params.TEST_PROFILE}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">Duration</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${currentBuild.durationString.replace(' and counting', '')}</td>
                                        </tr>
                                    </table>
                                    
                                    <div style="background: #fef3c7; border-left: 4px solid #f59e0b; padding: 12px; margin: 20px 0; border-radius: 4px;">
                                        <p style="margin: 0; color: #92400e;"><strong>Note:</strong> Some tests may have failed. Please check the reports for details.</p>
                                    </div>
                                    
                                    <div style="margin: 20px 0;">
                                        <h4 style="color: #1f2937; margin-bottom: 10px;">📊 Quick Links</h4>
                                        <p style="margin: 8px 0;">
                                            <a href="${BUILD_URL}" style="color: #2563eb; text-decoration: none;">🔗 Jenkins Build</a>
                                        </p>
                                        <p style="margin: 8px 0;">
                                            <a href="${reportUrl}" style="color: #2563eb; text-decoration: none;">📈 Allure Report</a>
                                        </p>
                                        <p style="margin: 8px 0;">
                                            <a href="${logsUrl}" style="color: #2563eb; text-decoration: none;">📋 Automation Logs</a>
                                        </p>
                                    </div>
                                    
                                    <p style="color: #6b7280; font-size: 12px; margin-top: 20px; border-top: 1px solid #e5e7eb; padding-top: 15px;">
                                        This is an automated notification from Jenkins CI/CD Pipeline<br>
                                        Timestamp: ${new Date().format('yyyy-MM-dd HH:mm:ss')}
                                    </p>
                                </div>
                            </div>
                        </body>
                    </html>""",
                    to: "${EMAIL_RECIPIENTS}",
                    mimeType: 'text/html',
                    attachLog: false
                )
            }
        }
    }
}
