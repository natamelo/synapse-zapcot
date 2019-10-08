/*
 *  ZapCot - New tables for voltage control solicitations.
 */

CREATE TABLE substation ( code TEXT PRIMARY KEY, name TEXT NOT NULL, company_code TEXT NOT NULL);

CREATE TABLE voltage_control_solicitation ( id INTEGER PRIMARY KEY, action_code TEXT, equipment_code TEXT, substation_code TEXT, staggered BOOLEAN, amount TEXT, voltage TEXT, FOREIGN KEY(substation_code) REFERENCES substation(code));

CREATE TABLE solicitation_event (user_id TEXT, status TEXT, time_stamp TEXT, voltage_control_id INTEGER, FOREIGN KEY(voltage_control_id) REFERENCES voltage_control_solicitation(id));

ALTER TABLE users ADD company_code TEXT;