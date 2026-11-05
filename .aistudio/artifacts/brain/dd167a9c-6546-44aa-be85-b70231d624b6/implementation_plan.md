# Reengenharia de Timbres MK-20 • Fidelidade Acústica de Músicas Originais

Reestruturação completa da inteligência de geração, calibração acústica e banco de dados de timbres da pedaleira MK-20 para atingir a fidelidade exata do som de estúdio de clássicos da guitarra e baixo.

> [!IMPORTANT]
> **Diagnóstico Acústico**: O som soa distante das músicas originais por três fatores fundamentais de engenharia de som:
> 1. **Falta de corte de agudos ásperos (High Cut)** no bloco CAB (em simuladores, sem High Cut entre 6.5 kHz e 7.5 kHz o som fica com zumbido/abelha digital não musical).
> 2. **Estrutura de ganho descalibrada**: nas gravações reais, o ganho de estúdio é menor do que se imagina ao vivo, vindo da soma de um boost transparente (TS-808/SD-1) empurrando a válvula do pré, e não de saturação excessiva solta.
> 3. **Ausência de catálogo calibrado de músicas famosas**: quando uma música conhecida é solicitada sem chave de API, o motor usava modelos genéricos padrão com equalização flat (50/50/50).

---

### 1. Visão Geral e Conceito Central

- **O que faz**: Transforma o sistema de criação e seleção de timbres em uma engine de modelagem profissional de estúdio. Integra um banco nativo de mais de 40 timbres históricos calibrados nota por nota (Guns N' Roses, Metallica, Pink Floyd, Dire Straits, AC/DC, Iron Maiden, SRV, Hendrix, John Mayer, etc.), calibra os blocos AMP, CAB e DS para respeitar as frequências de corte de gravação profissional, e aprimora os prompts da IA Gemini com diretrizes de produção musical.
- **Público-alvo**: Guitarristas e baixistas que utilizam a pedaleira MK-20 ao vivo ou em linha e exigem timbres prontos para mixagem, idênticos aos discos originais.
- **Valor agregado**: Elimina o som de "fita demo" ou "abelha digital", entregando peso articulado, corte de frequência na mix e dinâmica de palhetada real.

---

### 2. Experiência do Usuário e Design Visual

- **Aba "Clássicos do Rock & Estúdio" no Seletor de Timbres**:
  - Nova seção no diálogo de criação com cards visuais de músicas icônicas divididas por estilo (ex: *Sweet Child O' Mine*, *Comfortably Numb (Solo 2)*, *Master of Puppets*, *Sultans of Swing*, *Back in Black*, *Gravity*, *The Trooper*, *Pride and Joy*, etc.).
  - Ao selecionar uma música, carrega instantaneamente a cadeia completa de 9 blocos fiel aos equipamentos usados na gravação real (guitarra, captadores, pedais, amplificador e microfonação).
- **Barra de Calibração Rápida de Mixagem (Mix Master Bar)**:
  - Botão de 1 toque no cabeçalho do preset: **"Filtro de Mixagem de Estúdio"** que aplica instantaneamente Low Cut a 80 Hz, High Cut a 7.2 kHz e corte cirúrgico de médios-graves (400 Hz) na caixa/EQ para "limpar" qualquer timbre instantaneamente.
- **Exibição do Rig Real**:
  - Exibe no card de detalhes quais equipamentos analógicos reais foram emulados naquele timbre (ex: *"Gibson Les Paul 1959 -> Marshall Silver Jubilee 2555 + Celestion V30 (SM57) + Boss CE-2"*).

---

### 3. Decisões Técnicas e Acústicas

```
┌────────────────────────────────────────────────────────────────────────┐
│                        CADEIA ACÚSTICA CALIBRADA                       │
├─────────────┬─────────────┬─────────────┬─────────────┬────────────────┤
│  1. FX/NS   │   2. DS     │   3. AMP    │   4. CAB    │   5. EQ/DLY/REV│
│ Clean Comp  │ TS808 Boost │ JVM/800/SLO │ 4x12 / 2x12 │ Studio Post-EQ │
│ Gate Rápido │ Gain 0-15%  │ Gain 45-65% │ LowCut: 80Hz│ 400Hz: -3dB    │
│ Sem ruído   │ Level 80%+  │ Reso/Pres OK│ HiCut: 7kHz │ Delay no BPM   │
└─────────────┴─────────────┴─────────────┴─────────────┴────────────────┘
```

1. **Biblioteca Local Autêntica (`Mk20MasterSongDatabase`)**:
   - Mapear mais de 40 músicas e solos antológicos com parâmetros exatos dos modelos oficiais da MK-20 (01 a 120 de AMP, 01 a 52 de DS, gabinetes corretos e tempos de delay sincronizados em milissegundos com o andamento real).
2. **Revisão Acústica dos Filtros de Gabinete (`CAB`)**:
   - Ajustar `Low Cut` de fábrica para 75–90 Hz (remove subgraves de subwoofer desnecessários na guitarra).
   - Ajustar `High Cut` de fábrica para 6500–7500 Hz (elimina aspereza e fizzy digital acima de 8 kHz).
3. **Calibração das Relações de Ganho (`AMP` e `DS`)**:
   - Evitar saturar o bloco AMP em 100%. Em gravações de rock/metal, o som de alta definição é obtido com AMP entre 50% e 65% de ganho, empurrado pelo pedal `MW-808` ou `MW-SD1` com Gain baixo e Level alto para focar os médios.
4. **Instruções de Estúdio para a IA Gemini**:
   - Adicionar regras de ouro na system instruction do `Mk20ToneEngine`: sempre incluir Low Cut e High Cut realistas no CAB, calcular o delay em milissegundos baseado no BPM da música e ajustar o Reverb Mix para não embolar na mixagem.

---

### 4. Arquitetura e Estrutura de Componentes

- **`com.example.data.model.Mk20MasterSongDatabase`**: Catálogo estruturado de músicas de referência com metadados do artista, equipamento original, e objeto `Mk20Preset` pré-calibrado pronto para envio via BLE/USB.
- **`com.example.data.remote.Mk20ToneEngine`**:
  - `synthesizePresetLocally`: Busca primeiro correspondência direta ou por similaridade no banco de músicas famosas antes do fallback genérico.
  - Atualização do prompt Gemini com diretrizes acústicas de estúdio (anti-fizz, gain staging, mic placement).
- **`com.example.ui.components.PresetDialogs` / `MainScreen`**:
  - Seletor rápido de músicas lendárias integradas para teste instantâneo.
  - Botão de calibração sonora rápida de estúdio.
