import { auth, db } from './firebase-config.js';
import {
    signInAnonymously,
    onAuthStateChanged,
    signOut
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import {
    collection,
    query,
    where,
    getDocs,
    doc,
    setDoc,
    getDoc
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";

// DOM Elements
const authContainer = document.getElementById('auth-container');
const formContainer = document.getElementById('form-container');
const authForm = document.getElementById('auth-form');
const memberForm = document.getElementById('member-form');
const feedbackMsg = document.getElementById('feedback-msg');
const btnLogout = document.getElementById('btn-logout');

// Current Member ID (Not Auth UID)
let currentMemberId = null;

// Form Inputs Mapping (IDs must match Firestore keys we want)
const formFields = [
    'nome', 'dataNascimento', 'sexo', 'escolaridade', 'rg', 'orgaoExpeditor', 'cpf', 'naturalidade',
    'celular', 'telefoneFixo', 'cep', 'cidade', 'rua', 'numero', 'bairro',
    'estadoCivil', 'dataCasamento', 'conjuge', 'mae', 'pai',
    'filhosDetalhes', 'dataBatismo', 'igrejaBatismo', 'tipoMembro',
    'aceitoPor', 'igrejaAnterior', 'pastorAnterior', 'cargosExercidos',
    'talentosMinisterios', 'desejaFuncao'
];

const checkFields = ['temFilhos', 'batizado'];

// --- INIT AUTH (Anonymous for Connection) ---
// We authorize the *device* anonymously to read firestore, 
// but we identify the *member* by CPF Query.
signInAnonymously(auth).catch(err => {
    console.error("Auth error:", err);
    if(err.code === 'auth/operation-not-allowed') {
        alert("Erro Crítico: Login Anônimo não está ativado no Firebase Console! O site não vai funcionar.");
    }
});

// --- INPUT MASKS & VIACEP ---

function applyCpfMask(e) {
    let v = e.target.value.replace(/\D/g, "");
    v = v.replace(/(\d{3})(\d)/, "$1.$2");
    v = v.replace(/(\d{3})(\d)/, "$1.$2");
    v = v.replace(/(\d{3})(\d{1,2})$/, "$1-$2");
    e.target.value = v;
}
document.getElementById('login-cpf').addEventListener('input', applyCpfMask);
const formCpf = document.getElementById('cpf');
if(formCpf) formCpf.addEventListener('input', applyCpfMask);

function maskPhone(e) {
    let v = e.target.value.replace(/\D/g, "");
    v = v.replace(/^(\d{2})(\d)/g, "($1) $2");
    v = v.replace(/(\d)(\d{4})$/, "$1-$2");
    e.target.value = v;
}
document.getElementById('celular').addEventListener('input', maskPhone);
document.getElementById('telefoneFixo').addEventListener('input', maskPhone);

const cepInput = document.getElementById('cep');
cepInput.addEventListener('input', function(e) {
    let value = e.target.value.replace(/\D/g, "");
    value = value.replace(/^(\d{5})(\d)/, "$1-$2");
    e.target.value = value;
});

cepInput.addEventListener('blur', async function(e) {
    const cep = e.target.value.replace(/\D/g, "");
    if(cep.length === 8) {
        try {
            const resp = await fetch(`https://viacep.com.br/ws/${cep}/json/`);
            const data = await resp.json();
            if(!data.erro) {
                document.getElementById('rua').value = data.logradouro || "";
                document.getElementById('bairro').value = data.bairro || "";
                document.getElementById('cidade').value = data.localidade || "";
            }
        } catch(err) {
            console.error("ViaCEP error:", err);
        }
    }
});

// --- UI TOGGLES ---
function showAuthUI() {
    authContainer.classList.remove('hidden');
    formContainer.classList.add('hidden');
    currentMemberId = null;
    authForm.reset();
}

function showFormUI() {
    authContainer.classList.add('hidden');
    formContainer.classList.remove('hidden');
    formContainer.classList.add('fade-in');
}

// --- CPF LOGIN LOGIC ---
authForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    // Check if auth system completed anonymous sign-in!
    if (!auth.currentUser) {
        alert("Conectando ao servidor... Tente novamente em alguns segundos. Se persistir, verifique a internet.");
        return;
    }

    const btn = authForm.querySelector('button');
    const originalText = btn.textContent;
    btn.textContent = "Verificando...";
    btn.disabled = true;

    // Get Raw CPF (numbers only) to match Android format
    const rawCpf = document.getElementById('login-cpf').value.replace(/\D/g, '');

    if (rawCpf.length !== 11) {
        alert("CPF deve ter 11 dígitos.");
        btn.textContent = originalText;
        btn.disabled = false;
        return;
    }

    try {
        // Query Firestore for this CPF
        const usersRef = collection(db, "usuarios");
        const q = query(usersRef, where("cpf", "==", rawCpf));
        const querySnapshot = await getDocs(q);

        if (querySnapshot.empty) {
            alert("CPF não encontrado. Solicite o cadastro ao seu líder.");
            btn.textContent = originalText;
            btn.disabled = false;
        } else {
            // Found Member
            const userDoc = querySnapshot.docs[0];
            currentMemberId = userDoc.id;
            loadMemberData(currentMemberId);
            showFormUI();
            // Reset button state silently as view changes
            btn.textContent = originalText;
            btn.disabled = false;
        }

    } catch (error) {
        console.error("Login Error:", error);
        alert("Erro ao verificar CPF: " + error.message);
        btn.textContent = originalText;
        btn.disabled = false;
    }
});

btnLogout.addEventListener('click', () => {
    showAuthUI();
});

// --- FORM LOGIC ---

// Toggle "Filhos" textarea
document.getElementById('temFilhos').addEventListener('change', (e) => {
    const group = document.getElementById('group-filhos');
    group.style.display = e.target.checked ? 'flex' : 'none';
});

// SAVE DATA
memberForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const user = auth.currentUser;
    // Note: We check currentMemberId, not just user, because we are editing a specific member doc
    if (!currentMemberId) return;

    const btn = document.getElementById('btn-save');
    btn.textContent = "Salvando...";
    btn.disabled = true;

    // Collect Data
    const dataToSave = {};

    formFields.forEach(id => {
        const el = document.getElementById(id);
        if (el) dataToSave[id] = el.value;
    });

    checkFields.forEach(id => {
        const el = document.getElementById(id);
        if (el) dataToSave[id] = el.checked;
    });

    try {
        await setDoc(doc(db, "usuarios", currentMemberId), dataToSave, { merge: true });
        showFeedback("Dados salvos com sucesso!", "success");
    } catch (error) {
        console.error(error);
        showFeedback("Erro ao salvar: " + error.message, "error");
    } finally {
        btn.textContent = "Salvar Alterações";
        btn.disabled = false;
    }
});

// LOAD DATA
async function loadMemberData(uid) {
    try {
        const docSnap = await getDoc(doc(db, "usuarios", uid));
        if (docSnap.exists()) {
            const data = docSnap.data();

            formFields.forEach(id => {
                if (data[id]) document.getElementById(id).value = data[id];
            });

            checkFields.forEach(id => {
                if (data[id]) document.getElementById(id).checked = data[id];
            });

            // Trigger UI updates based on loaded data
            document.getElementById('temFilhos').dispatchEvent(new Event('change'));
        }
    } catch (error) {
        console.error("Error loading doc:", error);
    }
}

function showFeedback(msg, type) {
    feedbackMsg.textContent = msg;
    feedbackMsg.className = `feedback ${type}`;
    setTimeout(() => {
        feedbackMsg.textContent = "";
    }, 3000);
}
