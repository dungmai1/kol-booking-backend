-- Store structured Vietnam addresses (street, ward, province) composed in the city field.
ALTER TABLE kol_profile
    ALTER COLUMN city TYPE VARCHAR(255);
