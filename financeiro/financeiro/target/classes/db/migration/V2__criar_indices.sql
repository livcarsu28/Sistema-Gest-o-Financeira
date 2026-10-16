
-- =========================================================
-- SISTEMA FINANCEIRO ONG
-- V2 - Criação dos índices
-- MySQL 8
-- =========================================================


-- =========================================================
-- ÍNDICES: anotacao
-- =========================================================

CREATE INDEX idx_anotacao_lembrete
    ON anotacao (data_lembrete);


-- =========================================================
-- ÍNDICES: movimentacao
-- =========================================================

CREATE INDEX idx_movimentacao_data
    ON movimentacao (data_movimentacao);

CREATE INDEX idx_movimentacao_tipo
    ON movimentacao (tipo);

CREATE INDEX idx_movimentacao_status
    ON movimentacao (status_pagamento);

CREATE INDEX idx_movimentacao_vencimento
    ON movimentacao (data_vencimento);
