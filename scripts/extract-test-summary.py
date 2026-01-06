#!/usr/bin/env python3
"""
Extract test execution summary from TestNG results XML
Outputs JSON format for easy consumption by notification scripts
"""
import xml.etree.ElementTree as ET
import json
import sys
import os

def convert_class_name_to_feature(class_name):
    """Convert a Java class name to a human-readable feature name
    
    Examples:
        FeaturedCollectionTest -> Featured Collection
        ContinueYourJourneyTest -> Continue Your Journey
        LoginApiTest -> Login Api
    """
    # Extract simple class name (remove package)
    simple_name = class_name.split('.')[-1]
    
    # Remove 'Test' suffix if present
    if simple_name.endswith('Test'):
        simple_name = simple_name[:-4]
    
    # Split camelCase/PascalCase into words
    import re
    # Insert space before uppercase letters (but not at the start)
    words = re.sub('([A-Z])', r' \1', simple_name).strip()
    
    return words

def extract_summary(testng_results_path):
    """Extract test statistics and detailed test case info from testng-results.xml"""
    
    if not os.path.exists(testng_results_path):
        print(f"Error: File not found: {testng_results_path}", file=sys.stderr)
        sys.exit(1)
    
    try:
        tree = ET.parse(testng_results_path)
        root = tree.getroot()
        
        # Extract from <testng-results> root element
        total = int(root.get('total', 0))
        passed = int(root.get('passed', 0))
        failed = int(root.get('failed', 0))
        skipped = int(root.get('skipped', 0))
        
        # Calculate additional metrics
        pass_rate = (passed / total * 100) if total > 0 else 0
        
        # Extract detailed test case information
        test_cases = []
        serial_no = 1
        
        # Iterate through all test methods
        for suite in root.findall('.//suite'):
            for test in suite.findall('.//test'):
                for class_elem in test.findall('.//class'):
                    # Get class name for feature extraction
                    class_name = class_elem.get('name', 'Unknown')
                    feature_name = convert_class_name_to_feature(class_name)
                    
                    for test_method in class_elem.findall('.//test-method'):
                        # Skip configuration methods (like @BeforeMethod, @AfterMethod)
                        if test_method.get('is-config') == 'true':
                            continue
                        
                        test_name = test_method.get('name', 'Unknown')
                        status = test_method.get('status', 'UNKNOWN')
                        
                        # Get description from method or use test name
                        description = test_method.get('description', '')
                        if not description:
                            description = f"Test case: {test_name}"
                        
                        # Map status to emoji
                        status_emoji = {
                            'PASS': '✅ Passed',
                            'FAIL': '❌ Failed',
                            'SKIP': '⚠️ Skipped'
                        }.get(status, '❓ Unknown')
                        
                        test_cases.append({
                            'serial_no': serial_no,
                            'name': test_name,
                            'feature_name': feature_name,
                            'description': description,
                            'status': status_emoji,
                            'raw_status': status
                        })
                        serial_no += 1
        
        summary = {
            'total': total,
            'passed': passed,
            'failed': failed,
            'skipped': skipped,
            'pass_rate': round(pass_rate, 2),
            'test_cases': test_cases
        }
        
        return summary
        
    except Exception as e:
        print(f"Error parsing XML: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: python extract-test-summary.py <path-to-testng-results.xml>")
        sys.exit(1)
    
    results_path = sys.argv[1]
    summary = extract_summary(results_path)
    
    # Output as JSON
    print(json.dumps(summary, indent=2))
