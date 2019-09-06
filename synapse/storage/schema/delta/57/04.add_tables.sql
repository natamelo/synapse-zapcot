/*
 *  ZapCot - A table is a group of substations.
 */

CREATE TABLE substations_table (code TEXT PRIMARY KEY, name TEXT NOT NULL, company_code TEXT NOT NULL);

CREATE TABLE substation_table (substation_code TEXT, table_code TEXT, PRIMARY KEY (substation_code, table_code), FOREIGN KEY(substation_code) REFERENCES substation(code), FOREIGN KEY(table_code) REFERENCES substations_table(code));

CREATE TABLE user_substation_table (user_id TEXT, table_code TEXT, PRIMARY KEY (user_id, table_code), FOREIGN KEY(user_id) REFERENCES users(user_id), FOREIGN KEY(table_code) REFERENCES substations_table(code));

INSERT INTO substations_table (code, name, company_code) VALUES ('A2', 'Mesa de PIR e MIR', 'CTEEP');
INSERT INTO substations_table (code, name, company_code) VALUES ('A3', 'Mesa de ATI, MOS e SAL', 'CTEEP');

INSERT INTO substation_table (substation_code, table_code) VALUES ('A2', 'PIR');
INSERT INTO substation_table (substation_code, table_code) VALUES ('A2', 'MIR');

INSERT INTO substation_table (substation_code, table_code) VALUES ('A3', 'ATI');
INSERT INTO substation_table (substation_code, table_code) VALUES ('A3', 'MOS');
INSERT INTO substation_table (substation_code, table_code) VALUES ('A3', 'SAL');

