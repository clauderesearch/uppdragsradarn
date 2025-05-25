-- Script to import GeoNames data into the locations table
-- This is an alternative manual import method if Liquibase loadData fails
-- Run this after processing the GeoNames data with process_geonames.py

-- Check if the table is already populated (excluding our fallback entries)
DO $$
DECLARE
  location_count INTEGER;
BEGIN
  SELECT COUNT(*) INTO location_count FROM locations WHERE city != 'Remote' AND city != 'Stockholm';
  
  IF location_count > 0 THEN
    RAISE NOTICE 'Locations table already contains % entries (excluding Remote and Stockholm). Skipping import.', location_count;
  ELSE
    -- Copy data from the processed CSV file
    -- Update the path below to point to the actual location of your CSV file
    COPY locations (
        id, city, region, country_code, country_name, 
        latitude, longitude, population, geoname_id,
        is_remote_friendly, is_active, created_at, updated_at
    ) FROM '/home/jahwag/IdeaProjects/consultantcompass/backend/src/main/resources/db/data/geonames_cities.csv' WITH (FORMAT csv, HEADER true);
    
    RAISE NOTICE 'Imported locations from CSV file. Checking count...';
    
    -- Log completion
    SELECT COUNT(*) AS imported_locations FROM locations;
  END IF;
END $$;

-- Create indexes if they don't exist (these should already exist from Liquibase changesets)
CREATE INDEX IF NOT EXISTS idx_locations_city ON locations(city);
CREATE INDEX IF NOT EXISTS idx_locations_country_code ON locations(country_code);
CREATE INDEX IF NOT EXISTS idx_locations_geoname_id ON locations(geoname_id);
CREATE INDEX IF NOT EXISTS idx_locations_coordinates ON locations(latitude, longitude);

-- Ensure unique constraint (should already exist from Liquibase changesets)
ALTER TABLE locations 
DROP CONSTRAINT IF EXISTS uk_locations_city_region_country;

ALTER TABLE locations 
ADD CONSTRAINT uk_locations_city_region_country
UNIQUE (city, region, country_code);