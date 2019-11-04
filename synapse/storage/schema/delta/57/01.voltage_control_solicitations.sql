/*
 *  ZapCot - New tables for voltage control solicitations.
 */

CREATE TABLE substation ( code TEXT PRIMARY KEY, name TEXT NOT NULL, company_code TEXT NOT NULL);

CREATE TABLE solicitation_group ( id INTEGER PRIMARY KEY, creation_time_total TEXT);

CREATE TABLE voltage_control_solicitation ( id INTEGER PRIMARY KEY, group_id INTEGER, action_code TEXT, equipment_code TEXT, substation_code TEXT, staggered BOOLEAN, amount TEXT, voltage TEXT, room_id TEXT, FOREIGN KEY(substation_code) REFERENCES substation(code), FOREIGN KEY(group_id) REFERENCES solicitation_group(id), FOREIGN KEY(room_id) REFERENCES rooms(room_id));

CREATE TABLE solicitation_status_signature ( id INTEGER PRIMARY KEY, user_id TEXT, status TEXT, time_stamp TEXT, solicitation_id INTEGER, FOREIGN KEY(solicitation_id) REFERENCES voltage_control_solicitation(id));

CREATE TABLE solicitation_updates ( stream_id BIGINT NOT NULL, solicitation_id INTEGER, user_id TEXT, type TEXT NOT NULL, content TEXT NOT NULL);

ALTER TABLE users ADD company_code TEXT;