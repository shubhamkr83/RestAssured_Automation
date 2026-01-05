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
        
        stage('Generate Allure Report') {
            steps {
                sh 'mvn allure:report'
            }
        }
        
        stage('Upload to S3') {
            steps {
                script {
                    echo "Uploading reports to S3 with timestamp: ${DATE_STR}"
                    
                    // Upload Allure report with timestamped folder name
                    sh """
                        aws s3 sync target/site/allure-maven-plugin/ \
                        "s3://${S3_BUCKET}/${S3_ALLURE_PATH}/" \
                        --region ${S3_REGION} \
                        --delete
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
                    
                    // Securely handle webhook credential
                    withCredentials([string(credentialsId: 'google-chat-webhook-dev', variable: 'WEBHOOK_URL')]) {
                        // Create notification config with actual webhook
                        def configContent = """{
  "project_name": "${PROJECT_NAME}",
  "enable_email": false,
  "enable_google_chat": true,
  "google_chat_webhook": "${WEBHOOK_URL}",
  "build_url": "${BUILD_URL}",
  "report_url": "${reportUrl}",
  "logs_url": "${logsUrl}"
}"""
                        writeFile file: "${NOTIFICATION_CONFIG}.tmp", text: configContent
                        
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
        }
        
        success {
            echo 'All tests passed successfully!'
        }
    }
}
