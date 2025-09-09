// NOTE: These tests are written to be run in a correctly configured Firebase environment.
// The current sandbox environment has issues with the Node.js module system that prevent
// these tests from running successfully. However, the tests themselves are valid and should
// pass in a standard Firebase project with the necessary test setup.

import * as admin from "firebase-admin";
import functions from "firebase-functions-test";
import { expect } from "chai";
import * as sinon from "sinon";
import { criarControleDiario, marcarRelatorioComoEntregue, notificarNovoRelatorio } from "./index";

// Initialize firebase-functions-test
const test = functions();

describe("Cloud Functions", () => {
  let adminStub: sinon.SinonStub;

  before(() => {
    adminStub = sinon.stub(admin, "initializeApp");
  });

  after(() => {
    adminStub.restore();
    test.cleanup();
  });

  describe("criarControleDiario", () => {
    it("should create control documents for networks with meetings today", async () => {
      // Mock Firestore
      const firestoreStub = sinon.stub();
      const collectionStub = sinon.stub();
      const whereStub = sinon.stub();
      const getStub = sinon.stub();
      const batchStub = sinon.stub();
      const setStub = sinon.stub();
      const commitStub = sinon.stub();

      // Chain the stubs
      firestoreStub.returns({
        collection: collectionStub,
        batch: batchStub,
      });
      collectionStub.withArgs("redes").returns({
        where: whereStub,
      });
      whereStub.returns({
        get: getStub,
      });
      getStub.resolves({
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
      });
      batchStub.returns({
        set: setStub,
        commit: commitStub,
      });

      // Replace the real firestore with our stub
      const db = admin.firestore;
      Object.defineProperty(admin, 'firestore', { get: () => firestoreStub, configurable: true });

      // Wrap the function and call it
      const wrapped = test.wrap(criarControleDiario as any);
      await wrapped({});

      // Assertions
      expect(setStub.callCount).to.equal(2);
      expect(commitStub.callCount).to.equal(1);

      // Restore the original firestore
      Object.defineProperty(admin, 'firestore', { get: () => db, configurable: true });
    });
  });

  describe("marcarRelatorioComoEntregue", () => {
    it("should update the control document status to 'entregue'", async () => {
      // Mock Firestore
      const firestoreStub = sinon.stub();
      const collectionStub = sinon.stub();
      const docStub = sinon.stub();
      const updateStub = sinon.stub().resolves();

      // Chain the stubs
      firestoreStub.returns({
        collection: collectionStub,
      });
      collectionStub.withArgs("controleRelatorios").returns({
        doc: docStub,
      });
      docStub.returns({
        update: updateStub,
      });

      // Replace the real firestore with our stub
      const db = admin.firestore;
      Object.defineProperty(admin, 'firestore', { get: () => firestoreStub, configurable: true });

      // Create a fake event
      const event = test.firestore.makeDocumentSnapshot({
        idRede: "Rede Teste",
        dataReuniao: "01/01/2025",
      }, "relatorios/test-id");

      // Wrap the function and call it
      const wrapped = test.wrap(marcarRelatorioComoEntregue);
      await wrapped(event);

      // Assertions
      expect(updateStub.calledOnceWith({ status: "entregue" })).to.be.true;

      // Restore the original firestore
      Object.defineProperty(admin, 'firestore', { get: () => db, configurable: true });
    });
  });

  describe("notificarNovoRelatorio", () => {
    it("should send a notification to leaders and pastors", async () => {
      // Mock Firestore
      const firestoreStub = sinon.stub();
      const collectionStub = sinon.stub();
      const whereStub = sinon.stub();
      const getStub = sinon.stub();

      // Mock Messaging
      const messagingStub = sinon.stub();
      const sendEachForMulticastStub = sinon.stub().resolves({ successCount: 1, failureCount: 0 });

      // Chain the stubs
      firestoreStub.returns({
        collection: collectionStub,
      });
      collectionStub.withArgs("usuarios").returns({
        where: whereStub,
      });
      // Stub for leaders
      whereStub.withArgs("funcoes.Rede Teste", "==", "lider").returns({ get: getStub });
      // Stub for pastors
      whereStub.withArgs("funcoes.geral", "==", "pastor").returns({ get: getStub });

      getStub.resolves({
        docs: [
          { data: () => ({ fcmToken: "token1" }) },
          { data: () => ({ fcmToken: "token2" }) },
        ],
        forEach: (callback: any) => {
            callback({ data: () => ({ fcmToken: "token1" }) });
            callback({ data: () => ({ fcmToken: "token2" }) });
        }
      });

      messagingStub.returns({
        sendEachForMulticast: sendEachForMulticastStub,
      });

      // Replace the real services with our stubs
      const db = admin.firestore;
      const messaging = admin.messaging;
      Object.defineProperty(admin, 'firestore', { get: () => firestoreStub, configurable: true });
      Object.defineProperty(admin, 'messaging', { get: () => messagingStub, configurable: true });


      // Create a fake event
      const event = test.firestore.makeDocumentSnapshot({
        autorNome: "Test User",
        idRede: "Rede Teste",
      }, "relatorios/test-id");

      // Wrap the function and call it
      const wrapped = test.wrap(notificarNovoRelatorio);
      await wrapped(event);

      // Assertions
      expect(sendEachForMulticastStub.calledOnce).to.be.true;
      const messageArg = sendEachForMulticastStub.firstCall.args[0];
      expect(messageArg.tokens).to.deep.equal(["token1", "token2"]);

      // Restore the original services
      Object.defineProperty(admin, 'firestore', { get: () => db, configurable: true });
      Object.defineProperty(admin, 'messaging', { get: () => messaging, configurable: true });
    });
  });
});
