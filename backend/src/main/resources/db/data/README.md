# Location Data Import

This directory contains scripts and data for importing location information into the database.

## GeoNames Import Process

The location normalization system uses data from [GeoNames](https://www.geonames.org/), which provides comprehensive geographic data for the entire world.

### Step 1: Process GeoNames Data

The easiest way to generate the data is using the provided shell script:

```bash
cd /path/to/project/backend
./process-geonames.sh [--eu-only]
```

Alternatively, you can run the Python script directly:

```bash
cd /path/to/project/backend/src/main/resources/scripts
python process_geonames.py [--eu-only]
```

This will:
1. Download the cities15000.zip file from GeoNames (cities with a population > 15,000)
2. Extract and process the data
3. Generate a CSV file in `../db/data/geonames_cities.csv`

Options:
- `--eu-only`: Only include European cities
- `--output-file`: Specify a different output file path (Python script only)

### Step 2: Import to Database

There are two ways to import the data:

#### Option A: Using Liquibase (Automatic, Recommended)

Once the CSV file is generated, Liquibase will automatically import it during the next application startup. The import is controlled by the v17-add-location-normalization.xml changeset:

1. The first part of the migration creates the required tables and adds two default locations: "Remote" and "Stockholm"
2. The second part checks if:
   - The locations table is empty (except for Remote and Stockholm)
   - The CSV file exists in the expected location
3. If both conditions are met, it loads the data from the CSV file using Liquibase's `loadData` feature

If you've already run the migration before generating the CSV, you can force Liquibase to run again by:
- Updating the application version in pom.xml
- Running the application with the `spring.liquibase.drop-first=true` parameter (caution: this will reset your database)

#### Option B: Manual Import

If the automatic import doesn't work, you can import the data manually:

```bash
# Make sure PostgreSQL client tools are installed
sudo apt-get install postgresql-client  # On Debian/Ubuntu

# Update the path in the SQL script to point to your CSV file (if needed)
vi locations_import.sql

# Run the import script against your database
psql -h localhost -U your_db_user -d your_db_name -f locations_import.sql
```

The SQL script includes safeguards to prevent duplicate data by checking if the locations table already contains data.

## Data Structure

The processed GeoNames data includes:

- `id`: UUID for the location
- `city`: City name
- `region`: Region/state/province (may be empty)
- `country_code`: ISO 3166-1 alpha-2 country code
- `country_name`: Full country name
- `latitude`: Geographic latitude
- `longitude`: Geographic longitude
- `population`: Population (if available)
- `geoname_id`: Original GeoNames ID
- `is_remote_friendly`: Whether this is a remote work location
- `is_active`: Whether the location is active
- `created_at`: Creation timestamp
- `updated_at`: Last update timestamp

## Special Locations

The system includes special locations:

1. **Remote** - A location for remote work assignments
2. **Stockholm** - Default fallback location for Sweden

These are automatically created by the Liquibase migration regardless of whether you import the full GeoNames dataset.

## Updating Data

To update the GeoNames data:

1. Run the process_geonames.sh script again to generate a fresh CSV file
2. Either:
   - Use manual import with the SQL script
   - Reset your database and let Liquibase import it automatically

Note that updating existing data might cause conflicts with location assignments, so it's recommended to perform updates during maintenance windows.