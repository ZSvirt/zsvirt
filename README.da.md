<!-- synced-with: README.md@89c5cfc6 -->
<div align="center">
  <a href="https://zsvirt.io">
    <img
      src="https://raw.githubusercontent.com/zsvirt/.github/main/assets/zsvirt-logo.jpg"
      alt="ZSvirt-logo"
      width="180"
    >
  </a>
  <p align="center">
    <a
      href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
      target="_blank"
      rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/daily?language=Java"
        alt="ZSvirt/zsvirt daglig Java-rangering | Trendshift"
        width="250"
        height="55"
      /></a> <a href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
       target="_blank"
       rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/weekly?language=Java"
        alt="ZSvirt/zsvirt ugentlig Java-rangering | Trendshift"
        width="250"
        height="55"
      /></a>&nbsp;&nbsp;
  </p>
  <h1 align="center">
    Open source-virtualisering
    <br>
    Klar til virksomheder, drevet af fællesskabet
  </h1>

  <p align="center">
    <a href="https://zsvirt.io">
      <img
        src="https://img.shields.io/badge/Website-0F62FE?style=flat-square&logo=googlechrome&logoColor=white"
        alt="ZSvirt-websted"
      >
    </a>
    <a href="https://zsvirt.io/en/docs">
      <img
        src="https://img.shields.io/badge/Docs-7C3AED?style=flat-square&logo=readthedocs&logoColor=white"
        alt="Dokumentation"
      >
    </a>
    <a href="https://demo.zsvirt.io/">
      <img
        src="https://img.shields.io/badge/Live%20Demo-16A34A?style=flat-square&logo=internetcomputer&logoColor=white"
        alt="Live-demo"
      >
    </a>
    <a href="https://zsvirt.io/download">
      <img
        src="https://img.shields.io/badge/Download-F97316?style=flat-square&logo=data:image/svg%2Bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTUgMjBoMTR2LTJINXYyem0xNC05aC00VjNIOXY4SDVsNyA3IDctN3oiLz48L3N2Zz4%3D"
        alt="Download"
      >
    </a>
  </p>

  <p align="center" dir="ltr">
    <a href="./README.md">English</a> |
    <a href="./README_zh.md">简体中文</a> |
    <a href="./README.zht.md">繁體中文</a> |
    <a href="./README.ko.md">한국어</a> |
    <a href="./README.de.md">Deutsch</a> |
    <a href="./README.es.md">Español</a> |
    <a href="./README.fr.md">Français</a> |
    <a href="./README.it.md">Italiano</a> |
    <strong>Dansk</strong> |
    <a href="./README.ja.md">日本語</a> |
    <a href="./README.pl.md">Polski</a> |
    <a href="./README.ru.md">Русский</a> |
    <a href="./README.bs.md">Bosanski</a> |
    <a href="./README.ar.md">العربية</a> |
    <a href="./README.no.md">Norsk</a> |
    <a href="./README.br.md">Português (Brasil)</a> |
    <a href="./README.th.md">ไทย</a> |
    <a href="./README.tr.md">Türkçe</a> |
    <a href="./README.uk.md">Українська</a> |
    <a href="./README.bn.md">বাংলা</a> |
    <a href="./README.gr.md">Ελληνικά</a> |
    <a href="./README.vi.md">Tiếng Việt</a>
  </p>
</div>

> **Prøv det nu — der kræves ingen fysisk hardware.** Hent [qcow2- eller OVA-aftrykket](https://zsvirt.io/download), start det som en VM på VMware, VirtualBox, KVM eller en cloudvært (indlejret virtualisering), indstil en IP-adresse, og ZSvirt-administrationsnoden er klar. Se [Kør ZSvirt i en VM](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node), eller gå direkte til [Hurtig start](https://zsvirt.io/en/docs/quick-start).

## Hvad er ZSvirt
ZSvirt bringer [ZStack](https://www.zstack-cloud.com/)'s virksomhedsafprøvede ZSphere-virtualiseringsmotor ind i open source-verdenen. Med opbakning fra [ZStack](https://www.zstack-cloud.com/), en erfaren leder inden for infrastruktur, er ZSvirt en let og skalerbar platform til at køre og administrere virtuelle maskiner uden leverandørbinding.

Installer en administrationsnode, tilslut KVM-baserede værter, og administrer hele dit miljø — VM'er, klynger, lager og netværk — via en webgrænseflade, en RESTful API (med Terraform og SDK'er til Go/Python/Java) eller en CLI. Værktøjer til VMware-migrering — onlinemigrering, OVF-import og VMDK-upload — er indbygget.

## Produktrundvisning

<details open>
  <summary>
    <strong>📊 KONTROLPANEL — Samlet driftsoversigt</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png"
        alt="ZSvirt samlet driftskontrolpanel"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🗂️ OVERSIGT — Central administration af infrastruktur</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png"
        alt="ZSvirt central infrastrukturfortegnelse"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🔄 MIGRERINGSSTYRING — Migrering af arbejdsbelastninger</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png"
        alt="ZSvirt-migreringsstyring"
        width="100%"
      >
    </a>
  </p>
</details>

## Live-demo

[ZSvirt Live Demo](https://demo.zsvirt.io/) er et gratis hostet miljø, hvor du kan prøve ZSvirt online — uden installation eller tilmelding. Åbn linket, og klik på **Demo Login** for straks at begynde at udforske platformen.

## Arkitektur

ZSvirt bruger en modulær arkitektur bygget op omkring administration af virtualiseringsressourcer, administrationsplanet, udvidelsestjenester og driftsværktøjer.

Kernefunktionerne omfatter:

- **Computervirtualisering**: administration af værter, klynger, virtuelle maskiner, aftryk og livscyklus.
- **Netværksvirtualisering**: virtuelle netværk, netværkstjenester, sikkerhedsgrupper og relaterede funktioner.
- **Lagervirtualisering**: primært lager, sikkerhedskopilager, diskenheder, snapshots og administration af lagerressourcer.
- **Administrationsplan**: API-framework, rettighedsmodel, hændelser, alarmer, revision og systemdrift.
- **Udvidelsestjenester**: funktioner til migrering, katastrofeberedskab, overvågning, kvotestyring, adgangskontrol og virksomhedsdrift.
- **Værktøjer og integrationer**: installationsværktøjer, diagnoseværktøjer, migreringsværktøjer, automatiseringsscripts, agenter, CLI og integrationer med eksterne systemer.

På softwarearkitekturniveau lægger ZSvirt vægt på asynkronitet, tilstandsløshed, udvidelsesmuligheder og automatisering:

- **Asynkron arkitektur**: understøtter asynkrone meddelelser, asynkrone metoder og asynkrone HTTP-kald for at reducere blokering og forbedre systemets kapacitet.
- **Tilstandsløse tjenester**: individuelle anmodninger afhænger ikke af tilstanden fra andre anmodninger, hvilket gør tjenester lettere at skalere, gendanne og drive.
- **Pluginbaseret udvidelse**: understøtter vandret udvidelse af ressourcetyper, forretningsfunktioner og integrationsmuligheder gennem plugins.
- **Workflowmotor**: styrer udførelsesrækkefølgen for komplekse handlinger og understøtter tilbagerulning og genopretning ved fejl.
- **Tagging- og forespørgselsfunktioner**: understøtter udvidelse af ressourceegenskaber, ressourceklassifikation, samlede forespørgsler og automatiseringsorkestrering.
- **Automatiseret implementering**: bruger automatiseringsværktøjer til implementering, konfiguration og driftsopgaver, hvilket reducerer kompleksiteten ved implementering og vedligeholdelse.

<p align="center">
  <img
    src="./assets/zsvirt-architecture.svg"
    alt="ZSvirt-arkitektur"
    width="100%"
  >
</p>

## Guide til VMware-migrering

Når virksomheder revurderer deres virtualiseringsstrategier, er migrering fra VMware til alternative platforme blevet et vigtigt emne for organisationer, der ønsker omkostningskontrol, fleksibel infrastruktur og langsigtet driftsstabilitet.

ZSvirt leverer migreringsorienterede funktioner og driftsværktøjer, der hjælper brugere med at evaluere, planlægge og flytte arbejdsbelastninger fra eksisterende VMware-miljøer til ZSvirt-baseret virtualiseringsinfrastruktur.

- [Guide til VMware-migrering](https://zsvirt.io/vmware-alternative/)

<p align="center">
  <a href="https://zsvirt.io/vmware-alternative/">
    <img
      src="https://github.com/ZSvirt/.github/blob/main/assets/zvirt-migrate.png?raw=true"
      alt="Migrer fra VMware til ZSvirt"
      width="100%"
    >
  </a>
</p>

## Hurtig start

Den hurtigste måde at evaluere ZSvirt på er at følge hurtigstartguiden i produktdokumentationen. Den fører dig gennem klargøring af computer-, netværks- og lagerressourcer, initialisering af administrationstjenesten og oprettelse af din første virtuelle maskine.

Vil du prøve det i en VM først? Download [qcow2- eller OVA-aftrykket](https://zsvirt.io/download), og kør administrationsnoden i en valgfri hypervisor — se [Kør ZSvirt i en VM](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node).

🚀 [Hurtig start](https://zsvirt.io/en/docs/quick-start)<br>
▶️ [Video](https://youtu.be/LsSJlBRUvYw)

## Bedste praksis
ZSvirt drives af den samme virksomhedsmotor som ZSphere og bygger videre på dokumenteret succes hos kunder verden over som vist nedenfor.
<p align="center">
  <img
    src="https://github.com/ZSvirt/.github/blob/main/assets/zsvirt-partner-en.png?raw=true"
    alt="ZSvirt-kunder og -partnere verden over"
    width="100%"
  >
</p>

## Sammenligning af virtualiseringsplatforme

Proxmox VE sammenlignet med VMware vSphere og ZSvirt

<p align="center">
  <img
    src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-comparison.png"
    alt="Sammenligning af virtualiseringsplatformene Proxmox VE og ZSvirt"
    width="100%"
  >
</p>


## Styring

ZSvirt styres efter en enkel open source-styringsmodel, der definerer, hvordan projektet vedligeholdes, hvordan beslutninger træffes, og hvordan bidragydere samarbejder.

Dokumentet [GOVERNANCE.md](GOVERNANCE.md) er udgangspunktet for at lære om projektroller, vedligeholdernes ansvar, beslutningsprocesser, udgivelsesstyring og samarbejde i fællesskabet.

Efterhånden som fællesskabet vokser, kan styringsmodellen udvikle sig til at omfatte dedikerede vedligeholdere, arbejdsgrupper og mere formelle projektprocesser.

## Bidrag

Vi byder bidrag fra fællesskabet velkommen og sætter pris på dem. Uanset om du retter fejl, forbedrer dokumentationen, foreslår funktioner, tilføjer test eller deler praksis for implementering, migrering og drift, er dit bidrag med til at gøre ZSvirt bedre.

Hvis projektet er nyt for dig, kan du begynde med dokumentationsforbedringer, fejlrapporter, testverificering, migreringserfaringer eller diskussioner i fællesskabet. Udviklere er også velkomne til at bidrage med kodeforbedringer, bedre værktøjer og integrationer.

Vi kan anerkende aktive bidragydere gennem omtale i fællesskabet, udgivelsesnoter, bidragyderlister eller fremtidige fællesskabsprogrammer.

Læs følgende, før du bidrager:

- [CONTRIBUTING.md](CONTRIBUTING.md)

## Sikkerhed

Sikkerhedsprocessen for rapportering af sårbarheder er beskrevet i [SECURITY.md](SECURITY.md).

Rapportér ikke sikkerhedssårbarheder gennem offentlige GitHub Issues eller Discussions.

## Licens

ZSvirt er licenseret under [GNU General Public License v3.0](LICENSE).

Nogle repositories eller komponenter kan indeholde open source-software fra tredjeparter under andre licenser. Se `LICENSE`, `NOTICE` og relaterede filer i hvert repository for detaljer.

## Ressourcer

<table>
  <tr>
    <td width="50%">
      <h3>🌐 Fællesskabets websted</h3>
      <p>Udforsk ZSvirt-funktioner, anvendelsesmuligheder, nyheder og fællesskabsressourcer.</p>
      <a href="https://zsvirt.io"><strong>Besøg webstedet →</strong></a>
    </td>
    <td width="50%">
      <h3>▶️ Videoer</h3>
      <p>Se introduktionen til ZSvirt-produktet, og lær dets kernefunktioner at kende.</p>
      <a href="https://youtu.be/c6pYmlIoPIU"><strong>Se produktvideoen →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>📝 Blog</h3>
      <p>Læs om udgivelsesopdateringer, historier fra udviklingen og indsigt i virtualisering.</p>
      <a href="https://zsvirt.io/blog"><strong>Læs bloggen →</strong></a>
    </td>
    <td width="50%">
      <h3>💬 GitHub Discussions</h3>
      <p>Stil spørgsmål, del idéer, og kom i kontakt med ZSvirt-fællesskabet.</p>
      <a href="https://github.com/ZSvirt/zsvirt/discussions"><strong>Deltag i diskussionen →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>▶️ YouTube</h3>
      <p>Følg ZSvirt-kanalen for demonstrationer, vejledninger og produktopdateringer.</p>
      <a href="https://youtube.com/@ZSvirt"><strong>Følg på YouTube →</strong></a>
    </td>
    <td width="50%">
      <h3>💼 LinkedIn</h3>
      <p>Følg ZSvirt for projektnyheder, højdepunkter fra fællesskabet og brancheindsigt.</p>
      <a href="https://www.linkedin.com/in/zsvirt-community/"><strong>Følg på LinkedIn →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>𝕏 X</h3>
      <p>Få de seneste ZSvirt-meddelelser og opdateringer fra fællesskabet.</p>
      <a href="https://x.com/ZSvirt"><strong>Følg på X →</strong></a>
    </td>
    <td width="50%">
      <h3>🎮 Discord</h3>
      <p>Bliv en del af ZSvirt-fællesskabet for at stille spørgsmål, dele idéer og komme i kontakt med andre brugere.</p>
      <a href="https://discord.com/invite/KHsw63z9xA"><strong>Deltag på Discord →</strong></a>
    </td>
  </tr>
</table>
