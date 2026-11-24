CREATE TABLE publicacao_gasto_categoria (
    id_publicacao INT NOT NULL,
    id_categoria INT NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    total DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id_publicacao, id_categoria),
    CONSTRAINT fk_gasto_categoria_publicacao
        FOREIGN KEY (id_publicacao) REFERENCES publicacao_financeira (id_publicacao)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

