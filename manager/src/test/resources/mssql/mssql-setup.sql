create database gingersnap;
use gingersnap;
create schema gingersnap;

create table gingersnap.customer(id int primary key, fullname varchar(255), email varchar(255));
create table gingersnap.car_model(id int primary key, model varchar(255), brand varchar(255));
create table gingersnap.airline(id int not null primary key, iata char(2), name varchar(32));
create table gingersnap.gate(id int not null primary key, name varchar(4));

create table gingersnap.flight(
    id int not null primary key,
    name varchar(6),
    scheduled_time time,
    airline_id int foreign key references gingersnap.airline(id),
    gate_id int foreign key references gingersnap.gate(id)
);
