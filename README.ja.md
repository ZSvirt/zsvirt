<div align="center">
  <a href="https://zsvirt.io">
    <img
      src="https://raw.githubusercontent.com/zsvirt/.github/main/assets/zsvirt-logo.jpg"
      alt="ZSvirt ロゴ"
      width="180"
    >
  </a>
  <p align="center">
    <a
      href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
      target="_blank"
      rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/daily?language=Java"
        alt="ZSvirt/zsvirt の日次 Java ランキング | Trendshift"
        width="250"
        height="55"
      /></a> <a href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
       target="_blank"
       rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/weekly?language=Java"
        alt="ZSvirt/zsvirt の週次 Java ランキング | Trendshift"
        width="250"
        height="55"
      /></a>&nbsp;&nbsp;
  </p>
  <h1 align="center">
    オープンソース仮想化
    <br>
    エンタープライズ対応、コミュニティ主導
  </h1>

  <p align="center">
    <a href="https://zsvirt.io">
      <img
        src="https://img.shields.io/badge/%E3%82%A6%E3%82%A7%E3%83%96%E3%82%B5%E3%82%A4%E3%83%88-0F62FE?style=flat-square&logo=googlechrome&logoColor=white"
        alt="ZSvirt ウェブサイト"
      >
    </a>
    <a href="https://zsvirt.io/en/docs">
      <img
        src="https://img.shields.io/badge/%E3%83%89%E3%82%AD%E3%83%A5%E3%83%A1%E3%83%B3%E3%83%88-7C3AED?style=flat-square&logo=readthedocs&logoColor=white"
        alt="ドキュメント"
      >
    </a>
    <a href="https://demo.zsvirt.io/">
      <img
        src="https://img.shields.io/badge/%E3%83%A9%E3%82%A4%E3%83%96%20%E3%83%87%E3%83%A2-16A34A?style=flat-square&logo=internetcomputer&logoColor=white"
        alt="ライブデモ"
      >
    </a>
    <a href="https://zsvirt.io/download">
      <img
        src="https://img.shields.io/badge/%E3%83%80%E3%82%A6%E3%83%B3%E3%83%AD%E3%83%BC%E3%83%89-F97316?style=flat-square&logo=data:image/svg%2Bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTUgMjBoMTR2LTJINXYyem0xNC05aC00VjNIOXY4SDVsNyA3IDctN3oiLz48L3N2Zz4%3D"
        alt="ダウンロード"
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
    <strong>日本語</strong> |
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

> **今すぐ試す — 物理ハードウェアは不要です。** [qcow2 または OVA イメージ](https://zsvirt.io/download)を入手し、VMware、VirtualBox、KVM、またはクラウドホスト上で VM として起動（ネステッド仮想化）して IP を設定すれば、ZSvirt 管理ノードを利用できます。[VM 内で ZSvirt を実行する](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node)を参照するか、[クイックスタート](https://zsvirt.io/en/docs/quick-start)に進んでください。

## ZSvirt とは
ZSvirt は、[ZStack](https://www.zstack-cloud.com/) でエンタープライズ環境における実績を持つ ZSphere 仮想化エンジンを、オープンソースの世界に提供します。成熟したインフラストラクチャ企業である [ZStack](https://www.zstack-cloud.com/) の支援を受け、ZSvirt はベンダーロックインなしで仮想マシンを実行・管理できる、軽量でスケーラブルなプラットフォームです。

管理ノードをインストールして KVM ベースのホストを接続すれば、Web UI、RESTful API（Terraform および Go/Python/Java SDK に対応）、または CLI を通じて、VM、クラスター、ストレージ、ネットワークを含む環境全体を管理できます。オンライン移行、OVF インポート、VMDK アップロードなどの VMware 移行ツールも組み込まれています。

## 製品ツアー

<details open>
  <summary>
    <strong>📊 ダッシュボード — 統合運用概要</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png"
        alt="ZSvirt 統合運用ダッシュボード"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🗂️ インベントリ — インフラストラクチャの一元管理</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png"
        alt="ZSvirt インフラストラクチャの一元インベントリ"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🔄 移行管理 — ワークロード移行</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png"
        alt="ZSvirt 移行管理"
        width="100%"
      >
    </a>
  </p>
</details>

## ライブデモ

[ZSvirt ライブデモ](https://demo.zsvirt.io/)は、ZSvirt をオンラインで試せる無料のホスティング環境です。インストールもサインアップも必要ありません。リンクを開いて **Demo Login** をクリックすると、すぐにプラットフォームを試すことができます。

## アーキテクチャ

ZSvirt は、仮想化リソース管理、管理プレーン、拡張サービス、運用ツールを中心としたモジュール型アーキテクチャを採用しています。

主な機能は次のとおりです。

- **コンピュート仮想化**：ホスト、クラスター、仮想マシン、イメージ、およびライフサイクルの管理。
- **ネットワーク仮想化**：仮想ネットワーク、ネットワークサービス、セキュリティグループ、および関連機能。
- **ストレージ仮想化**：プライマリストレージ、バックアップストレージ、ボリューム、スナップショット、およびストレージリソースの管理。
- **管理プレーン**：API フレームワーク、権限モデル、イベント、アラーム、監査、およびシステム運用。
- **拡張サービス**：移行、災害復旧、監視、クォータ管理、アクセス制御、およびエンタープライズ運用のための機能。
- **ツールと連携**：インストールツール、診断ツール、移行ツール、自動化スクリプト、エージェント、CLI、および外部システム連携。

ソフトウェアアーキテクチャの観点では、ZSvirt は非同期処理、ステートレス性、拡張性、自動化を重視しています。

- **非同期アーキテクチャ**：非同期メッセージ、非同期メソッド、非同期 HTTP 呼び出しに対応し、ブロッキングを減らしてシステムのスループットを向上させます。
- **ステートレスサービス**：個々のリクエストは他のリクエストの状態に依存しないため、サービスのスケーリング、復旧、運用が容易になります。
- **プラグインベースの拡張性**：プラグインを通じて、リソースタイプ、ビジネス機能、連携機能を水平方向に拡張できます。
- **ワークフローエンジン**：複雑な処理の実行順序を管理し、障害発生時のロールバックと復旧に対応します。
- **タグ付けとクエリ機能**：リソース属性の拡張、リソースの分類、統合クエリ、自動化オーケストレーションに対応します。
- **自動デプロイ**：自動化ツールを使用してデプロイ、設定、運用タスクを処理し、デプロイと保守の複雑さを軽減します。

<p align="center">
  <img
    src="./assets/zsvirt-architecture.svg"
    alt="ZSvirt アーキテクチャ"
    width="100%"
  >
</p>

## VMware 移行ガイド

企業が仮想化戦略を見直す中、VMware から代替プラットフォームへの移行は、コスト管理、インフラストラクチャの柔軟性、長期的な運用安定性を求める組織にとって重要なテーマとなっています。

ZSvirt は、既存の VMware 環境から ZSvirt ベースの仮想化インフラストラクチャへワークロードを評価、計画、移行できるよう、移行向けの機能と運用ツールを提供します。

- [VMware 移行ガイド](https://zsvirt.io/vmware-alternative/)

<p align="center">
  <a href="https://zsvirt.io/vmware-alternative/">
    <img
      src="https://github.com/ZSvirt/.github/blob/main/assets/zvirt-migrate.png?raw=true"
      alt="VMware から ZSvirt への移行"
      width="100%"
    >
  </a>
</p>

## クイックスタート

ZSvirt を最も手早く評価するには、製品ドキュメントのクイックスタートガイドを参照してください。コンピュート、ネットワーク、ストレージの各リソースの準備、管理サービスの初期化、最初の仮想マシンの作成までを説明しています。

まず VM で試したい場合は、[qcow2 または OVA イメージ](https://zsvirt.io/download)をダウンロードし、任意のハイパーバイザー内で管理ノードを実行してください。詳しくは、[VM 内で ZSvirt を実行する](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node)を参照してください。

🚀 [クイックスタート](https://zsvirt.io/en/docs/quick-start)<br>
▶️ [動画](https://youtu.be/LsSJlBRUvYw)

## ベストプラクティス
ZSvirt は ZSphere と同じエンタープライズ向けエンジンを基盤とし、以下の世界各地のお客様における実績を受け継いでいます。
<p align="center">
  <img
    src="https://github.com/ZSvirt/.github/blob/main/assets/zsvirt-partner-en.png?raw=true"
    alt="ZSvirt の世界各地のお客様とパートナー"
    width="100%"
  >
</p>

## 仮想化プラットフォームの比較

Proxmox VE vs VMware vSphere vs ZSvirt

<p align="center">
  <img
    src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-comparison.png"
    alt="Proxmox VE と ZSvirt の仮想化プラットフォーム比較"
    width="100%"
  >
</p>


## ガバナンス

ZSvirt は、プロジェクトの保守方法、意思決定の方法、コントリビューターの協働方法を定めた、簡潔なオープンソースガバナンスモデルに基づいて運営されています。

[GOVERNANCE.md](GOVERNANCE.md) は、プロジェクト内の役割、メンテナーの責任、意思決定プロセス、リリース管理、コミュニティでの協働について学ぶための出発点です。

コミュニティの成長に伴い、ガバナンスモデルには専任メンテナー、ワーキンググループ、より正式なプロジェクトプロセスが加わる可能性があります。

## コントリビューション

コミュニティからのコントリビューションを歓迎し、感謝しています。バグ修正、ドキュメントの改善、機能の提案、テストの追加、デプロイ・移行・運用の実践例の共有など、皆様の貢献が ZSvirt をより良くします。

プロジェクトへの参加が初めての方は、ドキュメントの改善、Issue の報告、テスト検証、移行経験の共有、コミュニティでの議論から始められます。開発者によるコードの改善、ツールの強化、連携機能のコントリビューションも歓迎します。

活発に貢献してくださる方を、コミュニティからの謝辞、リリースノート、コントリビューター一覧、または今後のコミュニティプログラムで紹介する場合があります。

コントリビューションの前に、以下をお読みください。

- [CONTRIBUTING.md](CONTRIBUTING.md)

## セキュリティ

脆弱性を報告するためのセキュリティプロセスについては、[SECURITY.md](SECURITY.md) を参照してください。

セキュリティ脆弱性を公開の GitHub Issues や Discussions で報告しないでください。

## ライセンス

ZSvirt は [GNU General Public License v3.0](LICENSE) の下でライセンスされています。

一部のリポジトリまたはコンポーネントには、異なるライセンスのサードパーティ製オープンソースソフトウェアが含まれる場合があります。詳細については、各リポジトリの `LICENSE`、`NOTICE`、および関連ファイルを確認してください。

## リソース

<table>
  <tr>
    <td width="50%">
      <h3>🌐 コミュニティウェブサイト</h3>
      <p>ZSvirt の機能、ユースケース、ニュース、コミュニティリソースをご覧ください。</p>
      <a href="https://zsvirt.io"><strong>ウェブサイトを見る →</strong></a>
    </td>
    <td width="50%">
      <h3>▶️ 動画</h3>
      <p>ZSvirt の製品紹介を視聴し、その中核機能をご覧ください。</p>
      <a href="https://youtu.be/c6pYmlIoPIU"><strong>製品動画を見る →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>📝 ブログ</h3>
      <p>リリース情報、エンジニアリングストーリー、仮想化に関する知見をご覧ください。</p>
      <a href="https://zsvirt.io/blog"><strong>ブログを読む →</strong></a>
    </td>
    <td width="50%">
      <h3>💬 GitHub Discussions</h3>
      <p>質問やアイデアを共有し、ZSvirt コミュニティと交流できます。</p>
      <a href="https://github.com/ZSvirt/zsvirt/discussions"><strong>ディスカッションに参加 →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>▶️ YouTube</h3>
      <p>デモ、チュートリアル、製品アップデートを紹介する ZSvirt チャンネルをフォローしてください。</p>
      <a href="https://youtube.com/@ZSvirt"><strong>YouTube でフォロー →</strong></a>
    </td>
    <td width="50%">
      <h3>💼 LinkedIn</h3>
      <p>ZSvirt のプロジェクトニュース、コミュニティのハイライト、業界の知見をフォローしてください。</p>
      <a href="https://www.linkedin.com/in/zsvirt-community/"><strong>LinkedIn でフォロー →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>𝕏 X</h3>
      <p>ZSvirt の最新のお知らせとコミュニティ情報を入手できます。</p>
      <a href="https://x.com/ZSvirt"><strong>X でフォロー →</strong></a>
    </td>
    <td width="50%">
      <h3>🎮 Discord</h3>
      <p>ZSvirt コミュニティに参加して、質問やアイデアを共有し、他のユーザーと交流できます。</p>
      <a href="https://discord.com/invite/KHsw63z9xA"><strong>Discord に参加 →</strong></a>
    </td>
  </tr>
</table>
