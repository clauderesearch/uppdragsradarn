#!/usr/bin/env python3
"""
Process GeoNames cities15000.zip data and prepare it for database import.

This script downloads the cities15000.zip file from GeoNames, processes the data,
and generates a CSV file that can be imported into the locations table.

Usage:
    python process_geonames.py [--eu-only] [--output-file OUTPUT_FILE]

Options:
    --eu-only           Only include cities in European Union countries
    --output-file       Output file path (default: ../db/data/geonames_cities.csv)
"""

import argparse
import csv
import io
import json
import os
import time
import urllib.request
import uuid
import zipfile
from datetime import datetime

# EU country codes (ISO 3166-1 alpha-2)
EU_COUNTRIES = {
    'AT', 'BE', 'BG', 'HR', 'CY', 'CZ', 'DK', 'EE', 'FI', 'FR', 
    'DE', 'GR', 'HU', 'IE', 'IT', 'LV', 'LT', 'LU', 'MT', 'NL', 
    'PL', 'PT', 'RO', 'SK', 'SI', 'ES', 'SE'
}

# Add EEA and neighboring countries that might be relevant
EXTENDED_EUROPEAN_COUNTRIES = EU_COUNTRIES.union({
    'NO', 'IS', 'LI', 'CH', 'UK', 'UA', 'RS', 'ME', 'MK', 'AL', 'BA', 'XK', 'TR'
})

# GeoNames URL
GEONAMES_URL = 'https://download.geonames.org/export/dump/cities15000.zip'

# Country codes to country names mapping
COUNTRY_NAMES = {
    'SE': 'Sweden', 'NO': 'Norway', 'DK': 'Denmark', 'FI': 'Finland',
    'DE': 'Germany', 'FR': 'France', 'GB': 'United Kingdom', 'UK': 'United Kingdom',
    'ES': 'Spain', 'IT': 'Italy', 'NL': 'Netherlands', 'BE': 'Belgium',
    'PL': 'Poland', 'CZ': 'Czech Republic', 'AT': 'Austria', 'CH': 'Switzerland',
    'HU': 'Hungary', 'RO': 'Romania', 'BG': 'Bulgaria', 'GR': 'Greece', 'EL': 'Greece',
    'PT': 'Portugal', 'IE': 'Ireland', 'LT': 'Lithuania', 'LV': 'Latvia',
    'EE': 'Estonia', 'SK': 'Slovakia', 'SI': 'Slovenia', 'HR': 'Croatia',
    'CY': 'Cyprus', 'LU': 'Luxembourg', 'MT': 'Malta', 'IS': 'Iceland',
    'LI': 'Liechtenstein', 'UA': 'Ukraine', 'RS': 'Serbia', 'ME': 'Montenegro',
    'MK': 'North Macedonia', 'AL': 'Albania', 'BA': 'Bosnia and Herzegovina',
    'XK': 'Kosovo', 'TR': 'Turkey'
}

def download_geonames_data():
    """Download the GeoNames cities15000.zip file."""
    print(f"Downloading {GEONAMES_URL}...")
    with urllib.request.urlopen(GEONAMES_URL) as response:
        zip_data = response.read()
    
    print("Download complete.")
    return zip_data

def extract_geonames_data(zip_data):
    """Extract the cities15000.txt file from the zip data."""
    print("Extracting cities15000.txt from zip file...")
    with zipfile.ZipFile(io.BytesIO(zip_data)) as zip_ref:
        cities_data = zip_ref.read('cities15000.txt').decode('utf-8')
    
    print("Extraction complete.")
    return cities_data

def process_geonames_data(cities_data, eu_only=False):
    """Process the GeoNames data and convert it to our database format."""
    print("Processing GeoNames data...")
    
    rows = []
    lines = cities_data.strip().split('\n')
    
    # GeoNames column definitions
    # 0: geonameid - integer id of record in geonames database
    # 1: name - name of geographical point
    # 2: asciiname - name of geographical point in plain ascii characters
    # 3: alternatenames - alternate names for the feature
    # 4: latitude - latitude in decimal degrees (wgs84)
    # 5: longitude - longitude in decimal degrees (wgs84)
    # 6: feature class - see http://www.geonames.org/export/codes.html
    # 7: feature code - see http://www.geonames.org/export/codes.html
    # 8: country code - ISO-3166 2-letter country code
    # 9: cc2 - alternate country codes
    # 10: admin1 code - fipscode (admin1 is the largest administrative division)
    # 11: admin2 code - code for the second administrative division
    # 12: admin3 code - code for the third administrative division
    # 13: admin4 code - code for the fourth administrative division
    # 14: population - population in integer
    # 15: elevation - elevation in meters
    # 16: dem - digital elevation model, srtm3 or gtopo30
    # 17: timezone - the iana timezone id
    # 18: modification date - date of last modification in yyyy-MM-dd format
    
    for line in lines:
        fields = line.split('\t')
        
        if len(fields) < 19:
            continue
        
        geoname_id = int(fields[0])
        city = fields[1]
        country_code = fields[8]
        
        # Skip if we only want EU countries and this isn't one
        filter_set = EXTENDED_EUROPEAN_COUNTRIES if eu_only else None
        if filter_set and country_code not in filter_set:
            continue
        
        # Get other fields
        latitude = float(fields[4]) if fields[4] else None
        longitude = float(fields[5]) if fields[5] else None
        population = int(fields[14]) if fields[14] else None
        country_name = COUNTRY_NAMES.get(country_code, "")
        
        # Some GeoNames data doesn't have proper admin1 (region) data
        region = ""
        
        # Generate a UUID
        id_str = str(uuid.uuid4())
        
        # Created_at and updated_at dates
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        
        # Remote-friendly is false by default for real locations
        is_remote_friendly = "false"
        
        # Active is true
        is_active = "true"
        
        rows.append([
            id_str,              # id
            city,                # city
            region,              # region
            country_code,        # country_code
            country_name,        # country_name
            str(latitude) if latitude is not None else "",  # latitude
            str(longitude) if longitude is not None else "", # longitude
            str(population) if population is not None else "", # population
            str(geoname_id),     # geoname_id
            is_remote_friendly,  # is_remote_friendly
            is_active,           # is_active
            now,                 # created_at
            now                  # updated_at
        ])
    
    # Add a "Remote" location
    remote_id = str(uuid.uuid4())
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    rows.append([
        remote_id,          # id
        "Remote",           # city
        "",                 # region
        "SE",               # country_code
        "Sweden",           # country_name
        "",                 # latitude
        "",                 # longitude
        "",                 # population
        "0",                # geoname_id
        "true",             # is_remote_friendly
        "true",             # is_active
        now,                # created_at
        now                 # updated_at
    ])
    
    print(f"Processed {len(rows)} locations.")
    return rows

def write_csv(rows, output_file):
    """Write the processed data to a CSV file."""
    print(f"Writing data to {output_file}...")
    
    # Ensure the output directory exists
    os.makedirs(os.path.dirname(output_file), exist_ok=True)
    
    with open(output_file, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        # Write header row
        writer.writerow([
            'id', 'city', 'region', 'country_code', 'country_name',
            'latitude', 'longitude', 'population', 'geoname_id',
            'is_remote_friendly', 'is_active', 'created_at', 'updated_at'
        ])
        # Write data rows
        writer.writerows(rows)
    
    print(f"Data written to {output_file}.")

def main():
    parser = argparse.ArgumentParser(description='Process GeoNames data')
    parser.add_argument('--eu-only', action='store_true', help='Only include EU countries')
    parser.add_argument('--output-file', default='../db/data/geonames_cities.csv', 
                      help='Output file path')
    
    args = parser.parse_args()
    
    # Download and process GeoNames data
    zip_data = download_geonames_data()
    cities_data = extract_geonames_data(zip_data)
    rows = process_geonames_data(cities_data, args.eu_only)
    
    # Write the data to a CSV file
    write_csv(rows, args.output_file)
    
    print("Done!")

if __name__ == '__main__':
    main()