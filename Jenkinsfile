pipeline {
    agent any
    
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
        GOOGLE_CHAT_WEBHOOK = credentials('google-chat-webhook-dev')
        NOTIFICATION_CONFIG = 'scripts/notification-config.json'
    }
    
    parameters {
        choice(
            name: 'TEST_PROFILE',
            choices: ['all', 'bomb', 'buyerapp'],
            description: 'Which test profile to run'
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
                    
                    sh """
                        mvn clean test ${mavenProfile} \
                        -Dallure.results.directory=target/allure-results
                    """
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
                        s3://${S3_BUCKET}/${S3_ALLURE_PATH}/ \
                        --region ${S3_REGION} \
                        --delete
                    """
                    
                    // Upload logs with timestamped folder name
                    sh """
                        aws s3 sync target/logs/ \
                        s3://${S3_BUCKET}/${S3_LOGS_PATH}/ \
                        --region ${S3_REGION}
                    """
                    
                    // Upload TestNG results
                    sh """
                        aws s3 cp target/surefire-reports/testng-results.xml \
                        s3://${S3_BUCKET}/${S3_RESULTS_PATH} \
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
                    
                    // Create notification config with actual URLs
                    sh """
                        cat > ${NOTIFICATION_CONFIG}.tmp << EOF
{
  "project_name": "${PROJECT_NAME}",
  "enable_email": false,
  "enable_google_chat": true,
  "google_chat_webhook": "${GOOGLE_CHAT_WEBHOOK}",
  "build_url": "${BUILD_URL}",
  "report_url": "${reportUrl}",
  "logs_url": "${logsUrl}"
}
EOF
                    """
                    
                    // Send notifications
                    sh """
                        python3 scripts/send-notification.py \
                        test-summary.json \
                        ${NOTIFICATION_CONFIG}.tmp
                    """
                    
                    echo "✅ Notification sent to Google Chat"
                }
            }
        }
    }
    
    post {
        always {
            // Archive artifacts
            archiveArtifacts artifacts: 'target/surefire-reports/**, target/logs/**, test-summary.json', 
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
