# DinamiqApp — Arquitectura i guia tècnica

## 1. Flux de dades (de baix a dalt)

```
┌─────────────────────────────────────────────────────────┐
│                     MICRÒFON                            │
│              (Android AudioRecord API)                  │
└────────────────────────┬────────────────────────────────┘
                         │  PCM brut (ShortArray, 44100 Hz)
                         ▼
┌─────────────────────────────────────────────────────────┐
│                    AudioMeter.kt                        │
│  • Llegeix buffers de PCM contínuament                  │
│  • Calcula dB RMS per cada finestra de temps            │
│  • Emet AudioFrame(pcm: ShortArray, db: Float)          │
│                                                         │
│  Paràmetre clau: windowMs (freqüència d'escolta)        │
│  → Default 200ms, configurable des de l'app             │
└────────────────────────┬────────────────────────────────┘
                         │  AudioFrame cada N ms
                         ▼
┌─────────────────────────────────────────────────────────┐
│                  SignalProcessor.kt                     │
│                                                         │
│  FILTRE DE VEU (si hi ha perfil actiu):                 │
│  ┌───────────────────────────────────────────────┐      │
│  │  MfccExtractor.extract(pcm)                   │      │
│  │  → 13 coeficients MFCC (petjada espectral)    │      │
│  │  cosineSimilarity(mfcc, veuActiva.centroid)   │      │
│  │  → similitud 0..1                             │      │
│  │  Si similitud < llindar → frame IGNORAT       │      │
│  └───────────────────────────────────────────────┘      │
│                                                         │
│  SUAVITZAT EMA (frames acceptats):                      │
│  smoothedDb = alpha * db + (1-alpha) * smoothedDb       │
│  → alpha petit = molt suau (lent)                       │
│  → alpha gran = poc suau (ràpid)                        │
│                                                         │
│  HISTÈRESI:                                             │
│  Acumula N lectures de la mateixa dinàmica              │
│  → Canvia la dinàmica mostrada només quan es confirma   │
│                                                         │
│  Emet: AudioReading(db, level, precision)               │
└────────────────────────┬────────────────────────────────┘
                         │  AudioReading cada N ms
                         ▼
┌─────────────────────────────────────────────────────────┐
│              ScaleConverter.kt (singleton)              │
│                                                         │
│  Converteix dB → escala 0-100                           │
│  • profileMinDb / profileMaxDb → rang del perfil actiu  │
│  • appMinDb / appMaxDb → fallback global                │
│                                                         │
│  S'adapta automàticament al rang calibrat:              │
│  si toques pp→ff entre -45dB i -25dB,                  │
│  l'escala 0-100 cobreix exactament aquest rang          │
└────────────────────────┬────────────────────────────────┘
                         │  intensity (0-100), level, precision
                         ▼
┌─────────────────────────────────────────────────────────┐
│           MeasurementViewModel / MeterScreen            │
│                                                         │
│  • soundIntensity → el número (0-100)                   │
│  • currentLevel → pp / p / mf / f / ff / null          │
│  • precision → com de centrat estàs dins del rang       │
│    (0 = límit del rang, 1 = centre perfecte)            │
│                                                         │
│  La precision afecta la UI:                             │
│  → MeasurementScreen: opacitat del color de fons        │
│  → MeterScreen: mida i opacitat del cercle              │
└─────────────────────────────────────────────────────────┘
```

---

## 2. Mapa de fitxers

```
app/src/main/java/com/example/dinamiqapp/
│
├── audio/
│   ├── AudioMeter.kt          → Captura micròfon, emet AudioFrame
│   ├── MfccExtractor.kt       → FFT + Mel filterbank + DCT (reconeixement timbre)
│   └── SignalProcessor.kt     → Motor: filtre veu + EMA + histèresi
│
├── data/
│   ├── DynamicLevel.kt        → Enums (PP/P/MF/F/FF), DynamicRange,
│   │                            SettingsRepository (tot el DataStore de dinàmiques)
│   ├── ScaleConverter.kt      → Conversió dB ↔ escala 0-100
│   └── VoiceRepository.kt     → Emmagatzematge perfils de veu (DataStore)
│
├── navigation/
│   ├── Routes.kt              → Llista de rutes (Screen.Welcome, etc.)
│   └── AppNavGraph.kt         → Connexió rutes ↔ pantalles
│
└── ui/
    ├── screens/
    │   ├── Welcome/            → Pantalla d'inici + selector de veu
    │   ├── measurement/        → Mesura (símbol gran + color)
    │   ├── meter/              → Mesura (cercle + número)
    │   ├── settings/           → Ajust manual de rangs per sliders
    │   └── hearing/            → Calibratge per audició (per nivell i "al vol")
    ├── voices/                 → Gestió de veus/instruments (crear, millorar, eliminar)
    ├── appsettings/            → Paràmetres del motor (EMA, histèresi, percentils...)
    └── theme/                  → Colors per dinàmica, tipografia
```

---

## 3. Paràmetres que afecten el rendiment

### A. Velocitat de resposta de la UI

| Paràmetre | On es configura | Efecte | Valor actual |
|-----------|----------------|--------|-------------|
| **Freqüència d'escolta** | App → Paràmetres | Cada quants ms llegeix el micròfon. Baix = ràpid i precís, alt = lent i menys CPU | 200 ms |
| **Suavitzat EMA (alpha)** | App → Paràmetres | 0.05 = molt suau (va lent, estable). 1.0 = sense suavitzat (va ràpid, sorollós) | 0.25 |
| **Histèresi (lectures)** | App → Paràmetres | Quantes lectures consecutives cal per canviar la dinàmica mostrada. 1 = immediat, 10 = molt estable | 3 |

**Relació entre ells:** Si la freqüència és 200ms i la histèresi és 3, el canvi de dinàmica triga mínim 600ms. Si la freqüència és 100ms i la histèresi és 3, triga 300ms.

### B. Precisió del reconeixement de veu

| Paràmetre | On es configura | Efecte | Valor actual |
|-----------|----------------|--------|-------------|
| **Llindar similitud MFCC** | **Codi** (SignalConfig) | Quant ha de semblar el so a la veu entrenada. 0.70 = permissiu, 0.95 = molt estricte | 0.82 |
| **"Seguir aprenent"** | App → pantalla mesura (toggle) | Millora el model mentre toques. Auto-s'atura si 60s sense veu reconeguda | OFF per defecte |

**El llindar de similitud (0.82) és l'únic paràmetre important que ara ONLY es pot canviar al codi.** Si trobes que filtra massa (ignora sons teus) → baixes a 0.75. Si filtra poc (deixa passar ambient) → puges a 0.88.

### C. Calibratge dels rangs de dinàmica

| Paràmetre | On es configura | Efecte |
|-----------|----------------|--------|
| **Percentil pp** | App → Paràmetres | Quin % dels valors més baixos s'ignoren en el calibratge. 15% = ignora 15% de silencis |
| **Percentil ff** | App → Paràmetres | Fins on arriba el màxim. 90% = ignora el 10% de pics accidentals |
| **Rang dB global** | App → Paràmetres | Sensibilitat del micròfon (-70 a 0 dBFS). Fallback quan no hi ha calibratge |

---

## 4. Què pots gestionar des de l'app

### Pantalla d'inici
- Veure i seleccionar quin instrument/veu está actiu

### Configuració → Ajustar valors dinàmiques
- Moure els rangs de pp/p/mf/f/ff manualment amb sliders
- Restablir als valors per defecte del perfil actiu

### Configuració → Càlcul per audició
- **Per nivell**: gravar cada dinàmica individualment (amb compte enrere 3s)
- **Al vol**: tocar de pp a ff durant N segons → calcula tots els rangs automàticament
- Prémer "Aplicar" per fer efectius els canvis

### Configuració → Gestió de veus / instruments
- **Nova veu**: gravar 5-30s del teu instrument → genera el model MFCC → posa-li nom
- **Millorar**: afegir més mostres a un model existent (fusiona amb el centroid actual)
- **Seleccionar**: triar quina veu/instrument usa l'app
- **Eliminar**: esborrar un model

### Configuració → Paràmetres de l'app
| Control | Rang | Recomanació |
|---------|------|-------------|
| Límit inferior dB | -90 a -1 | Deixa a -70 (defecte) |
| Límit superior dB | -89 a 0 | Deixa a 0 (defecte) |
| Freqüència escolta | 50-1000 ms | 100-200ms per a ús normal |
| Suavitzat EMA | 0.05-1.0 | 0.15-0.35 (experimenta) |
| Histèresi | 1-10 lectures | 2-4 (experimenta) |
| Percentil pp | 5%-40% | 10-20% |
| Percentil ff | 60%-99% | 85-92% |
| Seguir aprenent | ON/OFF | Gestionar des de la pantalla de mesura |

### Pantalla de mesura (mentre mesures)
- Toggle "Seguir aprenent" (visible només si hi ha veu activa)

---

## 5. Què cal canviar des de l'IDE (Android Studio)

Aquests valors estan al codi i no es poden canviar des de l'app:

| Cosa a canviar | Fitxer | Línia clau | Nota |
|----------------|--------|-----------|------|
| **Llindar similitud MFCC** | `SignalProcessor.kt` | `voiceSimilarityThreshold: Float = 0.82f` | El més important a ajustar si la veu filtra malament |
| **Temps auto-stop d'aprenentatge** | `MeasurementViewModel.kt` | `INACTIVITY_STOP_MS = 60_000L` | 60s ara, pots pujar a 120s |
| **Durada del compte enrere** | `HearingCalcViewModel.kt` | `COUNTDOWN_SECONDS = 3` | |
| **Durada de gravació per nivell** | `HearingCalcViewModel.kt` | `LISTEN_DURATION_SECONDS = 3f` | |
| **Perfils de dinàmica per defecte** | `DynamicLevel.kt` | `object DefaultProfiles` | Casa i Concert hardcodejats |
| **Nombre de coeficients MFCC** | `MfccExtractor.kt` | `NUM_COEFFICIENTS = 13` | No tocar sense conèixer MFCC |
| **Threshold "keep learning"** | `MeasurementViewModel.kt` | `if (sim >= 0.90f)` | Similitud mínima per aprendre |

---

## 6. Arquitectura preparada per TensorFlow

L'estructura actual permet substituir el motor sense tocar la UI:

```
AudioEngine (interfície)
    │
    ├── SignalProcessor (implementació actual — MFCC template matching)
    │
    └── TensorFlowEngine (futura — mateixa interfície, motor diferent)
```

Per implementar TensorFlow cal:
1. Crear `TensorFlowEngine : AudioEngine` a `audio/`
2. Al `MeasurementViewModel`, substituir `SignalProcessor()` per `TensorFlowEngine()`
3. Res més canvia — la UI, el DataStore, les pantalles queden intactes

---

## 7. Persistència de dades (DataStore)

Tot es guarda automàticament i es recupera en reiniciar l'app:

| Dada | Clau DataStore |
|------|---------------|
| Rangs de dinàmica per perfil | `{perfil}_{LEVEL}_min/max` |
| Perfil actiu | `active_profile` |
| Rang dB global | `app_min_db`, `app_max_db` |
| Freqüència escolta | `app_refresh_ms` |
| Paràmetres motor | `signal_ema_alpha`, `signal_hysteresis`, etc. |
| Llista de veus | `voice_list` (IDs separats per comes) |
| Cada veu | `voice_{id}_name`, `voice_{id}_mfcc`, `voice_{id}_samples` |
| Veu activa | `active_voice_id` |
| Seguir aprenent (setting) | `keep_learning` |
