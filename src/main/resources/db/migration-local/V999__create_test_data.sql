INSERT INTO products (name, description, price_in_cents, currency, attributes)
SELECT
    -- More realistic product name
    (ARRAY['Premium','Ultra','Classic','Eco','Pro','Smart','Elite','Advanced'])[1 + (gs % 8)]
  || ' '
  || (ARRAY['Running Shoes','Backpack','T-Shirt','Jacket','Headphones','Watch','Laptop','Keyboard'])[1 + (gs % 8)]
  || ' '
  || (ARRAY['Series A','Series X','Edition','Model','Gen','Version'])[1 + (gs % 6)]
  || ' '
  || gs::text                                                     AS name,

  -- Realistic description
  'The '
  || (ARRAY['latest','innovative','high-quality','durable','stylish','lightweight'])[1 + (gs % 6)]
  || ' '
  || (ARRAY['performance','design','technology','material','craftsmanship'])[1 + (gs % 5)]
  || ' makes this product ideal for '
  || (ARRAY['daily use','sports','outdoor adventures','professionals','students','travel'])[1 + (gs % 6)]
  || '.'                                                           AS description,

  -- Price range: €5.00 – €999.99
  (500 + (random() * 99500)::int)::bigint                         AS price_in_cents,

  -- Currency variation (mostly EUR, some USD)
  (ARRAY['EUR','EUR','EUR','EUR','USD'])[1 + (gs % 5)]            AS currency,

  jsonb_build_object(
    'brand',      (ARRAY['Nike','Adidas','Puma','Apple','Samsung','Sony','NorthFace','Asics'])[1 + (gs % 8)],
    'color',      (ARRAY['red','blue','green','black','white','yellow','grey'])[1 + (gs % 7)],
    'size',       (ARRAY['XS','S','M','L','XL'])[1 + (gs % 5)],
    'category',   (ARRAY['Sports','Electronics','Fashion','Outdoor','Accessories'])[1 + (gs % 5)],
    'material',   (ARRAY['cotton','wool','polyester','leather','aluminum','plastic'])[1 + (gs % 6)],
    'rating',     (round((random() * 4 + 1)::numeric, 1))::text,
    'sku',        'SKU-' || lpad(gs::text, 6, '0')
  )                                                                 AS attributes

FROM generate_series(1, 10000) gs;
