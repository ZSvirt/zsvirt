<div align="center">
  <a href="https://zsvirt.io">
    <img
      src="https://raw.githubusercontent.com/zsvirt/.github/main/assets/zsvirt-logo.jpg"
      alt="ZSvirt 로고"
      width="180"
    >
  </a>
  <p align="center">
    <a
      href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
      target="_blank"
      rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/daily?language=Java"
        alt="ZSvirt/zsvirt 일간 Java 순위 | Trendshift"
        width="250"
        height="55"
      /></a> <a href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
       target="_blank"
       rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/weekly?language=Java"
        alt="ZSvirt/zsvirt 주간 Java 순위 | Trendshift"
        width="250"
        height="55"
      /></a>&nbsp;&nbsp;
  </p>
  <h1 align="center">
    오픈 소스 가상화
    <br>
    엔터프라이즈급 완성도, 커뮤니티 주도
  </h1>

  <p align="center">
    <a href="https://zsvirt.io">
      <img
        src="https://img.shields.io/badge/%EC%9B%B9%EC%82%AC%EC%9D%B4%ED%8A%B8-0F62FE?style=flat-square&logo=googlechrome&logoColor=white"
        alt="ZSvirt 웹사이트"
      >
    </a>
    <a href="https://zsvirt.io/en/docs">
      <img
        src="https://img.shields.io/badge/%EB%AC%B8%EC%84%9C-7C3AED?style=flat-square&logo=readthedocs&logoColor=white"
        alt="문서"
      >
    </a>
    <a href="https://demo.zsvirt.io/">
      <img
        src="https://img.shields.io/badge/%EB%9D%BC%EC%9D%B4%EB%B8%8C%20%EB%8D%B0%EB%AA%A8-16A34A?style=flat-square&logo=internetcomputer&logoColor=white"
        alt="라이브 데모"
      >
    </a>
    <a href="https://zsvirt.io/download">
      <img
        src="https://img.shields.io/badge/%EB%8B%A4%EC%9A%B4%EB%A1%9C%EB%93%9C-F97316?style=flat-square&logo=data:image/svg%2Bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTUgMjBoMTR2LTJINXYyem0xNC05aC00VjNIOXY4SDVsNyA3IDctN3oiLz48L3N2Zz4%3D"
        alt="다운로드"
      >
    </a>
  </p>

  <p align="center" dir="ltr">
    <a href="./README.md">English</a> |
    <a href="./README_zh.md">简体中文</a> |
    <a href="./README.zht.md">繁體中文</a> |
    <strong>한국어</strong> |
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

> **지금 바로 사용해 보세요. 물리 하드웨어가 필요하지 않습니다.** [qcow2 또는 OVA 이미지](https://zsvirt.io/download)를 받아 VMware, VirtualBox, KVM 또는 클라우드 호스트(중첩 가상화)에서 VM으로 부팅하고 IP를 설정하면 ZSvirt 관리 노드가 준비됩니다. [VM 내부에서 ZSvirt 실행](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node)을 참고하거나 [빠른 시작](https://zsvirt.io/en/docs/quick-start)으로 바로 이동하세요.

## ZSvirt란
ZSvirt는 [ZStack](https://www.zstack-cloud.com/)이 엔터프라이즈 환경에서 검증한 ZSphere 가상화 엔진을 오픈 소스 세계에 제공합니다. 성숙한 인프라 선도 기업인 [ZStack](https://www.zstack-cloud.com/)의 지원을 받는 ZSvirt는 공급업체 종속 없이 가상 머신을 실행하고 관리할 수 있는 가볍고 확장 가능한 플랫폼입니다.

관리 노드를 설치하고 KVM 기반 호스트를 연결하면 웹 UI, RESTful API(Terraform 및 Go/Python/Java SDK 지원) 또는 CLI를 통해 VM, 클러스터, 스토리지, 네트워크 등 전체 환경을 관리할 수 있습니다. 온라인 마이그레이션, OVF 가져오기, VMDK 업로드와 같은 VMware 마이그레이션 도구도 기본으로 제공됩니다.

## 제품 둘러보기

<details open>
  <summary>
    <strong>📊 대시보드 — 통합 운영 개요</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png"
        alt="ZSvirt 통합 운영 대시보드"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🗂️ 인벤토리 — 중앙 집중식 인프라 관리</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png"
        alt="ZSvirt 중앙 집중식 인프라 인벤토리"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🔄 마이그레이션 관리 — 워크로드 마이그레이션</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png"
        alt="ZSvirt 마이그레이션 관리"
        width="100%"
      >
    </a>
  </p>
</details>

## 라이브 데모

[ZSvirt 라이브 데모](https://demo.zsvirt.io/)는 설치나 가입 없이 온라인에서 ZSvirt를 체험할 수 있는 무료 호스팅 환경입니다. 링크를 열고 **Demo Login**을 클릭하면 즉시 플랫폼을 둘러볼 수 있습니다.

## 아키텍처

ZSvirt는 가상화 리소스 관리, 관리 플레인, 확장 서비스 및 운영 도구를 중심으로 구성된 모듈식 아키텍처를 사용합니다.

핵심 기능은 다음과 같습니다.

- **컴퓨팅 가상화**: 호스트, 클러스터, 가상 머신, 이미지 및 수명 주기 관리.
- **네트워크 가상화**: 가상 네트워크, 네트워크 서비스, 보안 그룹 및 관련 기능.
- **스토리지 가상화**: 기본 스토리지, 백업 스토리지, 볼륨, 스냅샷 및 스토리지 리소스 관리.
- **관리 플레인**: API 프레임워크, 권한 모델, 이벤트, 경보, 감사 및 시스템 운영.
- **확장 서비스**: 마이그레이션, 재해 복구, 모니터링, 할당량 관리, 액세스 제어 및 엔터프라이즈 운영 기능.
- **도구 및 통합**: 설치 도구, 진단 도구, 마이그레이션 도구, 자동화 스크립트, 에이전트, CLI 및 외부 시스템 통합.

소프트웨어 아키텍처 수준에서 ZSvirt는 비동기성, 무상태성, 확장성 및 자동화를 강조합니다.

- **비동기 아키텍처**: 비동기 메시지, 비동기 메서드 및 비동기 HTTP 호출을 지원하여 차단을 줄이고 시스템 처리량을 높입니다.
- **무상태 서비스**: 개별 요청이 다른 요청의 상태에 의존하지 않으므로 서비스 확장, 복구 및 운영이 더 쉬워집니다.
- **플러그인 기반 확장성**: 플러그인을 통해 리소스 유형, 비즈니스 기능 및 통합 기능을 수평으로 확장할 수 있습니다.
- **워크플로 엔진**: 복잡한 작업의 실행 순서를 관리하고 장애 상황에서 롤백 및 복구를 지원합니다.
- **태깅 및 쿼리 기능**: 리소스 속성 확장, 리소스 분류, 통합 쿼리 및 자동화 오케스트레이션을 지원합니다.
- **자동 배포**: 자동화 도구를 사용해 배포, 구성 및 운영 작업을 처리하여 배포와 유지 관리의 복잡성을 줄입니다.

<p align="center">
  <img
    src="./assets/zsvirt-architecture.svg"
    alt="ZSvirt 아키텍처"
    width="100%"
  >
</p>

## VMware 마이그레이션 가이드

기업이 가상화 전략을 재검토하면서 비용 관리, 인프라 유연성 및 장기적인 운영 안정성을 추구하는 조직에 VMware에서 대체 플랫폼으로의 마이그레이션이 중요한 주제가 되었습니다.

ZSvirt는 사용자가 기존 VMware 환경에서 ZSvirt 기반 가상화 인프라로 워크로드를 평가하고 계획하여 이전할 수 있도록 마이그레이션 중심 기능과 운영 도구를 제공합니다.

- [VMware 마이그레이션 가이드](https://zsvirt.io/vmware-alternative/)

<p align="center">
  <a href="https://zsvirt.io/vmware-alternative/">
    <img
      src="https://github.com/ZSvirt/.github/blob/main/assets/zvirt-migrate.png?raw=true"
      alt="VMware에서 ZSvirt로 마이그레이션"
      width="100%"
    >
  </a>
</p>

## 빠른 시작

ZSvirt를 가장 빠르게 평가하려면 제품 문서의 빠른 시작 가이드를 따르세요. 이 가이드는 컴퓨팅, 네트워크 및 스토리지 리소스를 준비하고 관리 서비스를 초기화한 후 첫 번째 가상 머신을 만드는 과정을 안내합니다.

먼저 VM에서 사용해 보고 싶으신가요? [qcow2 또는 OVA 이미지](https://zsvirt.io/download)를 다운로드해 원하는 하이퍼바이저 안에서 관리 노드를 실행하세요. 자세한 내용은 [VM 내부에서 ZSvirt 실행](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node)을 참고하세요.

🚀 [빠른 시작](https://zsvirt.io/en/docs/quick-start)<br>
▶️ [동영상](https://youtu.be/LsSJlBRUvYw)

## 모범 사례
ZSvirt는 ZSphere와 동일한 엔터프라이즈 엔진을 기반으로 하며, 아래와 같은 전 세계 고객을 통해 검증된 성과를 이어받았습니다.
<p align="center">
  <img
    src="https://github.com/ZSvirt/.github/blob/main/assets/zsvirt-partner-en.png?raw=true"
    alt="ZSvirt의 전 세계 고객 및 파트너"
    width="100%"
  >
</p>

## 가상화 플랫폼 비교

Proxmox VE, VMware vSphere, ZSvirt 비교

<p align="center">
  <img
    src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-comparison.png"
    alt="Proxmox VE와 ZSvirt 가상화 플랫폼 비교"
    width="100%"
  >
</p>


## 거버넌스

ZSvirt는 프로젝트 유지 관리 방식, 의사 결정 방식 및 기여자 협업 방식을 정의하는 간결한 오픈 소스 거버넌스 모델에 따라 운영됩니다.

[GOVERNANCE.md](GOVERNANCE.md) 문서는 프로젝트 역할, 메인테이너의 책임, 의사 결정 절차, 릴리스 관리 및 커뮤니티 협업을 알아보기 위한 출발점입니다.

커뮤니티가 성장함에 따라 전담 메인테이너, 워킹 그룹 및 더욱 공식적인 프로젝트 절차를 포함하도록 거버넌스 모델이 발전할 수 있습니다.

## 기여하기

커뮤니티의 기여를 환영하며 소중히 생각합니다. 버그 수정, 문서 개선, 기능 제안, 테스트 추가 또는 배포, 마이그레이션 및 운영 사례 공유 등 모든 기여는 ZSvirt를 더욱 발전시키는 데 도움이 됩니다.

프로젝트가 처음이라면 문서 개선, 이슈 보고, 테스트 검증, 마이그레이션 경험 공유 또는 커뮤니티 토론부터 시작할 수 있습니다. 개발자의 코드 개선, 도구 개선 및 통합 기여도 환영합니다.

활발하게 활동하는 기여자는 커뮤니티 감사 표시, 릴리스 노트, 기여자 목록 또는 향후 커뮤니티 프로그램을 통해 소개될 수 있습니다.

기여하기 전에 다음 문서를 읽어 주세요.

- [CONTRIBUTING.md](CONTRIBUTING.md)

## 보안

취약점 신고를 위한 보안 절차는 [SECURITY.md](SECURITY.md)에 설명되어 있습니다.

보안 취약점을 공개 GitHub Issues 또는 Discussions를 통해 신고하지 마세요.

## 라이선스

ZSvirt는 [GNU General Public License v3.0](LICENSE)에 따라 라이선스가 부여됩니다.

일부 리포지토리 또는 구성 요소에는 다른 라이선스가 적용되는 타사 오픈 소스 소프트웨어가 포함될 수 있습니다. 자세한 내용은 각 리포지토리의 `LICENSE`, `NOTICE` 및 관련 파일을 확인하세요.

## 리소스

<table>
  <tr>
    <td width="50%">
      <h3>🌐 커뮤니티 웹사이트</h3>
      <p>ZSvirt의 기능, 사용 사례, 뉴스 및 커뮤니티 리소스를 살펴보세요.</p>
      <a href="https://zsvirt.io"><strong>웹사이트 방문 →</strong></a>
    </td>
    <td width="50%">
      <h3>▶️ 동영상</h3>
      <p>ZSvirt 제품 소개 영상을 보고 핵심 기능을 알아보세요.</p>
      <a href="https://youtu.be/c6pYmlIoPIU"><strong>제품 동영상 보기 →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>📝 블로그</h3>
      <p>릴리스 업데이트, 엔지니어링 이야기 및 가상화에 관한 통찰을 확인하세요.</p>
      <a href="https://zsvirt.io/blog"><strong>블로그 읽기 →</strong></a>
    </td>
    <td width="50%">
      <h3>💬 GitHub Discussions</h3>
      <p>질문하고 아이디어를 공유하며 ZSvirt 커뮤니티와 소통하세요.</p>
      <a href="https://github.com/ZSvirt/zsvirt/discussions"><strong>토론 참여 →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>▶️ YouTube</h3>
      <p>데모, 튜토리얼 및 제품 업데이트를 보려면 ZSvirt 채널을 팔로우하세요.</p>
      <a href="https://youtube.com/@ZSvirt"><strong>YouTube에서 팔로우 →</strong></a>
    </td>
    <td width="50%">
      <h3>💼 LinkedIn</h3>
      <p>프로젝트 소식, 커뮤니티 주요 소식 및 업계 통찰을 보려면 ZSvirt를 팔로우하세요.</p>
      <a href="https://www.linkedin.com/in/zsvirt-community/"><strong>LinkedIn에서 팔로우 →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>𝕏 X</h3>
      <p>최신 ZSvirt 공지와 커뮤니티 업데이트를 받아보세요.</p>
      <a href="https://x.com/ZSvirt"><strong>X에서 팔로우 →</strong></a>
    </td>
    <td width="50%">
      <h3>🎮 Discord</h3>
      <p>ZSvirt 커뮤니티에 참여하여 질문하고 아이디어를 공유하며 다른 사용자들과 소통하세요.</p>
      <a href="https://discord.com/invite/KHsw63z9xA"><strong>Discord 참여 →</strong></a>
    </td>
  </tr>
</table>
