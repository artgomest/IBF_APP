import {onDocumentCreated} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";

// Inicializa o Firebase Admin para que nossas funções possam acessar outros serviços
admin.initializeApp();

/**
 * Função acionada sempre que um novo documento é criado na coleção 'relatorios'.
 * Esta é a nova sintaxe para a v2 do Cloud Functions.
 */
export const notificarNovoRelatorio = onDocumentCreated(
  "relatorios/{relatorioId}",
  async (event) => {
    // Na v2, os dados do evento vêm dentro de 'event.data'
    const snapshot = event.data;
    if (!snapshot) {
      logger.error("Nenhum dado associado ao evento de criação do relatório.");
      return;
    }

    const novoRelatorio = snapshot.data();

    // Extrai as informações relevantes do novo relatório
    const autorNome = novoRelatorio.autorNome;
    const idRede = novoRelatorio.idRede;

    if (!autorNome || !idRede) {
      logger.error("O relatório não contém autorNome ou idRede.", novoRelatorio);
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
        if (token) {
          tokens.push(token);
        }
      });
      pastorSnapshot.forEach((doc) => {
        const token = doc.data().fcmToken;
        if (token) {
          tokens.push(token);
        }
      });

      const tokensUnicos = [...new Set(tokens)];

      if (tokensUnicos.length === 0) {
        logger.info("Nenhum usuário com token encontrado para notificar.");
        return;
      }

      // Monta o corpo da notificação
      const payload = {
        notification: {
          title: `Novo Relatório: ${idRede}`,
          body: `O relatório da rede ${idRede} foi preenchido por ${autorNome}.`,
          sound: "default",
        },
      };

      logger.info("Enviando notificação para os tokens:", tokensUnicos);

      // Envia a notificação para todos os dispositivos encontrados
      await admin.messaging().sendToDevice(tokensUnicos, payload);
    } catch (error) {
      logger.error("Erro ao buscar usuários ou enviar notificação:", error);
    }
  }
);