#!/bin/bash
# Comprehensive MSLL Evaluation Tests

echo "========================================="
echo "MSLL Grammar Evaluation Tests"
echo "========================================="
echo ""

cd /Users/imac/Documents/code/github/msll

echo "1. Compiling..."
mvn -q clean compile test-compile
if [ $? -ne 0 ]; then
    echo "❌ Compilation failed"
    exit 1
fi
echo "✓ Compilation successful"
echo ""

echo "2. Testing JSON Grammar..."
mvn -q test -Dtest=JSONGrammarLoadTest 2>&1 | grep -E "(JSON grammar|Tests run|BUILD)"
echo ""

echo "3. Testing JavaScript Grammar..."
mvn -q test -Dtest=JavaScriptGrammarLoadTest 2>&1 | grep -E "(JavaScript grammar|Tests run|BUILD)"
echo ""

echo "4. Running JSON Parsing Tests..."
mvn -q test -Dtest=JSONEvaluationTest 2>&1 | grep -E "(test_|Tests run|BUILD)"
echo ""

echo "5. Running JavaScript Parsing Tests..."
mvn -q test -Dtest=JavaScriptEvaluationTest 2>&1 | grep -E "(test_|Tests run|BUILD)"
echo ""

echo "========================================="
echo "Test Summary"
echo "========================================="
mvn -q test -Dtest=*EvaluationTest 2>&1 | grep "Tests run"
