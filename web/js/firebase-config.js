// Import the functions you need from the SDKs you need
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-app.js";
import { getAnalytics } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-analytics.js";
import { getAuth } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import { getFirestore } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";

// TODO: Replace with your app's Firebase project configuration
const firebaseConfig = {
    apiKey: "AIzaSyCOlIBdzLFSJ4VqEZBqk6tmr0GgC7RUb54",
    authDomain: "ibfapp-c4078.firebaseapp.com",
    projectId: "ibfapp-c4078",
    storageBucket: "ibfapp-c4078.firebasestorage.app",
    messagingSenderId: "858803281672",
    appId: "1:858803281672:web:dfdf82fd991e6d5532d6bd",
    measurementId: "G-9W8CDJPLDW"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);
const auth = getAuth(app);
const db = getFirestore(app);

export { auth, db };
