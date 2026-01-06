#!/usr/bin/env python3
"""
Send test execution summary notifications to Email and Google Chat
"""
import json
import sys
import os
import requests
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
import smtplib

def send_email(summary_data, config):
    """Send email notification"""
    
    msg = MIMEMultipart('alternative')
    
    # Determine overall status
    failed_count = summary_data['failed']
    if failed_count == 0:
        overall_status = "✅ PASSED"
        status_color = "#34A853"
    else:
        overall_status = "❌ FAILED"
        status_color = "#EA4335"
    
    msg['Subject'] = f"Test Report: {config['project_name']} - {overall_status}"
    msg['From'] = config['email_from']
    msg['To'] = ', '.join(config['email_to'])
    
    # Build detailed test case table rows
    test_case_rows = ""
    for test_case in summary_data.get('test_cases', []):
        test_case_rows += f"""
            <tr style="border-bottom: 1px solid #e5e7eb;">
                <td style="padding: 12px; text-align: center; font-weight: 500;">{test_case['serial_no']}</td>
                <td style="padding: 12px; font-weight: 500; color: #1f2937;">{test_case['feature_name']}</td>
                <td style="padding: 12px; color: #4b5563;">{test_case['description']}</td>
                <td style="padding: 12px; text-align: center; font-weight: 600;">{test_case['status']}</td>
            </tr>"""
    
    # Create final status message
    if failed_count == 0:
        final_message = "🎉🎉 All test cases passed successfully 🎉🎉"
        final_bg = "#d4edda"
        final_color = "#155724"
    else:
        final_message = f"⚠️ {failed_count} test case(s) failed. Please review the detailed report."
        final_bg = "#f8d7da"
        final_color = "#721c24"
    
    # Create HTML email body
    html_body = f"""
    <html>
    <body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px;">
        <div style="max-width: 800px; margin: 0 auto; background-color: white; border-radius: 10px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);">
            
            <!-- Header -->
            <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
                <h1 style="color: white; margin: 0; font-size: 24px; text-shadow: 2px 2px 4px rgba(0,0,0,0.2);">
                    🔹🔹 Test Automation Execution Summary 🔹🔹
                </h1>
                <p style="color: #f0f0f0; margin: 10px 0 0 0; font-size: 16px;">
                    📱 🔹🔹 {config['project_name']} 🔹🔹 📱
                </p>
            </div>
            
            <!-- Content Container -->
            <div style="padding: 30px;">
                
                <!-- Execution Info -->
                <div style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); padding: 15px; border-radius: 8px; margin-bottom: 25px; text-align: center;">
                    <h2 style="color: white; margin: 0; font-size: 18px; text-shadow: 1px 1px 2px rgba(0,0,0,0.2);">
                        🚀 Build #{config.get('build_number', 'N/A')} - Test Execution 🚀
                    </h2>
                </div>
                
                <!-- Test Summary Table -->
                <h3 style="color: #1f2937; margin: 25px 0 15px 0; font-size: 18px; border-bottom: 3px solid #667eea; padding-bottom: 8px;">
                    📌 Test Summary Metrics 📊
                </h3>
                <table style="width: 100%; border-collapse: collapse; margin-bottom: 30px; border: 2px solid #e5e7eb; border-radius: 8px; overflow: hidden;">
                    <thead>
                        <tr style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
                            <th style="padding: 15px; text-align: left; color: white; font-size: 14px;">📌 Metric</th>
                            <th style="padding: 15px; text-align: center; color: white; font-size: 14px;">📊 Count</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr style="background-color: #f9fafb; border-bottom: 1px solid #e5e7eb;">
                            <td style="padding: 12px; font-weight: 500;">Total Tests</td>
                            <td style="padding: 12px; text-align: center; font-weight: bold; font-size: 16px;">{summary_data['total']}</td>
                        </tr>
                        <tr style="background-color: #d4f4dd; border-bottom: 1px solid #e5e7eb;">
                            <td style="padding: 12px; font-weight: 500;">✅ Passed</td>
                            <td style="padding: 12px; text-align: center; font-weight: bold; color: #059669; font-size: 16px;">{summary_data['passed']}</td>
                        </tr>
                        <tr style="background-color: #fee2e2; border-bottom: 1px solid #e5e7eb;">
                            <td style="padding: 12px; font-weight: 500;">❌ Failed</td>
                            <td style="padding: 12px; text-align: center; font-weight: bold; color: #dc2626; font-size: 16px;">{summary_data['failed']}</td>
                        </tr>
                        <tr style="background-color: #fef3c7;">
                            <td style="padding: 12px; font-weight: 500;">⚠️ Skipped</td>
                            <td style="padding: 12px; text-align: center; font-weight: bold; color: #f59e0b; font-size: 16px;">{summary_data['skipped']}</td>
                        </tr>
                    </tbody>
                </table>
                
                <!-- Detailed Test Case Report -->
                <h3 style="color: #1f2937; margin: 35px 0 15px 0; font-size: 18px; border-bottom: 3px solid #667eea; padding-bottom: 8px;">
                    📋 Detailed Test Case Report 📋
                </h3>
                <div style="max-height: 250px; overflow-y: auto; border: 2px solid #e5e7eb; border-radius: 8px; margin-bottom: 30px;">
                    <table style="width: 100%; border-collapse: collapse;">
                        <thead style="position: sticky; top: 0; z-index: 10;">
                            <tr style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
                                <th style="padding: 15px; text-align: center; color: white; font-size: 13px; width: 60px;">🔢 S No.</th>
                                <th style="padding: 15px; text-align: left; color: white; font-size: 13px; width: 200px;">📝 Test Feature Name</th>
                                <th style="padding: 15px; text-align: left; color: white; font-size: 13px;">📄 Description</th>
                                <th style="padding: 15px; text-align: center; color: white; font-size: 13px; width: 120px;">📌 Status</th>
                            </tr>
                        </thead>
                        <tbody style="background-color: white;">
                            {test_case_rows}
                        </tbody>
                    </table>
                </div>
                
                <!-- Final Status Message -->
                <div style="background-color: {final_bg}; border-left: 5px solid {status_color}; padding: 20px; border-radius: 8px; margin: 30px 0; text-align: center;">
                    <h3 style="color: {final_color}; margin: 0; font-size: 18px;">
                        {final_message}
                    </h3>
                </div>
                
                <!-- Quick Links Section -->
                <h3 style="color: #1f2937; margin: 30px 0 15px 0; font-size: 18px; border-bottom: 3px solid #667eea; padding-bottom: 8px;">
                    🔗 Quick Access Links
                </h3>
                <div style="background-color: #f9fafb; padding: 20px; border-radius: 8px; border: 1px solid #e5e7eb;">
                    <p style="margin: 10px 0;">
                        <strong style="color: #1f2937;">📊 Allure Report:</strong> 
                        <a href="{config['report_url']}" style="color: #667eea; text-decoration: none; font-weight: 500;">Click Here to View Report</a>
                    </p>
                    <p style="margin: 10px 0;">
                        <strong style="color: #1f2937;">🔧 Jenkins Build:</strong> 
                        <a href="{config['build_url']}" style="color: #667eea; text-decoration: none; font-weight: 500;">Click Here to View Build</a>
                    </p>
                    <p style="margin: 10px 0;">
                        <strong style="color: #1f2937;">📋 Automation Logs:</strong> 
                        <a href="{config['logs_url']}" style="color: #667eea; text-decoration: none; font-weight: 500;">Click Here to View Logs</a>
                    </p>
                </div>
                
                <!-- Footer -->
                <div style="margin-top: 40px; padding-top: 20px; border-top: 2px solid #e5e7eb; text-align: center;">
                    <p style="color: #6b7280; font-size: 13px; margin: 5px 0;">
                        📎 This is an automated report generated by the QA Automation Framework
                    </p>
                    <p style="color: #6b7280; font-size: 13px; margin: 5px 0;">
                        <strong>Regards,</strong><br>
                        QA Automation Team
                    </p>
                    <p style="color: #9ca3af; font-size: 11px; margin: 15px 0 0 0;">
                        Generated on: {config.get('timestamp', 'N/A')}
                    </p>
                </div>
                
            </div>
        </div>
    </body>
    </html>
    """
    
    msg.attach(MIMEText(html_body, 'html'))
    
    # Send email
    try:
        with smtplib.SMTP(config['smtp_host'], config['smtp_port']) as server:
            if config.get('smtp_use_tls', True):
                server.starttls()
            if config.get('smtp_username') and config.get('smtp_password'):
                server.login(config['smtp_username'], config['smtp_password'])
            server.send_message(msg)
        print("✅ Email sent successfully")
    except Exception as e:
        print(f"❌ Failed to send email: {e}", file=sys.stderr)

def send_google_chat(summary_data, config):
    """Send Google Chat notification via webhook with enhanced UI"""
    
    # Determine status and styling
    failed_count = summary_data['failed']
    pass_rate = float(summary_data['pass_rate'])
    
    if failed_count == 0:
        status_emoji = "🎉"
        status_text = "ALL TESTS PASSED"
        status_color = "#34A853"  # Green
        header_image = "https://img.icons8.com/color/96/000000/ok--v1.png"
    elif pass_rate >= 90:
        status_emoji = "⚠️"
        status_text = "MOSTLY PASSED"
        status_color = "#FBBC04"  # Yellow
        header_image = "https://img.icons8.com/color/96/000000/medium-risk.png"
    else:
        status_emoji = "❌"
        status_text = "TESTS FAILED"
        status_color = "#EA4335"  # Red
        header_image = "https://img.icons8.com/color/96/000000/high-risk.png"
    
    # Calculate execution time if available
    execution_time = summary_data.get('execution_time', 'N/A')
    
    # Create enhanced Google Chat card message
    message = {
        "cardsV2": [{
            "cardId": "test-report-card",
            "card": {
                "header": {
                    "title": f"{status_emoji} {config['project_name']}",
                    "subtitle": f"Status: {status_text}",
                    "imageUrl": header_image,
                    "imageType": "CIRCLE"
                },
                "sections": [
                    {
                        "header": "📊 Test Results Summary",
                        "collapsible": False,
                        "widgets": [
                            {
                                "decoratedText": {
                                    "topLabel": "Total Tests Executed",
                                    "text": f"<b>{summary_data['total']}</b>",
                                    "startIcon": {
                                        "knownIcon": "BOOKMARK"
                                    }
                                }
                            },
                            {
                                "decoratedText": {
                                    "topLabel": "Passed",
                                    "text": f"<font color='#34A853'><b>✅ {summary_data['passed']}</b></font>",
                                    "startIcon": {
                                        "knownIcon": "STAR"
                                    }
                                }
                            },
                            {
                                "decoratedText": {
                                    "topLabel": "Failed",
                                    "text": f"<font color='#EA4335'><b>❌ {summary_data['failed']}</b></font>",
                                    "startIcon": {
                                        "knownIcon": "DESCRIPTION"
                                    }
                                }
                            },
                            {
                                "decoratedText": {
                                    "topLabel": "Skipped",
                                    "text": f"<font color='#FBBC04'><b>⏭️ {summary_data['skipped']}</b></font>",
                                    "startIcon": {
                                        "knownIcon": "CLOCK"
                                    }
                                }
                            },
                            {
                                "divider": {}
                            },
                            {
                                "decoratedText": {
                                    "topLabel": "Pass Rate",
                                    "text": f"<font color='{status_color}'><b>{summary_data['pass_rate']}%</b></font>",
                                    "startIcon": {
                                        "knownIcon": "MULTIPLE_PEOPLE"
                                    }
                                }
                            }
                        ]
                    },
                    {
                        "header": "🔗 Quick Actions",
                        "collapsible": False,
                        "widgets": [
                            {
                                "buttonList": {
                                    "buttons": [
                                        {
                                            "text": "📊 View Report",
                                            "onClick": {
                                                "openLink": {
                                                    "url": config['report_url']
                                                }
                                            }
                                        },
                                        {
                                            "text": "🔧 Jenkins Build",
                                            "onClick": {
                                                "openLink": {
                                                    "url": config['build_url']
                                                }
                                            }
                                        },
                                        {
                                            "text": "📋 View Logs",
                                            "onClick": {
                                                "openLink": {
                                                    "url": config['logs_url']
                                                }
                                            }
                                        }
                                    ]
                                }
                            }
                        ]
                    }
                ]
            }
        }]
    }
    
    # Send to Google Chat webhook
    try:
        print(f"📤 Sending Google Chat notification to webhook...", file=sys.stderr)
        print(f"   Webhook URL (first 50 chars): {config['google_chat_webhook'][:50]}...", file=sys.stderr)
        
        response = requests.post(
            config['google_chat_webhook'],
            json=message,
            headers={'Content-Type': 'application/json; charset=UTF-8'},
            timeout=10
        )
        
        print(f"   Response Status Code: {response.status_code}", file=sys.stderr)
        print(f"   Response Body: {response.text[:200]}", file=sys.stderr)
        
        response.raise_for_status()
        print("✅ Google Chat notification sent successfully")
    except requests.exceptions.Timeout:
        print(f"❌ Failed to send Google Chat notification: Request timed out after 10 seconds", file=sys.stderr)
    except requests.exceptions.ConnectionError as e:
        print(f"❌ Failed to send Google Chat notification: Connection error - {e}", file=sys.stderr)
    except requests.exceptions.HTTPError as e:
        print(f"❌ Failed to send Google Chat notification: HTTP {response.status_code} - {response.text}", file=sys.stderr)
    except Exception as e:
        print(f"❌ Failed to send Google Chat notification: {type(e).__name__}: {e}", file=sys.stderr)

def main():
    if len(sys.argv) < 3:
        print("Usage: python send-notification.py <summary-json> <config-json>")
        sys.exit(1)
    
    summary_file = sys.argv[1]
    config_file = sys.argv[2]
    
    print(f"📥 Loading test summary from: {summary_file}", file=sys.stderr)
    # Load summary data
    with open(summary_file, 'r') as f:
        summary_data = json.load(f)
    print(f"   Tests: {summary_data['total']}, Passed: {summary_data['passed']}, Failed: {summary_data['failed']}", file=sys.stderr)
    
    print(f"📥 Loading notification config from: {config_file}", file=sys.stderr)
    # Load config
    with open(config_file, 'r') as f:
        config = json.load(f)
    
    print(f"   Email enabled: {config.get('enable_email', False)}", file=sys.stderr)
    print(f"   Google Chat enabled: {config.get('enable_google_chat', False)}", file=sys.stderr)
    
    # Send notifications
    if config.get('enable_email', False):
        print(f"\n📧 Sending email notification...", file=sys.stderr)
        send_email(summary_data, config)
    else:
        print(f"\n📧 Email notification is disabled", file=sys.stderr)
    
    if config.get('enable_google_chat', False):
        print(f"\n💬 Sending Google Chat notification...", file=sys.stderr)
        send_google_chat(summary_data, config)
    else:
        print(f"\n💬 Google Chat notification is disabled", file=sys.stderr)

if __name__ == '__main__':
    main()
