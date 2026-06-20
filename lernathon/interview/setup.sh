#!/bin/bash
# Setup script for AI Voice Interview System (Linux/Mac)

echo "========================================"
echo "AI Voice Interview System - Setup"
echo "========================================"
echo ""

# Check if Python is installed
if ! command -v python3 &> /dev/null; then
    echo "ERROR: Python 3 is not installed!"
    echo "Please install Python 3.9 or higher"
    exit 1
fi

echo "[1/5] Creating virtual environment..."
python3 -m venv venv
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to create virtual environment"
    exit 1
fi

echo ""
echo "[2/5] Activating virtual environment..."
source venv/bin/activate

echo ""
echo "[3/5] Upgrading pip..."
python -m pip install --upgrade pip

echo ""
echo "[4/5] Installing dependencies..."
pip install -r requirements.txt

echo ""
echo "[5/5] Setting up environment file..."
if [ ! -f .env ]; then
    cp .env.example .env
    echo "Created .env file - Please edit it and add your API keys!"
else
    echo ".env file already exists"
fi

echo ""
echo "========================================"
echo "Setup completed successfully!"
echo "========================================"
echo ""
echo "Next steps:"
echo "1. Edit .env file and add your GROQ_API_KEY"
echo "2. Configure your database in .env"
echo "3. Run: python -m database.connection (to initialize database)"
echo "4. Run: streamlit run frontend/app.py (to start the app)"
echo ""
echo "For testing: python scripts/utils.py check-env"
echo ""
