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
        alt="ZSvirt Java daglig – Trendshift"
        width="250"
        height="55"
      /></a> <a href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
       target="_blank"
       rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/weekly?language=Java"
        alt="ZSvirt Java ukentlig – Trendshift"
        width="250"
        height="55"
      /></a>&nbsp;&nbsp;
  </p>
  <h1 align="center">
    Virtualisering med åpen kildekode
    <br>
    Klar for bedrifter, drevet av fellesskapet
  </h1>

  <p align="center">
    <a href="https://zsvirt.io">
      <img
        src="https://img.shields.io/badge/Nettsted-0F62FE?style=flat-square&logo=googlechrome&logoColor=white"
        alt="Nettsted"
      >
    </a>
    <a href="https://zsvirt.io/en/docs">
      <img
        src="https://img.shields.io/badge/Dokumentasjon-7C3AED?style=flat-square&logo=readthedocs&logoColor=white"
        alt="Dokumentasjon"
      >
    </a>
    <a href="https://demo.zsvirt.io/">
      <img
        src="https://img.shields.io/badge/Nettbasert%20demo-16A34A?style=flat-square&logo=internetcomputer&logoColor=white"
        alt="Nettbasert demo"
      >
    </a>
    <a href="https://zsvirt.io/download">
      <img
        src="https://img.shields.io/badge/Nedlasting-F97316?style=flat-square&logo=data:image/svg%2Bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTUgMjBoMTR2LTJINXYyem0xNC05aC00VjNIOXY4SDVsNyA3IDctN3oiLz48L3N2Zz4%3D"
        alt="Nedlasting"
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
    <a href="./README.da.md">Dansk</a> |
    <a href="./README.ja.md">日本語</a> |
    <a href="./README.pl.md">Polski</a> |
    <a href="./README.ru.md">Русский</a> |
    <a href="./README.bs.md">Bosanski</a> |
    <a href="./README.ar.md">العربية</a> |
    <strong>Norsk</strong> |
    <a href="./README.br.md">Português (Brasil)</a> |
    <a href="./README.th.md">ไทย</a> |
    <a href="./README.tr.md">Türkçe</a> |
    <a href="./README.uk.md">Українська</a> |
    <a href="./README.bn.md">বাংলা</a> |
    <a href="./README.gr.md">Ελληνικά</a> |
    <a href="./README.vi.md">Tiếng Việt</a>
  </p>
</div>

> **Prøv nå — uten fysisk maskinvare.** Last ned en [qcow2- eller OVA-avbildningsfil](https://zsvirt.io/download), start den som en VM på VMware, VirtualBox, KVM eller en skyvert (nestet virtualisering), og angi en IP-adresse. Da er ZSvirt-administrasjonsnoden klar. Se [Kjør ZSvirt i en VM](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node) eller [Hurtigstart](https://zsvirt.io/en/docs/quick-start).

## Hva er ZSvirt?
ZSvirt bringer [ZStack](https://www.zstack-cloud.com/) sin ZSphere-virtualiseringsmotor, utprøvd i bedrifter, til miljøet for åpen kildekode. Med støtte fra den etablerte infrastrukturleverandøren [ZStack](https://www.zstack-cloud.com/) er ZSvirt en lett og skalerbar plattform for å kjøre og administrere virtuelle maskiner uten leverandørlåsing.

Installer en administrasjonsnode, koble til KVM-verter og administrer VM-er, klynger, lagring og nettverk via et webgrensesnitt, et RESTful API (med Terraform og Go/Python/Java-SDK-er) eller en CLI. Verktøy for VMware-migrering er innebygd: online-migrering, OVF-import og VMDK-opplasting.

## Produktomvisning

<details open>
  <summary>
    <strong>📊 DASHBORD — Samlet driftsoversikt</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png"
        alt="ZSvirt driftsoversikt"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🗂️ INVENTAR — Sentralisert infrastrukturadministrasjon</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png"
        alt="ZSvirt infrastrukturinventar"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🔄 MIGRERINGSADMINISTRASJON — Flytting av arbeidslaster</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png"
        alt="ZSvirt migreringsadministrasjon"
        width="100%"
      >
    </a>
  </p>
</details>

## Nettbasert demo

[ZSvirt-demoen](https://demo.zsvirt.io/) er et gratis driftet miljø for å prøve ZSvirt på nett, uten installasjon eller registrering. Åpne lenken og klikk **Demo Login** for å utforske plattformen.

## Arkitektur

ZSvirt har en modulær arkitektur bygget rundt administrasjon av virtualiseringsressurser, administrasjonsplanet, utvidelsestjenester og driftsverktøy.

Kjernefunksjonene omfatter:

- **Databehandlingsvirtualisering**: administrasjon av verter, klynger, virtuelle maskiner, avbildninger og livssykluser.
- **Nettverksvirtualisering**: virtuelle nettverk, nettverkstjenester, sikkerhetsgrupper og tilhørende funksjoner.
- **Lagringsvirtualisering**: primærlagring, sikkerhetskopilagring, volumer, øyeblikksbilder og administrasjon av lagringsressurser.
- **Administrasjonsplan**: API-rammeverk, rettighetsmodell, hendelser, alarmer, revisjon og systemdrift.
- **Utvidelsestjenester**: migrering, katastrofegjenoppretting, overvåking, kvoter, tilgangskontroll og bedriftsdrift.
- **Verktøy og integrasjoner**: installasjon, diagnostikk, migrering, automatiseringsskript, agenter, CLI og integrasjon med eksterne systemer.

Programvarearkitekturen vektlegger asynkronitet, tilstandsløshet, utvidbarhet og automatisering:

- **Asynkron arkitektur**: asynkrone meldinger, metoder og HTTP-kall reduserer blokkering og øker systemets gjennomstrømning.
- **Tilstandsløse tjenester**: enkeltforespørsler er uavhengige av andre forespørslers tilstand, noe som forenkler skalering, gjenoppretting og drift.
- **Utvidbarhet med programtillegg**: horisontal utvidelse av ressurstyper, forretningsfunksjoner og integrasjoner gjennom programtillegg.
- **Arbeidsflytmotor**: styrer rekkefølgen på komplekse operasjoner og støtter tilbakerulling og gjenoppretting ved feil.
- **Tagger og spørringer**: utvidelse av ressursattributter, ressursklassifisering, enhetlige spørringer og automatisert orkestrering.
- **Automatisert utrulling**: automatiseringsverktøy håndterer utrulling, konfigurasjon og drift og reduserer kompleksiteten i utrulling og vedlikehold.

<p align="center">
  <img
    src="./assets/zsvirt-architecture.svg"
    alt="ZSvirt arkitektur"
    width="100%"
  >
</p>

## Veiledning for VMware-migrering

Når bedrifter revurderer virtualiseringsstrategien, blir migrering fra VMware til andre plattformer viktig for å kontrollere kostnader, gjøre infrastrukturen fleksibel og sikre langsiktig driftsstabilitet.

ZSvirt tilbyr migreringsfunksjoner og driftsverktøy som hjelper brukerne med å vurdere, planlegge og flytte arbeidslaster fra eksisterende VMware-miljøer til ZSvirt-basert virtualiseringsinfrastruktur.

- [Veiledning for VMware-migrering](https://zsvirt.io/vmware-alternative/)

<p align="center">
  <a href="https://zsvirt.io/vmware-alternative/">
    <img
      src="https://github.com/ZSvirt/.github/blob/main/assets/zvirt-migrate.png?raw=true"
      alt="Migrering fra VMware til ZSvirt"
      width="100%"
    >
  </a>
</p>

## Hurtigstart

Den raskeste måten å evaluere ZSvirt på er å følge hurtigstarten i produktdokumentasjonen. Den viser hvordan du klargjør databehandlings-, nettverks- og lagringsressurser, initialiserer administrasjonstjenesten og oppretter din første virtuelle maskin.

Vil du først prøve i en VM? Last ned [qcow2- eller OVA-avbildningen](https://zsvirt.io/download) og kjør administrasjonsnoden i en hypervisor — se [Kjør ZSvirt i en VM](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node).

🚀 [Hurtigstart](https://zsvirt.io/en/docs/quick-start)<br>
▶️ [Video](https://youtu.be/LsSJlBRUvYw)

## Beste praksis
ZSvirt bruker samme bedriftsmotor som ZSphere og bygger på dokumenterte resultater hos de globale kundene nedenfor.
<p align="center">
  <img
    src="https://github.com/ZSvirt/.github/blob/main/assets/zsvirt-partner-en.png?raw=true"
    alt="ZSvirt kunder og partnere globalt"
    width="100%"
  >
</p>

## Sammenligning av virtualiseringsplattformer

Proxmox VE sammenlignet med VMware vSphere og ZSvirt

<p align="center">
  <img
    src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-comparison.png"
    alt="Sammenligning av Proxmox VE og ZSvirt"
    width="100%"
  >
</p>


## Prosjektstyring

ZSvirt følger en lett styringsmodell for åpen kildekode som beskriver vedlikehold, beslutninger og samarbeid mellom bidragsytere.

[GOVERNANCE.md](GOVERNANCE.md) beskriver prosjektroller, vedlikeholdernes ansvar, beslutningsprosesser, versjonshåndtering og samarbeid i fellesskapet.

Når fellesskapet vokser, kan modellen utvides med dedikerte vedlikeholdere, arbeidsgrupper og mer formelle prosesser.

## Bidra

Vi ønsker og verdsetter bidrag fra fellesskapet. Feilrettinger, bedre dokumentasjon, funksjonsforslag, tester og delte erfaringer om utrulling, migrering og drift gjør ZSvirt bedre.

Nye bidragsytere kan starte med dokumentasjon, feilrapporter, testverifisering, migreringserfaringer eller diskusjoner. Utviklere er også velkomne til å forbedre kode, verktøy og integrasjoner.

Aktive bidragsytere kan anerkjennes gjennom takksigelser, versjonsnotater, bidragsyterlister eller fremtidige fellesskapsprogrammer.

Les dette før du bidrar:

- [CONTRIBUTING.md](CONTRIBUTING.md)

## Sikkerhet

Prosessen for å rapportere sårbarheter er beskrevet i [SECURITY.md](SECURITY.md).

Ikke rapporter sikkerhetssårbarheter via offentlige GitHub Issues eller Discussions.

## Lisens

ZSvirt er lisensiert under [GNU General Public License v3.0](LICENSE).

Enkelte kodelagre eller komponenter kan inneholde tredjepartsprogramvare med åpen kildekode under andre lisenser. Se `LICENSE`, `NOTICE` og tilhørende filer i hvert kodelager.

## Ressurser

<table>
  <tr>
    <td width="50%">
      <h3>🌐 Fellesskapets nettsted</h3>
      <p>Utforsk ZSvirt-funksjoner, bruksområder, nyheter og fellesskapsressurser.</p>
      <a href="https://zsvirt.io"><strong>Besøk nettstedet →</strong></a>
    </td>
    <td width="50%">
      <h3>▶️ Videoer</h3>
      <p>Se produktpresentasjonen og bli kjent med ZSvirt sine kjernefunksjoner.</p>
      <a href="https://youtu.be/c6pYmlIoPIU"><strong>Se produktvideoen →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>📝 Blogg</h3>
      <p>Les versjonsnyheter, utviklingshistorier og innsikt i virtualisering.</p>
      <a href="https://zsvirt.io/blog"><strong>Les bloggen →</strong></a>
    </td>
    <td width="50%">
      <h3>💬 GitHub Discussions</h3>
      <p>Still spørsmål, del ideer og møt ZSvirt-fellesskapet.</p>
      <a href="https://github.com/ZSvirt/zsvirt/discussions"><strong>Delta i diskusjonen →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>▶️ YouTube</h3>
      <p>Følg ZSvirt-kanalen for demonstrasjoner, veiledninger og produktnyheter.</p>
      <a href="https://youtube.com/@ZSvirt"><strong>Følg på YouTube →</strong></a>
    </td>
    <td width="50%">
      <h3>💼 LinkedIn</h3>
      <p>Følg ZSvirt for prosjektnyheter, høydepunkter fra fellesskapet og bransjeinnsikt.</p>
      <a href="https://www.linkedin.com/in/zsvirt-community/"><strong>Følg på LinkedIn →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>𝕏 X</h3>
      <p>Få de nyeste kunngjøringene og oppdateringene fra ZSvirt-fellesskapet.</p>
      <a href="https://x.com/ZSvirt"><strong>Følg på X →</strong></a>
    </td>
    <td width="50%">
      <h3>🎮 Discord</h3>
      <p>Bli med i ZSvirt-fellesskapet for å stille spørsmål, dele ideer og møte andre brukere.</p>
      <a href="https://discord.com/invite/KHsw63z9xA"><strong>Bli med på Discord →</strong></a>
    </td>
  </tr>
</table>
