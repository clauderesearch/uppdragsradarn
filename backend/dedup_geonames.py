#!/usr/bin/env python3
import csv
import sys
from collections import defaultdict

def deduplicate_cities(input_file, output_file):
    # Dictionary to store unique city entries based on city+region+country_code key
    unique_entries = {}
    
    # Track cities with same city/region/country but different geoname_id
    city_geoname_groups = defaultdict(list)
    
    # Read the input CSV file
    with open(input_file, 'r', encoding='utf-8') as csvfile:
        reader = csv.DictReader(csvfile)
        fieldnames = reader.fieldnames
        
        # Create new fieldnames without lat/long
        new_fieldnames = [f for f in fieldnames if f not in ['latitude', 'longitude']]
        
        # Dictionary for counting duplicates
        dupes = defaultdict(int)
        
        # First pass - group by city/region/country and collect geoname_ids
        for row in reader:
            # Create a basic key based on city, region, and country_code
            basic_key = (row['city'], row['region'] or '', row['country_code'])
            geoname_id = row.get('geoname_id')
            
            # Store the row with its geoname_id for later processing
            city_geoname_groups[basic_key].append((geoname_id, row))
    
    # Second pass - process each group
    for basic_key, entries in city_geoname_groups.items():
        city, region, country_code = basic_key
        dupes[basic_key] = len(entries)
        
        if len(entries) == 1:
            # Only one entry, no deduplication needed
            geoname_id, row = entries[0]
            # Remove latitude and longitude
            new_row = {k: v for k, v in row.items() if k in new_fieldnames}
            unique_entries[basic_key] = new_row
        else:
            # Multiple entries with same city/region/country - need to deduplicate
            # First, try to pick the one with the highest population
            entries.sort(key=lambda x: int(x[1].get('population') or 0), reverse=True)
            
            # For cities with duplicate entries, prioritize keeping the entry with:
            # 1. Higher population (if different)
            # 2. If same population, pick the one with lower geoname_id (more canonical)
            if entries:
                # Sort by population (descending) and then by geoname_id (ascending)
                entries.sort(key=lambda x: (-int(x[1].get('population') or 0), int(x[0] or 999999999)))
                # Take just the first (best) entry
                geoname_id, row = entries[0]
                # Remove latitude and longitude
                new_row = {k: v for k, v in row.items() if k in new_fieldnames}
                unique_entries[basic_key] = new_row
                
                # Debug statements removed
    
    # Write the deduplicated data to the output file
    with open(output_file, 'w', encoding='utf-8', newline='') as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=new_fieldnames)
        writer.writeheader()
        writer.writerows(unique_entries.values())
    
    # Print stats
    total_entries = sum(dupes.values())
    unique_count = len(unique_entries)
    duplicate_count = total_entries - unique_count
    duplicate_cities = sum(1 for count in dupes.values() if count > 1)
    
    print(f"Original entries: {total_entries}")
    print(f"Unique entries: {unique_count}")
    print(f"Duplicate entries removed: {duplicate_count}")
    print(f"Cities with duplicates: {duplicate_cities}")
    
    # Print some examples of duplicates
    if duplicate_count > 0:
        print("\nExamples of duplicated cities:")
        examples = 0
        for key, count in sorted(dupes.items(), key=lambda x: x[1], reverse=True):
            if count > 1 and examples < 10:
                city, region, country = key
                region_info = f", {region}" if region else ""
                print(f"  {city}{region_info}, {country}: {count} occurrences")
                examples += 1

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python dedup_geonames.py <input_csv> <output_csv>")
        sys.exit(1)
    
    input_file = sys.argv[1]
    output_file = sys.argv[2]
    deduplicate_cities(input_file, output_file)