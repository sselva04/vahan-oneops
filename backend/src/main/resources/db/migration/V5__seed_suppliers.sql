-- Vahan's supplier list, taken from the SUPPLIER DETAILS sheet of the Child Parts Machining Tracker.
-- One row per supplier; each process a supplier does is a row in supplier_process with its own contact.
-- Process names are matched to stages: 'Milling' -> Conventional Milling, 'HT' -> Heat Treatment, and so on.
-- Written so it can never fail on a database where someone already typed one of the new stage names.

-- Stages that suppliers do and that are not in the list yet
INSERT INTO stage_master (name) SELECT 'Balancing' FROM (SELECT 1 AS one) t WHERE NOT EXISTS (SELECT 1 FROM stage_master WHERE LOWER(name) = LOWER('Balancing'));
INSERT INTO stage_master (name) SELECT 'Blackening and Engraving' FROM (SELECT 1 AS one) t WHERE NOT EXISTS (SELECT 1 FROM stage_master WHERE LOWER(name) = LOWER('Blackening and Engraving'));
INSERT INTO stage_master (name) SELECT 'CMM Inspection' FROM (SELECT 1 AS one) t WHERE NOT EXISTS (SELECT 1 FROM stage_master WHERE LOWER(name) = LOWER('CMM Inspection'));
INSERT INTO stage_master (name) SELECT 'Hot Chroming' FROM (SELECT 1 AS one) t WHERE NOT EXISTS (SELECT 1 FROM stage_master WHERE LOWER(name) = LOWER('Hot Chroming'));
INSERT INTO stage_master (name) SELECT 'Laser Cutting' FROM (SELECT 1 AS one) t WHERE NOT EXISTS (SELECT 1 FROM stage_master WHERE LOWER(name) = LOWER('Laser Cutting'));
INSERT INTO stage_master (name) SELECT 'Sheet Metal Folding' FROM (SELECT 1 AS one) t WHERE NOT EXISTS (SELECT 1 FROM stage_master WHERE LOWER(name) = LOWER('Sheet Metal Folding'));
INSERT INTO stage_master (name) SELECT 'Tool Machining' FROM (SELECT 1 AS one) t WHERE NOT EXISTS (SELECT 1 FROM stage_master WHERE LOWER(name) = LOWER('Tool Machining'));
INSERT INTO stage_master (name) SELECT 'Tool and Cutter' FROM (SELECT 1 AS one) t WHERE NOT EXISTS (SELECT 1 FROM stage_master WHERE LOWER(name) = LOWER('Tool and Cutter'));

-- SREE RAMAJEYAM ENGINEERING
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('SREE RAMAJEYAM ENGINEERING', 'Kamaraj Nagar', NULL, TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Marimuthu', '9788615373' FROM supplier s, stage_master m WHERE s.name = 'SREE RAMAJEYAM ENGINEERING' AND LOWER(m.name) = LOWER('Conventional Milling');
-- V.N.R INDUSTRIES
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('V.N.R INDUSTRIES', '1 st Sipcot, Rajsriya Layout', 'https://maps.app.goo.gl/XaP49CM45dB83eDKA', TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Manjunath', '9994576781' FROM supplier s, stage_master m WHERE s.name = 'V.N.R INDUSTRIES' AND LOWER(m.name) = LOWER('Conventional Milling');
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Manjunath', '9994576781' FROM supplier s, stage_master m WHERE s.name = 'V.N.R INDUSTRIES' AND LOWER(m.name) = LOWER('Surface Grinding');
-- SIVA SAKTHI ENGINEERING
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('SIVA SAKTHI ENGINEERING', 'MK Palli', NULL, TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Chandra Sekar', '9842052680' FROM supplier s, stage_master m WHERE s.name = 'SIVA SAKTHI ENGINEERING' AND LOWER(m.name) = LOWER('Turning');
-- PRECISION TECHNOLOGY
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('PRECISION TECHNOLOGY', '1 st Sipcot, Rajsriya Layout', 'https://maps.app.goo.gl/PCZyz6iXDXRp36KR6', TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Bharat', '7339397015' FROM supplier s, stage_master m WHERE s.name = 'PRECISION TECHNOLOGY' AND LOWER(m.name) = LOWER('CNC Turning');
-- V.J ENGINEERING
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('V.J ENGINEERING', 'MK Palli', NULL, TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Harish', '9047915956' FROM supplier s, stage_master m WHERE s.name = 'V.J ENGINEERING' AND LOWER(m.name) = LOWER('Turning');
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Vengatesh', '9751576336' FROM supplier s, stage_master m WHERE s.name = 'V.J ENGINEERING' AND LOWER(m.name) = LOWER('Drilling');
-- R.V. TOOLING
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('R.V. TOOLING', 'Kamaraj Nagar', NULL, TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Kalaiyarasan', '8778629407' FROM supplier s, stage_master m WHERE s.name = 'R.V. TOOLING' AND LOWER(m.name) = LOWER('Cylindrical Grinding');
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Raj Kumar', '9944921915' FROM supplier s, stage_master m WHERE s.name = 'R.V. TOOLING' AND LOWER(m.name) = LOWER('Surface Grinding');
-- TECHNO FIELD ENGINEERING
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('TECHNO FIELD ENGINEERING', 'MK Palli', NULL, TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Pasupathi', '8608745366' FROM supplier s, stage_master m WHERE s.name = 'TECHNO FIELD ENGINEERING' AND LOWER(m.name) = LOWER('Drilling');
-- REFINE TOOLING
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('REFINE TOOLING', 'Zuzuvadi', 'https://maps.app.goo.gl/RvvqvTgXZ8km1UnaA', TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Mani Vannan', '9942315810' FROM supplier s, stage_master m WHERE s.name = 'REFINE TOOLING' AND LOWER(m.name) = LOWER('Cylindrical Grinding');
-- SRI MURUGAVEL PRICISION TOOLING
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('SRI MURUGAVEL PRICISION TOOLING', '1 st Sipcot, Rajsriya Layout', 'https://maps.app.goo.gl/raBhbjCmJpdc1S719', TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Aravind', '9025393424' FROM supplier s, stage_master m WHERE s.name = 'SRI MURUGAVEL PRICISION TOOLING' AND LOWER(m.name) = LOWER('Surface Grinding');
-- RED STAR HEAT TREATMENT
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('RED STAR HEAT TREATMENT', '1 st Sipcot, Rajsriya Layout', 'https://maps.app.goo.gl/k3FHACUpcJWcwKmi7', TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Kathrivel', '7373734345' FROM supplier s, stage_master m WHERE s.name = 'RED STAR HEAT TREATMENT' AND LOWER(m.name) = LOWER('Heat Treatment');
-- TROOP
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('TROOP', '1 st Sipcot, Rajsriya Layout', NULL, TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Vignesh', '8124245671' FROM supplier s, stage_master m WHERE s.name = 'TROOP' AND LOWER(m.name) = LOWER('Wire Cut');
-- HARISHA INDUSTRY
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('HARISHA INDUSTRY', 'Kamaraj Nagar', NULL, TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Loganathan', '9600983843' FROM supplier s, stage_master m WHERE s.name = 'HARISHA INDUSTRY' AND LOWER(m.name) = LOWER('Cylindrical Grinding');
-- BALAJI INDUSTRIES
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('BALAJI INDUSTRIES', '1 st Sipcot, Rajsriya Layout', 'https://maps.app.goo.gl/LPYGv3RhucFs1Gdw8', TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Balaji, Navaneethan', '9944363952, 6374278193' FROM supplier s, stage_master m WHERE s.name = 'BALAJI INDUSTRIES' AND LOWER(m.name) = LOWER('VMC');
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Balaji, Navaneethan', '9944363952, 6374278193' FROM supplier s, stage_master m WHERE s.name = 'BALAJI INDUSTRIES' AND LOWER(m.name) = LOWER('Drilling');
-- ROBO PRECISION TECH
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('ROBO PRECISION TECH', '1 st Sipcot, Rajsriya Layout', 'https://maps.app.goo.gl/mNMqehynatyDmVzm6', TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Anand', '8056267340' FROM supplier s, stage_master m WHERE s.name = 'ROBO PRECISION TECH' AND LOWER(m.name) = LOWER('Wire Cut');
-- GC MILLING
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('GC MILLING', '1 st Sipcot, Rajsriya Layout', NULL, TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Paramasivan', '9487657119' FROM supplier s, stage_master m WHERE s.name = 'GC MILLING' AND LOWER(m.name) = LOWER('Drilling');
-- UNIQUE MEASUREMENT SERVICE
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('UNIQUE MEASUREMENT SERVICE', 'Hosur', NULL, TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Vengatesh', '9940027791' FROM supplier s, stage_master m WHERE s.name = 'UNIQUE MEASUREMENT SERVICE' AND LOWER(m.name) = LOWER('CMM Inspection');
-- BALAJI BLACKENING & ENGRAVING
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('BALAJI BLACKENING & ENGRAVING', '1 st Sipcot, Rajsriya Layout', 'https://maps.app.goo.gl/VnwpPA1fJ3TACfXW6', TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Vengatesh', '8680808161' FROM supplier s, stage_master m WHERE s.name = 'BALAJI BLACKENING & ENGRAVING' AND LOWER(m.name) = LOWER('Blackening and Engraving');
-- EMM KAY TOOLS
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('EMM KAY TOOLS', 'Zuzuvadi', 'https://maps.app.goo.gl/4CJYis2U2rJ2nivE6', TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Sanjeev', '9597029801' FROM supplier s, stage_master m WHERE s.name = 'EMM KAY TOOLS' AND LOWER(m.name) = LOWER('VMC');
-- VGP ENGINEERING WORKS
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('VGP ENGINEERING WORKS', 'MK Palli', NULL, TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Venu Gopal', NULL FROM supplier s, stage_master m WHERE s.name = 'VGP ENGINEERING WORKS' AND LOWER(m.name) = LOWER('Balancing');
-- DINESH ENGINEERING
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('DINESH ENGINEERING', '1 st Sipcot, Rajsriya Layout', NULL, TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Dinesh', '9952657920' FROM supplier s, stage_master m WHERE s.name = 'DINESH ENGINEERING' AND LOWER(m.name) = LOWER('Cylindrical Grinding');
-- VINOTH - CNC
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('VINOTH - CNC', '1 st Sipcot, Rajsriya Layout', 'https://maps.app.goo.gl/9UHpD4PDvxt27StDA', TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Vinoth', '8695176049' FROM supplier s, stage_master m WHERE s.name = 'VINOTH - CNC' AND LOWER(m.name) = LOWER('CNC Turning');
-- SSPT WIRECUT EDM
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('SSPT WIRECUT EDM', '1 st Sipcot, Rajsriya Layout', NULL, TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Siva', '7708796283' FROM supplier s, stage_master m WHERE s.name = 'SSPT WIRECUT EDM' AND LOWER(m.name) = LOWER('Wire Cut');
-- RMS HEAT TREATMENT
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('RMS HEAT TREATMENT', '1 st Sipcot, Rajsriya Layout', NULL, TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, NULL, NULL FROM supplier s, stage_master m WHERE s.name = 'RMS HEAT TREATMENT' AND LOWER(m.name) = LOWER('Heat Treatment');
-- LAKSHMI VACCUM HEAT TREATMENT
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('LAKSHMI VACCUM HEAT TREATMENT', '1 st Sipcot, Rajsriya Layout', 'https://maps.app.goo.gl/WggpuVYHkGmQaFFHA', TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, NULL, '8220003856' FROM supplier s, stage_master m WHERE s.name = 'LAKSHMI VACCUM HEAT TREATMENT' AND LOWER(m.name) = LOWER('Heat Treatment');
-- TOOL & CUTTER
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('TOOL & CUTTER', '1 st Sipcot, Rajsriya Layout', NULL, TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Saravanan', '9566425848' FROM supplier s, stage_master m WHERE s.name = 'TOOL & CUTTER' AND LOWER(m.name) = LOWER('Tool and Cutter');
-- HI TECH LASER CUTTING
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('HI TECH LASER CUTTING', '1 st Sipcot, Rajsriya Layout', NULL, TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, NULL, '9500968993' FROM supplier s, stage_master m WHERE s.name = 'HI TECH LASER CUTTING' AND LOWER(m.name) = LOWER('Laser Cutting');
-- AM TECH
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('AM TECH', 'Kamaraj Nagar', NULL, TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Prakash', '9894041912' FROM supplier s, stage_master m WHERE s.name = 'AM TECH' AND LOWER(m.name) = LOWER('Sheet Metal Folding');
-- GANESH CG
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('GANESH CG', 'Kamaraj Nagar', NULL, TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Ganesh', '9345765667, 9043423576' FROM supplier s, stage_master m WHERE s.name = 'GANESH CG' AND LOWER(m.name) = LOWER('Cylindrical Grinding');
-- CARB TOOLS
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('CARB TOOLS', '1 st Sipcot, Rajsriya Layout', NULL, TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Shiva Ram', '8220606368' FROM supplier s, stage_master m WHERE s.name = 'CARB TOOLS' AND LOWER(m.name) = LOWER('Tool Machining');
-- ACCURATE TOOLS
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('ACCURATE TOOLS', '1 st Sipcot, Rajsriya Layout', NULL, TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Santhosh', '9042652640' FROM supplier s, stage_master m WHERE s.name = 'ACCURATE TOOLS' AND LOWER(m.name) = LOWER('Conventional Milling');
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Santhosh', '9042652640' FROM supplier s, stage_master m WHERE s.name = 'ACCURATE TOOLS' AND LOWER(m.name) = LOWER('Turning');
-- ULTINO TECH CNC
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('ULTINO TECH CNC', '4th crocc Rajaji layout ,hosur-635126', 'https://maps.app.goo.gl/Pg64mB1Wk5PuF4M57', TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Kannan', '9342168460, 9884918131' FROM supplier s, stage_master m WHERE s.name = 'ULTINO TECH CNC' AND LOWER(m.name) = LOWER('Wire Cut');
-- ISWARIYA WIRETECH TOOLS
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('ISWARIYA WIRETECH TOOLS', 'Zuzuvadi', 'https://maps.app.goo.gl/7qZ7YcoEs1RCyzA29', TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Arendiran', '7538822465, 9600413601' FROM supplier s, stage_master m WHERE s.name = 'ISWARIYA WIRETECH TOOLS' AND LOWER(m.name) = LOWER('Wire Cut');
-- MADHA ENGINEERING
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('MADHA ENGINEERING', 'Zuzuvadi', 'https://maps.app.goo.gl/pvuFidMpwrLxbFcG9', TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Manickam', '7639369837' FROM supplier s, stage_master m WHERE s.name = 'MADHA ENGINEERING' AND LOWER(m.name) = LOWER('Drilling');
-- SREE RAM PRICISION TOOLS
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('SREE RAM PRICISION TOOLS', 'Zuzuvadi', 'https://maps.app.goo.gl/pvuFidMpwrLxbFcG9', TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Ranjith', NULL FROM supplier s, stage_master m WHERE s.name = 'SREE RAM PRICISION TOOLS' AND LOWER(m.name) = LOWER('Wire Cut');
-- M2 TOOLING
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('M2 TOOLING', '1 st Sipcot, Rajsriya Layout', 'https://maps.app.goo.gl/NZKQVWjJoiQAp9Hs9', TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Yuvaraj', '9597758126' FROM supplier s, stage_master m WHERE s.name = 'M2 TOOLING' AND LOWER(m.name) = LOWER('Drilling');
-- FAZ TOOLING
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('FAZ TOOLING', '1 st Sipcot, Rajsriya Layout', 'https://maps.app.goo.gl/f1S1MwJa5vpH2qT88', TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, 'Hussain', '9629727467' FROM supplier s, stage_master m WHERE s.name = 'FAZ TOOLING' AND LOWER(m.name) = LOWER('Brazing');
-- Sark Enterprises
INSERT INTO supplier (name, address, map_url, active, created_at) VALUES ('Sark Enterprises', '1 st Sipcot, Rajsriya Layout', 'https://maps.app.goo.gl/2pk14xycTBcPFpje6', TRUE, CURRENT_TIMESTAMP);
INSERT INTO supplier_process (supplier_id, stage_master_id, contact_person, contact_phone) SELECT s.id, m.id, NULL, '9655751146' FROM supplier s, stage_master m WHERE s.name = 'Sark Enterprises' AND LOWER(m.name) = LOWER('Hot Chroming');
