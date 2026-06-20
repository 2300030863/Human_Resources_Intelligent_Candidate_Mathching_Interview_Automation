#!/bin/bash
# Quick start script for AI Voice Interview System

echo "Starting AI Voice Interview System..."
echo ""

# Activate virtual environment
if [ -f venv/bin/activate ]; then
    source venv/bin/activate
else
    echo "ERROR: Virtual environment not found!"
    echo "Please run setup.sh first"
    exit 1
fi

# Check if .env exists
if [ ! -f .env ]; then
    echo "ERROR: .env file not found!"
    echo "Please copy .env.example to .env and configure it"
    exit 1
fi

echo "Starting Streamlit application..."
streamlit run frontend/app.py
