/*
 *  ZapCot - New tables for voltage control solicitations.
 */

CREATE TABLE substation ( code TEXT PRIMARY KEY, name TEXT NOT NULL, company_code TEXT NOT NULL);

CREATE TABLE voltage_control_solicitation ( id INTEGER PRIMARY KEY, action_code TEXT, equipment_code TEXT, substation_code TEXT, bar TEXT, value_ TEXT, request_user_id TEXT, creation_timestamp INTEGER, response_user_id TEXT, status TEXT, update_timestamp INTEGER, FOREIGN KEY(substation_code) REFERENCES substation(code));

CREATE TABLE solicitation_event (id INTEGER PRIMARY KEY, user_id TEXT, status TEXT, time_stamp TEXT, voltage_control_id INTEGER, FOREIGN KEY(voltage_control_id) REFERENCES voltage_control_solicitation(id));

ALTER TABLE users ADD company_code TEXT;