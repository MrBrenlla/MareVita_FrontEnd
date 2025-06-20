
# Guía de Execución da App MareVita

Este documento proporciona as instrucións necesarias para compilar, configurar e executar a aplicación móbil **MareVita**, desenvolvida en Kotlin con Android Studio como parte do Traballo Fin de Máster (TFM).

>BackEnd dispoñible en [MareVita_BackEnd](https://github.com/MrBrenlla/MareVita_BackEnd)


---

## 1. Requisitos Previos

Antes de comezar, asegúrese de ter instaladas as seguintes ferramentas no seu sistema:

- **Android Studio 2021.1** ou superior.
- **Java JDK 11** ou superior.
- **Kotlin 1.5** ou superior (incluído en Android Studio).
- Un dispositivo ou emulador Android con versión **Android 9.0 (Pie)** ou superior.
---

## 2. Compilación e Execución

1. Clona o repositorio:
   ```bash
   git clone https://github.com/usuario/marevita-app.git
   cd marevita-app
   ```
2. Abre o proxecto en Android Studio:
   - Selecciona **Open an existing project** e escolla a carpeta raíz.
3. Espera a que Gradle sincronice as dependencias.
4. Para compilar e executar no emulador ou dispositivo conectado:
   - Preme o botón **Run** ou executa:
     ```bash
     ./gradlew installDebug
     ```

---

## 3. Configuración do Backend

Antes de lanzar a aplicación, debes configurar a URL base do teu backend:

1. Abre `AppConfig.kt` en:
   ```
   app/src/main/java/gal/marevita/AppConfig.kt
   ```
2. Localiza a constante `BASE_URL` e modifícaa coa túa URL:
   ```kotlin
   object AppConfig {
       const val BASE_URL = "https://url_backend.gal/api/:PORTO"
   }
   ```

>*Se non se modificou o docker compose o porto debería ser o 8080*

---

## 4. Autor e Data

**Autor:** Túa Nome  
**Proxecto:** MareVita  
**Data:** Xuño de 2025
