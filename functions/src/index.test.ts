// NOTE: These tests are written to be run in a correctly configured Firebase environment.
// The current sandbox environment has issues that make it difficult to test v2 Cloud Functions.
// The tests below demonstrate the general approach to testing these functions, but they
// may require a more complex mocking strategy to run successfully.

import * as admin from "firebase-admin";
import { criarControleDiario, marcarRelatorioComoEntregue, notificarNovoRelatorio } from "./index";

jest.mock("firebase-admin", () => ({
  initializeApp: jest.fn(),
  firestore: jest.fn(() => ({
    collection: jest.fn(() => ({
      where: jest.fn(() => ({
        get: jest.fn(() => Promise.resolve({
          empty: false,
          size: 2,
          docs: [
            { id: "Rede 1" },
            { id: "Rede 2" },
          ],
          forEach: (callback: any) => {
            callback({ id: "Rede 1" });
            callback({ id: "Rede 2" });
          }
        })),
      })),
    })),
    batch: jest.fn(() => ({
      set: jest.fn(),
      commit: jest.fn(() => Promise.resolve()),
    })),
  })),
}));

describe("Cloud Functions", () => {
  describe("criarControleDiario", () => {
    it("should create control documents for networks with meetings today", async () => {
      const firestore = admin.firestore();
      const batch = (firestore as any).batch();

      // The v2 scheduler functions expect a req object that is compatible with
      // express.Request. A simple mock is provided here.
      const mockReq = { header: () => undefined };
      await criarControleDiario(mockReq as any);

      expect(batch.set).toHaveBeenCalledTimes(2);
      expect(batch.commit).toHaveBeenCalledTimes(1);
    });
  });

  describe("marcarRelatorioComoEntregue", () => {
    it("should update the control document status to 'entregue'", async () => {
      const updateMock = jest.fn(() => Promise.resolve());
      const docMock = jest.fn(() => ({
        update: updateMock,
      }));

      (admin.firestore as unknown as jest.Mock).mockReturnValue({
        collection: jest.fn(() => ({
          doc: docMock,
        })),
      });

      const event = {
        data: {
          data: () => ({
            idRede: "Rede Teste",
            dataReuniao: "01/01/2025",
          }),
        },
      };

      await marcarRelatorioComoEntregue(event as any);

      expect(docMock).toHaveBeenCalledWith("01-01-2025_Rede Teste");
      expect(updateMock).toHaveBeenCalledWith({ status: "entregue" });
    });
  });

  describe("notificarNovoRelatorio", () => {
    it("should send a notification to leaders and pastors", async () => {
      const sendEachForMulticastMock = jest.fn(() => Promise.resolve({ successCount: 2 }));
      const messagingMock = jest.fn(() => ({
        sendEachForMulticast: sendEachForMulticastMock,
      }));

      const getMock = jest.fn()
        .mockResolvedValueOnce({
          docs: [{ data: () => ({ fcmToken: "token1" }) }],
          forEach: (callback: any) => callback({ data: () => ({ fcmToken: "token1" }) })
        })
        .mockResolvedValueOnce({
          docs: [{ data: () => ({ fcmToken: "token2" }) }],
          forEach: (callback: any) => callback({ data: () => ({ fcmToken: "token2" }) })
        });

      const whereMock = jest.fn(() => ({
        get: getMock,
      }));

      (admin.firestore as unknown as jest.Mock).mockReturnValue({
        collection: jest.fn(() => ({
          where: whereMock,
        })),
      });

      Object.defineProperty(admin, 'messaging', { get: () => messagingMock, configurable: true });

      const event = {
        data: {
          data: () => ({
            autorNome: "Test User",
            idRede: "Rede Teste",
          }),
        },
      };

      await notificarNovoRelatorio(event as any);

      expect(whereMock).toHaveBeenCalledWith("funcoes.Rede Teste", "==", "lider");
      expect(whereMock).toHaveBeenCalledWith("funcoes.geral", "==", "pastor");
      expect(sendEachForMulticastMock).toHaveBeenCalledWith({
        tokens: ["token1", "token2"],
        notification: {
          title: "Novo Relatório: Rede Teste",
          body: "O relatório da rede Rede Teste foi preenchido por Test User.",
        },
        android: { priority: "high", notification: { sound: "default" } },
        apns: { payload: { aps: { sound: "default", contentAvailable: true } }, headers: { "apns-priority": "10" } },
      });
    });
  });
});
