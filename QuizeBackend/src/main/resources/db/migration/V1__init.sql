create extension if not exists pgcrypto;

create table roles (
    id uuid primary key default gen_random_uuid(),
    code varchar(32) not null unique,
    name varchar(64) not null unique,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0
);

create table users (
    id uuid primary key default gen_random_uuid(),
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    status varchar(32) not null,
    email_verified_at timestamptz null,
    last_login_at timestamptz null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0
);

create table user_profiles (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null unique references users(id) on delete cascade,
    first_name varchar(100),
    last_name varchar(100),
    avatar_url varchar(500),
    timezone varchar(64),
    locale varchar(32),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0
);

create table user_roles (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id) on delete cascade,
    role_id uuid not null references roles(id) on delete cascade,
    assigned_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0,
    constraint uq_user_roles unique (user_id, role_id)
);

create table password_reset_tokens (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id) on delete cascade,
    token_hash varchar(128) not null unique,
    expires_at timestamptz not null,
    used_at timestamptz null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0
);

create table refresh_tokens (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id) on delete cascade,
    token_hash varchar(128) not null unique,
    expires_at timestamptz not null,
    revoked_at timestamptz null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0
);

create table categories (
    id uuid primary key default gen_random_uuid(),
    name varchar(120) not null unique,
    slug varchar(140) not null unique,
    description varchar(500),
    sort_order int not null default 0,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0
);

create table quizzes (
    id uuid primary key default gen_random_uuid(),
    category_id uuid not null references categories(id),
    title varchar(160) not null,
    slug varchar(180) not null unique,
    description varchar(2000),
    status varchar(32) not null,
    duration_seconds int not null,
    current_version_id uuid null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0
);

create table questions (
    id uuid primary key default gen_random_uuid(),
    category_id uuid not null references categories(id),
    created_by uuid null references users(id),
    stem varchar(4000) not null,
    explanation varchar(4000),
    image_url varchar(500),
    difficulty_level varchar(32) not null,
    status varchar(32) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0
);

create table question_options (
    id uuid primary key default gen_random_uuid(),
    question_id uuid not null references questions(id) on delete cascade,
    option_order int not null,
    option_text varchar(2000) not null,
    is_correct boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0,
    constraint uq_question_option_order unique (question_id, option_order)
);

create table quiz_versions (
    id uuid primary key default gen_random_uuid(),
    quiz_id uuid not null references quizzes(id) on delete cascade,
    version_no int not null,
    title_snapshot varchar(160) not null,
    description_snapshot varchar(2000),
    duration_seconds int not null,
    passing_score int null,
    question_count int not null default 0,
    status varchar(32) not null,
    published_at timestamptz null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0,
    constraint uq_quiz_version unique (quiz_id, version_no)
);

alter table quizzes
    add constraint fk_quiz_current_version foreign key (current_version_id) references quiz_versions(id);

create table quiz_version_questions (
    id uuid primary key default gen_random_uuid(),
    quiz_version_id uuid not null references quiz_versions(id) on delete cascade,
    question_id uuid not null references questions(id),
    display_order int not null,
    marks int not null default 1,
    negative_marks int not null default 0,
    required_flag boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0,
    constraint uq_quiz_version_question unique (quiz_version_id, question_id),
    constraint uq_quiz_version_display_order unique (quiz_version_id, display_order)
);

create table quiz_attempts (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id),
    quiz_version_id uuid not null references quiz_versions(id),
    status varchar(32) not null,
    started_at timestamptz not null,
    submitted_at timestamptz null,
    auto_submitted boolean not null default false,
    score int not null default 0,
    max_score int not null default 0,
    correct_count int not null default 0,
    wrong_count int not null default 0,
    unanswered_count int not null default 0,
    elapsed_seconds int null,
    ip_address varchar(64),
    attempt_number int not null default 1,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0
);

create index idx_quiz_attempts_user_started_at on quiz_attempts (user_id, started_at desc);
create index idx_quiz_attempts_quiz_status on quiz_attempts (quiz_version_id, status);

create table attempt_answers (
    id uuid primary key default gen_random_uuid(),
    attempt_id uuid not null references quiz_attempts(id) on delete cascade,
    question_id uuid not null references questions(id),
    selected_option_id uuid null references question_options(id),
    is_correct boolean not null default false,
    answered_at timestamptz not null,
    response_time_ms bigint null,
    question_snapshot_json text,
    option_snapshot_json text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0,
    constraint uq_attempt_question unique (attempt_id, question_id)
);

create index idx_attempt_answers_attempt_id on attempt_answers (attempt_id);

create table audit_logs (
    id uuid primary key default gen_random_uuid(),
    actor_user_id uuid null references users(id),
    action varchar(32) not null,
    entity_type varchar(100) not null,
    entity_id varchar(64) not null,
    before_state text,
    after_state text,
    ip_address varchar(64),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0
);

create index idx_audit_logs_entity on audit_logs (entity_type, entity_id, created_at desc);
create index idx_audit_logs_actor on audit_logs (actor_user_id, created_at desc);

create index idx_users_status on users (status);
create index idx_categories_active on categories (active);
create index idx_questions_category_status on questions (category_id, status);
create index idx_quizzes_category_status on quizzes (category_id, status);
