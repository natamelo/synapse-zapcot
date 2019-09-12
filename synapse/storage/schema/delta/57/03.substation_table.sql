/*
 *  ZapCot - A table is a group of substations.
 */

CREATE TABLE substations_table (code TEXT PRIMARY KEY, name TEXT NOT NULL, company_code TEXT NOT NULL);

CREATE TABLE substation_table (table_code TEXT, substation_code TEXT, FOREIGN KEY(table_code) REFERENCES substations_table(code), FOREIGN KEY(substation_code) REFERENCES substation(code), PRIMARY KEY (table_code, substation_code));

CREATE TABLE user_substation_table (user_id TEXT NOT NULL, table_code TEXT NOT NULL, PRIMARY KEY (user_id, table_code));


