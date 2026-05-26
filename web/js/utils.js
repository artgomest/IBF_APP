// =============================================================================
// UTILS.JS — Funções compartilhadas entre app.js e cadastro.js
// =============================================================================

/**
 * Remove acentos e converte para minúsculas.
 */
export function normalizeText(value) {
    return (value || "")
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase();
}

// ---------------------------------------------------------------------------
// CPF
// ---------------------------------------------------------------------------

/**
 * Aplica máscara visual de CPF (000.000.000-00).
 */
export function applyCpfMask(e) {
    let v = e.target.value.replace(/\D/g, "");
    v = v.replace(/(\d{3})(\d)/, "$1.$2");
    v = v.replace(/(\d{3})(\d)/, "$1.$2");
    v = v.replace(/(\d{3})(\d{1,2})$/, "$1-$2");
    e.target.value = v;
}

/**
 * Retorna apenas os dígitos de um CPF.
 */
export function unmaskCpf(cpf) {
    return (cpf || "").replace(/\D/g, "");
}

/**
 * Valida CPF com dígitos verificadores (algoritmo oficial).
 * Retorna true se o CPF é válido.
 */
export function validarCpf(cpfRaw) {
    const cpf = unmaskCpf(cpfRaw);
    if (cpf.length !== 11) return false;

    // Rejeita CPFs com todos os dígitos iguais (111.111.111-11 etc.)
    if (/^(\d)\1{10}$/.test(cpf)) return false;

    // Calcula primeiro dígito verificador
    let soma = 0;
    for (let i = 0; i < 9; i++) {
        soma += parseInt(cpf.charAt(i)) * (10 - i);
    }
    let resto = (soma * 10) % 11;
    if (resto === 10) resto = 0;
    if (resto !== parseInt(cpf.charAt(9))) return false;

    // Calcula segundo dígito verificador
    soma = 0;
    for (let i = 0; i < 10; i++) {
        soma += parseInt(cpf.charAt(i)) * (11 - i);
    }
    resto = (soma * 10) % 11;
    if (resto === 10) resto = 0;
    if (resto !== parseInt(cpf.charAt(10))) return false;

    return true;
}

// ---------------------------------------------------------------------------
// Máscaras de telefone e CEP
// ---------------------------------------------------------------------------

export function maskPhone(e) {
    let v = e.target.value.replace(/\D/g, "");
    v = v.replace(/^(\d{2})(\d)/g, "($1) $2");
    v = v.replace(/(\d)(\d{4})$/, "$1-$2");
    e.target.value = v;
}

export function maskCep(e) {
    let value = e.target.value.replace(/\D/g, "");
    value = value.replace(/^(\d{5})(\d)/, "$1-$2");
    e.target.value = value;
}

export async function fetchViaCep(cep) {
    const raw = cep.replace(/\D/g, "");
    if (raw.length !== 8) return null;
    try {
        const resp = await fetch(`https://viacep.com.br/ws/${raw}/json/`);
        const data = await resp.json();
        if (data.erro) return null;
        return {
            logradouro: data.logradouro || "",
            bairro: data.bairro || "",
            cidade: data.localidade || "",
            uf: data.uf || ""
        };
    } catch {
        return null;
    }
}

// ---------------------------------------------------------------------------
// Datas e Idade
// ---------------------------------------------------------------------------

export function parseDate(value) {
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

export function calculateAge(value) {
    const date = parseDate(value);
    if (!date) return null;
    const today = new Date();
    let age = today.getFullYear() - date.getFullYear();
    const birthdayThisYear = new Date(today.getFullYear(), date.getMonth(), date.getDate());
    if (today < birthdayThisYear) age--;
    return age >= 0 ? age : null;
}

export function toInputDate(value) {
    if (/^\d{4}-\d{2}-\d{2}$/.test(value || "")) return value;
    const date = parseDate(value);
    if (!date) return "";
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

export function formatDate(value) {
    const date = parseDate(value);
    if (!date) return value || "";
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    return `${day}/${month}/${year}`;
}

// ---------------------------------------------------------------------------
// Cálculo de Rede
// ---------------------------------------------------------------------------

/**
 * Calcula a rede do membro com base em idade, sexo e estado civil.
 * Regras:
 *   - < 9 anos        → "Rede de Crianças"
 *   - 9 a 13 anos     → "Rede de Juniores"
 *   - 14 a 17 anos    → "Rede de Jovens"
 *   - 18+ solteiro(a) → "Rede de Jovens"
 *   - 18+ casado, masculino   → "Rede de Homens"
 *   - 18+ casado, feminino    → "Rede de Mulheres"
 *   - 18+ viúvo/divorciado, m → "Rede de Homens"
 *   - 18+ viúvo/divorciado, f → "Rede de Mulheres"
 */
export function calcularRedePorPerfil(dataNascimento, sexo, estadoCivil) {
    const age = calculateAge(dataNascimento);
    if (age === null) return { rede: null, motivo: "Data de nascimento inválida" };

    const sex = normalizeText(sexo);
    const civil = normalizeText(estadoCivil);

    if (age < 9) return { rede: "Rede de Crianças", motivo: null };
    if (age >= 9 && age <= 13) return { rede: "Rede de Juniores", motivo: null };
    if (age >= 14 && age < 18) return { rede: "Rede de Jovens", motivo: null };

    // 18+ anos
    const solteiro = civil.includes("solteir");
    if (solteiro) return { rede: "Rede de Jovens", motivo: null };

    // Casado, viúvo, divorciado, união estável → rede por sexo
    if (sex.includes("masculino")) return { rede: "Rede de Homens", motivo: null };
    if (sex.includes("feminino")) return { rede: "Rede de Mulheres", motivo: null };

    return { rede: "Rede de Jovens", motivo: "Sexo não identificado, classificado como jovens" };
}

/**
 * Calcula a rede de um filho (até 18 anos automaticamente pela idade).
 */
export function calculateChildNetwork(child) {
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
