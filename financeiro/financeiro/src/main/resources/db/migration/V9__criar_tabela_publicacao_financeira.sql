
-- V9 - Criação da tabela de publicação financeira

CREATE TABLE IF NOT EXISTS publicacao_financeira (
    id_publicacao INT NOT NULL AUTO_INCREMENT,
    mes_referencia INT NOT NULL,
    ano_referencia INT NOT NULL,
    total_gastos DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_entradas DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    data_publicacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status_atualizacao VARCHAR(50) NOT NULL DEFAULT 'Atualizado',
    observacao VARCHAR(255),
    id_usuario INT,

    CONSTRAINT pk_publicacao_financeira
        PRIMARY KEY (id_publicacao),

    CONSTRAINT fk_publicacao_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
