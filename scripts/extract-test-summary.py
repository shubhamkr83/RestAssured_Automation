#!/usr/bin/env python3
"""
Extract test execution summary from TestNG results XML
Outputs JSON format for easy consumption by notification scripts
"""
import xml.etree.ElementTree as ET
import json
import sys
import os

def extract_summary(testng_results_path):
    """Extract test statistics from testng-results.xml"""
    
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
        
        summary = {
            'total': total,
            'passed': passed,
            'failed': failed,
            'skipped': skipped,
            'pass_rate': round(pass_rate, 2)
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
