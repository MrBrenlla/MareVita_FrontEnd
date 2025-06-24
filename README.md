
# Guía de Execución da App MareVita

Este repositorio forma parte dun Traballo de Fin de Máster (TFM) no Máster Universitario en Enxeñaría Informática da Universidade da Coruña (UDC).

Contén o cliente móbil de MareVita, unha aplicación Android desenvolvida en Kotlin para dar soporte á pesca deportiva en Galicia. A app proporciona unha experiencia unificada ao pescador, combinando tecnoloxía e accesibilidade.

Funcionalidades principais:
- Rexistro e consulta de capturas con datos ambientais, imaxes e localización.
- Mapas con predicións meteorolóxicas, mareas e fases lunares.
- Alertas configurables baseadas en condicións ambientais.
- Dimensión social: amizades, perfil, e feed de capturas doutros usuarios.

O frontend conecta cos microservizos REST do backend e ofrece unha interface intuitiva, adaptada ao uso real no litoral galego.


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

----------

**Autor:** Brais García Brenlla  
**Proxecto:** MareVita  
**Data:** Xuño de 2025
