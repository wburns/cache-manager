create table gingersnap.customer(id int not null, fullname varchar(255), email varchar(255), constraint primary key (id));
create table gingersnap.car_model(id int not null, model varchar(255), brand varchar(255), constraint primary key (id));

insert into gingersnap.customer values (1, 'Jon Doe', 'jd@example.com');
insert into gingersnap.customer values (3, 'Bob', 'bob@example.com');
insert into gingersnap.customer values (4, 'Alice', 'alice@example.com');
insert into gingersnap.customer values (5, 'Mallory', 'mallory@example.com');

insert into gingersnap.car_model values (1, 'QQ', 'Chery');
insert into gingersnap.car_model values (2, 'Beetle', 'VW');

create table gingersnap.airline(id int not null, iata char(2), name varchar(32), constraint primary key (id));
create table gingersnap.gate(id int not null, name varchar(4), constraint primary key (id));
create table gingersnap.flight(id int not null, name varchar(6), scheduled_time time, airline_id int, gate_id int, constraint primary key (id), foreign key (airline_id) references airline(id), foreign key (gate_id) references gate (id));

insert into gingersnap.airline values (1, 'BA', 'British Airways');
insert into gingersnap.airline values (2, 'AF', 'Air France');

insert into gingersnap.gate values (1, 'A1');
insert into gingersnap.gate values (2, 'A2');
insert into gingersnap.gate values (3, 'B1');

insert into gingersnap.flight values (1, 'BA1234', '08:30:00', 1, 1);
insert into gingersnap.flight values (2, 'AF5678', '09:50:00', 2, 2);
