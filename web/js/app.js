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
    getDoc,
    writeBatch,
    deleteField
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
let currentMemberName = "";
let children = [];
let loadedChildIds = new Set();
let editingChildIndex = null;

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

const childForm = document.getElementById('child-form');
const childNameInput = document.getElementById('child-name');
const childBirthInput = document.getElementById('child-birth');
const childSexInput = document.getElementById('child-sex');
const childrenList = document.getElementById('children-list');

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
const cpfDisplay = document.getElementById('cpf-display');
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
    currentMemberName = "";
    children = [];
    loadedChildIds = new Set();
    editingChildIndex = null;
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

function normalizeText(value) {
    return (value || "")
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase();
}

function updateDependentFields() {
    const estadoCivil = normalizeText(document.getElementById('estadoCivil').value);
    const tipoMembro = normalizeText(document.getElementById('tipoMembro').value);
    const aceitoPor = normalizeText(document.getElementById('aceitoPor').value);
    const temFilhos = document.getElementById('temFilhos').checked;
    const batizado = document.getElementById('batizado').checked;
    const casado = estadoCivil.includes('casad') || estadoCivil.includes('uniao estavel');
    const membro = tipoMembro.includes('membro');
    const igrejaAnterior = aceitoPor.includes('transfer') ||
        aceitoPor.includes('reconcili') ||
        aceitoPor.includes('restaur');

    toggleFieldGroup('group-data-casamento', casado, ['dataCasamento']);
    toggleFieldGroup('group-conjuge', casado, ['conjuge']);
    toggleFieldGroup('group-filhos', temFilhos, []);
    toggleFieldGroup('group-data-batismo', batizado, ['dataBatismo']);
    toggleFieldGroup('group-igreja-batismo', batizado, ['igrejaBatismo']);
    toggleFieldGroup('group-aceito-por', membro, ['aceitoPor']);
    toggleFieldGroup('group-igreja-anterior', igrejaAnterior, ['igrejaAnterior', 'pastorAnterior']);

    renderChildren();
}

function toggleFieldGroup(groupId, visible, fieldIds) {
    const group = document.getElementById(groupId);
    if (!group) return;

    group.style.display = visible ? '' : 'none';
    if (!visible) {
        fieldIds.forEach(id => {
            const field = document.getElementById(id);
            if (field) field.value = "";
        });
    }
}

['estadoCivil', 'tipoMembro', 'aceitoPor', 'temFilhos', 'batizado'].forEach(id => {
    document.getElementById(id).addEventListener('change', updateDependentFields);
});

document.getElementById('btn-add-child').addEventListener('click', () => openChildForm());
document.getElementById('btn-cancel-child').addEventListener('click', closeChildForm);
document.getElementById('btn-save-child').addEventListener('click', saveChildFromForm);

function openChildForm(index = null) {
    editingChildIndex = index;
    const child = index !== null ? children[index] : null;

    childNameInput.value = child?.nome || "";
    childBirthInput.value = toInputDate(child?.dataNascimento || "");
    childSexInput.value = child?.sexo || "";
    childForm.classList.remove('hidden');
    childNameInput.focus();
}

function closeChildForm() {
    editingChildIndex = null;
    childForm.classList.add('hidden');
    childNameInput.value = "";
    childBirthInput.value = "";
    childSexInput.value = "";
}

function saveChildFromForm() {
    const age = calculateAge(childBirthInput.value);
    const child = {
        id: editingChildIndex !== null ? children[editingChildIndex].id : null,
        nome: childNameInput.value.trim(),
        dataNascimento: childBirthInput.value,
        sexo: childSexInput.value
    };

    if (!child.nome || !child.dataNascimento || !child.sexo) {
        showFeedback("Preencha nome, nascimento e sexo do filho.", "error");
        return;
    }

    if (age === null) {
        showFeedback("Data de nascimento do filho inválida.", "error");
        return;
    }

    if (editingChildIndex !== null) {
        children[editingChildIndex] = child;
    } else {
        children.push(child);
    }

    closeChildForm();
    renderChildren();
}

function renderChildren() {
    if (!childrenList) return;
    childrenList.innerHTML = "";

    if (!document.getElementById('temFilhos').checked) {
        return;
    }

    if (children.length === 0) {
        childrenList.innerHTML = '<p class="auth-helper">Nenhum filho cadastrado.</p>';
        return;
    }

    children.forEach((child, index) => {
        const card = document.createElement('div');
        card.className = 'child-card';
        const age = calculateAge(child.dataNascimento);
        const network = calculateChildNetwork(child);
        card.innerHTML = `
            <div>
                <strong>${child.nome}</strong>
                <span>${formatDate(child.dataNascimento)} • ${age ?? '-'} anos • ${network}</span>
            </div>
            <div class="child-card-actions">
                <button type="button" class="btn-secondary" data-action="edit" data-index="${index}">Editar</button>
                <button type="button" class="btn-text" data-action="remove" data-index="${index}">Remover</button>
            </div>
        `;
        childrenList.appendChild(card);
    });
}

childrenList.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-action]');
    if (!button) return;

    const index = Number(button.dataset.index);
    if (button.dataset.action === 'edit') {
        openChildForm(index);
    } else {
        children.splice(index, 1);
        renderChildren();
    }
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

    const validationMessage = validateMemberForm();
    if (validationMessage) {
        showFeedback(validationMessage, "error");
        btn.textContent = "Salvar AlteraÃ§Ãµes";
        btn.disabled = false;
        return;
    }

    dataToSave['cadastroCompleto'] = true;
    dataToSave['filhos'] = document.getElementById('temFilhos').checked ? children.map(child => childToParentData(child)) : [];
    dataToSave['filhosDetalhes'] = dataToSave['filhos'].map(child =>
        `${child.nome} (${formatDate(child.dataNascimento)}) - ${child.rede}`
    ).join('\n');

    try {
        await saveMemberAndChildren(dataToSave);
        showFeedback("Dados salvos com sucesso!", "success");
    } catch (error) {
        console.error(error);
        showFeedback("Erro ao salvar: " + error.message, "error");
    } finally {
        btn.textContent = "Salvar Alterações";
        btn.disabled = false;
    }
});

function validateMemberForm() {
    if (isVisible('group-data-casamento') && !document.getElementById('dataCasamento').value) {
        return "Informe a data de casamento.";
    }
    if (isVisible('group-conjuge') && !document.getElementById('conjuge').value.trim()) {
        return "Informe o nome do cônjuge.";
    }
    if (document.getElementById('temFilhos').checked && children.length === 0) {
        return "Cadastre pelo menos um filho.";
    }
    if (isVisible('group-data-batismo') && !document.getElementById('dataBatismo').value) {
        return "Informe a data de batismo.";
    }
    if (isVisible('group-igreja-batismo') && !document.getElementById('igrejaBatismo').value.trim()) {
        return "Informe a igreja de batismo.";
    }
    if (isVisible('group-igreja-anterior') && !document.getElementById('igrejaAnterior').value.trim()) {
        return "Informe a igreja anterior.";
    }
    if (isVisible('group-igreja-anterior') && !document.getElementById('pastorAnterior').value.trim()) {
        return "Informe o pastor anterior.";
    }
    return "";
}

function isVisible(id) {
    const element = document.getElementById(id);
    return element && element.style.display !== 'none';
}

async function saveMemberAndChildren(dataToSave) {
    const batch = writeBatch(db);
    const memberRef = doc(db, "usuarios", currentMemberId);
    const persistedChildren = [];
    const activeChildIds = new Set();

    if (document.getElementById('temFilhos').checked) {
        children.forEach(child => {
            const childRef = child.id ? doc(db, "usuarios", child.id) : doc(collection(db, "usuarios"));
            const childData = childToChildDocument(child, childRef.id);
            activeChildIds.add(childRef.id);
            persistedChildren.push(childToParentData({ ...child, id: childRef.id }));
            batch.set(childRef, childData, { merge: true });
        });
    }

    loadedChildIds.forEach(id => {
        if (!activeChildIds.has(id)) {
            batch.delete(doc(db, "usuarios", id));
        }
    });

    dataToSave.filhos = persistedChildren;
    dataToSave.filhosDetalhes = persistedChildren.map(child =>
        `${child.nome} (${formatDate(child.dataNascimento)}) - ${child.rede}`
    ).join('\n');
    batch.set(memberRef, dataToSave, { merge: true });
    await batch.commit();

    children = persistedChildren.map(child => ({
        id: child.id,
        nome: child.nome,
        dataNascimento: child.dataNascimento,
        sexo: child.sexo
    }));
    loadedChildIds = new Set(children.map(child => child.id).filter(Boolean));
    renderChildren();
}

// LOAD DATA
async function loadMemberData(uid) {
    try {
        const docSnap = await getDoc(doc(db, "usuarios", uid));
        if (docSnap.exists()) {
            const data = docSnap.data();
            currentMemberName = data.nome || "";

            formFields.forEach(id => {
                const field = document.getElementById(id);
                if (field && data[id]) field.value = data[id];
            });
            if (cpfDisplay) {
                cpfDisplay.value = data.cpf || "";
            }

            checkFields.forEach(id => {
                if (data[id]) document.getElementById(id).checked = data[id];
            });

            children = Array.isArray(data.filhos) ? data.filhos.map(child => ({
                id: child.id || null,
                nome: child.nome || "",
                dataNascimento: child.dataNascimento || "",
                sexo: child.sexo || ""
            })).filter(child => child.nome && child.dataNascimento) : [];
            loadedChildIds = new Set(children.map(child => child.id).filter(Boolean));

            // Trigger UI updates based on loaded data
            updateDependentFields();
        }
    } catch (error) {
        console.error("Error loading doc:", error);
    }
}

function childToParentData(child) {
    const age = calculateAge(child.dataNascimento);
    return {
        id: child.id || null,
        nome: child.nome,
        dataNascimento: child.dataNascimento,
        sexo: child.sexo,
        idade: age,
        rede: calculateChildNetwork(child)
    };
}

function childToChildDocument(child, childId) {
    const network = calculateChildNetwork(child);
    return {
        nome: child.nome,
        dataNascimento: child.dataNascimento,
        sexo: child.sexo,
        estadoCivil: deleteField(),
        idade: calculateAge(child.dataNascimento) ?? 0,
        responsavelUid: currentMemberId,
        responsavelNome: document.getElementById('nome').value.trim() || currentMemberName,
        tipoCadastro: "filho",
        cadastroCompleto: true,
        funcoes: {
            [network]: "Membro"
        }
    };
}

function calculateChildNetwork(child) {
    const age = calculateAge(child.dataNascimento);
    const sex = normalizeText(child.sexo);

    if (age === null) return "Rede de Jovens";
    if (age < 9) return "Rede de Crianças";
    if (age >= 9 && age <= 13) return "Rede de Juniores";
    if (age < 18) return "Rede de Jovens";
    if (sex.includes('masculino')) return "Rede de Homens";
    if (sex.includes('feminino')) return "Rede de Mulheres";
    return "Rede de Jovens";
}

function calculateAge(value) {
    const date = parseDate(value);
    if (!date) return null;

    const today = new Date();
    let age = today.getFullYear() - date.getFullYear();
    const birthdayThisYear = new Date(today.getFullYear(), date.getMonth(), date.getDate());
    if (today < birthdayThisYear) age--;
    return age >= 0 ? age : null;
}

function parseDate(value) {
    if (!value) return null;
    if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
        const [year, month, day] = value.split('-').map(Number);
        return new Date(year, month - 1, day);
    }
    if (/^\d{2}\/\d{2}\/\d{4}$/.test(value)) {
        const [day, month, year] = value.split('/').map(Number);
        return new Date(year, month - 1, day);
    }
    return null;
}

function toInputDate(value) {
    if (/^\d{4}-\d{2}-\d{2}$/.test(value || "")) return value;
    const date = parseDate(value);
    if (!date) return "";
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

function formatDate(value) {
    const date = parseDate(value);
    if (!date) return value || "";
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    return `${day}/${month}/${year}`;
}

function showFeedback(msg, type) {
    feedbackMsg.textContent = msg;
    feedbackMsg.className = `feedback ${type}`;
    setTimeout(() => {
        feedbackMsg.textContent = "";
    }, 3000);
}
