# Vendinha

O caixa de vendas da **Feirinha do Empreendedorismo** do 2º ano do Colégio Santa Chiara (Aracaju-SE).

Cada grupo usa um tablet. A criança toca na foto do produto para montar o pedido, toca nas notas e moedas que o cliente entregou, e o app mostra o troco desenhado em notas e moedas. Tudo funciona **sem internet** e fica salvo só no próprio tablet.

---

## 1. Abrir no Android Studio

1. Instale o **Android Studio** (versão estável mais recente).
2. Abra o Android Studio, clique em **Open** e escolha a pasta `Vendinha` (a que tem o `settings.gradle.kts`).
3. Na primeira vez, o Android Studio baixa o Gradle e as bibliotecas. Espere a barra de baixo terminar ("Gradle sync finished").
4. Para testar no emulador, escolha um tablet em **Device Manager** (ex.: *Pixel Tablet*) e clique em **Run ▶**.

Requisitos do projeto:

| Item | Versão |
|---|---|
| JDK | 17 ou mais novo (o Android Studio já traz um) |
| Gradle | 9.9.0 (baixado sozinho pelo wrapper) |
| Android Gradle Plugin | 9.5.1 (com Kotlin embutido) |
| Kotlin | 2.4.21 |
| compileSdk / targetSdk | 37 (Android 17) |
| minSdk | 24 (Android 7.0), o que cobre os Galaxy Tab A antigos |

Para compilar e testar pelo terminal:

```bash
./gradlew assembleDebug   # gera app/build/outputs/apk/debug/app-debug.apk
./gradlew test            # testes de regras de dinheiro, estoque, senha e banco (Room)
```

No Windows, use `gradlew.bat` no lugar de `./gradlew`.

---

## 2. Gerar o APK de release assinado

O APK de release é menor (cerca de 2 MB) e mais rápido. Ele precisa ser **assinado** com uma chave sua. **Guarde essa chave**: sem ela, você não consegue atualizar o app nos tablets depois.

### 2.1 Criar a chave (uma vez só)

Na pasta do projeto, rode (o `keytool` vem junto com o JDK do Android Studio):

```bash
keytool -genkeypair -v -keystore vendinha.jks -alias vendinha -keyalg RSA -keysize 2048 -validity 10000
```

Ele pergunta uma senha e alguns dados (nome, escola). Anote a senha.

### 2.2 Dizer ao Gradle onde está a chave

Crie o arquivo `keystore.properties` na pasta do projeto (ao lado do `settings.gradle.kts`):

```properties
storeFile=vendinha.jks
storePassword=SUA_SENHA
keyAlias=vendinha
keyPassword=SUA_SENHA
```

> `vendinha.jks` e `keystore.properties` já estão no `.gitignore`. **Nunca** publique esses arquivos.

### 2.3 Gerar o APK

```bash
./gradlew assembleRelease
```

O arquivo sai em `app/build/outputs/apk/release/app-release.apk`.

> Se o `keystore.properties` não existir, o Gradle gera `app-release-unsigned.apk`, que **não instala**. Também dá para assinar pelo Android Studio: **Build ▸ Generate Signed App Bundle / APK… ▸ APK**.

---

## 3. Instalar nos tablets

### Opção A: cabo USB (mais rápido para 10 tablets)

1. No tablet: **Configurações ▸ Sobre o tablet ▸ Informações do software**. Toque **7 vezes** em **Número de compilação** para liberar as Opções do desenvolvedor.
2. **Configurações ▸ Opções do desenvolvedor ▸ Depuração USB**: ligue.
3. Ligue o cabo no computador e aceite "Permitir depuração USB" no tablet.
4. No computador:
   ```bash
   adb install -r app-release.apk
   ```
5. Depois de instalar, desligue a Depuração USB.

### Opção B: enviando o arquivo `.apk`

1. Envie o `app-release.apk` para o tablet (pendrive/cabo copiando para a pasta *Download*, Google Drive, Quick Share ou WhatsApp).
2. Abra o arquivo no app **Meus Arquivos**.
3. O Android avisa que a instalação está bloqueada. Toque em **Configurações** e ligue **Permitir desta fonte** (em alguns modelos: **Configurações ▸ Aplicativos ▸ Meus Arquivos ▸ Instalar apps desconhecidos**; no Android 9: **Configurações ▸ Biometria e segurança ▸ Instalar apps desconhecidos**).
4. Volte e toque em **Instalar**.
5. Depois, desligue de novo a permissão de "apps desconhecidos".

---

## 4. Passo a passo do dia da feira

### Antes da feira (organizador)

1. **Instale** o app nos 10 tablets.
2. **Primeira abertura**: o app pede para **criar a senha do Organizador** (4 a 8 números) e confirmar. Use a mesma senha em todos os tablets para facilitar.
3. Carregue os tablets. Deixe o brilho médio.
4. Ative a **Fixação de tela** (veja a seção 5) para os alunos não saírem do app.

### Começar um grupo

1. Na tela **"Qual é o seu grupo?"**, toque na aba da turma (2º A a 2º E) e no card colorido do grupo. Confirme em **Começar**.
   - Turma ou grupo fora da lista? Toque em **Outro grupo**, escreva a turma e o nome e escolha uma cor.
   - O nome do grupo pode ser mudado depois no lápis ✏️ do Painel.
2. Abre o **Painel** do grupo.

### Cadastrar os produtos

1. No Painel, toque em **Produtos ▸ Novo item**.
2. **Tire a foto** do produto (ou escolha da galeria). A foto é obrigatória e é recortada em quadrado sozinha.
3. Escreva o **nome** e o **preço** (ex.: `2,50`).
   - Sabores e tamanhos viram itens separados: "Geladinho de morango", "Geladinho de uva", "Fatia de bolo".
4. **Controlar quantidade?**
   - **Ligado**: escreva quantos vocês têm. O app desconta a cada venda, avisa "Acabando" com 3 ou menos e bloqueia em "Esgotado".
   - **Desligado**: vende sem limite (bom para fatias de bolo e pipoca a granel). O app só conta quanto vendeu.
5. **Salvar item**. Para mudar o preço, repor ou desativar, toque no item na lista.

### Abrir o caixa

1. Toque em **Vender**. Na primeira vez abre **Abrir o caixa**.
2. Conte o dinheiro de troco tocando nas notas e moedas, e toque em **Começar com R$ …**. Ou toque em **Começar sem troco**.

### Vender

1. **Toque na foto** do produto. Cada toque soma 1. O pedido aparece embaixo, com **−** e **+**.
2. O **Total** aparece no visor escuro lá em cima.
3. Errou? **Limpar** zera o pedido.
4. Toque em **Cobrar**.
5. **Quanto o cliente deu?** Toque nas notas e moedas que o cliente entregou. O visor mostra **Falta** (em salmão) ou **Troco** (em verde). Tocar numa peça que já está na bandeja tira uma.
   - Pagou o valor exato? Toque em **Valor certinho**.
6. Toque em **Confirmar venda**. A venda é salva na hora (pode acabar a bateria que não se perde).
7. A tela fica verde: **Venda feita!** O troco aparece desenhado em notas e moedas. Entregue e toque em **Próximo cliente**.

### Durante a feira

- **Estoque**: quanto vendeu de cada item e quanto ainda tem.
- **Histórico**: todas as vendas, da mais recente para a mais antiga. Tocar numa venda mostra os detalhes.
- **Estornar** (o cliente desistiu): no Histórico, abra a venda e toque em **Estornar venda**. O organizador digita a senha. A venda fica marcada como estornada (nunca é apagada) e os produtos voltam para o estoque.

### Fechar o caixa

1. No Painel, toque em **Fechar caixa**. Aparece o resumo: total ganho, número de vendas, quanto vendeu de cada item e o **campeão de vendas**.
2. Toque em **Contar o dinheiro** e conte a caixinha tocando nas notas e moedas, ou digite o valor. O app compara com o **dinheiro esperado** (troco inicial + total ganho) e diz se **bateu certinho**, se **sobrou** ou se **faltou**. Dá para pular em **Fechar sem contar**.
3. Toque em **Fechar e salvar no histórico** e confirme.
4. Aparece o **Relatório**. Toque em **Compartilhar relatório** para mandar a imagem por WhatsApp ou imprimir.

### Trocar de grupo (rodízio)

1. Com o caixa fechado, o organizador **segura o cadeado 🔒 por 2 segundos** (canto de cima do Painel ou da tela inicial) e digita a senha.
2. Toque em **Zerar tablet** e confirme. O app apaga grupo, produtos, fotos, caixa e vendas, mas **os relatórios salvos ficam**.
3. O tablet volta para **"Qual é o seu grupo?"**, pronto para o próximo grupo.

### Área do Organizador (cadeado, segurar 2 s + senha)

- **Turmas e grupos**: cadastre as turmas da feira (do 2º ao 5º ano). Toque em **Nova turma** (ex.: "4º B"), abra a turma e use **Novo grupo** para cada grupo, escrevendo o que ele vende. Cada grupo ganha o próximo número e a cor desse número. Dá para **mudar o nome** e **apagar** turmas e grupos. O app já vem com as turmas do 2º ano da lista original.
- **Registro de vendas**: todas as compras feitas no tablet, da mais nova para a mais antiga, com **Total**, **Recebido** e **Troco** de cada uma. No topo aparecem os totais (vendido, recebido dos clientes e troco devolvido), e dá para filtrar por grupo. O registro **continua salvo depois do "Zerar tablet"** e entra no backup.
- **Reabrir caixa**: se o grupo fechou sem querer.
- **Zerar tablet**: para o próximo grupo.
- **Relatórios salvos**: todos os caixas fechados neste tablet. Toque para ver e compartilhar.
- **Exportar backup**: gera um arquivo `.json` com todos os relatórios para guardar no Drive ou mandar por e-mail.
- **Apagar histórico**: apaga os relatórios salvos e o registro de vendas. Pede duas confirmações.
- **Som de caixa registradora**: liga ou desliga o "ka-ching" da venda.

---

## 5. Dica: Fixação de tela (para os alunos não saírem do app)

O Android tem a **Fixação de tela**: o tablet fica preso no Vendinha até alguém destravar.

1. **Ativar** (uma vez por tablet):
   - Android 13/14 (Tab A9): **Configurações ▸ Segurança e privacidade ▸ Mais configurações de segurança ▸ Fixar apps**.
   - Android 11 (Tab A7 Lite): **Configurações ▸ Biometria e segurança ▸ Outras configurações de segurança ▸ Fixar janelas**.
   - Android 9 (Tab A antigo): **Configurações ▸ Biometria e segurança ▸ Outras configurações de segurança ▸ Fixar janelas**.
   - Ligue também **"Pedir PIN/padrão antes de liberar"**.
2. **Fixar**: abra o Vendinha, toque no botão **Recentes** (▢ ou |||), toque no **ícone do Vendinha** no topo do cartão e escolha **Fixar este app**.
3. **Soltar**: segure **Voltar** e **Recentes** ao mesmo tempo e digite o PIN do tablet.

---

## 6. Decisões tomadas (onde o documento deixava dúvida)

Sempre escolhemos a opção mais simples para as crianças:

1. **Senha do Organizador**: é pedida ao **entrar** na Área do Organizador (cadeado, segurar 2 s). Lá dentro, reabrir e zerar pedem só confirmação, e apagar relatórios pede **duas** confirmações. O **estorno** fica no Histórico do grupo e pede a senha na hora.
2. **Preços múltiplos de R$ 0,05**: não existe moeda de 1 centavo. Assim o troco sempre fecha em notas e moedas de verdade.
3. **Um caixa por grupo**: depois de fechado, o caixa fica só para consulta. Só o organizador reabre. Ao fechar de novo, o **mesmo relatório é atualizado** (não duplica). Para isso, a tabela `Fechamento` ganhou uma coluna extra `caixaId` (referência solta, sem chave estrangeira, para continuar sobrevivendo ao "Zerar").
4. **Grupo "Outro"**: fica com número 0. A criança escolhe uma das 5 cores e o nome aparece como "Cookies · 3º A".
5. **Repor estoque**: no item, a criança escreve "Quantos têm agora?". A quantidade inicial sobe junto, para a barra do Estoque continuar fazendo sentido.
6. **Itens nunca são apagados**, só **desativados**, porque as vendas antigas guardam o nome e o preço do momento.
7. **"Acabando"** aparece com **3 ou menos** unidades. **"Esgotado"** escurece o card e bloqueia o toque.
8. **Zerar com caixa aberto** é permitido, mas o app avisa para fechar antes e não perder o relatório.
9. **Senha com PBKDF2-HMAC-SHA1** + salt aleatório: é o único PBKDF2 disponível no Android 7 (minSdk 24).
10. **Foto pelo app de câmera do sistema** (`TakePicture`): o Vendinha **não pede permissão de câmera** nem de internet.
11. **Sem backup na nuvem** (`allowBackup=false`): os dados ficam só no tablet (LGPD). O backup é manual, pela Área do Organizador.
12. **Imagem do relatório** fica salva em `files/relatorios` (sobrevive ao Zerar) e é apagada junto com "Apagar relatórios".
13. **Som da venda** foi sintetizado especialmente para o app (sem direitos de terceiros).
14. **Orientação retrato**: travada no manifesto. Em tablets com **Android 16 ou mais novo**, o próprio Android pode ignorar essa trava em telas grandes. Por isso, todas as telas também funcionam deitadas (com rolagem). Nos tablets da feira (Android 9 a 14) a trava funciona normalmente.
15. **Testes de banco** (Room em memória) rodam com **Robolectric**, dentro do `./gradlew test`, sem precisar de emulador.
16. **Turmas no banco (versão 2)**: a lista do 2º ano saiu do código e virou as tabelas `turma` e `grupo_catalogo`. Apagar uma turma ou um grupo só tira da lista: vendas, relatórios e registro continuam. Os números dos grupos não se repetem: apagar o Grupo 1 e criar outro gera o Grupo 3.
17. **Registro de vendas** (tabela `registro_venda`) é gravado na **mesma transação** da venda e marcado no estorno. Não tem chave estrangeira, por isso sobrevive ao "Zerar". A migração 1→2 já copia para o registro as vendas que existiam.

---

## 7. Organização do código

```
app/src/main/java/br/com/santachiara/vendinha/
├── domain/        regras puras e testáveis: Dinheiro (centavos, R$ pt-BR, troco guloso),
│                  ContaCaixa, RegrasEstoque, Senha (PBKDF2), Resumo, GruposPredefinidos
├── data/          Room (entidades, DAOs com transações, migrações), repositórios,
│                  fotos (recorte 800 px / JPEG ~80%), compartilhar, relatório/backup JSON, som
├── ui/            tema (cores, Nunito), componentes (tecla, visor, notas e moedas,
│                  teclado numérico), telas + ViewModels, navegação
├── AppContainer   injeção de dependência manual (sem Hilt)
└── MainActivity
app/src/test/      testes unitários (domain) e de DAO (Room em memória)
app/schemas/       esquema do banco versão 1 (base para migrações futuras)
```

**Dinheiro** é sempre `Long` em centavos. Não há `Double` nem `Float` para dinheiro em nenhum lugar.

**Banco**: versão 2 (migração explícita 1→2 testada em `MigracaoTest`), com migrações explícitas em `data/db/VendinhaDatabase.kt` (objeto `Migracoes`). Não usa `fallbackToDestructiveMigration`, para nunca apagar vendas numa atualização.

---

## 8. Créditos e licenças

- Fonte **Nunito**, © The Nunito Project Authors, sob a SIL Open Font License 1.1 (`licencas/OFL-Nunito.txt`). Está empacotada no app, sem download.
- Marca **Feirinha do Empreendedorismo** © Colégio Santa Chiara.
- Cores das notas e moedas inspiradas na 2ª família do Real, desenhadas de forma simplificada (sem reproduzir as cédulas).
