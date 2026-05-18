const admin = require("firebase-admin");
admin.initializeApp({
  projectId: "ibfapp-c4078"
});
const db = admin.firestore();

async function run() {
  console.log("=== REDES ===");
  const redes = await db.collection("redes").get();
  redes.forEach(doc => {
    console.log(doc.id, "=>", doc.data());
  });

  console.log("\n=== USUARIOS ===");
  const usuarios = await db.collection("usuarios").get();
  usuarios.forEach(doc => {
    console.log(doc.id, "=>", {
      nome: doc.data().nome,
      fcmToken: doc.data().fcmToken ? "SIM" : "NÃO",
      funcoes: doc.data().funcoes
    });
  });

  console.log("\n=== CONTROLE RELATORIOS (Últimos 5) ===");
  const controle = await db.collection("controleRelatorios").orderBy("dataEsperada", "desc").limit(5).get();
  controle.forEach(doc => {
    console.log(doc.id, "=>", doc.data());
  });
}

run().catch(console.error);
