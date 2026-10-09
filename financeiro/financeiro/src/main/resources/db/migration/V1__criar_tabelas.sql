
-- =========================================================
-- SISTEMA FINANCEIRO ONG
-- V1 - Criação das tabelas
-- MySQL 8
-- =========================================================


-- =========================================================
-- TABELA: cargo
-- =========================================================

CREATE TABLE cargo (
    id_cargo INT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(50) NOT NULL,
    descricao VARCHAR(255),
    nivel_permissao VARCHAR(50) NOT NULL,

    CONSTRAINT pk_cargo PRIMARY KEY (id_cargo),
    CONSTRAINT uk_cargo_nome UNIQUE (nome)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================================================
-- TABELA: usuario
-- =========================================================

CREATE TABLE usuario (
    id_usuario INT NOT NULL AUTO_INCREMENT,
    nome_completo VARCHAR(150) NOT NULL,
    email VARCHAR(100) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_exclusao DATETIME,
    id_cargo INT NOT NULL,

    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uk_usuario_email UNIQUE (email),

    CONSTRAINT fk_usuario_cargo
        FOREIGN KEY (id_cargo)
        REFERENCES cargo (id_cargo)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================================================
-- TABELA: anotacao
-- =========================================================

CREATE TABLE anotacao (
    id_anotacao INT NOT NULL AUTO_INCREMENT,
    descricao VARCHAR(255) NOT NULL,
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_lembrete DATETIME,
    concluido BOOLEAN NOT NULL DEFAULT FALSE,
    id_usuario INT NOT NULL,

    CONSTRAINT pk_anotacao PRIMARY KEY (id_anotacao),

    CONSTRAINT fk_anotacao_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================================================
-- TABELA: categoria_movimentacao
-- =========================================================

CREATE TABLE categoria_movimentacao (
    id_categoria INT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(50) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_categoria_movimentacao PRIMARY KEY (id_categoria),
    CONSTRAINT uk_categoria_nome UNIQUE (nome)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================================================
-- TABELA: forma_pagamento
-- =========================================================

CREATE TABLE forma_pagamento (
    id_forma_pagamento INT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(50) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_forma_pagamento PRIMARY KEY (id_forma_pagamento),
    CONSTRAINT uk_forma_pagamento_nome UNIQUE (nome)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================================================
-- TABELA: movimentacao
-- =========================================================

CREATE TABLE movimentacao (
    id_movimentacao INT NOT NULL AUTO_INCREMENT,
    data_movimentacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo VARCHAR(10) NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    descricao VARCHAR(500),
    conta VARCHAR(30),
    recorrente BOOLEAN NOT NULL DEFAULT FALSE,
    numero_parcelas INT DEFAULT 1,
    parcela_atual INT DEFAULT 1,
    status_pagamento VARCHAR(20) NOT NULL DEFAULT 'PAGO',
    data_vencimento DATE,
    data_pagamento DATETIME,
    id_categoria INT NOT NULL,
    id_forma_pagamento INT,
    id_usuario INT NOT NULL,
    id_movimentacao_origem INT,

    CONSTRAINT pk_movimentacao PRIMARY KEY (id_movimentacao),

    CONSTRAINT fk_movimentacao_categoria
        FOREIGN KEY (id_categoria)
        REFERENCES categoria_movimentacao (id_categoria)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_movimentacao_forma_pagamento
        FOREIGN KEY (id_forma_pagamento)
        REFERENCES forma_pagamento (id_forma_pagamento)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_movimentacao_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_movimentacao_origem
        FOREIGN KEY (id_movimentacao_origem)
        REFERENCES movimentacao (id_movimentacao)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================================================
-- TABELA: imagem_midia
-- =========================================================

CREATE TABLE imagem_midia (
    id_midia INT NOT NULL AUTO_INCREMENT,
    categoria VARCHAR(50) NOT NULL,
    url_imagem VARCHAR(255) NOT NULL,
    data_upload DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario INT NOT NULL,
    id_movimentacao INT,

    CONSTRAINT pk_imagem_midia PRIMARY KEY (id_midia),

    CONSTRAINT fk_imagem_movimentacao
        FOREIGN KEY (id_movimentacao)
        REFERENCES movimentacao (id_movimentacao)
        ON DELETE SET NULL
        ON UPDATE CASCADE,

    CONSTRAINT fk_imagem_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================================================
-- TABELA: instituicao
-- =========================================================

CREATE TABLE instituicao (
    id_instituicao INT NOT NULL AUTO_INCREMENT,
    chave_pix VARCHAR(100),
    logradouro VARCHAR(150),
    numero VARCHAR(10),
    bairro VARCHAR(50),
    cidade VARCHAR(50),
    uf CHAR(2),
    cep VARCHAR(9),
    qr_code_pix VARCHAR(255),

    CONSTRAINT pk_instituicao PRIMARY KEY (id_instituicao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
