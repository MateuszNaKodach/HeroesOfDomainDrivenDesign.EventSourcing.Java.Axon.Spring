-- Read-model tables, previously created by Hibernate ddl-auto when the read models were JPA
-- entities. Now they are Spring Data R2DBC models, so Flyway owns their schema.
-- IF NOT EXISTS: existing development databases already contain these tables.

CREATE TABLE IF NOT EXISTS read_model_dwelling
(
    dwelling_id         VARCHAR(255) PRIMARY KEY,
    game_id             VARCHAR(255),
    creature_id         VARCHAR(255),
    cost_per_troop      JSONB,
    available_creatures INTEGER
);

CREATE INDEX IF NOT EXISTS idx_read_model_dwelling_game_id ON read_model_dwelling (game_id);

CREATE TABLE IF NOT EXISTS read_model_built_dwelling
(
    dwelling_id VARCHAR(255) PRIMARY KEY,
    game_id     VARCHAR(255),
    creature_id VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_read_model_built_dwelling_game_id ON read_model_built_dwelling (game_id);
