
ALTER TABLE movimentacao
    ADD COLUMN tipo_recorrencia VARCHAR(10) NULL,
    ADD COLUMN dia_mes INT NULL,
    ADD COLUMN dia_semana VARCHAR(10) NULL;
