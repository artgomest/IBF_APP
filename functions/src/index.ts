import {onDocumentCreated} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";

// Inicializa o Firebase Admin
admin.initializeApp();

/**
 * Função acionada sempre que um novo documento é criado na coleção 'relatorios'.
 */
export const notificarNovoRelatorio = onDocumentCreated(
  {
    document: "relatorios/{relatorioId}",
    region: "us-central1", // <-- ALTERAÇÃO FEITA AQUI
  },
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      logger.error("Nenhum dado associado ao evento de criação do relatório.");
      return;
    }

    const novoRelatorio = snapshot.data();
    const autorNome = novoRelatorio.autorNome;
    const idRede = novoRelatorio.idRede;

    if (!autorNome || !idRede) {
      logger.error("O relatório não contém autorNome ou idRede.");
      return;
    }

    const db = admin.firestore();
    const tokens: string[] = [];

    // Busca o líder da rede e todos os pastores em paralelo
    const liderQuery = db
      .collection("usuarios")
      .where(`funcoes.${idRede}`, "==", "lider")
      .get();

    const pastorQuery = db
      .collection("usuarios")
      .where("funcoes.geral", "==", "pastor")
      .get();

    try {
      const [liderSnapshot, pastorSnapshot] = await Promise.all([
        liderQuery,
        pastorQuery,
      ]);

      // Coleta os tokens dos usuários a serem notificados
      liderSnapshot.forEach((doc) => {
        const token = doc.data().fcmToken;
        if (token) tokens.push(token);
      });
      pastorSnapshot.forEach((doc) => {
        const token = doc.data().fcmToken;
        if (token) tokens.push(token);
      });

      const tokensUnicos = [...new Set(tokens)];
      if (tokensUnicos.length === 0) {
        logger.info("Nenhum usuário com token encontrado para notificar.");
        return;
      }

      const payload = {
        notification: {
          title: `Novo Relatório: ${idRede}`,
          body: `O relatório da rede ${idRede} foi preenchido por ${autorNome}.`,
          sound: "default",
        },
      };

      logger.info("Enviando notificação para tokens:", tokensUnicos);
      await admin.messaging().sendEachForMulticast({
          tokens: tokensUnicos,
          notification: payload.notification,
      });

    } catch (error) {
      logger.error("Ocorreu um erro geral ao processar as notificações:", error);
    }
  }
);