create table deferred_file_body_info(
  sha_256 varchar(64),
  size bigint not null,
  path varchar(240) not null
);

create table deferred_file_info(
  id bigint,
  filename varchar(240) not null,
  upload_time timestamp not null,
  content_type varchar(100) not null,
  sha_256 varchar(64) not null
);

create table deferred_request(
  username varchar(50) not null,
  priority integer not null,
  request_time timestamp not null,
  status varchar(5) not null check (status in ('OK', 'ERR', 'QUEUE', 'EXE', 'DEL')),
  topic varchar(50) not null,
  request_hash varchar(100),
  request bytea not null,
  response_time timestamp,
  response_headers bytea,
  response_entity_file_id bigint,
  response_entity_file_sha_256 varchar(64)
);

create table file_body_info(
  sha_256 varchar(64),
  size bigint not null,
  path varchar(240) not null
);

create table file_info(
  id bigint,
  filename varchar(240) not null,
  upload_time timestamp not null,
  content_type varchar(100) not null,
  sha_256 varchar(64) not null
);

create table person(
  code varchar(12),
  name varchar(80),
  surname varchar(80),
  email varchar(80)
);

create table user(
  id numeric(12),
  name varchar(80),
  password varchar(90)
);

create table user_role(
  user_id numeric(12),
  role text
);

create table validation(
  id bigint,
  context varchar(100) not null,
  expression varchar(500) not null,
  message varchar(500) not null
);
comment on column validation.context is 'Context - view name';
comment on column validation.expression is 'Expression - javascript logical expression';
comment on column validation.message is 'Message - error message, if expression is false';

alter table deferred_file_body_info add constraint pk_deferred_file_body_info primary key (sha_256);

alter table deferred_file_info add constraint pk_deferred_file_info primary key (id);

alter table deferred_request add constraint pk_deferred_request primary key (request_hash);
create index idx_deferred_request_priority_request_time on deferred_request(priority, request_time);
create index idx_deferred_request_username on deferred_request(username);

alter table file_body_info add constraint pk_file_body_info primary key (sha_256);

alter table file_info add constraint pk_file_info primary key (id);

alter table person add constraint pk_person primary key (code);

alter table user add constraint pk_user primary key (id);

alter table user_role add constraint pk_user_role primary key (user_id, role);

alter table validation add constraint pk_validation primary key (id);

alter table deferred_file_info add constraint fk_deferred_file_info_sha_256 foreign key (sha_256) references deferred_file_body_info(sha_256);
alter table file_info add constraint fk_file_info_sha_256 foreign key (sha_256) references file_body_info(sha_256);
alter table user_role add constraint fk_user_role_user_id foreign key (user_id) references user(id);
