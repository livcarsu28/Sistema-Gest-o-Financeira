
-- V10 - Categorias e formas de pagamento iniciais
-- Os registros existentes são preservados.

INSERT INTO categoria_movimentacao (nome, ativo, cor)
VALUES
('Doações', 1, '#16A34A'),
('Eventos beneficentes', 1, '#22C55E'),
('Patrocínios', 1, '#15803D'),
('Convênios e parcerias', 1, '#0D9488'),
('Campanhas de arrecadação', 1, '#059669'),
('Outras entradas', 1, '#4ADE80'),
('Alimentação', 1, '#F97316'),
('Água e energia', 1, '#EAB308'),
('Aluguel', 1, '#EA580C'),
('Materiais de limpeza', 1, '#06B6D4'),
('Materiais educativos', 1, '#3B82F6'),
('Transporte', 1, '#8B5CF6'),
('Salários', 1, '#DC2626'),
('Manutenção', 1, '#F59E0B'),
('Saúde', 1, '#EC4899'),
('Despesas administrativas', 1, '#64748B'),
('Eventos', 1, '#A855F7'),
('Outras despesas', 1, '#6B7280')
ON DUPLICATE KEY UPDATE id_categoria = id_categoria;

INSERT INTO forma_pagamento (nome, ativo)
VALUES
('PIX', 1),
('Dinheiro', 1),
('Cartão de crédito', 1),
('Cartão de débito', 1),
('Transferência bancária', 1),
('Boleto bancário', 1)
ON DUPLICATE KEY UPDATE id_forma_pagamento = id_forma_pagamento;
