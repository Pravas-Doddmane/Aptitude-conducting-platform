create table feedbacks (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id),
    quiz_id uuid null references quizzes(id) on delete set null,
    question_id uuid null references questions(id) on delete set null,
    rating int not null,
    message varchar(2000) not null,
    status varchar(32) not null,
    reviewed_by uuid null references users(id) on delete set null,
    reviewed_at timestamptz null,
    admin_note varchar(2000),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version bigint not null default 0
);

create index idx_feedbacks_status on feedbacks (status);
create index idx_feedbacks_created_at on feedbacks (created_at desc);
create index idx_feedbacks_quiz on feedbacks (quiz_id);
create index idx_feedbacks_question on feedbacks (question_id);
