import { criarControleDiario, notificarPastorNovoRelatorio } from "./index";

// Define outer mock functions so we can mock their behavior in each test
const mockCollection = jest.fn();
const mockBatch = {
  set: jest.fn(),
  commit: jest.fn(() => Promise.resolve()),
};
const mockMessaging = jest.fn();

jest.mock("firebase-admin", () => ({
  initializeApp: jest.fn(),
  firestore: jest.fn(() => ({
    collection: (col: string) => mockCollection(col),
    batch: () => mockBatch,
  })),
  messaging: () => mockMessaging(),
}));

describe("Cloud Functions", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("criarControleDiario", () => {
    it("should create control documents for networks with meetings today", async () => {
      // Mock collection search for networks with meetings today
      mockCollection.mockImplementation((col: string) => {
        if (col === "redes") {
          return {
            where: jest.fn(() => ({
              get: jest.fn(() => Promise.resolve({
                empty: false,
                size: 2,
                docs: [
                  { id: "Rede 1", data: () => ({ nome: "Rede 1" }) },
                  { id: "Rede 2", data: () => ({ nome: "Rede 2" }) },
                ],
                forEach: (callback: any) => {
                  callback({ id: "Rede 1", data: () => ({ nome: "Rede 1" }) });
                  callback({ id: "Rede 2", data: () => ({ nome: "Rede 2" }) });
                }
              })),
            })),
          };
        }
        if (col === "controleRelatorios") {
          return {
            doc: jest.fn(() => ({
              set: jest.fn(),
            })),
          };
        }
        return {};
      });

      // Call the raw handler directly
      await (criarControleDiario as any).run({} as any);

      expect(mockBatch.set).toHaveBeenCalledTimes(2);
      expect(mockBatch.commit).toHaveBeenCalledTimes(1);
    });
  });

  describe("notificarPastorNovoRelatorio", () => {
    it("should send a notification to pastors when a new report is created", async () => {
      const sendEachForMulticastMock = jest.fn(() => Promise.resolve({ successCount: 1, failureCount: 0 }));
      mockMessaging.mockReturnValue({
        sendEachForMulticast: sendEachForMulticastMock,
      });

      // Mock users search (where funcoes.geral == pastor) and controleRelatorios update
      const docUpdateMock = jest.fn(() => Promise.resolve());
      const docMock = jest.fn(() => ({ update: docUpdateMock }));
      const whereMock = jest.fn(() => ({
        get: jest.fn(() => Promise.resolve({
          docs: [{ data: () => ({ fcmToken: "pastor_token_1" }) }],
        })),
      }));

      mockCollection.mockImplementation((col: string) => {
        if (col === "controleRelatorios") return { doc: docMock };
        return { where: whereMock };
      });

      const event = {
        data: {
          data: () => ({
            autorNome: "Líder Teste",
            idRede: "Rede Teste",
            dataReuniao: "01/01/2025",
          }),
        },
      };

      // Call the raw handler directly
      await (notificarPastorNovoRelatorio as any).run(event as any);

      expect(whereMock).toHaveBeenCalledWith("funcoes.geral", "==", "pastor");
      expect(sendEachForMulticastMock).toHaveBeenCalled();
    });
  });
});
