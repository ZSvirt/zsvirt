<!-- synced-with: README.md@e9235837 -->
<div align="center">
  <a href="https://zsvirt.io">
    <img
      src="https://raw.githubusercontent.com/zsvirt/.github/main/assets/zsvirt-logo.jpg"
      alt="ZSvirt-Logo"
      width="180"
    >
  </a>
  <p align="center">
    <a
      href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
      target="_blank"
      rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/daily?language=Java"
        alt="ZSvirt täglich Java – Trendshift"
        width="250"
        height="55"
      /></a> <a href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
       target="_blank"
       rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/weekly?language=Java"
        alt="ZSvirt wöchentlich Java – Trendshift"
        width="250"
        height="55"
      /></a>&nbsp;&nbsp;
  </p>
  <h1 align="center">
    Open-Source-Virtualisierung
    <br>
    Für Unternehmen entwickelt, von der Community getragen
  </h1>

  <p align="center">
    <a href="https://zsvirt.io">
      <img
        src="https://img.shields.io/badge/Website-0F62FE?style=flat-square&logo=googlechrome&logoColor=white"
        alt="Website"
      >
    </a>
    <a href="https://zsvirt.io/en/docs">
      <img
        src="https://img.shields.io/badge/Dokumentation-7C3AED?style=flat-square&logo=readthedocs&logoColor=white"
        alt="Dokumentation"
      >
    </a>
    <a href="https://demo.zsvirt.io/">
      <img
        src="https://img.shields.io/badge/Live-Demo-16A34A?style=flat-square&logo=internetcomputer&logoColor=white"
        alt="Live-Demo"
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
    <strong>Deutsch</strong> |
    <a href="./README.es.md">Español</a> |
    <a href="./README.fr.md">Français</a> |
    <a href="./README.it.md">Italiano</a> |
    <a href="./README.da.md">Dansk</a> |
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

> **Jetzt ausprobieren — ohne physische Hardware.** Laden Sie das [qcow2- oder OVA-Image](https://zsvirt.io/download) herunter, starten Sie es als VM unter VMware, VirtualBox, KVM oder auf einem Cloud-Host (verschachtelte Virtualisierung) und konfigurieren Sie eine IP-Adresse. Damit ist der ZSvirt-Verwaltungsknoten bereit. Siehe [ZSvirt in einer VM ausführen](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node) oder [Schnelleinstieg](https://zsvirt.io/en/docs/quick-start).

## Was ist ZSvirt?
ZSvirt bringt die im Unternehmenseinsatz bewährte ZSphere-Virtualisierungsengine von [ZStack](https://www.zstack-cloud.com/) in die Open-Source-Welt. Mit Unterstützung des etablierten Infrastrukturanbieters [ZStack](https://www.zstack-cloud.com/) bietet ZSvirt eine schlanke, skalierbare Plattform zum Betrieb und zur Verwaltung virtueller Maschinen ohne Herstellerbindung.

Installieren Sie einen Verwaltungsknoten, verbinden Sie KVM-basierte Hosts und verwalten Sie VMs, Cluster, Speicher und Netzwerke über eine Weboberfläche, eine RESTful API (mit Terraform und Go-/Python-/Java-SDKs) oder eine CLI. Werkzeuge für die VMware-Migration — Online-Migration, OVF-Import und VMDK-Upload — sind integriert.

## Produktrundgang

<details open>
  <summary>
    <strong>📊 DASHBOARD — Zentrale Betriebsübersicht</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png"
        alt="ZSvirt Betriebsübersicht"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🗂️ INVENTAR — Zentrale Infrastrukturverwaltung</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png"
        alt="ZSvirt Infrastrukturinventar"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🔄 MIGRATIONSVERWALTUNG — Workloads migrieren</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png"
        alt="ZSvirt Migrationsverwaltung"
        width="100%"
      >
    </a>
  </p>
</details>

## Live-Demo

Die [ZSvirt-Live-Demo](https://demo.zsvirt.io/) ist eine kostenlose, gehostete Umgebung zum Ausprobieren — ohne Installation oder Registrierung. Öffnen Sie den Link und klicken Sie auf **Demo Login**, um die Plattform zu erkunden.

## Architektur

ZSvirt verwendet eine modulare Architektur mit Virtualisierungsressourcenverwaltung, Verwaltungsebene, Erweiterungsdiensten und Betriebswerkzeugen.

Zu den Kernfunktionen gehören:

- **Compute-Virtualisierung**: Verwaltung von Hosts, Clustern, virtuellen Maschinen, Images und Lebenszyklen.
- **Netzwerkvirtualisierung**: Virtuelle Netzwerke, Netzwerkdienste, Sicherheitsgruppen und zugehörige Funktionen.
- **Speichervirtualisierung**: Primär- und Backup-Speicher, Volumes, Snapshots und Speicherressourcenverwaltung.
- **Verwaltungsebene**: API-Framework, Berechtigungsmodell, Ereignisse, Alarme, Auditierung und Systembetrieb.
- **Erweiterungsdienste**: Migration, Notfallwiederherstellung, Überwachung, Kontingentverwaltung, Zugriffskontrolle und Unternehmensbetrieb.
- **Werkzeuge und Integrationen**: Installation, Diagnose, Migration, Automatisierungsskripte, Agents, CLI und Anbindung externer Systeme.

Die Softwarearchitektur legt Wert auf Asynchronität, Zustandslosigkeit, Erweiterbarkeit und Automatisierung:

- **Asynchrone Architektur**: Asynchrone Nachrichten, Methoden und HTTP-Aufrufe reduzieren Blockierungen und erhöhen den Systemdurchsatz.
- **Zustandslose Dienste**: Einzelne Anfragen hängen nicht vom Zustand anderer Anfragen ab. Das erleichtert Skalierung, Wiederherstellung und Betrieb.
- **Plugin-basierte Erweiterbarkeit**: Plugins erweitern Ressourcentypen, Geschäftsfunktionen und Integrationen horizontal.
- **Workflow-Engine**: Steuert die Reihenfolge komplexer Vorgänge und unterstützt Rollback und Wiederherstellung im Fehlerfall.
- **Tags und Abfragen**: Erweiterung von Ressourcenattributen, Ressourcenklassifizierung, einheitliche Abfragen und Automatisierungsorchestrierung.
- **Automatisierte Bereitstellung**: Automatisierungswerkzeuge übernehmen Bereitstellung, Konfiguration und Betrieb und verringern den Aufwand für Bereitstellung und Wartung.

<p align="center">
  <img
    src="./assets/zsvirt-architecture.svg"
    alt="ZSvirt Architektur"
    width="100%"
  >
</p>

## Leitfaden zur VMware-Migration

Bei der Neubewertung ihrer Virtualisierungsstrategien beschäftigen sich Unternehmen zunehmend mit der Migration von VMware zu anderen Plattformen, um Kosten zu kontrollieren, Infrastruktur flexibel zu gestalten und langfristig stabil zu betreiben.

ZSvirt bietet Migrationsfunktionen und Betriebswerkzeuge, mit denen Benutzer die Verlagerung von Workloads aus bestehenden VMware-Umgebungen auf eine ZSvirt-Infrastruktur bewerten, planen und durchführen können.

- [Leitfaden zur VMware-Migration](https://zsvirt.io/vmware-alternative/)

<p align="center">
  <a href="https://zsvirt.io/vmware-alternative/">
    <img
      src="https://github.com/ZSvirt/.github/blob/main/assets/zvirt-migrate.png?raw=true"
      alt="Migration von VMware zu ZSvirt"
      width="100%"
    >
  </a>
</p>

## Schnelleinstieg

Am schnellsten evaluieren Sie ZSvirt mit dem Schnelleinstieg der Produktdokumentation. Er führt durch die Vorbereitung von Compute-, Netzwerk- und Speicherressourcen, die Initialisierung des Verwaltungsdienstes und die Erstellung der ersten virtuellen Maschine.

Zuerst in einer VM ausprobieren? Laden Sie das [qcow2- oder OVA-Image](https://zsvirt.io/download) herunter und betreiben Sie den Verwaltungsknoten in einem Hypervisor — siehe [ZSvirt in einer VM ausführen](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node).

🚀 [Schnelleinstieg](https://zsvirt.io/en/docs/quick-start)<br>
▶️ [Video](https://youtu.be/LsSJlBRUvYw)

## Bewährte Praxis
ZSvirt nutzt dieselbe Enterprise-Engine wie ZSphere und baut auf den nachgewiesenen Erfolgen bei den folgenden weltweiten Kunden auf.
<p align="center">
  <img
    src="https://github.com/ZSvirt/.github/blob/main/assets/zsvirt-partner-en.png?raw=true"
    alt="ZSvirt Kunden und Partner weltweit"
    width="100%"
  >
</p>

## Vergleich von Virtualisierungsplattformen

Proxmox VE im Vergleich mit VMware vSphere und ZSvirt

<p align="center">
  <img
    src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-comparison.png"
    alt="Vergleich von Proxmox VE und ZSvirt"
    width="100%"
  >
</p>

## Roadmap

#### 2. Halbjahr 2026

| Bereich | Geplante Arbeit |
|---|---|
| **Sicherheit** | Verschlüsselung von VM-Datenträgern hinzufügen<br>Verschlüsselung der VM-Migration hinzufügen |
| **DRS & VM-Migration** | Flexiblere Migrationsoptionen hinzufügen:<br>• Datenträger einzeln migrieren<br>• Warm- und Kaltmigration für SAN-zu-SAN-Szenarien unterstützen |
| **ZMigrate** | **ZMigrate 2.1**:<br>• Vorabprüfung zur Risikominderung bei komplexen Migrationen<br>• Einfachere und intelligentere Migrationsabläufe |
| **Betrieb** | Metriken für VM-CPU-Ready-Zeit, Lese- und Schreiblatenz von VM-Datenträgern, Netzwerk-Paketverlustrate und Host-CPU-Ready-Zeit hinzufügen<br>Feinere Protokollstufen für die Weiterleitung an externes Syslog unterstützen<br>NVMe- und FC-Geräteüberwachung inklusive IOPS und Latenz hinzufügen<br>Integrierte QXL-Treiber in VMTools für bessere Darstellung und Auflösung der VM-Konsole |
| **Inventarverwaltung** | Registrierung von Speicher und VMs auf Registrierung am selben Standort erweitern |

## Projektführung

ZSvirt folgt einem schlanken Open-Source-Governance-Modell, das Wartung, Entscheidungsfindung und Zusammenarbeit der Mitwirkenden regelt.

[GOVERNANCE.md](GOVERNANCE.md) beschreibt Projektrollen, Verantwortlichkeiten der Maintainer, Entscheidungsprozesse, Release-Verwaltung und Zusammenarbeit in der Community.

Mit dem Wachstum der Community kann das Modell um zuständige Maintainer, Arbeitsgruppen und formellere Projektabläufe erweitert werden.

## Mitwirken

Wir begrüßen und schätzen Beiträge aus der Community. Fehlerbehebungen, bessere Dokumentation, Funktionsvorschläge, Tests sowie geteilte Erfahrungen zu Bereitstellung, Migration und Betrieb machen ZSvirt besser.

Neue Mitwirkende können mit Dokumentation, Fehlermeldungen, Testprüfungen, Migrationserfahrungen oder Community-Diskussionen beginnen. Entwickler sind ebenso eingeladen, Code, Werkzeuge und Integrationen zu verbessern.

Aktive Mitwirkende können durch Danksagungen, Release Notes, Mitwirkendenlisten oder künftige Community-Programme gewürdigt werden.

Bitte lesen Sie vor Ihrem Beitrag:

- [CONTRIBUTING.md](CONTRIBUTING.md)

## Sicherheit

Das Verfahren zur Meldung von Sicherheitslücken ist in [SECURITY.md](SECURITY.md) beschrieben.

Bitte melden Sie Sicherheitslücken nicht über öffentliche GitHub Issues oder Discussions.

## Lizenz

ZSvirt steht unter der [GNU General Public License v3.0](LICENSE).

Einige Repositorys oder Komponenten können Open-Source-Software Dritter unter anderen Lizenzen enthalten. Einzelheiten finden Sie in `LICENSE`, `NOTICE` und verwandten Dateien des jeweiligen Repositorys.

## Ressourcen

<table>
  <tr>
    <td width="50%">
      <h3>🌐 Community-Website</h3>
      <p>Entdecken Sie Funktionen, Einsatzszenarien, Neuigkeiten und Community-Ressourcen von ZSvirt.</p>
      <a href="https://zsvirt.io"><strong>Website besuchen →</strong></a>
    </td>
    <td width="50%">
      <h3>▶️ Videos</h3>
      <p>Sehen Sie die Produktvorstellung und lernen Sie die Kernfunktionen von ZSvirt kennen.</p>
      <a href="https://youtu.be/c6pYmlIoPIU"><strong>Produktvideo ansehen →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>📝 Blog</h3>
      <p>Lesen Sie Versionsneuigkeiten, Entwicklungsberichte und Einblicke in die Virtualisierung.</p>
      <a href="https://zsvirt.io/blog"><strong>Blog lesen →</strong></a>
    </td>
    <td width="50%">
      <h3>💬 GitHub Discussions</h3>
      <p>Stellen Sie Fragen, teilen Sie Ideen und tauschen Sie sich mit der ZSvirt-Community aus.</p>
      <a href="https://github.com/ZSvirt/zsvirt/discussions"><strong>Mitdiskutieren →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>▶️ YouTube</h3>
      <p>Folgen Sie dem ZSvirt-Kanal für Demonstrationen, Anleitungen und Produktneuigkeiten.</p>
      <a href="https://youtube.com/@ZSvirt"><strong>Auf YouTube folgen →</strong></a>
    </td>
    <td width="50%">
      <h3>💼 LinkedIn</h3>
      <p>Folgen Sie ZSvirt für Projektneuigkeiten, Community-Highlights und Brancheneinblicke.</p>
      <a href="https://www.linkedin.com/in/zsvirt-community/"><strong>Auf LinkedIn folgen →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>𝕏 X</h3>
      <p>Erhalten Sie aktuelle Ankündigungen und Community-Neuigkeiten von ZSvirt.</p>
      <a href="https://x.com/ZSvirt"><strong>Auf X folgen →</strong></a>
    </td>
    <td width="50%">
      <h3>🎮 Discord</h3>
      <p>Treten Sie der ZSvirt-Community bei, stellen Sie Fragen und tauschen Sie Ideen mit anderen Benutzern aus.</p>
      <a href="https://discord.com/invite/KHsw63z9xA"><strong>Auf Discord beitreten →</strong></a>
    </td>
  </tr>
</table>
