// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    // Esses plugins são declarados aqui para serem utilizados por módulos como 'app'.
    // O 'apply false' indica que eles apenas se tornam disponíveis, mas não são
    // aplicados diretamente a este projeto raiz. Cada módulo os aplicará individualmente.
    id("com.android.application") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    // Adicione quaisquer outros plugins que você gerencia via libs.versions.toml aqui.
}

