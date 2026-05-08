UPDATE kol_social_channel  SET platform = UPPER(platform) WHERE platform <> UPPER(platform);
UPDATE kol_pricing_package SET platform = UPPER(platform) WHERE platform <> UPPER(platform);
UPDATE booking_deliverable SET platform = UPPER(platform) WHERE platform <> UPPER(platform);