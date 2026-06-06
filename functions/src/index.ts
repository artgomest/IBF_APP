import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";

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

// Funções Agendadas (Cron) foram migradas para o Worker AWS Lambda!

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

// =================================================================================
// GATILHO 6 — Processamento de Cadastro Manual por Admin
// =================================================================================
export const processarCadastroAdmin = onDocumentCreated(
  "admin_cadastros_pendentes/{docId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;

    const dados = snapshot.data();
    if (dados.status !== "pendente") return;

    const nome = dados.nome;
    const email = dados.email;
    const senha = dados.senha;
    const papel = dados.papel;
    const rede = dados.rede;
    const docId = event.params.docId;

    try {
      // 1. Criar o usuário no Firebase Auth
      const userRecord = await admin.auth().createUser({
        email: email,
        password: senha,
        displayName: nome,
      });

      // 2. Criar os dados iniciais do usuário no Firestore
      const uid = userRecord.uid;
      const chaveRede = rede === "geral" ? "geral" : rede;
      
      const funcoes = { [chaveRede]: papel };
      
      await db.collection("usuarios").doc(uid).set({
        nome: nome,
        email: email,
        funcoes: funcoes,
        fcmToken: "",
        redesSelecionadas: [rede]
      });

      // 3. Atualizar status para concluído, remover a senha e adicionar UID
      await db.collection("admin_cadastros_pendentes").doc(docId).update({
        status: "concluido",
        uidCriado: uid,
        senha: admin.firestore.FieldValue.delete(),
        dataConclusao: admin.firestore.FieldValue.serverTimestamp()
      });

      logger.info(`[CADASTRO ADMIN] Usuário ${email} criado com sucesso com UID ${uid}.`);
    } catch (error: any) {
      logger.error(`[CADASTRO ADMIN] Erro ao criar usuário ${email}:`, error);
      await db.collection("admin_cadastros_pendentes").doc(docId).update({
        status: "erro",
        mensagemErro: error.message || "Erro desconhecido",
      });
    }
  }
);