-- V12 - Garante o registro institucional padrao (id_instituicao = 1) caso a tabela esteja vazia
INSERT INTO instituicao (id_instituicao, logradouro, numero, bairro, cidade, uf, cep)
SELECT 1, 'Rua da Esperanca', '128', 'Centro', 'Sao Paulo', 'SP', '01010-001'
WHERE NOT EXISTS (SELECT 1 FROM instituicao);
