-- Pilot Lagos partners for skyWash Neon (run in Neon SQL editor if deploy is delayed).
-- Nearby only returns shops within 25 km of pickup — use a Lagos address in the app.

INSERT INTO partners (id, name, city, area, address, lat, lng, rating, phone, email, is_active)
VALUES
  ('seed-yaba-fresh', 'Yaba Fresh Laundry', 'Lagos', 'Yaba', 'Herbert Macaulay Way, Yaba, Lagos', 6.5095, 3.3860, 4.8, '+2348010000001', 'ops@seed-yaba-fresh.partner.skywash.app', true),
  ('seed-surulere-clean', 'Surulere Clean Co', 'Lagos', 'Surulere', 'Adeniran Ogunsanya Street, Surulere, Lagos', 6.4969, 3.3566, 4.6, '+2348010000002', 'ops@seed-surulere-clean.partner.skywash.app', true),
  ('seed-vi-express', 'VI Express Wash', 'Lagos', 'Victoria Island', 'Adeola Odeku Street, Victoria Island, Lagos', 6.4281, 3.4219, 4.9, '+2348010000003', 'ops@seed-vi-express.partner.skywash.app', true),
  ('seed-lekki-fold', 'Lekki Fold & Go', 'Lagos', 'Lekki Phase 1', 'Admiralty Way, Lekki Phase 1, Lagos', 6.4474, 3.4722, 4.5, '+2348010000004', 'ops@seed-lekki-fold.partner.skywash.app', true),
  ('seed-ikeja-spark', 'Ikeja Spark Wash', 'Lagos', 'Ikeja', 'Allen Avenue, Ikeja, Lagos', 6.6018, 3.3515, 4.7, '+2348010000005', 'ops@seed-ikeja-spark.partner.skywash.app', true),
  ('seed-mainland-care', 'Mainland Care Laundry', 'Lagos', 'Somolu', 'Market Street, Somolu, Lagos', 6.5408, 3.3842, 4.4, '+2348010000006', 'ops@seed-mainland-care.partner.skywash.app', true)
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  city = EXCLUDED.city,
  area = EXCLUDED.area,
  address = EXCLUDED.address,
  lat = EXCLUDED.lat,
  lng = EXCLUDED.lng,
  rating = EXCLUDED.rating,
  phone = EXCLUDED.phone,
  email = EXCLUDED.email,
  is_active = true;
