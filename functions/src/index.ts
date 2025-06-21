import * as functions from "firebase-functions";

export const helloWorld = functions.https.onRequest((request, response) => {
  functions.logger.info("A função helloWorld foi chamada!");
  response.send("Olá do Firebase! A função de teste está funcionando.");
});