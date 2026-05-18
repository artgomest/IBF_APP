import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import { format } from "date-fns-tz";

admin.initializeApp();
const db = admin.firestore();

// =================================================================================
// HELPERS
// =================================================================================

/**
 * Envia notificações multicast para uma lista de tokens FCM.
 */
async function enviarNotificacao(
  tokens: string[],
  title: string,
  body: string,
  data?: { [key: string]: string }
) {
  const tokensUnicos = [...new Set(tokens)].filter(Boolean);
  if (tokensUnicos.length === 0) {
    logger.info("Nenhum token válido para enviar notificação.");
    return;
  }

  const message: admin.messaging.MulticastMessage = {
    tokens: tokensUnicos,
    notification: { title, body },
    data,
    android: {
      priority: "high",
      ttl: 0, // Entrega imediata
      notification: {
        sound: "default",
        channelId: "canal_padrao_ibf_v2" // Canal com som e vibração do app
      }
    },
    apns: {
      payload: { aps: { sound: "default", contentAvailable: true, badge: 1 } },
      headers: { "apns-priority": "10" }, // Prioridade imediata no iOS
    },
  };

  try {
    const response = await admin.messaging().sendEachForMulticast(message);
    logger.info(
      `Notificação enviada. Sucesso: ${response.successCount}, Falhas: ${response.failureCount}`
    );
  } catch (error) {
    logger.error("Erro ao enviar notificação:", error);
  }
}

/**
 * Busca os tokens FCM de todos os usuários com papel "pastor" (funcoes.geral == "pastor").
 */
async function buscarTokensDePastores(): Promise<string[]> {
  const pastorSnapshot = await db
    .collection("usuarios")
    .where("funcoes.geral", "==", "pastor")
    .get();
  return pastorSnapshot.docs.map((doc) => doc.data().fcmToken).filter(Boolean);
}

// =================================================================================
// GATILHO 1 — Notifica o PASTOR quando um relatório de rede é enviado
// (por líder OU secretário)
// =================================================================================

export const notificarPastorNovoRelatorio = onDocumentCreated(
  "relatorios/{relatorioId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      logger.error("Nenhum dado associado ao evento de criação do relatório.");
      return;
    }

    const novoRelatorio = snapshot.data();
    const autorNome = novoRelatorio.autorNome as string;
    const idRede = novoRelatorio.idRede as string;
    const dataReuniao = novoRelatorio.dataReuniao as string;

    if (!autorNome || !idRede) {
      logger.error("Relatório sem autorNome ou idRede.", novoRelatorio);
      return;
    }

    logger.info(
      `[NOVO RELATÓRIO DE REDE] Rede: ${idRede} | Autor: ${autorNome} | Data: ${dataReuniao}`
    );

    try {
      // Notifica exclusivamente o Pastor
      const tokensPastor = await buscarTokensDePastores();

      if (tokensPastor.length > 0) {
        await enviarNotificacao(
          tokensPastor,
          `📋 Relatório de Rede Enviado: ${idRede}`,
          `${autorNome} enviou o relatório de rede da ${idRede} referente a ${dataReuniao}.`,
          { tipo: "novo_relatorio", idRede, dataReuniao }
        );
        logger.info(
          `[NOVO RELATÓRIO DE REDE] Notificação enviada para ${tokensPastor.length} pastor(es).`
        );
      } else {
        logger.warn("[NOVO RELATÓRIO DE REDE] Nenhum pastor com token FCM encontrado.");
      }

      // Marca o relatório de controle como entregue (se existir)
      const docIdControle = `${dataReuniao.replace(/\//g, "-")}_${idRede}`;
      await db
        .collection("controleRelatorios")
        .doc(docIdControle)
        .update({ status: "entregue" })
        .catch(() =>
          logger.warn(
            `[NOVO RELATÓRIO DE REDE] Controle ${docIdControle} não encontrado — pode não existir ainda.`
          )
        );
    } catch (error) {
      logger.error("[NOVO RELATÓRIO DE REDE] Erro ao processar gatilho:", error);
    }
  }
);

// =================================================================================
// GATILHO 2 — Notifica o PASTOR quando um relatório de discipulado é concluído
// =================================================================================

export const notificarPastorDiscipuladoConcluido = onDocumentUpdated(
  "reunioes/{reuniaoId}",
  async (event) => {
    const change = event.data;
    if (!change) {
      logger.error("Nenhum dado associado ao evento de atualização do discipulado.");
      return;
    }

    const antes = change.before.data();
    const depois = change.after.data();

    // Verifica se mudou para relatorioPreenchido: true e não estava antes
    if (depois.relatorioPreenchido === true && antes.relatorioPreenchido !== true) {
      const liderUid = depois.liderUid as string;
      const membroNome = depois.membroNome as string;
      const rede = depois.rede as string;

      logger.info(
        `[DISCIPULADO CONCLUÍDO] Lider: ${liderUid} | Membro: ${membroNome} | Rede: ${rede}`
      );

      try {
        // Busca nome do líder na coleção de usuários
        let liderNome = "Um líder";
        if (liderUid) {
          const liderDoc = await db.collection("usuarios").doc(liderUid).get();
          if (liderDoc.exists) {
            liderNome = liderDoc.data()?.nome || "Um líder";
          }
        }

        const tokensPastor = await buscarTokensDePastores();
        if (tokensPastor.length > 0) {
          await enviarNotificacao(
            tokensPastor,
            `👥 Discipulado Concluído: ${liderNome}`,
            `${liderNome} concluiu o discipulado com ${membroNome} (Rede: ${rede || "Sem Rede"}).`,
            {
              tipo: "discipulado_concluido",
              liderUid,
              liderNome,
              membroNome,
              rede,
            }
          );
          logger.info(
            `[DISCIPULADO CONCLUÍDO] Notificação enviada para ${tokensPastor.length} pastor(es).`
          );
        } else {
          logger.warn("[DISCIPULADO CONCLUÍDO] Nenhum pastor com token FCM encontrado.");
        }
      } catch (error) {
        logger.error("[DISCIPULADO CONCLUÍDO] Erro ao processar gatilho:", error);
      }
    }
  }
);

// =================================================================================
// AGENDADA 1 — Cria documentos de controle às 00:01 para redes com reunião hoje
// =================================================================================

export const criarControleDiario = onSchedule(
  {
    schedule: "every day 00:01",
    timeZone: "America/Sao_Paulo",
    region: "southamerica-east1",
  },
  async () => {
    const agora = new Date();
    const isoDay = Number(format(agora, "i", { timeZone: "America/Sao_Paulo" }));
    // ISO day: Monday=1, ..., Sunday=7. Our pattern: Sunday=1, Monday=2, ..., Saturday=7.
    const diaDaSemanaHoje = isoDay === 7 ? 1 : isoDay + 1;

    const dataFormatada = format(agora, "dd/MM/yyyy", {
      timeZone: "America/Sao_Paulo",
    });

    logger.info(`[CONTROLE DIÁRIO] Iniciando para ${dataFormatada} (dia ${diaDaSemanaHoje}).`);

    try {
      const redesSnapshot = await db
        .collection("redes")
        .where("diaDaSemana", "==", diaDaSemanaHoje)
        .get();

      if (redesSnapshot.empty) {
        logger.info(`[CONTROLE DIÁRIO] Nenhuma rede com reunião hoje (dia ${diaDaSemanaHoje}).`);
        return;
      }

      const batch = db.batch();

      redesSnapshot.forEach((doc) => {
        const nomeRede = doc.data().nome as string;
        if (!nomeRede) return;

        const docId = `${dataFormatada.replace(/\//g, "-")}_${nomeRede}`;
        const controleRef = db.collection("controleRelatorios").doc(docId);
        batch.set(
          controleRef,
          { idRede: nomeRede, dataEsperada: dataFormatada, status: "pendente" },
          { merge: true }
        );
      });

      await batch.commit();
      logger.info(
        `[CONTROLE DIÁRIO] ${redesSnapshot.size} docs de controle criados para ${dataFormatada}.`
      );
    } catch (error) {
      logger.error("[CONTROLE DIÁRIO] Erro:", error);
    }
  }
);

// =================================================================================
// AGENDADA 2 — Lembrete do dia da reunião
// Roda a cada 10 minutos e verifica quais redes têm horário de reunião = agora/próximo.
// O horário de cada rede fica salvo em Firestore como "horarioReuniao" (ex: "19:30").
// Evita notificações duplicadas usando o campo "lembreteReuniaoEnviado: true" no controle diário.
// =================================================================================

export const lembreteDiaDeReuniao = onSchedule(
  {
    schedule: "every 10 minutes",
    timeZone: "America/Sao_Paulo",
    region: "southamerica-east1",
  },
  async () => {
    const agora = new Date();
    const tz = "America/Sao_Paulo";
    const dataFormatada = format(agora, "dd/MM/yyyy", { timeZone: tz });

    // Pega o dia e hora no fuso local de São Paulo de forma segura
    const isoDay = Number(format(agora, "i", { timeZone: tz }));
    const diaDaSemanaHoje = isoDay === 7 ? 1 : isoDay + 1; // Domingo=1 ... Sábado=7
    const horaAgora = Number(format(agora, "H", { timeZone: tz }));
    const minAgora = Number(format(agora, "m", { timeZone: tz }));
    const minutosAgoraAbs = horaAgora * 60 + minAgora;

    logger.info(
      `[LEMBRETE REUNIÃO] Verificando redes para o dia ${diaDaSemanaHoje} no horário atual (${horaAgora}:${minAgora}).`
    );

    try {
      // Busca redes com reunião hoje
      const redesSnapshot = await db
        .collection("redes")
        .where("diaDaSemana", "==", diaDaSemanaHoje)
        .get();

      if (redesSnapshot.empty) {
        logger.info(`[LEMBRETE REUNIÃO] Nenhuma rede com reunião agendada para hoje (dia ${diaDaSemanaHoje}).`);
        return;
      }

      for (const redeDoc of redesSnapshot.docs) {
        const nomeRede = redeDoc.data().nome as string;
        const horarioReuniao = redeDoc.data().horarioReuniao as string; // formato "HH:MM"
        if (!nomeRede || !horarioReuniao) continue;

        const [h, m] = horarioReuniao.split(":").map(Number);
        if (isNaN(h) || isNaN(m)) continue;

        const minutosReuniaoAbs = h * 60 + m;

        // Se a reunião já começou ou começa em até 10 minutos (e no máximo 30 minutos de atraso)
        // Isso dá uma margem flexível caso a função atrase ou execute perto da hora.
        if (minutosAgoraAbs >= minutosReuniaoAbs - 10 && minutosAgoraAbs < minutosReuniaoAbs + 30) {
          // Verifica se já enviamos o lembrete hoje usando o controle diário
          const docId = `${dataFormatada.replace(/\//g, "-")}_${nomeRede}`;
          const controleRef = db.collection("controleRelatorios").doc(docId);
          const controleDoc = await controleRef.get();

          if (controleDoc.exists && controleDoc.data()?.lembreteReuniaoEnviado === true) {
            logger.info(`[LEMBRETE REUNIÃO] Lembrete já enviado hoje para a rede ${nomeRede}.`);
            continue;
          }

          logger.info(`[LEMBRETE REUNIÃO] Disparando lembrete para a rede ${nomeRede} (horário configurado: ${horarioReuniao}).`);

          // Busca secretários E líderes da rede
          const [secretariosSnap, lideresSnap] = await Promise.all([
            db.collection("usuarios").where(`funcoes.${nomeRede}`, "==", "secretario").get(),
            db.collection("usuarios").where(`funcoes.${nomeRede}`, "==", "lider").get(),
          ]);

          const tokens: string[] = [
            ...secretariosSnap.docs.map((d) => d.data().fcmToken),
            ...lideresSnap.docs.map((d) => d.data().fcmToken),
          ].filter(Boolean);

          if (tokens.length > 0) {
            await enviarNotificacao(
              tokens,
              `📅 Reunião da ${nomeRede} hoje!`,
              `Olá! A reunião da sua rede começa às ${horarioReuniao}. Não esqueça de preencher o relatório ao final. 😉`,
              { tipo: "lembrete_reuniao", idRede: nomeRede }
            );
            logger.info(
              `[LEMBRETE REUNIÃO] Notificação enviada para ${tokens.length} responsável(eis) da ${nomeRede}.`
            );
          } else {
            logger.warn(`[LEMBRETE REUNIÃO] Nenhum token encontrado para a ${nomeRede}.`);
          }

          // Marca o controle diário para evitar duplicações de envio
          await controleRef.set(
            {
              idRede: nomeRede,
              dataEsperada: dataFormatada,
              lembreteReuniaoEnviado: true,
            },
            { merge: true }
          );
        }
      }
    } catch (error) {
      logger.error("[LEMBRETE REUNIÃO] Erro:", error);
    }
  }
);

// =================================================================================
// AGENDADA 3 — Cobrança de relatório pendente (todo dia às 10:00)
// Notifica APENAS líderes e secretários — não o pastor.
// =================================================================================

export const lembreteDePendencia = onSchedule(
  {
    schedule: "every day 10:00",
    timeZone: "America/Sao_Paulo",
    region: "southamerica-east1",
  },
  async () => {
    logger.info("[PENDÊNCIA] Verificando relatórios pendentes.");

    const agora = new Date();
    // Ontem em milissegundos
    const dataFormatadaOntem = format(
      new Date(agora.getTime() - 24 * 60 * 60 * 1000),
      "dd/MM/yyyy",
      { timeZone: "America/Sao_Paulo" }
    );

    try {
      const pendentesSnapshot = await db
        .collection("controleRelatorios")
        .where("dataEsperada", "==", dataFormatadaOntem)
        .where("status", "==", "pendente")
        .get();

      if (pendentesSnapshot.empty) {
        logger.info(`[PENDÊNCIA] Nenhum relatório pendente de ontem (${dataFormatadaOntem}).`);
        return;
      }

      for (const doc of pendentesSnapshot.docs) {
        const nomeRede = doc.data().idRede as string;
        if (!nomeRede) continue;

        const [secretariosSnap, lideresSnap] = await Promise.all([
          db.collection("usuarios").where(`funcoes.${nomeRede}`, "==", "secretario").get(),
          db.collection("usuarios").where(`funcoes.${nomeRede}`, "==", "lider").get(),
        ]);

        const tokensResponsaveis: string[] = [
          ...secretariosSnap.docs.map((d) => d.data().fcmToken),
          ...lideresSnap.docs.map((d) => d.data().fcmToken),
        ].filter(Boolean);

        if (tokensResponsaveis.length > 0) {
          await enviarNotificacao(
            tokensResponsaveis,
            `⚠️ Relatório Pendente: ${nomeRede}`,
            `O relatório da rede ${nomeRede} de ${dataFormatadaOntem} ainda não foi enviado. Por favor, preencha o mais rápido possível.`,
            { tipo: "relatorio_pendente", idRede: nomeRede }
          );
          logger.info(`[PENDÊNCIA] Notificação de atraso enviada para os responsáveis da ${nomeRede}.`);
        } else {
          logger.warn(`[PENDÊNCIA] Nenhum token de responsável encontrado para a ${nomeRede}.`);
        }
      }
    } catch (error) {
      logger.error("[PENDÊNCIA] Erro:", error);
    }
  }
);

// =================================================================================
// GATILHO 4 — Notifica o LÍDER e SECRETÁRIO quando o PASTOR visualiza um relatório
// =================================================================================

export const notificarVisualizacaoRelatorio = onDocumentCreated(
  {
    document: "visualizacoesRelatorio/{visualizacaoId}",
    minInstances: 1, // Mantém a instância quente para eliminar o cold start
  },
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      logger.error("Nenhum dado associado ao evento de visualização.");
      return;
    }

    const dados = snapshot.data();
    const idRede = dados.idRede as string;
    const dataReuniao = dados.dataReuniao as string;
    const pastorUid = dados.pastorUid as string;

    if (!idRede || !dataReuniao) {
      logger.error("Visualização sem idRede ou dataReuniao.", dados);
      return;
    }

    logger.info(
      `[VISUALIZAÇÃO DE RELATÓRIO] Rede: ${idRede} | Data: ${dataReuniao} | Pastor UID: ${pastorUid}`
    );

    try {
      // Busca nome do pastor na coleção de usuários
      let pastorNome = "O Pastor";
      if (pastorUid) {
        const pastorDoc = await db.collection("usuarios").doc(pastorUid).get();
        if (pastorDoc.exists) {
          pastorNome = pastorDoc.data()?.nome || "O Pastor";
        }
      }

      // Busca secretários E líderes da rede
      const [secretariosSnap, lideresSnap] = await Promise.all([
        db.collection("usuarios").where(`funcoes.${idRede}`, "==", "secretario").get(),
        db.collection("usuarios").where(`funcoes.${idRede}`, "==", "lider").get(),
      ]);

      const tokens: string[] = [
        ...secretariosSnap.docs.map((d) => d.data().fcmToken),
        ...lideresSnap.docs.map((d) => d.data().fcmToken),
      ].filter(Boolean);

      if (tokens.length > 0) {
        await enviarNotificacao(
          tokens,
          `👀 Relatório Visualizado!`,
          `${pastorNome} visualizou o relatório da ${idRede} referente a ${dataReuniao}.`,
          { tipo: "relatorio_visualizado", idRede, dataReuniao }
        );
        logger.info(
          `[VISUALIZAÇÃO DE RELATÓRIO] Notificação enviada para ${tokens.length} responsável(eis) da ${idRede}.`
        );
      } else {
        logger.warn(`[VISUALIZAÇÃO DE RELATÓRIO] Nenhum token encontrado para a ${idRede}.`);
      }
    } catch (error) {
      logger.error("[VISUALIZAÇÃO DE RELATÓRIO] Erro ao processar gatilho:", error);
    }
  }
);

// =================================================================================
// GATILHO 5 — Cobrança Manual de relatório pelo Pastor
// =================================================================================
export const notificarCobrancaManual = onDocumentCreated(
  {
    document: "cobrancasManual/{cobrancaId}",
    minInstances: 1, // High responsiveness to ensure quick notifications
  },
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      logger.error("Nenhum dado associado ao evento de cobrança manual.");
      return;
    }

    const dados = snapshot.data();
    const idRede = dados.idRede as string;
    const dataReuniao = dados.dataReuniao as string;
    const pastorUid = dados.pastorUid as string;

    if (!idRede || !dataReuniao) {
      logger.error("Cobrança manual sem idRede ou dataReuniao.", dados);
      return;
    }

    logger.info(
      `[COBRANÇA MANUAL] Rede: ${idRede} | Data: ${dataReuniao} | Pastor UID: ${pastorUid}`
    );

    try {
      // 1. Busca nome do pastor na coleção de usuários
      let pastorNome = "O Pastor";
      if (pastorUid) {
        const pastorDoc = await db.collection("usuarios").doc(pastorUid).get();
        if (pastorDoc.exists) {
          pastorNome = pastorDoc.data()?.nome || "O Pastor";
        }
      }

      // 2. Busca secretários E líderes da rede
      const [secretariosSnap, lideresSnap] = await Promise.all([
        db.collection("usuarios").where(`funcoes.${idRede}`, "==", "secretario").get(),
        db.collection("usuarios").where(`funcoes.${idRede}`, "==", "lider").get(),
      ]);

      const tokens: string[] = [
        ...secretariosSnap.docs.map((d) => d.data().fcmToken),
        ...lideresSnap.docs.map((d) => d.data().fcmToken),
      ].filter(Boolean);

      // 3. Envia notificação de alta prioridade
      if (tokens.length > 0) {
        await enviarNotificacao(
          tokens,
          `⚠️ Relatório Pendente: ${idRede}`,
          `Olá! ${pastorNome} solicitou o envio do relatório da ${idRede} de ${dataReuniao}. Por favor, preencha o quanto antes! 😉`,
          { tipo: "cobranca_manual", idRede, dataReuniao }
        );
        logger.info(
          `[COBRANÇA MANUAL] Notificação de cobrança enviada para ${tokens.length} responsável(eis) da ${idRede}.`
        );
      } else {
        logger.warn(`[COBRANÇA MANUAL] Nenhum token de responsável encontrado para a ${idRede}.`);
      }
    } catch (error) {
      logger.error("[COBRANÇA MANUAL] Erro ao processar gatilho:", error);
    }
  }
);