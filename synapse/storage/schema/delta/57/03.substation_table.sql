/*
 *  ZapCot - A table is a group of substations.
 */

CREATE TABLE substation_table (code TEXT PRIMARY KEY, name TEXT NOT NULL, company_code TEXT NOT NULL);

CREATE TABLE user_substation_table (user_id TEXT NOT NULL, table_code TEXT NOT NULL, PRIMARY KEY (user_id, table_code));


