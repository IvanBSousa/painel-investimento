# Item 6 — Validações de entrada, paginação e arredondamento

## Objetivo

Completar as validações de campos obrigatórios, limites de valor/prazo,
paginação e arredondamento, sem alterar a configuração ou o esquema do
banco do grupo.

## 1. Campos da simulação

Arquivo: `src/main/java/caixaverso/application/dto/SimulacaoRequestDTO.java`.

| Campo | Regra |
|---|---|
| `valorInvestido` | Obrigatório, positivo, com até 16 dígitos inteiros e 2 casas decimais. |
| `prazoMeses` | Obrigatório, inteiro e maior ou igual a 1. |
| `produto` | Obrigatório, não vazio nem composto apenas de espaços, com até 255 caracteres. |
| `clienteId` | Continua opcional; quando informado, deve ser positivo. |

Anteriormente, `@Positive` e `@Min` permitiam valores nulos. Foram adicionadas
as validações de obrigatoriedade e mensagens explicativas em português.

Em `SimulacaoResource.java`, o próprio corpo da requisição também passou a
ser obrigatório. Corpo ausente ou JSON `null` retornam HTTP 400.

## 2. Limites monetários e de prazo

Arquivos: `SimulacaoRequestDTO.java` e `SimulacaoUseCase.java`.

O limite de valor acompanha as colunas existentes `NUMERIC(18,2)`:
`9999999999999999.99`. Não foi criado um limite comercial arbitrário.

- A entrada rejeita valores que excedam a precisão permitida ou tenham mais
  de 2 casas decimais.
- O resultado é verificado antes da persistência para garantir que cabe na
  coluna `valor_final`.
- Prazos que excedam a capacidade do cálculo decimal retornam HTTP 400.
- O prazo mínimo continua sendo 1 mês.
- Não foi definido um prazo máximo de negócio. A operação `BigDecimal.pow`
  aceita expoentes até 999999999; o resultado pode ultrapassar o limite de
  armazenamento antes disso.

Os limites comerciais de valor e prazo ainda poderão ser definidos pelo
grupo. Esta entrega trata os limites técnicos já existentes.

## 3. Tratamento de entradas inválidas

Novos arquivos:

- `src/main/java/caixaverso/application/exception/EntradaInvalidaException.java`
- `src/main/java/caixaverso/interfaceadapter/exception/EntradaInvalidaExceptionMapper.java`

Produto inexistente, paginação inválida e limites de cálculo/armazenamento
passaram a retornar HTTP 400 com mensagem JSON, por exemplo:

```json
{
  "mensagem": "Produto não encontrado."
}
```

Esses erros são detectados antes de salvar a simulação.

As violações das anotações de validação continuam usando o tratamento
existente do Quarkus. Não houve padronização de todos os erros da API:
essa atividade pertence ao item 5.

## 4. Paginação

Arquivos: `SimulacaoResource.java` e `SimulacaoUseCase.java`.

| Parâmetro | Regra |
|---|---|
| `page` | Maior ou igual a 0; padrão mantido em 0. |
| `size` | Maior ou igual a 1; padrão mantido em 20. |
| `page * size` | Até 2147483647, limite do deslocamento utilizado pelo JPA. |

A multiplicação é verificada usando `long`, evitando transbordamento de
inteiros antes da validação.

Exemplos rejeitados com HTTP 400:

```text
/simulacoes?page=-1
/simulacoes?size=0
/simulacoes?page=2147483647&size=2
```

Não foi criado um máximo arbitrário de itens por página. Esse limite
operacional pode ser definido posteriormente pelo grupo.

## 5. Precisão e arredondamento

Arquivo: `SimulacaoUseCase.java`.

O cálculo composto deixou de converter o capital para `double` e passou a
usar `BigDecimal` com `MathContext.DECIMAL128`. O valor final é arredondado
para 2 casas decimais com `RoundingMode.HALF_UP`.

A conversão anual/mensal existente foi preservada: ainda usa `Math.pow`
e taxa mensal com 10 casas decimais. Portanto, não se trata de uma
reescrita de toda a fórmula usando exclusivamente operações decimais.

Arquivo: `SimulacaoRepositoryImpl.java`.

A média por produto/dia passou a dividir com 2 casas decimais e `HALF_UP`.
Isso evita a exceção que poderia ocorrer em divisões com resultado periódico.

Exemplos:

```text
100,01 / 3 = 33,34
2,01 / 2   = 1,01
```

Valores de entrada com mais de 2 casas decimais são rejeitados, não
arredondados silenciosamente. A melhoria de precisão pode alterar centavos
em casos antes afetados pela conversão do capital para `double`.

## 6. Validações relacionadas

| Arquivos | Alteração |
|---|---|
| `LoginRequestDTO.java` e `AutenticacaoResource.java` | Corpo, usuário e senha obrigatórios; textos vazios ou apenas com espaços são rejeitados. |
| `InvestimentosResource.java` | O `clienteId` da consulta deve ser positivo. |
| `PerfilRiscoResource.java` | O `clienteId` da consulta deve ser positivo. |
| `TelemetriaResource.java` | A validação de período invertido foi movida para antes da consulta ao banco. |

A rejeição de período invertido já existia, mas a consulta acontecia antes
da validação.

Não foram alteradas as políticas de autorização. A simulação continua
pública e seu `clienteId` continua opcional.

## 7. Testes

Arquivos novos:

- `src/test/java/caixaverso/interfaceadapter/resource/ValidacaoEntradaTest.java`
- `src/test/java/caixaverso/interfaceadapter/resource/TelemetriaResourceTest.java`
- `src/test/java/caixaverso/interfaceadapter/exception/EntradaInvalidaExceptionMapperTest.java`

Arquivos ampliados:

- `src/test/java/caixaverso/application/usecase/SimulacaoUseCaseTest.java`
- `src/test/java/caixaverso/persistence/repository/SimulacaoRepositoryImplTest.java`

Cobertura acrescentada:

- Campos ausentes, nulos, vazios e inválidos.
- Limites monetários, tamanho do produto e IDs positivos.
- Paginação inválida e limite técnico do deslocamento.
- Arredondamento e preservação de centavos.
- Resultado ou prazo fora da capacidade, sem persistência.
- Período invertido sem consulta ao banco.
- Mapeamento de erro de entrada para HTTP 400 com mensagem JSON.

Os 46 testes da seleção executada passaram. A seleção pode ser executada
sem banco e sem Docker:

```powershell
mvn test "-Dtest=ValidacaoEntradaTest,SimulacaoUseCaseTest,SimulacaoRepositoryImplTest,TelemetriaResourceTest,EntradaInvalidaExceptionMapperTest"
```

Também foram feitas verificações HTTP na aplicação local com H2,
confirmando rejeições com HTTP 400, ausência de gravações nas requisições
inválidas e preservação de login, simulação válida e consultas autenticadas.
Essas verificações HTTP foram executadas durante o desenvolvimento, mas
não foram adicionadas como testes HTTP automatizados ao repositório.

A suíte de integração com PostgreSQL não foi executada nesta etapa.

## 8. Documentação

O `README.md` recebeu uma seção com as regras, decisões de arredondamento,
limites técnicos e comando dos testes.

## 9. Separação do PR e da configuração local H2

As alterações do item 6 não dependem de mudanças no `application.properties`,
no `pom.xml`, no esquema ou nos dados iniciais do banco.

Existem mudanças anteriores de desenvolvimento local para H2. Elas devem
ficar fora do commit e do PR do item 6:

- Não incluir as mudanças locais de `application.properties`.
- Não incluir as mudanças locais de `pom.xml`.
- No `README.md`, selecionar apenas a seção do item 6, deixando de fora as
  mudanças anteriores relacionadas ao H2 e aos requisitos de execução.
- Incluir os arquivos Java e testes do item 6.
- Este documento pode ser incluído se o grupo desejar.

Evitar `git add .`, pois incluiria também a configuração local.

## Sugestão de descrição do PR

**Título:** Completa validações de entrada, paginação e arredondamento.

**Resumo:** Adiciona validação de obrigatórios e limites técnicos, rejeita
paginação inválida, trata produto inexistente e resultados fora da
capacidade com HTTP 400, melhora a precisão do cálculo composto e define
arredondamento monetário com HALF_UP. Inclui testes e documentação, sem
alterar a configuração ou o esquema do banco do grupo.
