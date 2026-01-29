#!/bin/bash

# Script pour lancer les tests et afficher le rapport de coverage

echo "🧪 Running tests with coverage..."
./mvnw clean test jacoco:report

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Tests passed successfully!"
    echo ""
    echo "📊 Coverage report generated at:"
    echo "   file://$(pwd)/target/site/jacoco/index.html"
    echo ""
    echo "📈 To view the report, open the file in your browser:"
    echo "   open target/site/jacoco/index.html"
    echo ""

    # Extract coverage summary from jacoco.csv
    if [ -f "target/site/jacoco/jacoco.csv" ]; then
        echo "📋 Coverage Summary:"
        echo "===================="
        awk -F',' 'NR==1 {next} {
            inst_missed+=$4; inst_covered+=$5;
            branch_missed+=$6; branch_covered+=$7;
            line_missed+=$8; line_covered+=$9;
        } END {
            inst_total=inst_missed+inst_covered;
            branch_total=branch_missed+branch_covered;
            line_total=line_missed+line_covered;

            inst_pct = inst_total > 0 ? (inst_covered*100.0/inst_total) : 0;
            branch_pct = branch_total > 0 ? (branch_covered*100.0/branch_total) : 0;
            line_pct = line_total > 0 ? (line_covered*100.0/line_total) : 0;

            printf "Instructions: %.1f%% (%d/%d)\n", inst_pct, inst_covered, inst_total;
            printf "Branches:     %.1f%% (%d/%d)\n", branch_pct, branch_covered, branch_total;
            printf "Lines:        %.1f%% (%d/%d)\n", line_pct, line_covered, line_total;
        }' target/site/jacoco/jacoco.csv
        echo ""
    fi
else
    echo ""
    echo "❌ Tests failed!"
    echo "   Check the output above for details."
    exit 1
fi
