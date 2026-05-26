# Documentação de Telas e Funcionalidades - IBF App

Este documento descreve a estrutura de telas (Activities e Fragments) e as respectivas funcionalidades presentes no aplicativo IBF App. O aplicativo foi construído utilizando a linguagem Kotlin e é voltado para a gestão de redes, membros, relatórios e reuniões de uma igreja, com acessos segmentados por perfis de liderança.

---

## 🏠 1. Telas Iniciais e Navegação Principal

- **`MainActivity.kt`**: Tela de entrada do aplicativo (Splash ou Login). Responsável por checar a sessão do usuário no Firebase e direcionar para o fluxo adequado.
- **`HomeActivity.kt`**: Tela principal de navegação pós-login. Concentra o menu principal e atalhos rápidos dependendo do tipo de usuário (Pastor, Secretário ou Líder).

---

## 📊 2. Dashboards (Painéis de Controle)

Os dashboards são telas iniciais personalizadas de acordo com o nível hierárquico do usuário, garantindo que cada perfil veja as métricas e acessos relevantes.

- **`PastorDashboardActivity.kt`**: Painel exclusivo para os pastores. Apresenta uma visão global da igreja, métricas gerais, atalhos para todas as redes e relatórios compilados de toda a congregação.
- **`SecretarioDashboardActivity.kt`**: Painel voltado à secretaria da igreja. Foca na parte administrativa, como gestão de usuários, liberação de cadastros pendentes e ajustes estruturais das redes.
- **`LiderDashboardActivity.kt`**: Painel dos líderes de célula/rede. Mostra os dados apenas da rede sob responsabilidade do líder logado, como frequência, relatórios pendentes e agenda local.

---

## 👥 3. Gestão de Usuários e Membros

O aplicativo diferencia **Usuários** (pessoas que usam o app, como líderes e secretários) de **Membros** (as pessoas que compõem as células e redes).

- **`CadastroUsuarioActivity.kt`**: Tela para registro de um novo usuário no sistema do app (criação de conta).
- **`CadastroMembroActivity.kt`**: Tela onde líderes ou secretários podem cadastrar novos membros na igreja/rede (pessoas físicas).
- **`FichaMembroActivity.kt`**: Visualização e edição completa do perfil e histórico de um membro específico.
- **`DetalhesUsuarioActivity.kt`**: Tela que exibe as permissões e informações do perfil de um usuário de sistema.
- **`MembrosRedeActivity.kt` / `ListaUsuariosRedeActivity.kt`**: Telas de listagem para visualizar todos os membros e usuários alocados em uma determinada rede.
- **`SolicitacaoCadastroActivity.kt`**: Tela de gestão de aprovações. Novos usuários podem solicitar acesso e a secretaria/pastores aprovam por aqui.
  - **`SolicitacoesPendentesFragment.kt`**: Aba mostrando os usuários que aguardam aprovação.
  - **`UsuariosAtivosFragment.kt`**: Aba listando os usuários que já foram aprovados e estão usando a plataforma.

---

## 📅 4. Agenda e Reuniões

A gestão de encontros, cultos e células é feita através destas telas:

- **`AgendaActivity.kt`**: Visão do calendário, permitindo consultar os eventos programados para a rede ou para a igreja em geral.
- **`AgendarReuniaoActivity.kt`**: Formulário para criação de um novo evento, com data, horário, local e descrição.
- **`DetalhesReuniaoActivity.kt`**: Visualização expandida de um evento específico, podendo conter a lista de presenças ou links de relatórios.
- **`RelatorioReuniaoActivity.kt`**: Após a conclusão de uma reunião, o líder usa essa tela para informar quantas pessoas estiveram presentes, visitantes, ofertas, etc.

---

## 📈 5. Relatórios e Estatísticas

O controle do crescimento e da saúde das redes é centralizado nestas funções:

- **`EstatisticasActivity.kt`**: Tela de métricas gerais sobre crescimento e consolidação.
- **`LiderGraficosActivity.kt`**: Painel com gráficos do MPAndroidChart, permitindo ao líder visualizar o desempenho da sua própria rede de forma visual.
- **`LiderStatusRelatoriosActivity.kt`**: Permite ao líder conferir quais relatórios ele já enviou, quais estão pendentes ou em atraso.
- **`PastorRelatoriosActivity.kt`**: Visão gerencial para o pastor acompanhar a entrega e os dados dos relatórios de todos os líderes simultaneamente.
- **`SelecionarRelatorioActivity.kt` / `SelecionarRelatorioSheet.kt`**: Telas (ou modais de rodapé) usadas para filtrar e buscar um relatório específico dentro de um histórico.

---

## ⚙️ 6. Perfil e Configurações de Rede

Para personalização e administração das informações estruturais:

- **`PerfilActivity.kt`**: Onde o usuário logado (seja líder, pastor ou secretário) pode editar sua foto (via Glide/Firebase Storage), senha e dados de contato.
  - **`SelecionarPerfilSheet.kt`**: Modal para possível troca ou visualização de informações rápidas de perfil (ou troca de função, caso aplicável).
- **`ConfiguracoesRedeActivity.kt`**: Permite alterar regras ou visualizar os dados técnicos da rede do usuário.
- **`EditarRedeActivity.kt` / `FormularioRedeActivity.kt`**: Telas para criação (secretaria) e edição (líder/secretaria) do nome da rede, cor de identificação, dias de reunião e líderes responsáveis.

---

*Gerado automaticamente para documentação e organização do projeto.*
