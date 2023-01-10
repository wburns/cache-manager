create schema gingersnap;
create table gingersnap.customer (id int primary key, fullname varchar(255), email varchar(255));
create table gingersnap.car_model (id int primary key, model varchar(255), brand varchar(255));
create table gingersnap.airline (id int primary key, iata char(2), name varchar(32));
create table gingersnap.gate (id int primary key, name varchar(4));

create table gingersnap.flight (
    id int primary key,
    name varchar(6),
    scheduled_time time,
    airline_id int,
    gate_id int
);

alter table gingersnap.flight add foreign key (airline_id) references gingersnap.airline(id);
alter table gingersnap.flight add foreign key (gate_id) references gingersnap.gate(id);