<!-- synced-with: README.md@89c5cfc6 -->
<div align="center">
  <a href="https://zsvirt.io">
    <img
      src="https://raw.githubusercontent.com/zsvirt/.github/main/assets/zsvirt-logo.jpg"
      alt="ZSvirt 標誌"
      width="180"
    >
  </a>

  <p align="center">
    <a
      href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
      target="_blank"
      rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/daily?language=Java"
        alt="ZSvirt/zsvirt 每日 Java 趨勢 | Trendshift"
        width="250"
        height="55"
      /></a> <a href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
       target="_blank"
       rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/weekly?language=Java"
        alt="ZSvirt/zsvirt 每週 Java 趨勢 | Trendshift"
        width="250"
        height="55"
      /></a>&nbsp;&nbsp;
  </p>

  <h1 align="center">
    開源虛擬化平台
    <br>
    企業級能力，社群共同驅動
  </h1>

  <p align="center">
    <a href="https://zsvirt.io/zh">
      <img
        src="https://img.shields.io/badge/官方網站-0F62FE?style=flat-square&logo=googlechrome&logoColor=white"
        alt="ZSvirt 官方網站"
      >
    </a>
    <a href="https://zsvirt.io/docs">
      <img
        src="https://img.shields.io/badge/產品文件-7C3AED?style=flat-square&logo=readthedocs&logoColor=white"
        alt="產品文件"
      >
    </a>
    <a href="https://demo.zsvirt.io/">
      <img
        src="https://img.shields.io/badge/線上體驗-16A34A?style=flat-square&logo=internetcomputer&logoColor=white"
        alt="線上體驗"
      >
    </a>
    <a href="https://zsvirt.io/zh/download">
      <img
        src="https://img.shields.io/badge/下載中心-F97316?style=flat-square&logo=data:image/svg%2Bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTUgMjBoMTR2LTJINXYyem0xNC05aC00VjNIOXY4SDVsNyA3IDctN3oiLz48L3N2Zz4%3D"
        alt="下載中心"
      >
    </a>
  </p>

  <p align="center" dir="ltr">
    <a href="./README.md">English</a> |
    <a href="./README_zh.md">简体中文</a> |
    <strong>繁體中文</strong> |
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

> **立即體驗 — 無需實體伺服器。** 從[下載中心](https://zsvirt.io/zh/download)取得 qcow2 或 OVA 映像檔，在 VMware、VirtualBox、KVM 或雲端主機（巢狀虛擬化）中建立一台虛擬機，設定 IP 後，ZSvirt 管理節點即可就緒。請參閱[在虛擬機中執行 ZSvirt](https://docs.zsvirt.io/docs/quick-start/nested-virtualization-management-node)，或直接前往[快速入門](https://zsvirt.io/docs/quick-start)。

## ZSvirt 介紹

ZSvirt 將 [ZStack](https://www.zstack.io/) 經企業級實務驗證的 ZSphere 虛擬化引擎帶入開源世界。ZSvirt 由成熟的基礎架構領導廠商 [ZStack](https://www.zstack.io/) 支援，是一個輕量、可擴充的平台，讓使用者能夠執行及管理虛擬機，並避免供應商鎖定。

安裝管理節點並連接採用 KVM 的主機後，即可透過 Web 介面、RESTful API（搭配 Terraform 與 Go/Python/Java SDK）或 CLI，管理整個環境中的虛擬機、叢集、儲存和網路。平台內建 VMware 遷移工具，支援線上遷移、OVF 匯入及 VMDK 上傳。

## 產品導覽

<details open>
  <summary>
    <strong>📊 儀表板 — 統一維運總覽</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png"
        alt="ZSvirt 統一維運儀表板"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🗂️ 資源清單 — 集中管理基礎架構</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png"
        alt="ZSvirt 集中式基礎架構資源清單"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🔄 遷移管理 — 工作負載遷移</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png"
        alt="ZSvirt 遷移管理"
        width="100%"
      >
    </a>
  </p>
</details>

## 線上體驗

[ZSvirt 線上體驗環境](https://demo.zsvirt.io/)是可免費線上試用 ZSvirt 的託管環境，無需安裝或註冊。開啟連結並按一下 **Demo Login**，即可立即探索此平台。

## 系統架構

ZSvirt 採用模組化架構，圍繞虛擬化資源管理、管理平面、擴充服務及維運工具建構。

核心能力包括：

- **運算虛擬化**：提供主機、叢集、虛擬機、映像檔及其生命週期管理能力。
- **網路虛擬化**：提供虛擬網路、網路服務、安全群組及相關功能。
- **儲存虛擬化**：提供主儲存區、備份儲存區、磁碟區、快照及儲存資源管理能力。
- **管理平面**：提供 API 框架、權限模型、事件、警示、稽核及系統維運能力。
- **擴充服務**：提供遷移、災難復原、監控、配額管理、存取控制及企業維運等能力。
- **工具與整合**：提供安裝工具、診斷工具、遷移工具、自動化指令碼、代理程式、CLI 及外部系統整合能力。

在軟體架構層面，ZSvirt 著重非同步、無狀態、可擴充性及自動化：

- **非同步架構**：支援非同步訊息、非同步方法及非同步 HTTP 呼叫，以減少阻塞並提高系統輸送量。
- **無狀態服務**：個別請求不依賴其他請求的狀態，讓服務更容易擴充、復原及維運。
- **外掛式擴充**：透過外掛橫向擴充資源類型、業務能力及系統整合能力。
- **工作流程引擎**：管理複雜操作的執行順序，並支援失敗情境下的回滾與復原。
- **標籤與查詢能力**：支援資源屬性擴充、資源分類、統一查詢及自動化協調。
- **自動化部署**：透過自動化工具完成部署、設定及維運工作，降低部署與維護複雜度。

<p align="center">
  <img
    src="./assets/zsvirt-architecture.svg"
    alt="ZSvirt 系統架構"
    width="100%"
  >
</p>

## VMware 遷移指南

隨著企業重新評估虛擬化策略，對於希望控制成本、提高基礎架構彈性並維持長期穩定營運的組織而言，從 VMware 遷移至其他平台已成為重要議題。

ZSvirt 提供以遷移為導向的功能與維運工具，協助使用者評估、規劃，並將工作負載從現有 VMware 環境移至以 ZSvirt 為基礎的虛擬化基礎架構。

🔄 [檢視 VMware 遷移指南](https://zsvirt.io/zh/vmware-alternative)

<p align="center">
  <a href="https://zsvirt.io/zh/vmware-alternative">
    <img
      src="https://github.com/ZSvirt/.github/blob/main/assets/zvirt-migrate.png?raw=true"
      alt="從 VMware 遷移至 ZSvirt"
      width="100%"
    >
  </a>
</p>

## 快速入門

評估 ZSvirt 最快的方式是依照產品文件中的快速入門指南操作。該指南將引導你準備運算、網路及儲存資源，初始化管理服務，並建立第一台虛擬機。

想先在虛擬機中試用嗎？請從[下載中心](https://zsvirt.io/zh/download)取得 qcow2 或 OVA 映像檔，並在任何 Hypervisor 中執行管理節點——詳情請參閱[在虛擬機中執行 ZSvirt](https://docs.zsvirt.io/docs/quick-start/nested-virtualization-management-node)。

🚀 [快速入門](https://zsvirt.io/docs/quick-start)<br>
▶️ [影片](https://youtu.be/LsSJlBRUvYw)

## 最佳實務

ZSvirt 採用與 ZSphere 相同的企業級虛擬化引擎，承襲了經全球客戶實務驗證的成功經驗。

<p align="center">
  <img
    src="https://github.com/ZSvirt/.github/blob/main/assets/zsvirt-partner-en.png?raw=true"
    alt="ZSvirt 全球客戶與合作夥伴"
    width="100%"
  >
</p>

## 虛擬化平台比較

Proxmox VE、VMware vSphere 與 ZSvirt 比較

<p align="center">
  <img
    src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-comparison.png"
    alt="Proxmox VE 與 ZSvirt 虛擬化平台比較"
    width="100%"
  >
</p>


## 社群治理

ZSvirt 採用輕量的開源治理模式，明確定義專案的維護方式、決策流程，以及貢獻者的協作方式。

如需瞭解專案角色、維護者職責、決策流程、版本發布管理及社群協作方式，請先閱讀 [GOVERNANCE.md](GOVERNANCE.md)。

隨著社群持續成長，ZSvirt 的治理模式可能逐步納入專責維護者、工作小組及更正式的專案流程。

## 參與貢獻

我們歡迎並感謝社群的每一項貢獻。無論是修正錯誤、改善文件、提出功能建議、增加測試，或分享部署、遷移及維運實務，你的貢獻都能讓 ZSvirt 變得更好。

如果你剛開始接觸本專案，可以從改善文件、回報問題、測試驗證、分享遷移經驗或參與社群討論著手。我們也歡迎開發者貢獻程式碼改進、工具強化及系統整合。

活躍貢獻者可能會透過社群致謝、版本資訊、貢獻者名單或未來的社群計畫獲得肯定。

參與貢獻前，請先閱讀：

- [CONTRIBUTING.md](CONTRIBUTING.md)

## 安全性

安全漏洞回報流程請參閱 [SECURITY.md](SECURITY.md)。

請勿透過公開的 GitHub Issues 或 Discussions 回報安全漏洞。

## 開源授權條款

ZSvirt 採用 [GNU General Public License v3.0](LICENSE) 授權條款發布。

部分程式碼儲存庫或元件可能包含採用其他授權條款的第三方開源軟體。詳情請查閱各儲存庫中的 `LICENSE`、`NOTICE` 及相關檔案。

## 相關資源

<table>
  <tr>
    <td width="50%">
      <h3>🌐 社群網站</h3>
      <p>探索 ZSvirt 的產品功能、使用情境、專案動態及社群資源。</p>
      <a href="https://zsvirt.io/zh"><strong>造訪網站 →</strong></a>
    </td>
    <td width="50%">
      <h3>▶️ 產品影片</h3>
      <p>觀看 ZSvirt 產品介紹，瞭解其核心功能與技術能力。</p>
      <a href="https://youtu.be/c6pYmlIoPIU"><strong>觀看產品影片 →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>📝 部落格</h3>
      <p>閱讀版本更新、工程實務及虛擬化技術文章。</p>
      <a href="https://zsvirt.io/zh/blog"><strong>閱讀部落格 →</strong></a>
    </td>
    <td width="50%">
      <h3>💬 GitHub Discussions</h3>
      <p>提出問題、分享想法，並與 ZSvirt 社群成員交流。</p>
      <a href="https://github.com/ZSvirt/zsvirt/discussions"><strong>參與討論 →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>▶️ YouTube</h3>
      <p>追蹤 ZSvirt 頻道，觀看產品示範、教學及專案更新。</p>
      <a href="https://youtube.com/@ZSvirt"><strong>追蹤 YouTube →</strong></a>
    </td>
    <td width="50%">
      <h3>💼 LinkedIn</h3>
      <p>追蹤 ZSvirt，掌握專案新聞、社群焦點及產業觀點。</p>
      <a href="https://www.linkedin.com/in/zsvirt-community/"><strong>追蹤 LinkedIn →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>𝕏 X</h3>
      <p>取得 ZSvirt 的最新公告及社群動態。</p>
      <a href="https://x.com/ZSvirt"><strong>追蹤 X →</strong></a>
    </td>
    <td width="50%">
      <h3>🎮 Discord 社群</h3>
      <p>加入 ZSvirt 社群，在這裡提問、分享想法，並與其他使用者交流。</p>
      <a href="https://discord.com/invite/KHsw63z9xA"><strong>加入 Discord 社群 →</strong></a>
    </td>
  </tr>
</table>
