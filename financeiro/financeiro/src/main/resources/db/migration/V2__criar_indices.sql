-- =========================================================
-- SISTEMA FINANCEIRO ONG
-- V2 - Criação dos índices
-- PostgreSQL
-- =========================================================


-- =========================================================
-- ÍNDICES: usuario
-- =========================================================

CREATE INDEX idx_usuario_cargo
    ON usuario (id_cargo);


-- =========================================================
-- ÍNDICES: anotacao
-- =========================================================

CREATE INDEX idx_anotacao_usuario
    ON anotacao (id_usuario);

CREATE INDEX idx_anotacao_lembrete
    ON anotacao (data_lembrete);


-- =========================================================
-- ÍNDICES: movimentacao
-- =========================================================

CREATE INDEX idx_movimentacao_forma_pagamento
    ON movimentacao (id_forma_pagamento);

CREATE INDEX idx_movimentacao_origem
    ON movimentacao (id_movimentacao_origem);

CREATE INDEX idx_movimentacao_data
    ON movimentacao (data_movimentacao);

CREATE INDEX idx_movimentacao_tipo
    ON movimentacao (tipo);

CREATE INDEX idx_movimentacao_status
    ON movimentacao (status_pagamento);

CREATE INDEX idx_movimentacao_categoria
    ON movimentacao (id_categoria);

CREATE INDEX idx_movimentacao_usuario
    ON movimentacao (id_usuario);

CREATE INDEX idx_movimentacao_vencimento
    ON movimentacao (data_vencimento);


-- =========================================================
-- ÍNDICES: imagem_midia
-- =========================================================

CREATE INDEX idx_imagem_usuario
    ON imagem_midia (id_usuario);

CREATE INDEX idx_imagem_movimentacao
    ON imagem_midia (id_movimentacao);