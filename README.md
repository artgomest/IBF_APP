# IBF App

Aplicativo desenvolvido para gerenciamento interno e de membros da igreja, focado em facilitar as atividades de líderes, secretários e pastores. O sistema oferece recursos de administração e controle de rede.

## 🚀 Funcionalidades

- **Dashboard (Pastor / Secretário):** Visão geral e estatísticas integradas (através de gráficos usando MPAndroidChart).
- **Gestão de Membros:** Cadastro completo, consulta e edição das fichas de membros.
- **Controle de Reuniões e Relatórios:** Ferramentas para que líderes de rede informem status e preencham relatórios das reuniões.
- **Autenticação e Perfis:** Integração segura com o Firebase Authentication para login e gerenciamento de permissões (Baseado em papéis como Pastor, Secretário, Líder).
- **Notificações Push:** Mensagens em tempo real utilizando Firebase Cloud Messaging (FCM).

## 🛠 Tecnologias e Bibliotecas Utilizadas

- **Linguagem:** Kotlin 2.1.0
- **SDK Mínimo:** 24 (Android 7.0)
- **SDK Alvo:** 35
- **UI & Views:** ViewBinding, Material Components, ConstraintLayout, ViewPager2, SwipeRefreshLayout
- **Backend as a Service (BaaS):** Firebase (Authentication, Firestore Database, Storage, Cloud Messaging e Hosting/Functions para serviços Web)
- **Gráficos e Relatórios:** MPAndroidChart
- **Carregamento de Imagens:** Glide

## ⚙️ Como Executar o Projeto

1. Clone o repositório:
   ```bash
   git clone https://github.com/artgomest/IBF_APP.git
   ```
2. Abra a pasta clonada no **Android Studio**.
3. Certifique-se de possuir o arquivo de configuração do Firebase (`google-services.json`) localizado na pasta `app/`. (Entre em contato com a administração do projeto para obter as credenciais).
4. Aguarde o Gradle baixar todas as dependências e sincronizar o projeto.
5. Selecione um dispositivo virtual (Emulador) ou físico e clique em **Run** (Executar).

## 🗂 Estrutura do Projeto

- `app/` - Código fonte principal do aplicativo Android.
- `web/` - Arquivos de frontend voltados para serviços na web ou relatórios complementares.
- `functions/` - Funções do Firebase (Cloud Functions) utilizadas no backend.
- `firebase.json` / `firestore.rules` / `storage.rules` - Configurações de serviços e regras de segurança do Firebase.
