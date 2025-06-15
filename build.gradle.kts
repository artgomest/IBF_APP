// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    // Esses plugins são declarados aqui para serem utilizados por módulos como 'app'.
    // O 'apply false' indica que eles apenas se tornam disponíveis, mas não são
    // aplicados diretamente a este projeto raiz. Cada módulo os aplicará individualmente.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    // Adicione quaisquer outros plugins que você gerencia via libs.versions.toml aqui.
}

