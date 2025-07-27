#!/bin/bash

echo "Starting frontend from: $(pwd)/frontend"
echo "Node version: $(node --version)"
echo "NPM version: $(npm --version)"

# Change to frontend directory
cd frontend || exit 1

# Check if package.json exists
if [ ! -f "package.json" ]; then
    echo "Error: package.json not found in frontend directory"
    exit 1
fi

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

# Start the React development server
echo "Starting React development server on port 3001..."
PORT=3001 npm start
