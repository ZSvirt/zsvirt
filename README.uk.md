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
        alt="ZSvirt Java за тиждень — Trendshift"
        width="250"
        height="55"
      /></a>&nbsp;&nbsp;
  </p>
  <h1 align="center">
    Віртуалізація з відкритим кодом
    <br>
    Готова для підприємств, розвивається спільнотою
  </h1>

  <p align="center">
    <a href="https://zsvirt.io">
      <img
        src="https://img.shields.io/badge/%D0%92%D0%B5%D0%B1%D1%81%D0%B0%D0%B9%D1%82-0F62FE?style=flat-square&logo=googlechrome&logoColor=white"
        alt="Вебсайт"
      >
    </a>
    <a href="https://zsvirt.io/en/docs">
      <img
        src="https://img.shields.io/badge/%D0%94%D0%BE%D0%BA%D1%83%D0%BC%D0%B5%D0%BD%D1%82%D0%B0%D1%86%D1%96%D1%8F-7C3AED?style=flat-square&logo=readthedocs&logoColor=white"
        alt="Документація"
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
        src="https://img.shields.io/badge/%D0%97%D0%B0%D0%B2%D0%B0%D0%BD%D1%82%D0%B0%D0%B6%D0%B5%D0%BD%D0%BD%D1%8F-F97316?style=flat-square&logo=data:image/svg%2Bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTUgMjBoMTR2LTJINXYyem0xNC05aC00VjNIOXY4SDVsNyA3IDctN3oiLz48L3N2Zz4%3D"
        alt="Завантаження"
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
    <a href="./README.no.md">Norsk</a> |
    <a href="./README.br.md">Português (Brasil)</a> |
    <a href="./README.th.md">ไทย</a> |
    <a href="./README.tr.md">Türkçe</a> |
    <strong>Українська</strong> |
    <a href="./README.bn.md">বাংলা</a> |
    <a href="./README.gr.md">Ελληνικά</a> |
    <a href="./README.vi.md">Tiếng Việt</a>
  </p>
</div>

> **Спробуйте зараз — без фізичного обладнання.** Завантажте [образ qcow2 або OVA](https://zsvirt.io/download), запустіть його як ВМ у VMware, VirtualBox, KVM або на хмарному хості (вкладена віртуалізація), налаштуйте IP-адресу — і вузол керування ZSvirt готовий. Див. [Запуск ZSvirt усередині ВМ](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node) або [Швидкий старт](https://zsvirt.io/en/docs/quick-start).

## Що таке ZSvirt?
ZSvirt відкриває доступ до перевіреного в корпоративному середовищі рушія віртуалізації ZSphere від [ZStack](https://www.zstack-cloud.com/). За підтримки досвідченого постачальника інфраструктури [ZStack](https://www.zstack-cloud.com/) ZSvirt пропонує легку масштабовану платформу для запуску віртуальних машин і керування ними без прив’язки до постачальника.

Установіть вузол керування, під’єднайте хости KVM і керуйте ВМ, кластерами, сховищами та мережами через вебінтерфейс, RESTful API (із Terraform і SDK Go/Python/Java) або CLI. Вбудовано інструменти міграції VMware: онлайн-міграцію, імпорт OVF і завантаження VMDK.

## Огляд продукту

<details open>
  <summary>
    <strong>📊 ПАНЕЛЬ — Єдиний огляд операцій</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png"
        alt="Операційна панель ZSvirt"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🗂️ ІНВЕНТАРИЗАЦІЯ — Централізоване керування інфраструктурою</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png"
        alt="Інвентаризація інфраструктури ZSvirt"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🔄 КЕРУВАННЯ МІГРАЦІЯМИ — Перенесення робочих навантажень</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png"
        alt="Керування міграціями ZSvirt"
        width="100%"
      >
    </a>
  </p>
</details>

## Онлайн-демо

[Демо ZSvirt](https://demo.zsvirt.io/) — безкоштовне розміщене середовище для знайомства з платформою онлайн без установлення та реєстрації. Відкрийте посилання й натисніть **Demo Login**, щоб почати.

## Архітектура

ZSvirt має модульну архітектуру, що охоплює керування ресурсами віртуалізації, площину керування, сервіси розширень та експлуатаційні інструменти.

Основні можливості:

- **Віртуалізація обчислень**: керування хостами, кластерами, віртуальними машинами, образами та життєвими циклами.
- **Віртуалізація мережі**: віртуальні мережі, мережеві сервіси, групи безпеки та пов’язані функції.
- **Віртуалізація сховищ**: основні й резервні сховища, томи, знімки та керування ресурсами зберігання.
- **Площина керування**: API-фреймворк, модель дозволів, події, сповіщення, аудит та експлуатація системи.
- **Сервіси розширень**: міграція, аварійне відновлення, моніторинг, квоти, контроль доступу та корпоративні операції.
- **Інструменти й інтеграції**: установлення, діагностика, міграція, сценарії автоматизації, агенти, CLI та інтеграція зовнішніх систем.

Програмна архітектура зосереджена на асинхронності, відсутності стану, розширюваності й автоматизації:

- **Асинхронна архітектура**: асинхронні повідомлення, методи та HTTP-виклики зменшують блокування й підвищують пропускну здатність системи.
- **Сервіси без стану**: запити не залежать від стану інших запитів, що спрощує масштабування, відновлення й експлуатацію.
- **Розширення плагінами**: горизонтальне розширення типів ресурсів, бізнес-функцій та інтеграцій через плагіни.
- **Рушій робочих процесів**: керує порядком складних операцій і підтримує відкат та відновлення в разі збоїв.
- **Мітки й запити**: розширення атрибутів, класифікація ресурсів, єдині запити та автоматизована оркестрація.
- **Автоматизоване розгортання**: інструменти автоматизують розгортання, налаштування й операції, знижуючи складність упровадження та обслуговування.

<p align="center">
  <img
    src="./assets/zsvirt-architecture.svg"
    alt="Архітектура ZSvirt"
    width="100%"
  >
</p>

## Посібник із міграції з VMware

Коли підприємства переглядають стратегії віртуалізації, перехід із VMware на інші платформи стає важливим для контролю витрат, гнучкості інфраструктури й довгострокової операційної стабільності.

ZSvirt надає функції міграції та експлуатаційні інструменти для оцінювання, планування й перенесення робочих навантажень з наявних середовищ VMware на інфраструктуру віртуалізації ZSvirt.

- [Посібник із міграції з VMware](https://zsvirt.io/vmware-alternative/)

<p align="center">
  <a href="https://zsvirt.io/vmware-alternative/">
    <img
      src="https://github.com/ZSvirt/.github/blob/main/assets/zvirt-migrate.png?raw=true"
      alt="Міграція з VMware на ZSvirt"
      width="100%"
    >
  </a>
</p>

## Швидкий старт

Найшвидше оцінити ZSvirt допоможе посібник зі швидкого старту в документації. Він описує підготовку обчислювальних, мережевих ресурсів і сховищ, ініціалізацію сервісу керування та створення першої віртуальної машини.

Хочете спершу спробувати у ВМ? Завантажте [образ qcow2 або OVA](https://zsvirt.io/download) і запустіть вузол керування в гіпервізорі — див. [Запуск ZSvirt усередині ВМ](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node).

🚀 [Швидкий старт](https://zsvirt.io/en/docs/quick-start)<br>
▶️ [Відео](https://youtu.be/LsSJlBRUvYw)

## Найкращі практики
ZSvirt використовує той самий корпоративний рушій, що й ZSphere, та спирається на перевірені результати впроваджень у наведених нижче клієнтів по всьому світу.
<p align="center">
  <img
    src="https://github.com/ZSvirt/.github/blob/main/assets/zsvirt-partner-en.png?raw=true"
    alt="Клієнти й партнери ZSvirt у світі"
    width="100%"
  >
</p>

## Порівняння платформ віртуалізації

Порівняння Proxmox VE, VMware vSphere і ZSvirt

<p align="center">
  <img
    src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-comparison.png"
    alt="Порівняння Proxmox VE і ZSvirt"
    width="100%"
  >
</p>


## Керування проєктом

ZSvirt дотримується легкої моделі керування відкритим проєктом, яка визначає супровід, ухвалення рішень і співпрацю учасників.

Документ [GOVERNANCE.md](GOVERNANCE.md) описує ролі, обов’язки супроводжувачів, процеси ухвалення рішень, керування випусками та співпрацю спільноти.

Зі зростанням спільноти модель може включати окремих супроводжувачів, робочі групи й формальніші процеси.

## Участь у проєкті

Ми вітаємо й цінуємо внески спільноти. Виправлення помилок, поліпшення документації, пропозиції функцій, тести й обмін практиками розгортання, міграції та експлуатації допомагають удосконалювати ZSvirt.

Новачки можуть почати з документації, повідомлень про проблеми, перевірки тестів, досвіду міграції чи обговорень. Розробників також запрошуємо вдосконалювати код, інструменти й інтеграції.

Активні учасники можуть отримати визнання в подяках спільноти, примітках до випусків, списках учасників чи майбутніх програмах спільноти.

Перед участю прочитайте:

- [CONTRIBUTING.md](CONTRIBUTING.md)

## Безпека

Процедуру повідомлення про вразливості описано в [SECURITY.md](SECURITY.md).

Не повідомляйте про вразливості через публічні GitHub Issues або Discussions.

## Ліцензія

ZSvirt поширюється за ліцензією [GNU General Public License v3.0](LICENSE).

Деякі репозиторії чи компоненти можуть містити стороннє ПЗ з відкритим кодом за іншими ліцензіями. Подробиці наведено у файлах `LICENSE`, `NOTICE` та пов’язаних файлах кожного репозиторію.

## Ресурси

<table>
  <tr>
    <td width="50%">
      <h3>🌐 Сайт спільноти</h3>
      <p>Дізнайтеся про функції, сценарії використання, новини й ресурси спільноти ZSvirt.</p>
      <a href="https://zsvirt.io"><strong>Відвідати сайт →</strong></a>
    </td>
    <td width="50%">
      <h3>▶️ Відео</h3>
      <p>Перегляньте презентацію ZSvirt і дізнайтеся про основні можливості.</p>
      <a href="https://youtu.be/c6pYmlIoPIU"><strong>Дивитися відео про продукт →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>📝 Блог</h3>
      <p>Читайте новини випусків, інженерні історії та матеріали про віртуалізацію.</p>
      <a href="https://zsvirt.io/blog"><strong>Читати блог →</strong></a>
    </td>
    <td width="50%">
      <h3>💬 GitHub Discussions</h3>
      <p>Ставте запитання, діліться ідеями та спілкуйтеся зі спільнотою ZSvirt.</p>
      <a href="https://github.com/ZSvirt/zsvirt/discussions"><strong>Долучитися до обговорення →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>▶️ YouTube</h3>
      <p>Стежте за каналом ZSvirt із демонстраціями, навчальними матеріалами й оновленнями продукту.</p>
      <a href="https://youtube.com/@ZSvirt"><strong>Підписатися на YouTube →</strong></a>
    </td>
    <td width="50%">
      <h3>💼 LinkedIn</h3>
      <p>Стежте за новинами проєкту, подіями спільноти й оглядами галузі.</p>
      <a href="https://www.linkedin.com/in/zsvirt-community/"><strong>Підписатися на LinkedIn →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>𝕏 X</h3>
      <p>Отримуйте найновіші оголошення ZSvirt і новини спільноти.</p>
      <a href="https://x.com/ZSvirt"><strong>Підписатися на X →</strong></a>
    </td>
    <td width="50%">
      <h3>🎮 Discord</h3>
      <p>Приєднуйтеся до спільноти ZSvirt, щоб ставити запитання, ділитися ідеями й знайомитися з іншими користувачами.</p>
      <a href="https://discord.com/invite/KHsw63z9xA"><strong>Приєднатися в Discord →</strong></a>
    </td>
  </tr>
</table>
