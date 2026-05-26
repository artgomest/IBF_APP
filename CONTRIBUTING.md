# Contribuindo com o IBF App

Obrigado por demonstrar interesse em contribuir para o **IBF App**! A sua ajuda é fundamental para mantermos a ferramenta segura e atualizada.

## 📌 Passos Iniciais

1. Faça um **Fork** deste repositório na sua conta.
2. Clone o repositório bifurcado para a sua máquina local:
   ```bash
   git clone https://github.com/SEU_USUARIO/IBF_APP.git
   ```
3. Crie uma branch para a sua funcionalidade, correção ou melhoria:
   ```bash
   git checkout -b feature/nome-da-sua-feature
   ```
   *(Padrões de nome: `feature/...`, `bugfix/...`, `hotfix/...`, `docs/...`)*

## 💻 Desenvolvimento e Padrões de Código

- Escreva código em **Kotlin** seguindo o [Kotlin Style Guide](https://developer.android.com/kotlin/style-guide).
- Prefira a utilização do **ViewBinding** em detrimento de `findViewById` ou extensões sintéticas.
- Mantenha a estrutura de pacotes organizada (`com.ibf.app.ui`, `com.ibf.app.data`, `com.ibf.app.services`, etc).
- Para qualquer novo recurso que exija chamadas ao Firebase, lembre-se de tratar possíveis erros de rede e adicionar estados de *loading*.

## 🚀 Enviando suas Alterações

1. Verifique se o projeto compila sem erros.
2. Adicione os arquivos que deseja enviar e faça o commit detalhado:
   ```bash
   git add .
   git commit -m "feat: Adiciona componente X à tela de Ficha de Membro"
   ```
   *Dica: Utilize o [Conventional Commits](https://www.conventionalcommits.org/pt-br/v1.0.0/) para formatar suas mensagens.*
3. Envie para o seu repositório remoto:
   ```bash
   git push origin feature/nome-da-sua-feature
   ```
4. Abra um **Pull Request (PR)** detalhando as razões da mudança, o que foi alterado e como testar.

Agradecemos imensamente a sua contribuição!
