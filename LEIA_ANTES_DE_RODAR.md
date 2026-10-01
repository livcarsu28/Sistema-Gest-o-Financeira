# LEIA ANTES DE RODAR O PROJETO

Este projeto utiliza:

- Java 21
- Spring Boot
- PostgreSQL
- Flyway
- Maven

O banco de dados utiliza variáveis de ambiente para armazenar as informações de conexão.

Por segurança, usuário e senha do PostgreSQL **não são armazenados diretamente no código-fonte e não devem ser enviados para o GitHub**.

Por isso, antes de executar o projeto pela primeira vez, siga os passos abaixo.


---

# 1. Atualizar sua branch

Antes de começar, é importante trazer as alterações mais recentes da `main`.

Primeiro, verifique em qual branch você está:

```bash
git branch
```

A branch atual aparecerá com `*`.

Por exemplo:

```text
* movimentacoes
  main
```

Depois atualize as informações do repositório:

```bash
git fetch origin
```

Atualize a `main`:

```bash
git switch main
git pull origin main
```

Depois volte para sua branch:

```bash
git switch NOME_DA_SUA_BRANCH
```

Exemplo:

```bash
git switch movimentacoes
```

E traga as alterações da `main`:

```bash
git merge main
```

Se não houver conflitos, sua branch estará atualizada.


---

# 2. PostgreSQL

Para executar o sistema, é necessário ter acesso a um banco PostgreSQL.

O banco utilizado no desenvolvimento é:

```text
financeiro
```

A aplicação se conecta por padrão ao PostgreSQL através da porta:

```text
5432
```

Exemplo de conexão:

```text
jdbc:postgresql://localhost:5432/financeiro
```


---

# 3. NÃO colocar senha no application.properties

O arquivo:

```text
src/main/resources/application.properties
```

está configurado utilizando variáveis de ambiente:

```properties
spring.application.name=financeiro

spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
```

NÃO substitua essas variáveis pela sua senha diretamente no arquivo.

Por exemplo, NÃO faça:

```properties
spring.datasource.password=minhasenha123
```

Isso pode fazer com que sua senha seja enviada para o GitHub.


---

# 4. Criar o arquivo .env

Na raiz do repositório, crie um arquivo chamado:

```text
.env
```

A estrutura ficará aproximadamente assim:

```text
Sistema-Gest-o-Financeira/
│
├── .env
├── .env.example
├── .gitignore
│
└── financeiro/
    └── financeiro/
        ├── pom.xml
        └── src/
```

Dentro do `.env`, coloque:

```env
DB_URL=jdbc:postgresql://localhost:5432/financeiro
DB_USER=postgres
DB_PASSWORD=SUA_SENHA_DO_POSTGRES
```

Substitua:

```text
SUA_SENHA_DO_POSTGRES
```

pela senha configurada no seu PostgreSQL.

Exemplo:

```env
DB_URL=jdbc:postgresql://localhost:5432/financeiro
DB_USER=postgres
DB_PASSWORD=senha_exemplo
```

IMPORTANTE: nunca compartilhe sua senha real.


---

# 5. O arquivo .env NÃO deve ir para o GitHub

O `.env` contém informações privadas.

Por isso, o `.gitignore` do projeto deve conter:

```gitignore
.env
```

Antes de fazer qualquer commit, execute:

```bash
git status
```

O arquivo `.env` NÃO deve aparecer entre os arquivos que serão enviados.

Se `.env` aparecer no `git status`, NÃO faça o commit até corrigir o problema.


---

# 6. Carregar as variáveis de ambiente

IMPORTANTE:

Criar o arquivo `.env` não faz com que o Spring Boot leia automaticamente esse arquivo quando a aplicação é iniciada diretamente.

Antes de executar o projeto, abra o terminal na raiz do repositório e execute:

```bash
set -a
source .env
set +a
```

Esses comandos carregam:

```text
DB_URL
DB_USER
DB_PASSWORD
```

como variáveis de ambiente do terminal atual.

É necessário fazer isso novamente quando abrir um terminal novo.


---

# 7. Verificar se as variáveis foram carregadas

Para verificar a URL:

```bash
echo $DB_URL
```

Deve aparecer:

```text
jdbc:postgresql://localhost:5432/financeiro
```

Para verificar o usuário:

```bash
echo $DB_USER
```

Deve aparecer algo como:

```text
postgres
```

NÃO é necessário executar:

```bash
echo $DB_PASSWORD
```

Evite mostrar sua senha no terminal.


---

# 8. Banco de dados e Flyway

O projeto utiliza Flyway para controlar a estrutura do banco de dados.

As migrations estão em:

```text
src/main/resources/db/migration/
```

Atualmente:

```text
V1__criar_tabelas.sql
V2__criar_indices.sql
V3__criar_views.sql
```

O Flyway executa os arquivos em ordem:

```text
V1
 ↓
V2
 ↓
V3
```

O V1 cria as tabelas do sistema.

O V2 cria os índices.

O V3 cria as views.

Ao executar o projeto pela primeira vez em um banco vazio, o Flyway deve executar as migrations necessárias automaticamente.


---

# 9. IMPORTANTE: não alterar migrations antigas

Depois que uma migration do Flyway já foi utilizada e compartilhada pelo grupo, NÃO altere o arquivo antigo.

Por exemplo, não modificar:

```text
V1__criar_tabelas.sql
V2__criar_indices.sql
V3__criar_views.sql
```

Se for necessário alterar o banco posteriormente, crie uma nova migration.

Exemplo:

```text
V4__adicionar_campo_telefone.sql
```

Depois:

```text
V5__alterar_alguma_tabela.sql
```

Isso mantém o histórico do banco organizado e evita problemas com o Flyway.


---

# 10. Executar o projeto

Entre na pasta que contém o `pom.xml`.

No projeto atual:

```bash
cd financeiro/financeiro
```

Depois execute:

```bash
./mvnw spring-boot:run
```

Caso o projeto não possua `mvnw`, utilize:

```bash
mvn spring-boot:run
```


---

# 11. Verificar se iniciou corretamente

Se tudo estiver funcionando, o terminal deverá mostrar mensagens indicando que:

```text
PostgreSQL conectou
Spring Boot iniciou
Tomcat iniciou na porta 8080
Flyway validou/executou as migrations
```

Uma mensagem semelhante a:

```text
Tomcat started on port 8080
```

indica que o servidor web foi iniciado.


---

# 12. Acessar o sistema

Com a aplicação executando, acesse:

```text
http://localhost:8080
```

No GitHub Codespaces, utilize a URL disponibilizada na aba:

```text
PORTS
```

para a porta:

```text
8080
```


---

# 13. Tela de login do Spring Security

Caso apareça uma tela de login automaticamente, isso não significa que ocorreu um erro.

O projeto possui Spring Security.

Enquanto a autenticação definitiva do sistema ainda não estiver configurada, o Spring pode gerar automaticamente um usuário e uma senha temporária.

No terminal poderá aparecer:

```text
Using generated security password: ...
```

O usuário padrão é:

```text
user
```

e a senha é a senha temporária exibida no terminal.

Essa autenticação é apenas para desenvolvimento.

Posteriormente, o sistema utilizará os usuários cadastrados no banco de dados.


---

# 14. Erro relacionado a DB_URL, DB_USER ou DB_PASSWORD

Se aparecer algum erro indicando que:

```text
DB_URL
DB_USER
DB_PASSWORD
```

não foram encontrados, provavelmente as variáveis de ambiente não foram carregadas.

Volte para a raiz do projeto e execute novamente:

```bash
set -a
source .env
set +a
```

Depois execute o Spring novamente.


---

# 15. Erro de conexão com PostgreSQL

Se aparecer algo como:

```text
Connection refused
```

ou algum erro de conexão com:

```text
localhost:5432
```

verifique:

1. Se o PostgreSQL está executando.
2. Se o banco `financeiro` existe.
3. Se a porta é `5432`.
4. Se `DB_USER` está correto.
5. Se `DB_PASSWORD` está correto.
6. Se `DB_URL` está correto.


---

# 16. Antes de fazer commit

Sempre execute:

```bash
git status
```

Confira cuidadosamente os arquivos.

NUNCA envie para o GitHub:

```text
.env
senhas
tokens
chaves privadas
credenciais do banco
```

Os arquivos abaixo podem ser versionados normalmente:

```text
.env.example
application.properties
V1__criar_tabelas.sql
V2__criar_indices.sql
V3__criar_views.sql
código Java
HTML
CSS
JavaScript
```


---

# RESUMO PARA RODAR O PROJETO

Depois que o ambiente estiver configurado, normalmente será necessário apenas:

### 1. Abrir o terminal na raiz

### 2. Carregar as variáveis

```bash
set -a
source .env
set +a
```

### 3. Entrar na pasta do Spring

```bash
cd financeiro/financeiro
```

### 4. Rodar

```bash
./mvnw spring-boot:run
```

### 5. Abrir a aplicação

```text
http://localhost:8080
```

ou utilizar a URL da porta 8080 fornecida pelo GitHub Codespaces.


---

# IMPORTANTE

O arquivo `.env` é individual.

Cada integrante deve possuir o próprio `.env`.

Não envie o `.env` para o GitHub.

Não coloque senhas diretamente no código.

Em caso de alteração na estrutura do banco, não edite migrations antigas que já foram compartilhadas. Crie uma nova migration do Flyway.