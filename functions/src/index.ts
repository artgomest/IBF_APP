import { onDocumentCreated } from "firebase-functions/v2/firestore";

import * as admin from "firebase-admin";

import * as logger from "firebase-functions/logger";



// Inicializa o Firebase Admin

admin.initializeApp();



export const notificarNovoRelatorio = onDocumentCreated(

  "relatorios/{relatorioId}",
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
      logger.error(
        "O relatório não contém autorNome ou idRede.",
        novoRelatorio
      );
      return;
    }

    const db = admin.firestore();
    const tokens: string[] = [];
    try {
      const liderSnapshot = await db
        .collection("usuarios")
        .where(`funcoes.${idRede}`, "==", "lider")
        .get();



      const pastorSnapshot = await db

        .collection("usuarios")

        .where("funcoes.geral", "==", "pastor")

        .get();



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



      if (tokens.length === 0) {

        logger.info(

          `Nenhum token encontrado para notificar na rede ${idRede}.`

        );

        return;

      }



      // ✅ Montagem correta do MulticastMessage tipado

      const message: admin.messaging.MulticastMessage = {

        tokens: tokens,

        notification: {

          title: `Novo Relatório: ${idRede}`,

          body: `O relatório da rede ${idRede} foi preenchido por ${autorNome}.`,

        },

        android: {

          priority: "high",

          notification: {

            sound: "default",

          },

        },

        apns: {

          payload: {

            aps: {

              sound: "default",

              contentAvailable: true,

            },

          },

          headers: {

            "apns-priority": "10",

          },

        },

      };



      logger.info("Enviando notificação para os tokens:", tokens);



      const response = await admin.messaging().sendEachForMulticast(message);



      logger.info("Resultado do envio:", response);



      if (response.failureCount > 0) {

        response.responses.forEach((resp, idx) => {

          if (!resp.success) {

            logger.error(

              `Erro ao enviar para token ${tokens[idx]}:`,

              resp.error

            );

          }

        });

      }

    } catch (error) {

      logger.error("Erro ao buscar usuários ou enviar notificação:", error);

    }

  }

);