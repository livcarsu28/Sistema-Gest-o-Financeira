# Executar o sistema com MySQL

## Requisitos

- Java 21 com `JAVA_HOME` configurado.
- PowerShell e VS Code.
- MySQL 8 instalado ou Docker Desktop com Docker Compose.
- Acesso à internet para o primeiro download do Maven e das dependências.

Execute os comandos iniciais na raiz `Sistema-Gest-o-Financeira`. Use sua branch de trabalho; não faça alterações diretamente na `main`.

## Variáveis individuais

Copie o exemplo somente se ainda não tiver um `.env`:

```powershell
if (-not (Test-Path -LiteralPath .env)) {
    Copy-Item -LiteralPath .env.example -Destination .env
}
```

Edite `.env` localmente. Configure `DB_URL`, `DB_USER` e `DB_PASSWORD` para seu banco. Para o Docker, configure também `MYSQL_ROOT_PASSWORD` com uma senha diferente da senha do usuário da aplicação. Não use `root` como `DB_USER`.

A referência de conexão local é `jdbc:mysql://localhost:3306/financeiro`. Preserve outro endereço se seu banco estiver em outro servidor. Não publique `.env` nem mostre suas senhas no terminal.

Docker Compose lê `.env` da raiz. Spring Boot não carrega esse arquivo automaticamente. No mesmo PowerShell em que executará a aplicação, carregue os valores sem exibi-los:

```powershell
foreach ($linha in Get-Content -LiteralPath .env) {
    if ($linha -match '^\s*(DB_URL|DB_USER|DB_PASSWORD|MYSQL_ROOT_PASSWORD)\s*=(.*)$') {
        $nomeVariavel = $Matches[1]
        $valorVariavel = $Matches[2].Trim()
        if ($valorVariavel.Length -ge 2) {
            if (($valorVariavel.StartsWith('"') -and $valorVariavel.EndsWith('"')) -or ($valorVariavel.StartsWith("'") -and $valorVariavel.EndsWith("'"))) {
                $valorVariavel = $valorVariavel.Substring(1, $valorVariavel.Length - 2)
            }
        }
        [Environment]::SetEnvironmentVariable($nomeVariavel, $valorVariavel, 'Process')
    }
}
```

Use uma atribuição por linha, sem comentários ao final do valor. Coloque valores com espaços ou `#` entre aspas simples. No Compose, aspas simples também evitam a interpolação de `$` na senha. Não use expressões de expansão de variáveis no arquivo; o carregamento acima trata os valores literalmente.

Confira apenas se as variáveis necessárias estão presentes:

```powershell
foreach ($nomeVariavel in 'DB_URL', 'DB_USER', 'DB_PASSWORD') {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($nomeVariavel, 'Process'))) {
        throw "Variável ausente: $nomeVariavel"
    }
}
```

Para iniciar pelo VS Code, configure as mesmas variáveis na execução local da IDE. Um terminal novo precisa carregá-las novamente.

## Banco MySQL

Escolha uma das opções abaixo. Não inicie outro servidor na porta 3306 se ela já estiver ocupada.

### Docker

O Compose usa MySQL 8.4, publica a porta apenas no computador local e persiste os dados em `mysql_data`. O volume antigo do PostgreSQL permanece intacto e não é usado pelo MySQL.

Depois de revisar as credenciais, execute:

```powershell
docker compose config --quiet
docker compose up -d mysql
docker compose ps mysql
```

Na primeira inicialização de um volume vazio, a imagem cria o banco `financeiro` e o usuário informado. Aguarde o servidor ficar pronto antes de iniciar o Spring Boot.

As variáveis de criação não alteram usuários e senhas de um volume já inicializado. Se houver um volume MySQL existente, confira suas credenciais; não o apague para solucionar problemas de conexão. Não execute `docker compose down -v` nem comandos de limpeza de volumes. Não remova containers antigos com `--remove-orphans` nesta transição.

### MySQL instalado

Conecte-se com uma conta administrativa e verifique primeiro se `financeiro` já existe. Somente se estiver ausente e sua criação estiver autorizada, execute:

```sql
CREATE DATABASE financeiro CHARACTER SET utf8mb4;
```

Solicite ao responsável pelo banco um usuário próprio para a aplicação. Ele deve conseguir acessar `financeiro` e executar as operações necessárias às migrations, incluindo criação de tabelas, índices e views. Não utilize credenciais administrativas na aplicação.

Os scripts V1–V3 criam a estrutura; não importam os registros do PostgreSQL.

## Compilar sem acessar o banco

```powershell
Set-Location financeiro/financeiro
.\mvnw.cmd dependency:resolve
.\mvnw.cmd -DskipTests compile test-compile
```

O arquivo `.mvn/wrapper/maven-wrapper.properties` faz parte do projeto e é necessário para o Wrapper. A compilação atualiza os recursos em `target/classes` com os scripts de `src/main/resources`. `target/` não deve ser versionado.

O comando acima compila os testes, mas não os executa. O teste `contextLoads()` usa `@SpringBootTest`: executá-lo pode conectar ao banco e disparar migrations. Execute-o somente depois de autorizar a validação no banco escolhido.

## Histórico Flyway e inicialização

Antes de iniciar, confirme o banco de destino e a autorização para aplicar migrations. Em banco existente, o responsável deve revisar a tabela `flyway_schema_history` e os checksums. V1–V3 foram adaptadas de PostgreSQL para MySQL; a existência de arquivos no Git não comprova quais versões foram aplicadas em cada banco.

Não altere migrations já aplicadas. Não execute `repair`, `baseline` ou `clean` para contornar erros. Se houver divergência de histórico ou tabelas existentes sem histórico, interrompa a inicialização e combine a transição com a equipe.

Depois dessa conferência e autorização:

```powershell
.\mvnw.cmd spring-boot:run
```

O Flyway usa o mesmo DataSource da aplicação, procura `src/main/resources/db/migration` e aplica V1, V2 e V3 em ordem. `spring.jpa.hibernate.ddl-auto=validate` foi preservado: Hibernate não deve criar ou atualizar a estrutura por conta própria. Como ainda não há entidades JPA, essa validação não comprova a presença de todas as tabelas.

Confira nos logs a conexão MySQL, a validação e aplicação do Flyway e a inicialização do Tomcat. Verifique as oito tabelas, as três views e as versões bem-sucedidas no histórico, sem modificar seus registros manualmente.

Abra `http://localhost:8080/` ou `http://localhost:8080/dashboard`. Sem sessão autenticada, o sistema encaminha para o formulário padrão em `/login`. Após autenticar, o destino é `/dashboard`. Não é necessário criar um template `login.html` para esse formulário gerado pelo Spring Security.

No desenvolvimento, use o usuário `user` e a senha gerada pela instância atual, exibida no seu terminal. Essa senha muda a cada reinicialização; não use a senha de uma execução anterior. A autenticação definitiva será tratada separadamente.

Para iniciar pela raiz do repositório sem carregar as variáveis manualmente a cada execução:

```powershell
.\iniciar.ps1
```

Se a política padrão do PowerShell bloquear o script local, execute `powershell -NoProfile -ExecutionPolicy RemoteSigned -File .\iniciar.ps1`. Essa opção vale somente para o processo iniciado e não altera a política permanente do computador. Se houver bloqueio por política da organização, use a execução pela IDE.

O script lê `DB_URL`, `DB_USER` e `DB_PASSWORD` do `.env` local quando ainda não estiverem definidas no ambiente, valida o prefixo JDBC MySQL e verifica a porta 8080. Não encerra processos nem altera credenciais. O `.env` é opcional se as variáveis já estiverem configuradas. Use valores literais e aspas correspondentes, como explicado anteriormente neste guia.

Ao iniciar pela IDE, configure as variáveis na configuração de execução e selecione a classe `financeiro.FinanceiroApplication` deste módulo. O script não é executado automaticamente pela IDE.

Aguarde `Started FinanceiroApplication`, além da mensagem de inicialização do Tomcat. Se houver `APPLICATION FAILED TO START` ou um erro posterior, a aplicação não está pronta. Uma porta 8080 atendida por outra instância não comprova que a execução atual iniciou corretamente.

Para validar após reiniciar, encerre somente a instância deste projeto pelo terminal ou pela IDE, inicie novamente e abra uma janela privada. Acesse `/login`, autentique com a senha da nova execução e confirme o dashboard e seus arquivos visuais. O Flyway continua ativo; confira o histórico antes de iniciar contra um banco ainda não validado.

## Problemas comuns

- Variáveis ausentes: carregue `.env` no mesmo terminal ou configure a execução da IDE.
- `Connection refused`: confira o servidor, a porta publicada e o endereço de `DB_URL`.
- `Unknown database`: confira se `financeiro` existe no servidor correto.
- `Access denied`: confira usuário, senha e permissões. Alterar `.env` não altera credenciais de um volume Docker existente.
- Porta 3306 ocupada: use o servidor já instalado ou combine uma porta alternativa no Compose e em `DB_URL`.
- Erro de checksum Flyway: interrompa e revise o histórico com o responsável pelo banco.
- SQL PostgreSQL em `target/classes`: compile novamente os recursos atuais antes de iniciar. Não edite os arquivos gerados.
- Falha no Wrapper: confira Java 21, `JAVA_HOME`, conexão com Maven Central e presença de `.mvn/wrapper/maven-wrapper.properties`.

## Próxima etapa: dashboard

O dashboard demonstrativo permanece preservado. Após validar o ambiente, o back-end deverá entregar entradas recebidas e despesas pagas filtradas por `data_pagamento`, saldo mensal como diferença desses valores, quantidade de registros do mês incluindo pendentes e vencidos e a chave Pix da instituição.

A view `vw_resumo_financeiro` atual soma todo o histórico e não oferece filtro mensal. A contagem por data de registro exige confirmar com a equipe se `data_movimentacao` é realmente imutável e representa o cadastro; não existe uma coluna separada de data de criação nessa tabela.

## Antes do commit

```powershell
git status --short
git diff --check
git diff
git diff --cached --stat
```

Revise POM, Compose, `.env.example`, `.gitignore`, configuração do Wrapper e este guia. As exclusões de arquivos `target/` devem ocorrer apenas no índice, mantendo os arquivos locais. Não inclua credenciais nem arquivos individuais da IDE. Nenhum commit ou push faz parte desta configuração.
