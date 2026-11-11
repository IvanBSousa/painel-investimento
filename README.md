# painel-investimento

## Projeto desenvolvido para Semana de Arquitetura do CaixaVerso

## Banco de dados: PostgreSQL

Requisitos: JDK 21 ou superior e Docker com Docker Compose.
O projeto usa PostgreSQL 17, Hibernate ORM/Panache e o driver
`quarkus-jdbc-postgresql`.

O arquivo `.env` contém as credenciais locais e não é versionado.
Em uma nova cópia do repositório, crie-o a partir de `.env.example`:

```powershell
Copy-Item .env.example .env
```

Edite `POSTGRES_PASSWORD` no `.env` antes de iniciar o banco. O arquivo
`.env.example` contém apenas uma senha ilustrativa, nunca credenciais reais.
`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_HOST` e
`POSTGRES_PORT` são lidos pelo Quarkus a partir do `.env` na raiz do projeto.
Variáveis de ambiente do sistema têm precedência. O Compose usa as mesmas
credenciais e substitui o host por `postgres` e a porta por `5432` dentro da
rede dos containers.

Para executar apenas o banco e desenvolver a aplicação localmente:

```powershell
docker compose up -d postgres
.\mvnw.cmd quarkus:dev
```

Para executar o banco e a aplicação em containers, primeiro gere o pacote:

```powershell
.\mvnw.cmd package -DskipTests
docker compose up -d --build
```

A aplicação fica disponível em <http://localhost:8080>. O Compose aguarda
o PostgreSQL estar pronto antes de iniciar a aplicação. O banco é exposto
somente em `localhost`, na porta definida por `POSTGRES_PORT`.

**Atenção:** foi mantido o comportamento original de
`drop-and-create`: cada inicialização da aplicação apaga e recria as tabelas
e executa `src\main\resources\import.sql`, incluindo os usuários e produtos
de demonstração. Não aponte essa configuração para um banco com dados que
precisem ser preservados. Para um banco previamente provisionado, configure
`QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY=validate` e
`QUARKUS_HIBERNATE_ORM_SQL_LOAD_SCRIPT=no-file` no ambiente da aplicação.

O volume `postgres-data` mantém os arquivos do PostgreSQL ao parar os
containers com `docker compose down`, mas não impede a recriação das
tabelas pela aplicação. Não use `docker compose down -v` se precisar
preservar o volume. Alterar a senha no `.env` não altera a senha de um banco
já inicializado: nesse caso, atualize também a senha do usuário no PostgreSQL.
Dados de uma instalação anterior do SQL Server não são transferidos
automaticamente; o volume anterior não é removido por esta alteração.

Os testes de persistência com `@QuarkusTest` usam Dev Services com um
PostgreSQL isolado e exigem Docker, sem acessar o banco do `.env`:

```powershell
.\mvnw.cmd verify
```

### Usuário e senha para acesso ao sistema

- Usuário: admin
- Senha: admin


- Usuário: user
- Senha: user


## Validações de entrada e arredondamento (item 6)

As entradas inválidas retornam HTTP 400. A simulação exige corpo JSON,
`valorInvestido` positivo com até 16 dígitos inteiros e 2 casas decimais,
`prazoMeses` inteiro positivo e `produto` preenchido com até 255 caracteres.
O limite monetário corresponde às colunas existentes `NUMERIC(18,2)`,
não a um novo limite de negócio. `clienteId` continua opcional na simulação
pública; quando informado, deve ser positivo. Os IDs das consultas também
devem ser positivos. O login exige corpo, usuário e senha preenchidos.

Na listagem de simulações, `page` começa em 0 e `size` deve ser positivo
(padrões: 0 e 20). O deslocamento `page * size` não pode ultrapassar
2147483647, limite da API de paginação JPA. Não foi criado um máximo
arbitrário de itens por página. Períodos de telemetria invertidos são
rejeitados antes de consultar o banco.

Produto inexistente, resultado acima de 9999999999999999.99 ou prazo
incompatível com a capacidade de cálculo retornam HTTP 400 com `mensagem`,
sem salvar a simulação. A potência decimal aceita expoentes até 999999999;
resultados podem exceder o armazenamento antes desse prazo. Não há novo
prazo máximo de negócio.

O cálculo composto usa `BigDecimal` com precisão `DECIMAL128`, mantendo a
conversão anual/mensal existente com taxa de 10 casas decimais. O valor final
e a média por produto/dia são arredondados para 2 casas com `HALF_UP`.
Valores de entrada com mais de 2 casas são rejeitados, não arredondados
silenciosamente. Essa correção pode alterar centavos em casos antes
afetados pela conversão do capital para `double`.

Os testes dessas regras podem rodar sem banco nem Docker:

```powershell
mvn test "-Dtest=ValidacaoEntradaTest,SimulacaoUseCaseTest,SimulacaoRepositoryImplTest,TelemetriaResourceTest,EntradaInvalidaExceptionMapperTest"
```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/desafio_api-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
