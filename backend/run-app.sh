#!/bin/bash
cd /Users/ssivared/log-analysis-system/backend
export CLASSPATH="target/classes:$(cat classpath.txt)"
java -cp $CLASSPATH -Dspring.profiles.active=local com.loganalyzer.LogAnalyzerApplication
