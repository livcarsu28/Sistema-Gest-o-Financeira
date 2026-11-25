ALTER TABLE categoria_movimentacao ADD COLUMN descricao_publica VARCHAR(255) NULL;
ALTER TABLE publicacao_gasto_categoria ADD COLUMN descricao VARCHAR(255) NULL;

UPDATE categoria_movimentacao
SET descricao_publica = CASE nome
    WHEN 'Alimentação' THEN 'Alimentos, bebidas e ingredientes para as refeições.'
    WHEN 'Água e energia' THEN 'Contas de abastecimento de água e energia elétrica.'
    WHEN 'Aluguel' THEN 'Locação de imóveis utilizados pela instituição.'
    WHEN 'Materiais de limpeza' THEN 'Produtos e materiais para limpeza e conservação dos espaços.'
    WHEN 'Materiais educativos' THEN 'Materiais escolares, livros e insumos para oficinas e atividades educativas.'
    WHEN 'Transporte' THEN 'Combustível, fretes e deslocamentos a serviço da instituição.'
    WHEN 'Salários' THEN 'Remuneração e encargos da equipe contratada.'
    WHEN 'Manutenção' THEN 'Consertos, reparos e conservação de instalações e equipamentos.'
    WHEN 'Saúde' THEN 'Materiais, serviços e despesas de saúde.'
    WHEN 'Despesas administrativas' THEN 'Serviços administrativos, tarifas e materiais de escritório.'
    WHEN 'Eventos' THEN 'Materiais, serviços e estrutura para eventos.'
    WHEN 'Outras despesas' THEN 'Despesas que não se enquadram nas demais categorias.'
END
WHERE nome IN ('Alimentação', 'Água e energia', 'Aluguel', 'Materiais de limpeza',
    'Materiais educativos', 'Transporte', 'Salários', 'Manutenção', 'Saúde',
    'Despesas administrativas', 'Eventos', 'Outras despesas');
