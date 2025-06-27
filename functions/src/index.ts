import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import { format } from "date-fns-tz";

admin.initializeApp();
const db = admin.firestore();

// =================================================================================
// SUA FUNÇÃO ORIGINAL (INTACTA)
// =================================================================================
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
      logger.error("O relatório não contém autorNome ou idRede.", novoRelatorio);
      return;
    }
    const tokens: string[] = [];
    try {
      const liderSnapshot = await db.collection("usuarios").where(`funcoes.${idRede}`, "==", "lider").get();
      const pastorSnapshot = await db.collection("usuarios").where("funcoes.geral", "==", "pastor").get();
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
        logger.info(`Nenhum token encontrado para notificar na rede ${idRede}.`);
        return;
      }
      const message: admin.messaging.MulticastMessage = {
        tokens: tokensUnicos,
        notification: {
          title: `Novo Relatório: ${idRede}`,
          body: `O relatório da rede ${idRede} foi preenchido por ${autorNome}.`,
        },
        android: { priority: "high", notification: { sound: "default" } },
        apns: { payload: { aps: { sound: "default", contentAvailable: true } }, headers: { "apns-priority": "10" } },
      };
      logger.info("Enviando notificação para os tokens:", tokensUnicos);
      const response = await admin.messaging().sendEachForMulticast(message);
      logger.info("Resultado do envio:", response);
    } catch (error) {
      logger.error("Erro ao buscar usuários ou enviar notificação:", error);
    }
  }
);

// =================================================================================
// NOVAS FUNÇÕES PARA LEMBRETES
// =================================================================================

/**
 * [AGENDADA 1 - Roda todo dia 00:01]
 * Cria os documentos de controle para os relatórios esperados do dia.
 */
export const criarControleDiario = onSchedule(
  {
    schedule: "every day 00:01",
    timeZone: "America/Sao_Paulo",
    region: "southamerica-east1",
  },
  async (event) => {
    const hoje = new Date();
    const diaDaSemanaHoje = hoje.getDay();
    const redesSnapshot = await db.collection("redes").where("diaDaSemana", "==", diaDaSemanaHoje).get();
    if (redesSnapshot.empty) {
      logger.info(`Nenhuma rede com reunião hoje.`);
      return;
    }
    const batch = db.batch();
    const dataFormatada = format(hoje, "dd/MM/yyyy", { timeZone: "America/Sao_Paulo" });
    redesSnapshot.forEach((doc) => {
      const nomeRede = doc.id;
      const docId = `${dataFormatada.replace(/\//g, "-")}_${nomeRede}`;
      const controleRef = db.collection("controleRelatorios").doc(docId);
      batch.set(controleRef, { idRede: nomeRede, dataEsperada: dataFormatada, status: "pendente" });
    });
    await batch.commit();
    logger.info(`${redesSnapshot.size} docs de controle criados para ${dataFormatada}.`);
  }
);

/**
 * [AGENDADA 2 - Roda todo dia às 21:30]
 * Envia um lembrete para os secretários das redes que têm reunião no dia.
 */
export const lembreteDiaDaReuniao = onSchedule(
  {
    schedule: "every day 21:30",
    timeZone: "America/Sao_Paulo",
    region: "southamerica-east1",
  },
  async (event) => {
    const hoje = new Date();
    const diaDaSemanaHoje = hoje.getDay() + 1;
    const redesDeHoje = await db.collection("redes").where("diaDaSemana", "==", diaDaSemanaHoje).get();
    if (redesDeHoje.empty) return;

    for (const redeDoc of redesDeHoje.docs) {
      const nomeRede = redeDoc.id;
      const secretariosSnapshot = await db.collection("usuarios").where(`funcoes.${nomeRede}`, "==", "secretario").get();
      const tokens = secretariosSnapshot.docs.map(doc => doc.data().fcmToken).filter(Boolean);
      if (tokens.length > 0) {
          // --- CORREÇÃO APLICADA AQUI ---
          const payload = {
            notification: {
              title: `Lembrete: Reunião da ${nomeRede}`,
              body: `Olá! A reunião da sua rede é hoje. Não se esqueça de preencher o relatório ao final. 😉`,
            },
            // Adicionando configuração de alta prioridade para Android e iOS
            android: {
              priority: "high" as const,
              notification: {
                sound: "default",
              },
            },
            apns: {
              payload: {
                aps: {
                  sound: "default",
                },
              },
              headers: {
                "apns-priority": "10",
              },
            },
          };

          logger.info(`[LEMBRETE HOJE] Enviando notificação de ALTA PRIORIDADE para secretários da ${nomeRede}.`);
        await admin.messaging().sendEachForMulticast({ tokens, notification: payload.notification });
        logger.info(`Lembrete de reunião enviado para secretários da ${nomeRede}.`);
      }
    }
  }
);

/**
 * [AGENDADA 3 - Roda a cada 3 horas]
 * Verifica relatórios pendentes do dia anterior e envia cobrança.
 */
export const lembreteDePendencia = onSchedule(
  {
    schedule: "every 3 hours",
    timeZone: "America/Sao_Paulo",
    region: "southamerica-east1",
  },
  async (event) => {
    const ontem = new Date();
    ontem.setDate(ontem.getDate() - 1);
    const dataFormatadaOntem = format(ontem, "dd/MM/yyyy", { timeZone: "America/Sao_Paulo" });

    const pendentesSnapshot = await db.collection("controleRelatorios").where("dataEsperada", "==", dataFormatadaOntem).where("status", "==", "pendente").get();
    if (pendentesSnapshot.empty) {
      logger.info("Nenhum relatório pendente de ontem.");
      return;
    }
    for (const doc of pendentesSnapshot.docs) {
      const nomeRede = doc.data().idRede;
      const secretariosSnapshot = await db.collection("usuarios").where(`funcoes.${nomeRede}`, "==", "secretario").get();
      const tokens = secretariosSnapshot.docs.map(sDoc => sDoc.data().fcmToken).filter(Boolean);
      if (tokens.length > 0) {
        const payload = { notification: {
          title: "Atenção: Relatório Pendente!",
          body: `O relatório da ${nomeRede} de ontem ainda não foi preenchido. Por favor, envie o mais rápido possível.`,
        }};
        await admin.messaging().sendEachForMulticast({ tokens, notification: payload.notification });
        logger.info(`Notificação de pendência enviada para ${nomeRede}.`);
      }
    }
  }
);


/**
 * [GATILHO 2 - Roda quando um relatório é criado]
 * Atualiza o status do controle para "entregue".
 */
export const marcarRelatorioComoEntregue = onDocumentCreated(
  "relatorios/{relatorioId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;

    const novoRelatorio = snapshot.data();
    const idRede = novoRelatorio.idRede;
    const dataReuniao = novoRelatorio.dataReuniao; 
    
    const docIdControle = `${dataReuniao.replace(/\//g, "-")}_${idRede}`;
    const controleRef = db.collection("controleRelatorios").doc(docIdControle);
    await controleRef.update({ status: "entregue" });
    logger.info(`Status do relatório ${docIdControle} atualizado para 'entregue'.`);
  }
);

// =================================================================================
// NOVA FUNÇÃO DE LEMBRETE PARA O DIA DA REUNIÃO
// =================================================================================

/**
 * [NOVA FUNÇÃO AGENDADA - Roda todo dia às 09:00]
 * Envia um lembrete para os secretários das redes que têm reunião NO DIA DE HOJE.
 */
export const lembreteReuniaoHoje = onSchedule(
  {
    schedule: "every day 00:30", // Roda todo dia às 00:30 da manhã
    timeZone: "America/Sao_Paulo",
    region: "southamerica-east1",
  },
  async (event) => {
    logger.info("[LEMBRETE HOJE] Iniciando verificação de reuniões para hoje.");

    const hoje = new Date();
    // getDay() do JS: Domingo=0, Segunda=1, etc. Nosso padrão é Domingo=1.
    const diaDaSemanaHoje = hoje.getDay() + 1;

    logger.info(`[LEMBRETE HOJE] Buscando redes com diaDaSemana = ${diaDaSemanaHoje}`);

    try {
      // 1. Busca todas as redes que têm reunião HOJE.
      const redesDeHojeSnapshot = await db
        .collection("redes")
        .where("diaDaSemana", "==", diaDaSemanaHoje)
        .get();

      if (redesDeHojeSnapshot.empty) {
        logger.info("[LEMBRETE HOJE] Nenhuma rede com reunião hoje. Encerrando.");
        return;
      }

      const nomesDasRedes = redesDeHojeSnapshot.docs.map(doc => doc.data().nome);
      logger.info(`[LEMBRETE HOJE] Redes encontradas: ${nomesDasRedes.join(", ")}`);

      // 2. Para cada rede, busca os secretários e envia a notificação.
      for (const nomeRede of nomesDasRedes) {
        const secretariosSnapshot = await db
          .collection("usuarios")
          .where(`funcoes.${nomeRede}`, "==", "secretario")
          .get();
        
        const tokens = secretariosSnapshot.docs
            .map(doc => doc.data().fcmToken)
            .filter(Boolean); // Filtra quaisquer tokens nulos ou vazios

        if (tokens.length > 0) {
          const payload = {
            notification: {
              title: `Lembrete: Reunião da ${nomeRede}`,
              body: `Olá! A reunião da sua rede é hoje. Não se esqueça de preencher o relatório ao final.`,
              sound: "default",
            },
          };

          logger.info(`[LEMBRETE HOJE] Enviando notificação para secretários da ${nomeRede}.`);
          await admin.messaging().sendEachForMulticast({ tokens, notification: payload.notification });
        } else {
            logger.warn(`[LEMBRETE HOJE] Nenhum secretário com token encontrado para a ${nomeRede}.`);
        }
      }
    } catch (error) {
      logger.error("[LEMBRETE HOJE] Erro ao executar a função:", error);
    }
  }
);