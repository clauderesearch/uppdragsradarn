#!/bin/bash
# Script to download and process GeoNames data for location normalization

set -e  # Exit on any error

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_SCRIPT="$SCRIPT_DIR/src/main/resources/scripts/process_geonames.py"
OUTPUT_DIR="$SCRIPT_DIR/src/main/resources/db/data"
OUTPUT_FILE="$OUTPUT_DIR/geonames_cities.csv"

# Make sure the output directory exists
mkdir -p "$OUTPUT_DIR"

# Check for Python
if ! command -v python3 &> /dev/null; then
    echo "Python 3 is required. Please install it and try again."
    exit 1
fi

echo "========================================================"
echo "GeoNames Location Data Processor for ConsultantCompass"
echo "========================================================"
echo ""
echo "This script will download and process GeoNames data"
echo "for use with the location normalization feature."
echo ""
echo "The processed data will be saved to:"
echo "$OUTPUT_FILE"
echo ""

# Parse command line arguments
EU_ONLY=""
while [[ $# -gt 0 ]]; do
    case $1 in
        --eu-only)
            EU_ONLY="--eu-only"
            shift
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Prompt for confirmation
read -p "Proceed with download and processing? [y/N] " response
if [[ ! "$response" =~ ^[Yy]$ ]]; then
    echo "Operation cancelled."
    exit 0
fi

echo ""
echo "Starting download and processing..."
echo ""

# Run the Python script
python3 "$PYTHON_SCRIPT" $EU_ONLY --output-file "$OUTPUT_FILE"

echo ""
echo "========================================================"
echo "Processing complete!"
echo ""
echo "To import this data into your database, you can use:"
echo "psql -h <host> -U <user> -d <dbname> -f $OUTPUT_DIR/locations_import.sql"
echo ""
echo "Remember to update the SQL file to point to the CSV file path."
echo "========================================================"