// =============================================================================
// CADASTRO.JS — Wizard de Cadastro Público de Membro
// =============================================================================

import { auth, db } from './firebase-config.js';
import {
    signInAnonymously
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import {
    collection, query, where, getDocs, doc, writeBatch, getDoc, setDoc, serverTimestamp, deleteField
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";
import {
    applyCpfMask, unmaskCpf, validarCpf,
    maskPhone, maskCep, fetchViaCep,
    calculateAge, formatDate, normalizeText,
    calcularRedePorPerfil, calculateChildNetwork
} from './utils.js';

// ─── Auth ────────────────────────────────────────────────────────────────────
signInAnonymously(auth).catch(err => {
    console.error("Auth error:", err);
    if (err.code === 'auth/operation-not-allowed') {
        alert("Erro Crítico: Login Anônimo não está ativado no Firebase Console!");
    }
});

// ─── DOM ─────────────────────────────────────────────────────────────────────
const landingContainer = document.getElementById('landing-container');
const wizardContainer = document.getElementById('wizard-container');
const successContainer = document.getElementById('success-container');
const feedbackMsg = document.getElementById('w-feedback-msg');
const btnStartCadastro = document.getElementById('btn-start-cadastro');
const btnBackLanding = document.getElementById('btn-wizard-back-landing');
const btnNext = document.getElementById('btn-next');
const btnPrev = document.getElementById('btn-prev');

const TOTAL_STEPS = 7;
let currentStep = 0;
let children = [];
let editingChildIndex = null;

// ─── Masks ───────────────────────────────────────────────────────────────────
document.getElementById('w-cpf').addEventListener('input', applyCpfMask);
document.getElementById('w-celular').addEventListener('input', maskPhone);
const telefoneFixo = document.getElementById('w-telefoneFixo');
if (telefoneFixo) telefoneFixo.addEventListener('input', maskPhone);

const cepInput = document.getElementById('w-cep');
cepInput.addEventListener('input', maskCep);
cepInput.addEventListener('blur', async () => {
    const data = await fetchViaCep(cepInput.value);
    if (data) {
        document.getElementById('w-rua').value = data.logradouro;
        document.getElementById('w-bairro').value = data.bairro;
        document.getElementById('w-cidade').value = data.cidade;
    }
});

// ─── Conditional Fields ─────────────────────────────────────────────────────
function updateConditionalFields() {
    const estadoCivil = normalizeText(document.getElementById('w-estadoCivil').value);
    const tipoMembro = normalizeText(document.getElementById('w-tipoMembro').value);
    const aceitoPor = normalizeText(document.getElementById('w-aceitoPor').value);
    const batizado = document.getElementById('w-batizado').checked;
    const temFilhos = document.getElementById('w-temFilhos').checked;

    const casado = estadoCivil.includes('casad') || estadoCivil.includes('uniao estavel');
    const membro = tipoMembro.includes('membro');
    const igrejaAnterior = aceitoPor.includes('transfer') ||
        aceitoPor.includes('reconcili') ||
        aceitoPor.includes('restaur');

    toggle('w-group-data-casamento', casado);
    toggle('w-group-conjuge', casado);
    toggle('w-group-data-batismo', batizado);
    toggle('w-group-igreja-batismo', batizado);
    toggle('w-group-aceito-por', membro);
    toggle('w-group-igreja-anterior', igrejaAnterior);
    toggle('w-group-filhos', temFilhos);
}

function toggle(id, show) {
    const el = document.getElementById(id);
    if (el) el.style.display = show ? '' : 'none';
}

['w-estadoCivil', 'w-tipoMembro', 'w-aceitoPor', 'w-batizado', 'w-temFilhos'].forEach(id => {
    document.getElementById(id).addEventListener('change', updateConditionalFields);
});

// ─── Children ────────────────────────────────────────────────────────────────
document.getElementById('w-btn-add-child').addEventListener('click', () => openChildForm());
document.getElementById('w-btn-cancel-child').addEventListener('click', closeChildForm);
document.getElementById('w-btn-save-child').addEventListener('click', saveChild);

function openChildForm(index = null) {
    editingChildIndex = index;
    const child = index !== null ? children[index] : null;
    document.getElementById('w-child-name').value = child?.nome || "";
    document.getElementById('w-child-birth').value = child?.dataNascimento || "";
    document.getElementById('w-child-sex').value = child?.sexo || "";
    document.getElementById('w-child-form').classList.remove('hidden');
    document.getElementById('w-child-name').focus();
}

function closeChildForm() {
    editingChildIndex = null;
    document.getElementById('w-child-form').classList.add('hidden');
    document.getElementById('w-child-name').value = "";
    document.getElementById('w-child-birth').value = "";
    document.getElementById('w-child-sex').value = "";
}

function saveChild() {
    const nome = document.getElementById('w-child-name').value.trim();
    const dataNascimento = document.getElementById('w-child-birth').value;
    const sexo = document.getElementById('w-child-sex').value;

    if (!nome || !dataNascimento || !sexo) {
        showFeedback("Preencha nome, nascimento e sexo do filho.", "error");
        return;
    }
    const age = calculateAge(dataNascimento);
    if (age === null) {
        showFeedback("Data de nascimento do filho inválida.", "error");
        return;
    }

    const child = { nome, dataNascimento, sexo };

    if (editingChildIndex !== null) {
        children[editingChildIndex] = child;
    } else {
        children.push(child);
    }

    closeChildForm();
    renderChildren();
}

function renderChildren() {
    const list = document.getElementById('w-children-list');
    if (!list) return;
    list.innerHTML = "";

    if (children.length === 0) {
        list.innerHTML = '<p class="auth-helper">Nenhum filho cadastrado.</p>';
        return;
    }

    children.forEach((child, index) => {
        const age = calculateAge(child.dataNascimento);
        const network = calculateChildNetwork(child);
        const card = document.createElement('div');
        card.className = 'child-card';
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
        list.appendChild(card);
    });
}

document.getElementById('w-children-list').addEventListener('click', (event) => {
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

// ─── Navigation ──────────────────────────────────────────────────────────────

btnStartCadastro.addEventListener('click', (e) => {
    e.preventDefault();
    landingContainer.classList.add('hidden');
    wizardContainer.classList.remove('hidden');
    wizardContainer.classList.add('fade-in');
    goToStep(0);
});

btnBackLanding.addEventListener('click', () => {
    wizardContainer.classList.add('hidden');
    landingContainer.classList.remove('hidden');
    resetWizard();
});

btnNext.addEventListener('click', () => {
    if (currentStep === TOTAL_STEPS - 1) {
        submitCadastro();
    } else {
        if (validateStep(currentStep)) {
            goToStep(currentStep + 1);
        }
    }
});

btnPrev.addEventListener('click', () => {
    if (currentStep > 0) goToStep(currentStep - 1);
});

function goToStep(step) {
    currentStep = step;

    // Update panels
    document.querySelectorAll('.wizard-panel').forEach(p => p.classList.remove('active'));
    const panel = document.querySelector(`.wizard-panel[data-panel="${step}"]`);
    if (panel) {
        panel.classList.add('active');
        panel.classList.add('fade-in');
    }

    // Update stepper
    document.querySelectorAll('.wizard-step-indicator').forEach((ind, i) => {
        ind.classList.remove('active', 'completed');
        if (i < step) ind.classList.add('completed');
        if (i === step) ind.classList.add('active');
    });

    // Update nav buttons
    btnPrev.style.visibility = step === 0 ? 'hidden' : 'visible';
    if (step === TOTAL_STEPS - 1) {
        btnNext.textContent = "Finalizar Cadastro ✓";
    } else {
        btnNext.textContent = "Próximo →";
    }

    // Build review if on last step
    if (step === TOTAL_STEPS - 1) buildReview();

    // Scroll top
    window.scrollTo({ top: 0, behavior: 'smooth' });

    clearFeedback();
}

// ─── Step Validation ─────────────────────────────────────────────────────────

function validateStep(step) {
    clearFieldErrors();

    switch (step) {
        case 0: return validateStep0();
        case 1: return validateStep1();
        case 2: return true; // Endereço é opcional
        case 3: return validateStep3();
        case 4: return validateStep4();
        case 5: return validateStep5();
        case 6: return validateStep6();
        default: return true;
    }
}

function validateStep0() {
    const nome = val('w-nome');
    const dataNascimento = val('w-dataNascimento');
    const sexo = val('w-sexo');
    const cpf = val('w-cpf');

    if (!nome) return fieldError('w-nome', 'Informe seu nome completo.');
    if (!dataNascimento) return fieldError('w-dataNascimento', 'Informe a data de nascimento.');
    if (!sexo) return fieldError('w-sexo', 'Selecione o sexo.');
    if (!cpf) return fieldError('w-cpf', 'Informe o CPF.');
    if (!validarCpf(cpf)) return fieldError('w-cpf', 'CPF inválido. Verifique os dígitos.');

    return true;
}

function validateStep1() {
    const celular = val('w-celular');
    if (!celular || celular.replace(/\D/g, '').length < 10) {
        return fieldError('w-celular', 'Informe um celular válido.');
    }
    return true;
}

function validateStep3() {
    const estadoCivil = val('w-estadoCivil');
    if (!estadoCivil) return fieldError('w-estadoCivil', 'Selecione o estado civil.');

    const casado = normalizeText(estadoCivil).includes('casad') || normalizeText(estadoCivil).includes('uniao estavel');
    if (casado) {
        if (!val('w-conjuge')) return fieldError('w-conjuge', 'Informe o nome do cônjuge.');
    }
    return true;
}

function validateStep4() {
    const tipoMembro = val('w-tipoMembro');
    if (!tipoMembro) return fieldError('w-tipoMembro', 'Selecione o tipo de vínculo.');

    const batizado = document.getElementById('w-batizado').checked;
    if (batizado) {
        if (!val('w-dataBatismo')) return fieldError('w-dataBatismo', 'Informe a data de batismo.');
        if (!val('w-igrejaBatismo')) return fieldError('w-igrejaBatismo', 'Informe a igreja de batismo.');
    }

    const aceitoPor = normalizeText(val('w-aceitoPor'));
    const needsPrev = aceitoPor.includes('transfer') || aceitoPor.includes('reconcili') || aceitoPor.includes('restaur');
    if (needsPrev) {
        if (!val('w-igrejaAnterior')) return fieldError('w-igrejaAnterior', 'Informe a igreja anterior.');
        if (!val('w-pastorAnterior')) return fieldError('w-pastorAnterior', 'Informe o pastor anterior.');
    }
    return true;
}

function validateStep5() {
    const temFilhos = document.getElementById('w-temFilhos').checked;
    if (temFilhos && children.length === 0) {
        showFeedback("Cadastre pelo menos um filho ou desmarque a opção.", "error");
        return false;
    }
    return true;
}

function validateStep6() {
    if (!document.getElementById('w-aceite').checked) {
        showFeedback("Você precisa concordar com o uso dos dados para finalizar.", "error");
        return false;
    }
    return true;
}

// ─── Review Builder ──────────────────────────────────────────────────────────

function buildReview() {
    const container = document.getElementById('review-content');

    const dataNascimento = val('w-dataNascimento');
    const sexo = val('w-sexo');
    const estadoCivil = val('w-estadoCivil');
    const redeResult = calcularRedePorPerfil(dataNascimento, sexo, estadoCivil);

    let html = '';

    // Rede calculada
    const redeCss = getRedeCssClass(redeResult.rede);
    html += `
        <div class="rede-info">
            <strong>Sua rede calculada automaticamente:</strong>
            <span class="rede-badge ${redeCss}">${redeResult.rede || 'Não identificada'}</span>
            ${redeResult.motivo ? `<br><small style="color:var(--muted);">${redeResult.motivo}</small>` : ''}
        </div>
    `;

    // Dados pessoais
    html += reviewSection('Dados Pessoais', [
        ['Nome', val('w-nome')],
        ['Data de Nascimento', formatDate(dataNascimento)],
        ['Idade', `${calculateAge(dataNascimento)} anos`],
        ['Sexo', sexo],
        ['CPF', val('w-cpf')],
        ['RG', val('w-rg')],
        ['Escolaridade', val('w-escolaridade')],
        ['Naturalidade', val('w-naturalidade')]
    ]);

    // Contato
    html += reviewSection('Contato', [
        ['Celular', val('w-celular')],
        ['Telefone Fixo', val('w-telefoneFixo')]
    ]);

    // Endereço
    html += reviewSection('Endereço', [
        ['CEP', val('w-cep')],
        ['Cidade', val('w-cidade')],
        ['Bairro', val('w-bairro')],
        ['Rua', val('w-rua')],
        ['Número', val('w-numero')]
    ]);

    // Família
    const rows = [['Estado Civil', estadoCivil]];
    if (val('w-conjuge')) rows.push(['Cônjuge', val('w-conjuge')]);
    if (val('w-dataCasamento')) rows.push(['Data Casamento', formatDate(val('w-dataCasamento'))]);
    rows.push(['Pai', val('w-pai')], ['Mãe', val('w-mae')]);
    html += reviewSection('Família', rows);

    // Eclesiástico
    const eclRows = [
        ['Batizado', document.getElementById('w-batizado').checked ? 'Sim' : 'Não'],
        ['Data Batismo', formatDate(val('w-dataBatismo'))],
        ['Igreja Batismo', val('w-igrejaBatismo')],
        ['Tipo Membro', val('w-tipoMembro')],
        ['Aceito Por', val('w-aceitoPor')],
        ['Igreja Anterior', val('w-igrejaAnterior')],
        ['Pastor Anterior', val('w-pastorAnterior')],
        ['Talentos', val('w-talentosMinisterios')],
        ['Deseja Função', val('w-desejaFuncao')]
    ];
    html += reviewSection('Dados Eclesiásticos', eclRows);

    // Filhos
    if (children.length > 0) {
        const childRows = children.map(c => [
            c.nome,
            `${formatDate(c.dataNascimento)} — ${calculateAge(c.dataNascimento)} anos — ${calculateChildNetwork(c)}`
        ]);
        html += reviewSection('Filhos', childRows);
    }

    container.innerHTML = html;
}

function reviewSection(title, rows) {
    const filtered = rows.filter(r => r[1]);
    if (filtered.length === 0) return '';
    return `
        <div class="review-section">
            <h4>${title}</h4>
            ${filtered.map(r => `
                <div class="review-row">
                    <span class="review-label">${r[0]}</span>
                    <span class="review-value">${r[1]}</span>
                </div>
            `).join('')}
        </div>
    `;
}

function getRedeCssClass(rede) {
    if (!rede) return '';
    const r = normalizeText(rede);
    if (r.includes('jovens')) return 'rede-jovens';
    if (r.includes('homens')) return 'rede-homens';
    if (r.includes('mulheres')) return 'rede-mulheres';
    if (r.includes('crianca')) return 'rede-criancas';
    if (r.includes('juniores')) return 'rede-juniores';
    return '';
}

// ─── Submit ──────────────────────────────────────────────────────────────────

async function submitCadastro() {
    if (!validateStep6()) return;

    if (!auth.currentUser) {
        showFeedback("Conectando ao servidor... Tente novamente em alguns segundos.", "error");
        return;
    }

    btnNext.textContent = "Salvando...";
    btnNext.disabled = true;

    const cpfRaw = unmaskCpf(val('w-cpf'));
    const dataNascimento = val('w-dataNascimento');
    const sexo = val('w-sexo');
    const estadoCivil = val('w-estadoCivil');
    const redeResult = calcularRedePorPerfil(dataNascimento, sexo, estadoCivil);
    const nomeMembro = val('w-nome').trim();

    try {
        // 1. Verificar duplicidade de CPF
        const cpfIndexRef = doc(db, "cpfIndex", cpfRaw);
        const cpfSnap = await getDoc(cpfIndexRef);
        if (cpfSnap.exists()) {
            showFeedback("Este CPF já possui cadastro. Se precisar acessar sua ficha, use a opção 'Já tenho cadastro'.", "error");
            btnNext.textContent = "Finalizar Cadastro ✓";
            btnNext.disabled = false;
            return;
        }

        // Verificação adicional na coleção usuarios (para CPFs legados sem índice)
        const legacyQ = query(collection(db, "usuarios"), where("cpf", "==", cpfRaw));
        const legacySnap = await getDocs(legacyQ);
        if (!legacySnap.empty) {
            showFeedback("Este CPF já possui cadastro. Use a opção 'Já tenho cadastro' para acessar sua ficha.", "error");
            btnNext.textContent = "Finalizar Cadastro ✓";
            btnNext.disabled = false;
            return;
        }

        // 2. Construir dados do membro
        const batch = writeBatch(db);
        const memberRef = doc(collection(db, "usuarios"));
        const memberId = memberRef.id;

        const funcoes = {};
        if (redeResult.rede) {
            funcoes[redeResult.rede] = "Membro";
        }

        const childrenData = [];
        const childrenUids = [];

        // 3. Criar ou vincular filhos como documentos
        if (document.getElementById('w-temFilhos').checked && children.length > 0) {
            for (const child of children) {
                const childNetwork = calculateChildNetwork(child);
                const childAge = calculateAge(child.dataNascimento);
                let childRef = null;
                
                // Buscar duplicidade por dataNascimento e tipoCadastro="filho"
                const qChild = query(
                    collection(db, "usuarios"),
                    where("tipoCadastro", "==", "filho"),
                    where("dataNascimento", "==", child.dataNascimento)
                );
                const querySnapshot = await getDocs(qChild);
                
                // Checar se existe um filho com nome parecido (Primeiro nome + 1 sobrenome igual)
                const newChildNames = normalizeText(child.nome).split(' ').filter(n => n.length > 2);
                let existingChildData = null;
                let existingChildId = null;

                querySnapshot.forEach((docSnapshot) => {
                    const data = docSnapshot.data();
                    const existingNames = normalizeText(data.nome).split(' ').filter(n => n.length > 2);
                    
                    if (newChildNames.length > 0 && existingNames.length > 0) {
                        // Primeiro nome igual?
                        if (newChildNames[0] === existingNames[0]) {
                            // Tem mais algum sobrenome em comum?
                            let hasCommonLastName = false;
                            if (newChildNames.length === 1 && existingNames.length === 1) {
                                hasCommonLastName = true; 
                            } else {
                                const newLastNames = newChildNames.slice(1);
                                const existingLastNames = existingNames.slice(1);
                                hasCommonLastName = newLastNames.some(lastName => existingLastNames.includes(lastName));
                            }
                            
                            if (hasCommonLastName) {
                                existingChildId = docSnapshot.id;
                                existingChildData = data;
                            }
                        }
                    }
                });

                if (existingChildId && existingChildData) {
                    // Já existe. Vamos apenas VINCULAR atualizando o array de responsáveis
                    childRef = doc(db, "usuarios", existingChildId);
                    
                    let respUids = existingChildData.responsaveisUids || [];
                    let respNomes = existingChildData.responsaveisNomes || [];
                    
                    // Migração de dados antigos para a estrutura de array
                    if (existingChildData.responsavelUid && !respUids.includes(existingChildData.responsavelUid)) {
                        respUids.push(existingChildData.responsavelUid);
                    }
                    if (existingChildData.responsavelNome && !respNomes.includes(existingChildData.responsavelNome)) {
                        respNomes.push(existingChildData.responsavelNome);
                    }
                    
                    // Adicionar o novo pai/mãe
                    if (!respUids.includes(memberId)) {
                        respUids.push(memberId);
                    }
                    if (!respNomes.includes(nomeMembro)) {
                        respNomes.push(nomeMembro);
                    }

                    let updateData = {
                        responsaveisUids: respUids,
                        responsaveisNomes: respNomes,
                        // Mantém a primeira string para retrocompatibilidade em listagens antigas
                        responsavelUid: respUids[0],
                        responsavelNome: respNomes[0]
                    };

                    if (sexo === "Masculino" && (!existingChildData.pai || existingChildData.pai.trim() === "")) {
                        updateData.pai = nomeMembro;
                    } else if (sexo === "Feminino" && (!existingChildData.mae || existingChildData.mae.trim() === "")) {
                        updateData.mae = nomeMembro;
                    }

                    batch.update(childRef, updateData);
                } else {
                    // É um filho novo. Vamos criar.
                    childRef = doc(collection(db, "usuarios"));
                    
                    const nomeConjuge = val('w-conjuge');
                    const initialUids = [memberId];
                    const initialNomes = [nomeMembro];
                    
                    let nomePai = "";
                    let nomeMae = "";

                    if (sexo === "Masculino") {
                        nomePai = nomeMembro;
                        nomeMae = nomeConjuge || "";
                    } else if (sexo === "Feminino") {
                        nomeMae = nomeMembro;
                        nomePai = nomeConjuge || "";
                    }
                    
                    if (nomeConjuge) {
                        initialNomes.push(nomeConjuge);
                    }
                    
                    const childDoc = {
                        nome: child.nome,
                        dataNascimento: child.dataNascimento,
                        sexo: child.sexo,
                        idade: childAge ?? 0,
                        pai: nomePai,
                        mae: nomeMae,
                        responsaveisUids: initialUids,
                        responsaveisNomes: initialNomes,
                        responsavelUid: memberId, // retrocompatibilidade
                        responsavelNome: nomeMembro, // retrocompatibilidade
                        tipoCadastro: "filho",
                        cadastroCompleto: true,
                        origemCadastro: "auto_membro_web",
                        formularioVersao: "2.0",
                        criadoPeloCadastroDoResponsavel: true,
                        funcoes: { [childNetwork]: "Membro" }
                    };
                    
                    batch.set(childRef, childDoc);
                }

                childrenUids.push(childRef.id);
                childrenData.push({
                    id: childRef.id,
                    nome: child.nome,
                    dataNascimento: child.dataNascimento,
                    sexo: child.sexo,
                    idade: childAge,
                    rede: childNetwork
                });
            }
        }

        const memberData = {
            nome: nomeMembro,
            nomeNormalizado: normalizeText(nomeMembro),
            dataNascimento: dataNascimento,
            sexo: sexo,
            cpf: cpfRaw,
            cpfNormalizado: cpfRaw,
            escolaridade: val('w-escolaridade'),
            rg: val('w-rg'),
            orgaoExpeditor: val('w-orgaoExpeditor'),
            naturalidade: val('w-naturalidade'),
            celular: val('w-celular'),
            telefoneFixo: val('w-telefoneFixo'),
            cep: val('w-cep'),
            cidade: val('w-cidade'),
            bairro: val('w-bairro'),
            rua: val('w-rua'),
            numero: val('w-numero'),
            estadoCivil: estadoCivil,
            dataCasamento: val('w-dataCasamento'),
            conjuge: val('w-conjuge'),
            pai: val('w-pai'),
            mae: val('w-mae'),
            batizado: document.getElementById('w-batizado').checked,
            dataBatismo: val('w-dataBatismo'),
            igrejaBatismo: val('w-igrejaBatismo'),
            tipoMembro: val('w-tipoMembro'),
            aceitoPor: val('w-aceitoPor'),
            igrejaAnterior: val('w-igrejaAnterior'),
            pastorAnterior: val('w-pastorAnterior'),
            cargosExercidos: val('w-cargosExercidos'),
            talentosMinisterios: val('w-talentosMinisterios'),
            desejaFuncao: val('w-desejaFuncao'),
            temFilhos: document.getElementById('w-temFilhos').checked,
            filhos: childrenData,
            filhosUids: childrenUids,
            filhosDetalhes: childrenData.map(c =>
                `${c.nome} (${formatDate(c.dataNascimento)}) - ${c.rede}`
            ).join('\n'),
            funcoes: funcoes,
            redeCalculada: redeResult.rede,
            precisaRevisaoRede: !!redeResult.motivo,
            motivoRevisaoRede: redeResult.motivo || "",
            cadastroCompleto: true,
            tipoCadastro: "membro_principal",
            origemCadastro: "auto_membro_web",
            statusValidacao: "pendente_validacao",
            formularioVersao: "2.0",
            aceiteUsoDados: true,
            aceiteUsoDadosEm: serverTimestamp()
        };

        batch.set(memberRef, memberData);

        // 4. Criar índice CPF
        batch.set(cpfIndexRef, {
            usuarioId: memberId,
            createdAt: serverTimestamp(),
            origemCadastro: "auto_membro_web"
        });

        // 5. Commit
        await batch.commit();

        // 6. Mostrar sucesso
        showSuccess(nomeMembro, redeResult.rede);

    } catch (error) {
        console.error("Erro ao salvar cadastro:", error);
        showFeedback("Erro ao salvar: " + error.message, "error");
        btnNext.textContent = "Finalizar Cadastro ✓";
        btnNext.disabled = false;
    }
}

function showSuccess(name, rede) {
    wizardContainer.classList.add('hidden');
    successContainer.classList.remove('hidden');
    successContainer.classList.add('fade-in');

    document.getElementById('success-name').textContent = name;
    const redeBadge = document.getElementById('success-rede');
    const css = getRedeCssClass(rede);
    redeBadge.innerHTML = `<span class="rede-badge ${css}">${rede || 'Pendente de classificação'}</span>`;

    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

function val(id) {
    const el = document.getElementById(id);
    return el ? el.value.trim() : "";
}

function showFeedback(msg, type) {
    feedbackMsg.textContent = msg;
    feedbackMsg.className = `feedback ${type}`;
    setTimeout(() => { feedbackMsg.textContent = ""; feedbackMsg.className = "feedback"; }, 5000);
}

function clearFeedback() {
    feedbackMsg.textContent = "";
    feedbackMsg.className = "feedback";
}

function fieldError(id, msg) {
    const el = document.getElementById(id);
    if (el) {
        el.classList.add('has-error');
        const existing = el.parentElement.querySelector('.field-error');
        if (!existing) {
            const errEl = document.createElement('div');
            errEl.className = 'field-error';
            errEl.textContent = msg;
            el.parentElement.appendChild(errEl);
        }
        el.focus();
    }
    showFeedback(msg, "error");
    return false;
}

function clearFieldErrors() {
    document.querySelectorAll('.has-error').forEach(el => el.classList.remove('has-error'));
    document.querySelectorAll('.field-error').forEach(el => el.remove());
}

function resetWizard() {
    currentStep = 0;
    children = [];
    editingChildIndex = null;
    document.querySelectorAll('#wizard-container input, #wizard-container select').forEach(el => {
        if (el.type === 'checkbox') el.checked = false;
        else el.value = "";
    });
    clearFieldErrors();
    clearFeedback();
    updateConditionalFields();
    goToStep(0);
}
