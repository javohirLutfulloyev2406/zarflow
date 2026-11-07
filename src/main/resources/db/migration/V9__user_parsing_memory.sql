CREATE TABLE user_parsing_memory (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    phrase      VARCHAR(120) NOT NULL,
    category_id UUID NOT NULL REFERENCES categories (id) ON DELETE RESTRICT,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_user_parsing_memory_user_phrase ON user_parsing_memory (user_id, phrase);
