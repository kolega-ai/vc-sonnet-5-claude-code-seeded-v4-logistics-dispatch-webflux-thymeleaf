-- Seed data for Field Dispatch Operations (deterministic IDs rely on insertion order)

INSERT INTO regions (name, code) VALUES
('North Metro', 'NM1'),
('South Bay', 'SB2'),
('West Valley', 'WV3'),
('East Ridge', 'ER4');

INSERT INTO users (email, password_hash, full_name, role, region_id, phone) VALUES
('admin@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Ava Ops Admin', 'OPS_ADMIN', NULL, '555-2824'),
('dispatcher1@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Derek Dispatch', 'DISPATCHER', 1, '555-1409'),
('dispatcher2@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Dana Rowe', 'DISPATCHER', 2, '555-5506'),
('dispatcher3@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Marco Diaz', 'DISPATCHER', 3, '555-5012'),
('dispatcher4@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Priya Nair', 'DISPATCHER', 4, '555-4657'),
('worker1@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Sam Rivera', 'FIELD_WORKER', 1, '555-3286'),
('worker2@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Jordan Lee', 'FIELD_WORKER', 2, '555-2679'),
('worker3@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Casey Nguyen', 'FIELD_WORKER', 3, '555-9935'),
('worker4@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Taylor Brooks', 'FIELD_WORKER', 4, '555-2424'),
('worker5@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Morgan Diaz', 'FIELD_WORKER', 1, '555-7912'),
('worker6@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Riley Chen', 'FIELD_WORKER', 2, '555-1520'),
('worker7@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Avery Scott', 'FIELD_WORKER', 3, '555-1488'),
('worker8@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Jamie Cole', 'FIELD_WORKER', 4, '555-2535'),
('worker9@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Drew Patel', 'FIELD_WORKER', 1, '555-4582'),
('worker10@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Skyler Ford', 'FIELD_WORKER', 2, '555-4811'),
('customer1@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Northgate Logistics Contact', 'CUSTOMER', 1, '555-9279'),
('customer2@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Blue Harbor Retail Contact', 'CUSTOMER', 2, '555-1434'),
('customer3@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Summit Manufacturing Contact', 'CUSTOMER', 3, '555-4257'),
('customer4@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Cedar Grove Clinic Contact', 'CUSTOMER', 4, '555-9928'),
('customer5@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Union Freight Co Contact', 'CUSTOMER', 1, '555-7873'),
('customer6@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Maple Street Cafe Contact', 'CUSTOMER', 2, '555-4611'),
('customer7@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Pinecrest Apartments Contact', 'CUSTOMER', 3, '555-8359'),
('customer8@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Harborview Hospital Contact', 'CUSTOMER', 4, '555-5557'),
('customer9@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Redwood Tech Park Contact', 'CUSTOMER', 1, '555-1106'),
('customer10@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Lakeside School District Contact', 'CUSTOMER', 2, '555-3615'),
('customer11@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Valley Farms Co-op Contact', 'CUSTOMER', 3, '555-7924'),
('customer12@fielddispatch.io', '$2b$10$jjOdpSYoavLHwtn23d8X7uqasZxHNZyr4zSFMdSOkj56oAjCyIvlm', 'Metro Office Plaza Contact', 'CUSTOMER', 4, '555-6574');

INSERT INTO dispatcher_regions (dispatcher_id, region_id) VALUES
(2, 1),
(3, 2),
(4, 3),
(5, 4),
(2, 4);

INSERT INTO customers (user_id, name, email, phone, region_id, notes) VALUES
(16, 'Northgate Logistics', 'customer1@fielddispatch.io', '555-5552', 1, 'Seeded customer account'),
(17, 'Blue Harbor Retail', 'customer2@fielddispatch.io', '555-3547', 2, 'Seeded customer account'),
(18, 'Summit Manufacturing', 'customer3@fielddispatch.io', '555-4527', 3, 'Seeded customer account'),
(19, 'Cedar Grove Clinic', 'customer4@fielddispatch.io', '555-6514', 4, 'Seeded customer account'),
(20, 'Union Freight Co', 'customer5@fielddispatch.io', '555-2674', 1, 'Seeded customer account'),
(21, 'Maple Street Cafe', 'customer6@fielddispatch.io', '555-2519', 2, 'Seeded customer account'),
(22, 'Pinecrest Apartments', 'customer7@fielddispatch.io', '555-7224', 3, 'Seeded customer account'),
(23, 'Harborview Hospital', 'customer8@fielddispatch.io', '555-2584', 4, 'Seeded customer account'),
(24, 'Redwood Tech Park', 'customer9@fielddispatch.io', '555-6881', 1, 'Seeded customer account'),
(25, 'Lakeside School District', 'customer10@fielddispatch.io', '555-6635', 2, 'Seeded customer account'),
(26, 'Valley Farms Co-op', 'customer11@fielddispatch.io', '555-5333', 3, 'Seeded customer account'),
(27, 'Metro Office Plaza', 'customer12@fielddispatch.io', '555-1711', 4, 'Seeded customer account');

INSERT INTO service_locations (customer_id, label, address_line, city, state, postal_code) VALUES
(1, 'Site 1', '104 Industrial Way', 'Hillsboro', 'OR', '97008'),
(2, 'Site 1', '107 Industrial Way', 'Gresham', 'OR', '97015'),
(3, 'Site 1', '110 Industrial Way', 'Portland', 'OR', '97022'),
(3, 'Site 2', '111 Industrial Way', 'Beaverton', 'OR', '97023'),
(4, 'Site 1', '113 Industrial Way', 'Beaverton', 'OR', '97029'),
(5, 'Site 1', '116 Industrial Way', 'Hillsboro', 'OR', '97036'),
(6, 'Site 1', '119 Industrial Way', 'Gresham', 'OR', '97043'),
(6, 'Site 2', '120 Industrial Way', 'Portland', 'OR', '97044'),
(7, 'Site 1', '122 Industrial Way', 'Portland', 'OR', '97050'),
(8, 'Site 1', '125 Industrial Way', 'Beaverton', 'OR', '97057'),
(9, 'Site 1', '128 Industrial Way', 'Hillsboro', 'OR', '97064'),
(9, 'Site 2', '129 Industrial Way', 'Gresham', 'OR', '97065'),
(10, 'Site 1', '131 Industrial Way', 'Gresham', 'OR', '97071'),
(11, 'Site 1', '134 Industrial Way', 'Portland', 'OR', '97078'),
(12, 'Site 1', '137 Industrial Way', 'Beaverton', 'OR', '97085'),
(12, 'Site 2', '138 Industrial Way', 'Hillsboro', 'OR', '97086');

INSERT INTO service_types (name, description, requires_proof, base_rate) VALUES
('Standard Delivery', 'Package or parcel delivery', TRUE, 25.0),
('Furniture Installation', 'On-site assembly and installation', TRUE, 120.0),
('HVAC Inspection', 'Routine HVAC system inspection', FALSE, 90.0),
('Equipment Repair', 'On-site equipment repair service', TRUE, 150.0),
('Signature Pickup', 'Pickup requiring signed acknowledgement', TRUE, 20.0),
('Site Survey', 'Non-invasive site assessment', FALSE, 60.0);

INSERT INTO workers (user_id, region_id, availability, certifications) VALUES
(6, 1, 'AVAILABLE', 'Forklift Certified, OSHA-10'),
(7, 2, 'AVAILABLE', 'HVAC License Level 2'),
(8, 3, 'AVAILABLE', 'Electrical Safety Cert'),
(9, 4, 'ON_JOB', 'First Aid, CPR'),
(10, 1, 'AVAILABLE', 'Heavy Equipment Cert'),
(11, 2, 'OFF_DUTY', 'OSHA-30'),
(12, 3, 'AVAILABLE', 'Refrigerant Handling Cert'),
(13, 4, 'AVAILABLE', 'Confined Space Entry'),
(14, 1, 'UNAVAILABLE', 'First Aid, CPR'),
(15, 2, 'AVAILABLE', 'Forklift Certified');

INSERT INTO jobs (customer_id, location_id, service_type_id, region_id, assigned_worker_id, status, priority, scheduled_start, scheduled_end, description, reschedule_count, created_at) VALUES
(1, 1, 1, 1, NULL, 'unassigned', 'low', now() + interval '-10 days' + interval '8 hours', now() + interval '-10 days' + interval '10 hours', 'Service call #1 for customer account', 1, now() - interval '44 hours'),
(2, 2, 2, 2, 6, 'assigned', 'urgent', now() + interval '-9 days' + interval '9 hours', now() + interval '-9 days' + interval '11 hours', 'Service call #2 for customer account', 0, now() - interval '43 hours'),
(3, 3, 3, 3, 3, 'en_route', 'high', now() + interval '-8 days' + interval '10 hours', now() + interval '-8 days' + interval '12 hours', 'Service call #3 for customer account', 0, now() - interval '42 hours'),
(4, 5, 4, 4, 8, 'arrived', 'normal', now() + interval '-7 days' + interval '11 hours', now() + interval '-7 days' + interval '13 hours', 'Service call #4 for customer account', 0, now() - interval '41 hours'),
(5, 6, 5, 1, 5, 'in_progress', 'low', now() + interval '-6 days' + interval '12 hours', now() + interval '-6 days' + interval '14 hours', 'Service call #5 for customer account', 0, now() - interval '40 hours'),
(6, 8, 6, 2, 10, 'completed', 'urgent', now() + interval '-5 days' + interval '13 hours', now() + interval '-5 days' + interval '15 hours', 'Service call #6 for customer account', 0, now() - interval '39 hours'),
(7, 9, 1, 3, 3, 'cancelled', 'high', now() + interval '-4 days' + interval '14 hours', now() + interval '-4 days' + interval '16 hours', 'Service call #7 for customer account', 0, now() - interval '38 hours'),
(8, 10, 2, 4, 8, 'failed', 'normal', now() + interval '-3 days' + interval '15 hours', now() + interval '-3 days' + interval '17 hours', 'Service call #8 for customer account', 0, now() - interval '37 hours'),
(9, 11, 3, 1, NULL, 'unassigned', 'low', now() + interval '-2 days' + interval '8 hours', now() + interval '-2 days' + interval '10 hours', 'Service call #9 for customer account', 0, now() - interval '36 hours'),
(10, 13, 4, 2, 2, 'assigned', 'urgent', now() + interval '-1 days' + interval '9 hours', now() + interval '-1 days' + interval '11 hours', 'Service call #10 for customer account', 1, now() - interval '35 hours'),
(11, 14, 5, 3, 3, 'en_route', 'high', now() + interval '0 days' + interval '10 hours', now() + interval '0 days' + interval '12 hours', 'Service call #11 for customer account', 0, now() - interval '34 hours'),
(12, 16, 6, 4, 8, 'arrived', 'normal', now() + interval '1 days' + interval '11 hours', now() + interval '1 days' + interval '13 hours', 'Service call #12 for customer account', 0, now() - interval '33 hours'),
(1, 1, 1, 1, 1, 'in_progress', 'low', now() + interval '2 days' + interval '12 hours', now() + interval '2 days' + interval '14 hours', 'Service call #13 for customer account', 0, now() - interval '32 hours'),
(2, 2, 2, 2, 6, 'completed', 'urgent', now() + interval '3 days' + interval '13 hours', now() + interval '3 days' + interval '15 hours', 'Service call #14 for customer account', 0, now() - interval '31 hours'),
(3, 3, 3, 3, 3, 'cancelled', 'high', now() + interval '4 days' + interval '14 hours', now() + interval '4 days' + interval '16 hours', 'Service call #15 for customer account', 0, now() - interval '30 hours'),
(4, 5, 4, 4, 8, 'failed', 'normal', now() + interval '5 days' + interval '15 hours', now() + interval '5 days' + interval '17 hours', 'Service call #16 for customer account', 0, now() - interval '29 hours'),
(5, 6, 5, 1, NULL, 'unassigned', 'low', now() + interval '6 days' + interval '8 hours', now() + interval '6 days' + interval '10 hours', 'Service call #17 for customer account', 0, now() - interval '28 hours'),
(6, 8, 6, 2, 10, 'assigned', 'urgent', now() + interval '7 days' + interval '9 hours', now() + interval '7 days' + interval '11 hours', 'Service call #18 for customer account', 0, now() - interval '27 hours'),
(7, 9, 1, 3, 3, 'en_route', 'high', now() + interval '8 days' + interval '10 hours', now() + interval '8 days' + interval '12 hours', 'Service call #19 for customer account', 1, now() - interval '26 hours'),
(8, 10, 2, 4, 8, 'arrived', 'normal', now() + interval '9 days' + interval '11 hours', now() + interval '9 days' + interval '13 hours', 'Service call #20 for customer account', 0, now() - interval '25 hours'),
(9, 11, 3, 1, 9, 'in_progress', 'low', now() + interval '-10 days' + interval '12 hours', now() + interval '-10 days' + interval '14 hours', 'Service call #21 for customer account', 0, now() - interval '24 hours'),
(10, 13, 4, 2, 2, 'completed', 'urgent', now() + interval '-9 days' + interval '13 hours', now() + interval '-9 days' + interval '15 hours', 'Service call #22 for customer account', 0, now() - interval '23 hours'),
(11, 14, 5, 3, 3, 'cancelled', 'high', now() + interval '-8 days' + interval '14 hours', now() + interval '-8 days' + interval '16 hours', 'Service call #23 for customer account', 0, now() - interval '22 hours'),
(12, 16, 6, 4, 8, 'failed', 'normal', now() + interval '-7 days' + interval '15 hours', now() + interval '-7 days' + interval '17 hours', 'Service call #24 for customer account', 0, now() - interval '21 hours'),
(1, 1, 1, 1, NULL, 'unassigned', 'low', now() + interval '-6 days' + interval '8 hours', now() + interval '-6 days' + interval '10 hours', 'Service call #25 for customer account', 0, now() - interval '20 hours'),
(2, 2, 2, 2, 6, 'assigned', 'urgent', now() + interval '-5 days' + interval '9 hours', now() + interval '-5 days' + interval '11 hours', 'Service call #26 for customer account', 0, now() - interval '19 hours'),
(3, 3, 3, 3, 3, 'en_route', 'high', now() + interval '-4 days' + interval '10 hours', now() + interval '-4 days' + interval '12 hours', 'Service call #27 for customer account', 0, now() - interval '18 hours'),
(4, 5, 4, 4, 8, 'arrived', 'normal', now() + interval '-3 days' + interval '11 hours', now() + interval '-3 days' + interval '13 hours', 'Service call #28 for customer account', 1, now() - interval '17 hours'),
(5, 6, 5, 1, 5, 'in_progress', 'low', now() + interval '-2 days' + interval '12 hours', now() + interval '-2 days' + interval '14 hours', 'Service call #29 for customer account', 0, now() - interval '16 hours'),
(6, 8, 6, 2, 10, 'completed', 'urgent', now() + interval '-1 days' + interval '13 hours', now() + interval '-1 days' + interval '15 hours', 'Service call #30 for customer account', 0, now() - interval '15 hours'),
(7, 9, 1, 3, 3, 'cancelled', 'high', now() + interval '0 days' + interval '14 hours', now() + interval '0 days' + interval '16 hours', 'Service call #31 for customer account', 0, now() - interval '14 hours'),
(8, 10, 2, 4, 8, 'failed', 'normal', now() + interval '1 days' + interval '15 hours', now() + interval '1 days' + interval '17 hours', 'Service call #32 for customer account', 0, now() - interval '13 hours'),
(9, 11, 3, 1, NULL, 'unassigned', 'low', now() + interval '2 days' + interval '8 hours', now() + interval '2 days' + interval '10 hours', 'Service call #33 for customer account', 0, now() - interval '12 hours'),
(10, 13, 4, 2, 2, 'assigned', 'urgent', now() + interval '3 days' + interval '9 hours', now() + interval '3 days' + interval '11 hours', 'Service call #34 for customer account', 0, now() - interval '11 hours'),
(11, 14, 5, 3, 3, 'en_route', 'high', now() + interval '4 days' + interval '10 hours', now() + interval '4 days' + interval '12 hours', 'Service call #35 for customer account', 0, now() - interval '10 hours'),
(12, 16, 6, 4, 8, 'arrived', 'normal', now() + interval '5 days' + interval '11 hours', now() + interval '5 days' + interval '13 hours', 'Service call #36 for customer account', 0, now() - interval '9 hours'),
(1, 1, 1, 1, 1, 'in_progress', 'low', now() + interval '6 days' + interval '12 hours', now() + interval '6 days' + interval '14 hours', 'Service call #37 for customer account', 1, now() - interval '8 hours'),
(2, 2, 2, 2, 6, 'completed', 'urgent', now() + interval '7 days' + interval '13 hours', now() + interval '7 days' + interval '15 hours', 'Service call #38 for customer account', 0, now() - interval '7 hours'),
(3, 3, 3, 3, 3, 'cancelled', 'high', now() + interval '8 days' + interval '14 hours', now() + interval '8 days' + interval '16 hours', 'Service call #39 for customer account', 0, now() - interval '6 hours'),
(4, 5, 4, 4, 8, 'failed', 'normal', now() + interval '9 days' + interval '15 hours', now() + interval '9 days' + interval '17 hours', 'Service call #40 for customer account', 0, now() - interval '5 hours'),
(5, 6, 5, 1, NULL, 'unassigned', 'low', now() + interval '-10 days' + interval '8 hours', now() + interval '-10 days' + interval '10 hours', 'Service call #41 for customer account', 0, now() - interval '4 hours'),
(6, 8, 6, 2, 10, 'assigned', 'urgent', now() + interval '-9 days' + interval '9 hours', now() + interval '-9 days' + interval '11 hours', 'Service call #42 for customer account', 0, now() - interval '3 hours'),
(7, 9, 1, 3, 3, 'en_route', 'high', now() + interval '-8 days' + interval '10 hours', now() + interval '-8 days' + interval '12 hours', 'Service call #43 for customer account', 0, now() - interval '2 hours'),
(8, 10, 2, 4, 8, 'arrived', 'normal', now() + interval '-7 days' + interval '11 hours', now() + interval '-7 days' + interval '13 hours', 'Service call #44 for customer account', 0, now() - interval '1 hours');

INSERT INTO routes (worker_id, region_id, route_date) VALUES
(1, 1, current_date),
(2, 2, current_date),
(3, 3, current_date),
(4, 4, current_date);

INSERT INTO route_stops (route_id, job_id, sequence) VALUES
(1, 13, 1),
(1, 37, 2),
(2, 10, 1),
(2, 22, 2),
(2, 34, 3),
(3, 3, 1),
(3, 7, 2),
(3, 11, 3),
(3, 15, 4);

INSERT INTO proofs (job_id, uploaded_by, category, visibility, stored_filename, original_filename, content_type, size_bytes) VALUES
(4, 13, 'SIGNATURE', 'CUSTOMER_VISIBLE', 'seed-proof-1.txt', 'proof_signature_1.txt', 'text/plain', 200),
(5, 10, 'PHOTO', 'CUSTOMER_VISIBLE', 'seed-proof-2.txt', 'proof_photo_2.txt', 'text/plain', 217),
(6, 15, 'FORM', 'CUSTOMER_VISIBLE', 'seed-proof-3.txt', 'proof_form_3.txt', 'text/plain', 234),
(8, 13, 'COMPLETION_DOC', 'INTERNAL', 'seed-proof-4.txt', 'proof_completion_doc_4.txt', 'text/plain', 251),
(12, 13, 'SIGNATURE', 'CUSTOMER_VISIBLE', 'seed-proof-5.txt', 'proof_signature_5.txt', 'text/plain', 268),
(13, 6, 'PHOTO', 'CUSTOMER_VISIBLE', 'seed-proof-6.txt', 'proof_photo_6.txt', 'text/plain', 285),
(14, 11, 'FORM', 'CUSTOMER_VISIBLE', 'seed-proof-7.txt', 'proof_form_7.txt', 'text/plain', 302),
(16, 13, 'COMPLETION_DOC', 'INTERNAL', 'seed-proof-8.txt', 'proof_completion_doc_8.txt', 'text/plain', 319),
(20, 13, 'SIGNATURE', 'CUSTOMER_VISIBLE', 'seed-proof-9.txt', 'proof_signature_9.txt', 'text/plain', 336),
(21, 14, 'PHOTO', 'CUSTOMER_VISIBLE', 'seed-proof-10.txt', 'proof_photo_10.txt', 'text/plain', 353),
(22, 7, 'FORM', 'CUSTOMER_VISIBLE', 'seed-proof-11.txt', 'proof_form_11.txt', 'text/plain', 370),
(24, 13, 'COMPLETION_DOC', 'INTERNAL', 'seed-proof-12.txt', 'proof_completion_doc_12.txt', 'text/plain', 387),
(28, 13, 'SIGNATURE', 'CUSTOMER_VISIBLE', 'seed-proof-13.txt', 'proof_signature_13.txt', 'text/plain', 404),
(29, 10, 'PHOTO', 'CUSTOMER_VISIBLE', 'seed-proof-14.txt', 'proof_photo_14.txt', 'text/plain', 421);

INSERT INTO incidents (job_id, reported_by, assigned_to, severity, status, internal_notes, customer_summary, resolved_at) VALUES
(1, 6, 2, 'low', 'open', 'Internal triage notes: verified access, coordinating with worker.', 'We are aware of an issue with this job and are working to resolve it.', NULL),
(6, 15, 3, 'medium', 'triaged', 'Internal triage notes: verified access, coordinating with worker.', 'We are aware of an issue with this job and are working to resolve it.', NULL),
(11, 8, 4, 'high', 'assigned', 'Internal triage notes: verified access, coordinating with worker.', 'We are aware of an issue with this job and are working to resolve it.', NULL),
(16, 13, 5, 'critical', 'resolved', 'Internal triage notes: verified access, coordinating with worker.', 'We are aware of an issue with this job and are working to resolve it.', now() - interval '1 day'),
(21, 14, 2, 'low', 'closed', 'Internal triage notes: verified access, coordinating with worker.', 'We are aware of an issue with this job and are working to resolve it.', now() - interval '1 day'),
(26, 11, 3, 'medium', 'open', 'Internal triage notes: verified access, coordinating with worker.', 'We are aware of an issue with this job and are working to resolve it.', NULL),
(31, 8, 4, 'high', 'triaged', 'Internal triage notes: verified access, coordinating with worker.', 'We are aware of an issue with this job and are working to resolve it.', NULL),
(36, 13, 5, 'critical', 'assigned', 'Internal triage notes: verified access, coordinating with worker.', 'We are aware of an issue with this job and are working to resolve it.', NULL),
(41, 6, 2, 'low', 'resolved', 'Internal triage notes: verified access, coordinating with worker.', 'We are aware of an issue with this job and are working to resolve it.', now() - interval '1 day');

INSERT INTO job_messages (job_id, sender_id, sender_role, body) VALUES
(1, 16, 'CUSTOMER', 'Hi, could you confirm the arrival window for this job?'),
(1, 2, 'DISPATCHER', 'Confirmed — worker is scheduled within the stated window.'),
(2, 17, 'CUSTOMER', 'Hi, could you confirm the arrival window for this job?'),
(2, 3, 'DISPATCHER', 'Confirmed — worker is scheduled within the stated window.'),
(3, 18, 'CUSTOMER', 'Hi, could you confirm the arrival window for this job?'),
(3, 4, 'DISPATCHER', 'Confirmed — worker is scheduled within the stated window.'),
(4, 19, 'CUSTOMER', 'Hi, could you confirm the arrival window for this job?'),
(4, 5, 'DISPATCHER', 'Confirmed — worker is scheduled within the stated window.'),
(5, 20, 'CUSTOMER', 'Hi, could you confirm the arrival window for this job?'),
(5, 2, 'DISPATCHER', 'Confirmed — worker is scheduled within the stated window.'),
(6, 21, 'CUSTOMER', 'Hi, could you confirm the arrival window for this job?'),
(6, 3, 'DISPATCHER', 'Confirmed — worker is scheduled within the stated window.'),
(7, 22, 'CUSTOMER', 'Hi, could you confirm the arrival window for this job?'),
(7, 4, 'DISPATCHER', 'Confirmed — worker is scheduled within the stated window.'),
(8, 23, 'CUSTOMER', 'Hi, could you confirm the arrival window for this job?'),
(8, 5, 'DISPATCHER', 'Confirmed — worker is scheduled within the stated window.'),
(9, 24, 'CUSTOMER', 'Hi, could you confirm the arrival window for this job?'),
(9, 2, 'DISPATCHER', 'Confirmed — worker is scheduled within the stated window.'),
(10, 25, 'CUSTOMER', 'Hi, could you confirm the arrival window for this job?'),
(10, 3, 'DISPATCHER', 'Confirmed — worker is scheduled within the stated window.'),
(11, 26, 'CUSTOMER', 'Hi, could you confirm the arrival window for this job?'),
(11, 4, 'DISPATCHER', 'Confirmed — worker is scheduled within the stated window.'),
(12, 27, 'CUSTOMER', 'Hi, could you confirm the arrival window for this job?'),
(12, 5, 'DISPATCHER', 'Confirmed — worker is scheduled within the stated window.'),
(13, 16, 'CUSTOMER', 'Hi, could you confirm the arrival window for this job?'),
(13, 2, 'DISPATCHER', 'Confirmed — worker is scheduled within the stated window.'),
(14, 17, 'CUSTOMER', 'Hi, could you confirm the arrival window for this job?'),
(14, 3, 'DISPATCHER', 'Confirmed — worker is scheduled within the stated window.'),
(15, 18, 'CUSTOMER', 'Hi, could you confirm the arrival window for this job?'),
(15, 4, 'DISPATCHER', 'Confirmed — worker is scheduled within the stated window.'),
(16, 19, 'CUSTOMER', 'Hi, could you confirm the arrival window for this job?'),
(16, 5, 'DISPATCHER', 'Confirmed — worker is scheduled within the stated window.');

INSERT INTO notifications (user_id, title, body, is_read) VALUES
(6, 'New job assigned', 'You have a new job on your schedule.', TRUE),
(7, 'New job assigned', 'You have a new job on your schedule.', FALSE),
(8, 'New job assigned', 'You have a new job on your schedule.', TRUE),
(9, 'New job assigned', 'You have a new job on your schedule.', FALSE),
(10, 'New job assigned', 'You have a new job on your schedule.', TRUE),
(11, 'New job assigned', 'You have a new job on your schedule.', FALSE),
(12, 'New job assigned', 'You have a new job on your schedule.', TRUE),
(13, 'New job assigned', 'You have a new job on your schedule.', FALSE),
(14, 'New job assigned', 'You have a new job on your schedule.', TRUE),
(15, 'New job assigned', 'You have a new job on your schedule.', FALSE),
(16, 'Job status updated', 'Your service job status has changed.', FALSE),
(17, 'Job status updated', 'Your service job status has changed.', FALSE),
(18, 'Job status updated', 'Your service job status has changed.', FALSE),
(19, 'Job status updated', 'Your service job status has changed.', FALSE),
(20, 'Job status updated', 'Your service job status has changed.', FALSE),
(21, 'Job status updated', 'Your service job status has changed.', FALSE);

INSERT INTO webhooks (name, target_url, secret, active, created_by) VALUES
('Job Status Sync', 'http://localhost:8080/api/mock/webhook-sink', 'whsec_seed_local_demo_secret', TRUE, 1);

INSERT INTO audit_log (actor_id, actor_email, action, entity_type, entity_id, before_value, after_value, authorization_basis) VALUES
(1, 'admin@fielddispatch.io', 'SEED_INIT', 'SYSTEM', NULL, NULL, 'Database seeded with demo data', 'system-bootstrap');
