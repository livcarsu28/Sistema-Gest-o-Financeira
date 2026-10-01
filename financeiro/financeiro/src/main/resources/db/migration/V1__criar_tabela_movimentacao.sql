CREATE TABLE IF NOT EXISTS movimentacao (
    id_movimentacao INTEGER GENERATED ALWAYS AS IDENTITY,
    data_movimentacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    tipo VARCHAR(10) NOT NULL,
    valor DECIMAL(12,2) NOT NULL,

    descricao VARCHAR(500),
    conta VARCHAR(30),

    recorrente BOOLEAN NOT NULL DEFAULT FALSE,

    numero_parcelas INTEGER DEFAULT 1,
    parcela_atual INTEGER DEFAULT 1,

    status_pagamento VARCHAR(20) NOT NULL DEFAULT 'NÃO PAGO',

    data_vencimento DATE,
    data_pagamento TIMESTAMP,

    id_categoria INTEGER NOT NULL,
    id_forma_pagamento INTEGER,
    id_usuario INTEGER NOT NULL,
    id_movimentacao_origem INTEGER,

    PRIMARY KEY (id_movimentacao),

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

    CONSTRAINT fk_movimentacao_origem
        FOREIGN KEY (id_movimentacao_origem)
        REFERENCES movimentacao (id_movimentacao)
        ON DELETE SET NULL
        ON UPDATE CASCADE,

    CONSTRAINT fk_movimentacao_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);