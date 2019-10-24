/*
 *  ZapCot - New tables for voltage control solicitations.
 */

CREATE TABLE substation ( code TEXT PRIMARY KEY, name TEXT NOT NULL, company_code TEXT NOT NULL);

CREATE TABLE voltage_control_solicitation ( id INTEGER PRIMARY KEY, action_code TEXT, equipment_code TEXT, substation_code TEXT, staggered BOOLEAN, amount TEXT, voltage TEXT, FOREIGN KEY(substation_code) REFERENCES substation(code));

CREATE TABLE solicitation_status_signature (user_id TEXT, status TEXT, time_stamp TEXT, solicitation_id INTEGER, FOREIGN KEY(solicitation_id) REFERENCES voltage_control_solicitation(id));

CREATE TABLE solicitation_updates (stream_id BIGINT NOT NULL, solicitation_id INTEGER, user_id TEXT NOT NULL, type TEXT NOT NULL, content TEXT NOT NULL);

CREATE TABLE solicitation_room (solicitation_id INTEGER, room_id TEXT, FOREIGN KEY(solicitation_id) REFERENCES voltage_control_solicitation(id), FOREIGN KEY(room_id) REFERENCES rooms(room_id));

ALTER TABLE users ADD company_code TEXT;