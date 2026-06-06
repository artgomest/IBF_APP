import { auth, db } from "./firebase-config.js";
import { createUserWithEmailAndPassword } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import { collection, addDoc, getDocs, serverTimestamp } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";

document.addEventListener("DOMContentLoaded", () => {
    carregarRedes();
    carregarLinkDownloadApp();

    const formAcesso = document.getElementById("form-acesso");
    if (formAcesso) {
        formAcesso.addEventListener("submit", processarCadastro);
    }
});

async function carregarRedes() {
    const selectRede = document.getElementById("a-rede");
    if (!selectRede) return;

    try {
        const redesSnapshot = await getDocs(collection(db, "redes"));
        selectRede.innerHTML = '<option value="">Selecione a Rede</option>';
        
        const redes = [];
        redesSnapshot.forEach((doc) => {
            redes.push({ id: doc.id, nome: doc.data().nome });
        });
        
        redes.sort((a, b) => a.nome.localeCompare(b.nome));
        
        redes.forEach(rede => {
            const option = document.createElement("option");
            option.value = rede.nome;
            option.textContent = rede.nome;
            selectRede.appendChild(option);
        });
    } catch (error) {
        console.error("Erro ao carregar redes:", error);
        selectRede.innerHTML = '<option value="">Erro ao carregar redes</option>';
    }
}

async function carregarLinkDownloadApp() {
    try {
        const response = await fetch('/version.json');
        if (response.ok) {
            const data = await response.json();
            if (data && data.linkDownload) {
                const btns = document.querySelectorAll('.btn-download-app');
                btns.forEach(btn => {
                    btn.href = data.linkDownload;
                });
            }
        }
    } catch (error) {
        console.error("Erro ao buscar versão do app:", error);
    }
}

function showFeedback(msg, isError = false) {
    const feedbackEl = document.getElementById("a-feedback-msg");
    if (!feedbackEl) return;
    
    feedbackEl.textContent = msg;
    feedbackEl.style.display = "block";
    feedbackEl.style.color = isError ? "var(--danger)" : "var(--primary)";
}

async function processarCadastro(e) {
    e.preventDefault();
    
    const nome = document.getElementById("a-nome").value.trim();
    const email = document.getElementById("a-email").value.trim();
    const senha = document.getElementById("a-senha").value;
    const papel = document.getElementById("a-papel").value;
    const rede = document.getElementById("a-rede").value;
    const btnSubmit = document.getElementById("btn-submit");

    if (!nome || !email || !senha || !papel || !rede) {
        showFeedback("Preencha todos os campos obrigatórios.", true);
        return;
    }

    if (senha.length < 6) {
        showFeedback("A senha deve ter no mínimo 6 caracteres.", true);
        return;
    }

    btnSubmit.disabled = true;
    btnSubmit.textContent = "Criando conta...";

    try {
        // 1. Criar o usuário no Firebase Auth
        const userCredential = await createUserWithEmailAndPassword(auth, email, senha);
        const user = userCredential.user;

        // 2. Salvar solicitação no Firestore
        await addDoc(collection(db, "solicitacoesCadastro"), {
            uid: user.uid,
            nome: nome,
            email: email,
            papelSolicitado: papel,
            redeId: rede,
            status: "pendente",
            tipo: "acesso_app",
            timestamp: serverTimestamp()
        });

        // 3. Sucesso! Esconder form e mostrar tela de sucesso
        document.getElementById("form-container").classList.add("hidden");
        document.getElementById("success-container").classList.remove("hidden");

    } catch (error) {
        console.error("Erro ao cadastrar:", error);
        
        if (error.code === 'auth/email-already-in-use') {
            try {
                // Usuário já existe no Firebase Auth. Vamos salvar apenas a solicitação no Firestore.
                await addDoc(collection(db, "solicitacoesCadastro"), {
                    uid: "existente",
                    nome: nome,
                    email: email,
                    papelSolicitado: papel,
                    redeId: rede,
                    status: "pendente",
                    tipo: "acesso_app",
                    timestamp: serverTimestamp()
                });

                document.getElementById("form-container").classList.add("hidden");
                document.getElementById("success-container").classList.remove("hidden");
                return;
            } catch (firestoreError) {
                console.error("Erro ao salvar solicitação existente:", firestoreError);
            }
        }
        
        btnSubmit.disabled = false;
        btnSubmit.textContent = "Criar Conta";
        
        let msg = "Erro ao criar conta. Tente novamente mais tarde.";
        if (error.code === 'auth/weak-password') {
            msg = "A senha é muito fraca.";
        } else if (error.code === 'auth/invalid-email') {
            msg = "E-mail inválido.";
        }
        showFeedback(msg, true);
    }
}
