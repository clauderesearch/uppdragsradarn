# Location Normalization

This document describes the location normalization system built for ConsultantCompass.

## Overview

The location normalization system allows the application to:

1. Standardize location names across different job listings and providers
2. Support geographic searches and filtering
3. Handle remote work locations
4. Support multiple locations per assignment
5. Provide better search capabilities for users

## Architecture

The location system consists of:

1. **Core database tables**:
   - `locations`: Canonical location information
   - `location_aliases`: Alternative names/spellings of locations
   - `assignment_locations`: Join table for assignments and locations

2. **Service layer**:
   - `LocationService`: Handles location normalization and searching
   - Batch processing for normalizing existing assignments

3. **Data sources**:
   - GeoNames database integration (cities with 15,000+ population)
   - Special handling for remote work

## Using GeoNames Data

The system is designed to use the GeoNames database for comprehensive location information.

### Quick Start

1. Run the provided script to download and process GeoNames data:
   ```
   ./process-geonames.sh [--eu-only]
   ```

2. The script will:
   - Download cities15000.zip from GeoNames
   - Process the data into the required format
   - Generate a CSV file in `src/main/resources/db/data/geonames_cities.csv`

3. Import the data with:
   ```
   # Update the file path in the SQL script first
   vi src/main/resources/db/data/locations_import.sql
   
   # Then run the import
   psql -h <db-host> -U <db-user> -d <db-name> -f src/main/resources/db/data/locations_import.sql
   ```

### Technical Details

The GeoNames processing script:
1. Downloads data for cities with populations > 15,000
2. Maps GeoNames fields to our database schema
3. Adds special handling for remote work
4. Generates properly formatted UUIDs for each location

## Location Normalization Process

When a new assignment is created or updated:

1. The `location` field from the provider is analyzed
2. Remote work indicators are detected
3. Location text is matched against known locations using:
   - Exact matches from the locations table
   - Aliases from the location_aliases table
   - Fuzzy matching for similar text
4. If a match is found, an assignment-location association is created
5. If no match is found, fallback mechanisms handle the case

## Migration Process

For existing assignments:

1. The batch process can be triggered via the admin API
2. Each assignment's location field is processed and normalized
3. Assignment-location associations are created

## API Endpoints

Admin API endpoints are available at:

- `GET /api/admin/locations/search?query=<term>` - Search for locations
- `GET /api/admin/locations/{id}` - Get location by ID
- `POST /api/admin/locations` - Create or update a location
- `GET /api/admin/locations/normalize?locationText=<text>` - Test location normalization
- `POST /api/admin/locations/normalize-all` - Start batch normalization

## Frontend Integration

The frontend can use the normalized location data:
1. Assignment DTOs include both the original location string and normalized location information
2. Search can use either the legacy location field or normalized locations
3. Autocomplete suggestions can use the locations table

## Performance Considerations

1. Indexes are created on key fields for performance
2. The pg_trgm extension is used for text similarity searches
3. Caching is implemented for location normalization

## Future Enhancements

Potential future enhancements:
1. Radius-based geographic searches
2. Map visualization of job locations
3. Expanded location attributes (timezone, etc.)
4. More sophisticated handling of administrative divisions