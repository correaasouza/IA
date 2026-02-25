-- Alinha a sequence de locatario apos seed com id fixo (id=1 no baseline).
SELECT setval(
  pg_get_serial_sequence('locatario', 'id'),
  COALESCE((SELECT MAX(id) FROM locatario), 1),
  true
);
