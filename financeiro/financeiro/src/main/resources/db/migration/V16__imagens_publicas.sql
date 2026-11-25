ALTER TABLE imagem_midia
    ADD COLUMN conteudo LONGBLOB NULL,
    ADD COLUMN publicada BOOLEAN NOT NULL DEFAULT FALSE,
    ADD INDEX idx_imagem_url (url_imagem);
