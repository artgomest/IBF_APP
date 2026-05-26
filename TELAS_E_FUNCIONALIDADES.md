# Documentação de Telas, Estruturas e Funcionalidades - IBF App

Este documento fornece um descritivo profundo da estrutura de componentes UI de cada tela (Activities, Fragments e BottomSheets) do aplicativo IBF App, desenvolvido em Kotlin/Android. O objetivo desta documentação é prover a um agente de Inteligência Artificial ou desenvolvedor todo o contexto visual e estrutural necessário para manutenção, criação de testes ou refatoração, eliminando a necessidade de screenshots.

---

## 🏠 1. Telas Iniciais e Autenticação

### `activity_main.xml` (MainActivity)
**Propósito:** Tela de login e porta de entrada do aplicativo.
**Estrutura de Componentes UI:**
- `ScrollView`: Permite rolagem em telas menores.
- `ImageView`: Logotipo do aplicativo centralizado.
- 2x `EditText`: Campos para inserção de e-mail e senha.
- `MaterialButton`: Botão primário para confirmar o "Login".
- 2x `TextView`: Links textuais (ex: "Esqueci minha senha" ou "Cadastrar-se").

### `activity_home.xml` (HomeActivity)
**Propósito:** Intermediário simples pós-login.
**Estrutura de Componentes UI:**
- `ImageView`: Ícone ou banner de boas vindas.
- `TextView`: Mensagem de saudação.
- `MaterialButton`: Ação principal (ex: prosseguir para dashboard).

---

## 📊 2. Dashboards (Painéis de Controle)

### `activity_pastor_dashboard.xml` (PastorDashboardActivity)
**Propósito:** Painel gerencial superior, com métricas consolidadas da igreja.
**Estrutura de Componentes UI:**
- `CoordinatorLayout` + `SwipeRefreshLayout`: Base com suporte a pull-to-refresh.
- 3x `TextView`: Cabeçalho (Título, saudação e subtítulo).
- `ScrollView` contendo uma grade de `MaterialCardView`s:
  - Card 1: 3x `TextView` (Total de membros ativos).
  - Card 2: 3x `TextView` (Média de presença/frequência).
  - Card 3: 3x `TextView` (Relatórios entregues no prazo).
  - Card 4: 5x `TextView` (Estatísticas financeiras e de batismos).
  - Cards 5 e 6: Atalhos visuais (`ImageView` + `TextView`s) para acessar as redes e os perfis.
- `BottomNavigationView`: Menu inferior persistente para navegação.

### `activity_secretario_dashboard.xml` (SecretarioDashboardActivity)
**Propósito:** Foco em ações administrativas.
**Estrutura de Componentes UI:**
- `CoordinatorLayout` + `SwipeRefreshLayout`.
- 4x `TextView`: Cabeçalho descritivo com filtros ou avisos rápidos.
- `RecyclerView`: Lista principal exibindo ações pendentes (ex: usuários para aprovar) ou redes cadastradas.
- `BottomNavigationView`: Menu inferior.

### `activity_lider_dashboard.xml` (LiderDashboardActivity)
**Propósito:** Gerenciamento da célula/rede específica.
**Estrutura de Componentes UI:**
- `CoordinatorLayout` com 3x `TextView` no cabeçalho.
- `ScrollView` organizando `MaterialCardView`s em formato de atalhos rápidos:
  - 5x Cards estruturados com `ImageView` (ícone) e `TextView` (rótulo) para: Agenda, Gráficos de Frequência, Status de Relatórios, Membros e Configurações.
- `BottomNavigationView`: Menu inferior.

---

## 👥 3. Gestão de Membros e Usuários

### `activity_ficha_membro.xml` (FichaMembroActivity)
**Propósito:** Visualização e edição detalhada de todos os dados de um membro. É a tela mais densa do sistema.
**Estrutura de Componentes UI:**
- `CoordinatorLayout` com cabeçalho (`TextView` e `ImageView`).
- `ScrollView` longo contendo:
  - Aproximadamente 17 blocos de `TextInputLayout` + `TextInputEditText` (Dados Pessoais: Nome, CPF, Telefone, Endereço, etc.).
  - Blocos de `AutoCompleteTextView` (Para campos de seleção com busca, ex: Estado Civil, Rede).
  - 2x `MaterialCheckBox` (Para validações binárias, ex: "Batizado?", "Membro Ativo?").
  - `View` (Separador visual).
  - `RecyclerView`: Possivelmente para listar o histórico do membro (reuniões comparecidas, cursos).
- `ExtendedFloatingActionButton` e 3x `FloatingActionButton`: Menu flutuante expansível no canto inferior direito para ações rápidas (Editar, Salvar, Arquivar).

### `activity_cadastro_membro.xml` e `activity_cadastro_usuario.xml`
**Propósito:** Criação de novas contas.
**Estrutura de Componentes UI:**
- `ScrollView` agrupando uma `ImageView` de ilustração/logo.
- Múltiplos blocos de `TextInputLayout` + `TextInputEditText` (Campos de entrada de dados textuais).
- `Spinner` (Apenas no de Usuário) para selecionar o "Perfil" de acesso (Pastor, Secretário, Líder).
- `MaterialButton`: Botão de finalização do cadastro.

### `activity_lista_usuarios_rede.xml` e `activity_membros_rede.xml`
**Propósito:** Exibir listas de pessoas de uma rede.
**Estrutura de Componentes UI:**
- `CoordinatorLayout` e cabeçalho base.
- No caso de usuários, há um `TabLayout` e um `ViewPager2` (abas para separar Ativos vs. Pendentes).
- No caso de membros, há um campo de busca (`TextInputLayout` + `TextInputEditText`) seguido de um `SwipeRefreshLayout` + `RecyclerView`.
- `FloatingActionButton`: Botão para adicionar um novo membro/usuário.

### `fragment_solicitacoes_pendentes.xml` e `fragment_usuarios_ativos.xml`
**Propósito:** Abas da tela de gerenciamento de usuários.
**Estrutura de Componentes UI:**
- Apenas um `SwipeRefreshLayout` englobando um `RecyclerView`.

---

## 📅 4. Agenda e Reuniões

### `activity_agenda.xml` (AgendaActivity)
**Propósito:** Calendário e lista de eventos.
**Estrutura de Componentes UI:**
- `CoordinatorLayout` contendo um cabeçalho rico (múltiplos `TextView` e `ImageView`).
- `MaterialCardView` + `GridLayout`: Usado para montar a visualização de calendário mensal.
- `SwipeRefreshLayout` + `RecyclerView`: Lista dos eventos do dia selecionado.
- `FloatingActionButton`: Botão para cadastrar novo evento.

### `activity_agendar_reuniao.xml` (AgendarReuniaoActivity)
**Propósito:** Formulário de criação de reunião.
**Estrutura de Componentes UI:**
- `AppBarLayout` + `Toolbar`: Barra superior de navegação com título.
- `NestedScrollView` agrupando:
  - `MaterialCardView` 1: Campos gerais (Data, Hora, Local usando `TextInputLayout`, `TextInputEditText` e `AutoCompleteTextView`).
  - `MaterialCardView` 2: Campos complementares ou descrição.
- 3x `MaterialButton`: Botões de ações (Ex: Salvar, Cancelar, Adicionar Lembrete).

### `activity_detalhes_reuniao.xml` e `activity_relatorio_reuniao.xml`
**Propósito:** Visualizar e relatar o andamento de um evento que já ocorreu.
**Estrutura de Componentes UI:**
- `ScrollView` com a ficha do evento (`MaterialCardView` e `TextView`s informativos).
- O relatório em si traz interações complexas:
  - `MaterialButtonToggleGroup` com 2x `MaterialButton` (Seleção de opções estilo switch/radio).
  - Vários `TextInputLayout` para inserção manual numéricos (visitantes, valor ofertado).
  - `ProgressBar`: Indicador visual de salvamento/upload.
  - Vários `MaterialButton` ao final da tela.

---

## 📈 5. Relatórios e Gráficos

### `activity_lider_graficos.xml` (LiderGraficosActivity)
**Propósito:** Visualização dos dados da rede do líder.
**Estrutura de Componentes UI:**
- `SwipeRefreshLayout` + `ScrollView`.
- 3x `MaterialCardView`s atuando como containeres analíticos:
  - O primeiro e o terceiro contêm componentes da biblioteca MPAndroidChart (`BarChart`) renderizando os gráficos de barras.
  - O do meio contém apenas `TextView`s, atuando como sumário textual.

### `activity_lider_status_relatorios.xml`
**Propósito:** Lista de status de pendência.
**Estrutura de Componentes UI:**
- Cabeçalho básico, seguido diretamente de um `SwipeRefreshLayout` + `RecyclerView` focado na lista de "Enviados vs Pendentes".

---

## ⚙️ 6. Componentes de UI Recorrentes (List Items e Modais)

O projeto usa exaustivamente `MaterialCardView` para itens das listas (`RecyclerView`). 

- **`item_reuniao.xml`**: Card com ícones (`ImageView`) e `TextView`s para Data/Hora e Local.
- **`item_usuario_rede.xml`**: Card exibindo Foto, Nome, Função e Ícones de ação (aprovar/rejeitar/editar).
- **`item_rede_status_pastor.xml`**: Componente de lista extremamente denso usado no dashboard do pastor. Possui dezenas de `TextView`s tabulares para mostrar um resumo completo de uma rede inteira em uma única linha expansível.
- **`sheet_selecionar_perfil.xml`** / **`sheet_selecionar_relatorio.xml`**: Layouts para uso com `BottomSheetDialog`. Composições extremamente simples contendo apenas o texto de título (`TextView`) e os botões de ação (`Button` ou `RecyclerView` no caso de listas dentro do modal).
