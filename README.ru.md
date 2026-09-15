<div align="center">
  <a href="https://zsvirt.io">
    <img
      src="https://raw.githubusercontent.com/zsvirt/.github/main/assets/zsvirt-logo.jpg"
      alt="Логотип ZSvirt"
      width="180"
    >
  </a>
  <p align="center">
    <a
      href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
      target="_blank"
      rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/daily?language=Java"
        alt="ZSvirt Java за день — Trendshift"
        width="250"
        height="55"
      /></a> <a href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
       target="_blank"
       rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/weekly?language=Java"
        alt="ZSvirt Java за неделю — Trendshift"
        width="250"
        height="55"
      /></a>&nbsp;&nbsp;
  </p>
  <h1 align="center">
    Виртуализация с открытым исходным кодом
    <br>
    Готова для бизнеса, развивается сообществом
  </h1>

  <p align="center">
    <a href="https://zsvirt.io">
      <img
        src="https://img.shields.io/badge/%D0%A1%D0%B0%D0%B9%D1%82-0F62FE?style=flat-square&logo=googlechrome&logoColor=white"
        alt="Сайт"
      >
    </a>
    <a href="https://zsvirt.io/en/docs">
      <img
        src="https://img.shields.io/badge/%D0%94%D0%BE%D0%BA%D1%83%D0%BC%D0%B5%D0%BD%D1%82%D0%B0%D1%86%D0%B8%D1%8F-7C3AED?style=flat-square&logo=readthedocs&logoColor=white"
        alt="Документация"
      >
    </a>
    <a href="https://demo.zsvirt.io/">
      <img
        src="https://img.shields.io/badge/%D0%9E%D0%BD%D0%BB%D0%B0%D0%B9%D0%BD-%D0%B4%D0%B5%D0%BC%D0%BE-16A34A?style=flat-square&logo=internetcomputer&logoColor=white"
        alt="Онлайн-демо"
      >
    </a>
    <a href="https://zsvirt.io/download">
      <img
        src="https://img.shields.io/badge/%D0%A1%D0%BA%D0%B0%D1%87%D0%B0%D1%82%D1%8C-F97316?style=flat-square&logo=data:image/svg%2Bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTUgMjBoMTR2LTJINXYyem0xNC05aC00VjNIOXY4SDVsNyA3IDctN3oiLz48L3N2Zz4%3D"
        alt="Скачать"
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
    <strong>Русский</strong> |
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

> **Попробуйте сейчас — без физического оборудования.** Скачайте [образ qcow2 или OVA](https://zsvirt.io/download), запустите его как ВМ в VMware, VirtualBox, KVM или на облачном хосте (вложенная виртуализация), задайте IP-адрес — и узел управления ZSvirt готов. См. [Запуск ZSvirt внутри ВМ](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node) или [Быстрый старт](https://zsvirt.io/en/docs/quick-start).

## Что такое ZSvirt?
ZSvirt открывает доступ к проверенному в корпоративной среде движку виртуализации ZSphere от [ZStack](https://www.zstack-cloud.com/). При поддержке опытного поставщика инфраструктуры [ZStack](https://www.zstack-cloud.com/) ZSvirt предлагает лёгкую масштабируемую платформу для запуска виртуальных машин и управления ими без привязки к поставщику.

Установите узел управления, подключите хосты KVM и управляйте ВМ, кластерами, хранилищами и сетями через веб-интерфейс, RESTful API (с Terraform и SDK для Go/Python/Java) или CLI. Встроены инструменты миграции VMware: онлайн-миграция, импорт OVF и загрузка VMDK.

## Обзор продукта

<details open>
  <summary>
    <strong>📊 ПАНЕЛЬ УПРАВЛЕНИЯ — Единый обзор операций</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png"
        alt="Панель управления ZSvirt"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🗂️ ИНВЕНТАРИЗАЦИЯ — Централизованное управление инфраструктурой</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png"
        alt="Инвентаризация инфраструктуры ZSvirt"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🔄 УПРАВЛЕНИЕ МИГРАЦИЕЙ — Перенос рабочих нагрузок</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png"
        alt="Управление миграцией ZSvirt"
        width="100%"
      >
    </a>
  </p>
</details>

## Онлайн-демо

[Демо ZSvirt](https://demo.zsvirt.io/) — бесплатная размещённая среда для знакомства с платформой онлайн, без установки и регистрации. Откройте ссылку и нажмите **Demo Login**, чтобы начать.

## Архитектура

ZSvirt использует модульную архитектуру, объединяющую управление ресурсами виртуализации, плоскость управления, сервисы расширений и эксплуатационные инструменты.

Основные возможности:

- **Виртуализация вычислений**: управление хостами, кластерами, виртуальными машинами, образами и жизненными циклами.
- **Виртуализация сети**: виртуальные сети, сетевые сервисы, группы безопасности и сопутствующие функции.
- **Виртуализация хранилищ**: основные и резервные хранилища, тома, снимки и управление ресурсами хранения.
- **Плоскость управления**: API-фреймворк, модель разрешений, события, оповещения, аудит и эксплуатация системы.
- **Сервисы расширений**: миграция, аварийное восстановление, мониторинг, квоты, контроль доступа и корпоративная эксплуатация.
- **Инструменты и интеграции**: установка, диагностика, миграция, скрипты автоматизации, агенты, CLI и интеграция внешних систем.

Программная архитектура делает акцент на асинхронности, отсутствии состояния, расширяемости и автоматизации:

- **Асинхронная архитектура**: асинхронные сообщения, методы и HTTP-вызовы уменьшают блокировки и повышают пропускную способность системы.
- **Сервисы без состояния**: запросы не зависят от состояния других запросов, что упрощает масштабирование, восстановление и эксплуатацию.
- **Расширение плагинами**: горизонтальное расширение типов ресурсов, бизнес-функций и интеграций через плагины.
- **Движок рабочих процессов**: управляет порядком сложных операций и поддерживает откат и восстановление при сбоях.
- **Метки и запросы**: расширение атрибутов, классификация ресурсов, единые запросы и автоматизированная оркестрация.
- **Автоматизированное развёртывание**: средства автоматизации выполняют развёртывание, настройку и эксплуатационные задачи, снижая сложность внедрения и обслуживания.

<p align="center">
  <img
    src="./assets/zsvirt-architecture.svg"
    alt="Архитектура ZSvirt"
    width="100%"
  >
</p>

## Руководство по миграции с VMware

При пересмотре стратегий виртуализации переход с VMware на другие платформы становится важен для организаций, стремящихся контролировать расходы, повысить гибкость инфраструктуры и обеспечить долгосрочную стабильность эксплуатации.

ZSvirt предоставляет функции миграции и эксплуатационные инструменты для оценки, планирования и переноса рабочих нагрузок из существующих сред VMware на инфраструктуру виртуализации ZSvirt.

- [Руководство по миграции с VMware](https://zsvirt.io/vmware-alternative/)

<p align="center">
  <a href="https://zsvirt.io/vmware-alternative/">
    <img
      src="https://github.com/ZSvirt/.github/blob/main/assets/zvirt-migrate.png?raw=true"
      alt="Миграция с VMware на ZSvirt"
      width="100%"
    >
  </a>
</p>

## Быстрый старт

Самый быстрый способ оценить ZSvirt — пройти руководство по быстрому старту в документации. Оно объясняет подготовку вычислительных, сетевых ресурсов и хранилищ, инициализацию сервиса управления и создание первой виртуальной машины.

Хотите сначала попробовать в ВМ? Скачайте [образ qcow2 или OVA](https://zsvirt.io/download) и запустите узел управления внутри гипервизора — см. [Запуск ZSvirt внутри ВМ](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node).

🚀 [Быстрый старт](https://zsvirt.io/en/docs/quick-start)<br>
▶️ [Видео](https://youtu.be/LsSJlBRUvYw)

## Лучшие практики
ZSvirt использует тот же корпоративный движок, что и ZSphere, и опирается на проверенные результаты внедрений у представленных ниже клиентов по всему миру.
<p align="center">
  <img
    src="https://github.com/ZSvirt/.github/blob/main/assets/zsvirt-partner-en.png?raw=true"
    alt="Клиенты и партнёры ZSvirt по всему миру"
    width="100%"
  >
</p>

## Сравнение платформ виртуализации

Сравнение Proxmox VE, VMware vSphere и ZSvirt

<p align="center">
  <img
    src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-comparison.png"
    alt="Сравнение Proxmox VE и ZSvirt"
    width="100%"
  >
</p>


## Управление проектом

ZSvirt следует лёгкой модели управления открытым проектом, определяющей сопровождение, принятие решений и взаимодействие участников.

В [GOVERNANCE.md](GOVERNANCE.md) описаны роли проекта, обязанности сопровождающих, принятие решений, управление выпусками и сотрудничество сообщества.

С ростом сообщества модель может включать выделенных сопровождающих, рабочие группы и более формальные процессы.

## Участие в проекте

Мы приветствуем и ценим вклад сообщества. Исправление ошибок, улучшение документации, предложения функций, тесты и обмен практиками развёртывания, миграции и эксплуатации помогают улучшать ZSvirt.

Новички могут начать с документации, сообщений о проблемах, проверки тестов, опыта миграции или обсуждений. Разработчики также могут улучшать код, инструменты и интеграции.

Активные участники могут получить признание в благодарностях сообщества, примечаниях к выпускам, списках участников или будущих программах сообщества.

Перед участием прочитайте:

- [CONTRIBUTING.md](CONTRIBUTING.md)

## Безопасность

Процедура сообщения об уязвимостях описана в [SECURITY.md](SECURITY.md).

Не сообщайте об уязвимостях через публичные GitHub Issues или Discussions.

## Лицензия

ZSvirt распространяется по лицензии [GNU General Public License v3.0](LICENSE).

Некоторые репозитории или компоненты могут включать стороннее ПО с открытым исходным кодом под другими лицензиями. Подробности см. в `LICENSE`, `NOTICE` и связанных файлах каждого репозитория.

## Ресурсы

<table>
  <tr>
    <td width="50%">
      <h3>🌐 Сайт сообщества</h3>
      <p>Узнайте о функциях, сценариях использования, новостях и ресурсах сообщества ZSvirt.</p>
      <a href="https://zsvirt.io"><strong>Перейти на сайт →</strong></a>
    </td>
    <td width="50%">
      <h3>▶️ Видео</h3>
      <p>Посмотрите презентацию ZSvirt и познакомьтесь с основными возможностями.</p>
      <a href="https://youtu.be/c6pYmlIoPIU"><strong>Смотреть видео о продукте →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>📝 Блог</h3>
      <p>Читайте новости выпусков, истории разработки и материалы о виртуализации.</p>
      <a href="https://zsvirt.io/blog"><strong>Читать блог →</strong></a>
    </td>
    <td width="50%">
      <h3>💬 GitHub Discussions</h3>
      <p>Задавайте вопросы, делитесь идеями и общайтесь с сообществом ZSvirt.</p>
      <a href="https://github.com/ZSvirt/zsvirt/discussions"><strong>Присоединиться к обсуждению →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>▶️ YouTube</h3>
      <p>Подпишитесь на канал ZSvirt с демонстрациями, учебными материалами и обновлениями продукта.</p>
      <a href="https://youtube.com/@ZSvirt"><strong>Подписаться на YouTube →</strong></a>
    </td>
    <td width="50%">
      <h3>💼 LinkedIn</h3>
      <p>Следите за новостями проекта, событиями сообщества и отраслевыми обзорами.</p>
      <a href="https://www.linkedin.com/in/zsvirt-community/"><strong>Подписаться на LinkedIn →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>𝕏 X</h3>
      <p>Получайте свежие объявления ZSvirt и новости сообщества.</p>
      <a href="https://x.com/ZSvirt"><strong>Подписаться на X →</strong></a>
    </td>
    <td width="50%">
      <h3>🎮 Discord</h3>
      <p>Присоединяйтесь к сообществу ZSvirt, чтобы задавать вопросы, делиться идеями и знакомиться с другими пользователями.</p>
      <a href="https://discord.com/invite/KHsw63z9xA"><strong>Присоединиться в Discord →</strong></a>
    </td>
  </tr>
</table>
