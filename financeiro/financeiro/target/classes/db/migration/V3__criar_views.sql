
-- =========================================================
-- SISTEMA FINANCEIRO ONG
-- V3 - Criação das Views
-- MySQL 8
-- =========================================================


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
WHERE m.status_pagamento IN ('PENDENTE', 'VENCIDO');


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
    ON m.id_usuario = u.id_usuario;


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

FROM movimentacao;
