const admin = require('firebase-admin');
admin.initializeApp({
  projectId: 'ibfapp-c4078'
});
const db = admin.firestore();

async function run() {
  console.log("Checking users...");
  const snap = await db.collection('usuarios').get();
  snap.forEach(d => {
    console.log('USER_DOC:', d.id, JSON.stringify(d.data()));
  });
  console.log("Done checking users.");
}

run().catch(console.error);
