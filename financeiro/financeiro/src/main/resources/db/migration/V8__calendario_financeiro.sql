ALTER TABLE categoria_movimentacao ADD COLUMN cor VARCHAR(7) NULL;

ALTER TABLE anotacao ADD COLUMN id_categoria INT NULL,
    ADD CONSTRAINT fk_anotacao_categoria FOREIGN KEY (id_categoria)
        REFERENCES categoria_movimentacao (id_categoria) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE movimentacao
    ADD COLUMN observacao VARCHAR(30) NULL,
    ADD COLUMN serie VARCHAR(36) NULL,
    ADD COLUMN ordem_serie INT NULL,
    ADD COLUMN data_programada DATE NULL,
    ADD COLUMN excluida BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN serie_encerrada BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT uk_movimentacao_serie_ordem UNIQUE (serie, ordem_serie);
-- =========================================================
-- VIEW: contas pendentes
-- =========================================================

CREATE OR REPLACE VIEW vw_contas_pendentes AS
SELECT
    m.id_movimentacao,
    m.descricao,
    m.tipo,
    m.valor,
    m.data_vencimento,
    m.status_pagamento,
    c.nome AS categoria
FROM movimentacao m
INNER JOIN categoria_movimentacao c
    ON m.id_categoria = c.id_categoria
WHERE m.excluida = FALSE AND m.status_pagamento IN ('PENDENTE', 'VENCIDO');


-- =========================================================
-- VIEW: extrato
-- =========================================================

CREATE OR REPLACE VIEW vw_extrato AS
SELECT
    m.id_movimentacao,
    m.data_movimentacao,
    m.tipo,
    m.valor,
    m.descricao,
    m.conta,
    m.status_pagamento,
    m.data_vencimento,
    m.data_pagamento,
    c.nome AS categoria,
    fp.nome AS forma_pagamento,
    u.nome_completo AS usuario
FROM movimentacao m
INNER JOIN categoria_movimentacao c
    ON m.id_categoria = c.id_categoria
LEFT JOIN forma_pagamento fp
    ON m.id_forma_pagamento = fp.id_forma_pagamento
INNER JOIN usuario u
    ON m.id_usuario = u.id_usuario
WHERE m.excluida = FALSE;


-- =========================================================
-- VIEW: resumo financeiro
-- =========================================================

CREATE OR REPLACE VIEW vw_resumo_financeiro AS
SELECT
    COALESCE(
        SUM(
            CASE
                WHEN tipo = 'ENTRADA'
                     AND status_pagamento = 'PAGO'
                THEN valor
                ELSE 0
            END
        ),
        0
    ) AS total_entradas,

    COALESCE(
        SUM(
            CASE
                WHEN tipo = 'SAIDA'
                     AND status_pagamento = 'PAGO'
                THEN valor
                ELSE 0
            END
        ),
        0
    ) AS total_saidas,

    COALESCE(
        SUM(
            CASE
                WHEN tipo = 'ENTRADA'
                     AND status_pagamento = 'PAGO'
                THEN valor

                WHEN tipo = 'SAIDA'
                     AND status_pagamento = 'PAGO'
                THEN -valor

                ELSE 0
            END
        ),
        0
    ) AS saldo

FROM movimentacao WHERE excluida = FALSE;
