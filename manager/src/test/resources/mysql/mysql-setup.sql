create table gingersnap.customer(id int not null, fullname varchar(255), email varchar(255), constraint primary key (id));
create table gingersnap.car_model(id int not null, model varchar(255), brand varchar(255), constraint primary key (id));
create table gingersnap.airline(id int not null, iata char(2), name varchar(32), constraint primary key (id));
create table gingersnap.gate(id int not null, name varchar(4), constraint primary key (id));
create table gingersnap.flight(id int not null, name varchar(6), scheduled_time time, airline_id int, gate_id int, constraint primary key (id), foreign key (airline_id) references airline(id), foreign key (gate_id) references gate (id));
