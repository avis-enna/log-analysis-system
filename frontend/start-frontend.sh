#!/bin/bash
cd "$(dirname "$0")"
echo "Starting frontend from: $(pwd)"
echo "Node version: $(node --version)"
echo "NPM version: $(npm --version)"

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install --legacy-peer-deps
fi

# Check if react-scripts exists
if [ ! -f "node_modules/.bin/react-scripts" ]; then
    echo "react-scripts not found, reinstalling..."
    npm install react-scripts --legacy-peer-deps
fi

# Set environment variables
export PORT=3001
export BROWSER=none
export CI=false

echo "Starting React development server on port 3001..."
exec ./node_modules/.bin/react-scripts start
