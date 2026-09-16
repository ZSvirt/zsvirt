<!-- synced-with: README.md@e9235837 -->
<div align="center" dir="rtl">
  <a href="https://zsvirt.io">
    <img
      src="https://raw.githubusercontent.com/zsvirt/.github/main/assets/zsvirt-logo.jpg"
      alt="شعار ZSvirt"
      width="180"
    >
  </a>
  <p align="center">
    <a
      href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
      target="_blank"
      rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/daily?language=Java"
        alt="اتجاه ZSvirt/zsvirt اليومي للغة Java | Trendshift"
        width="250"
        height="55"
      /></a> <a href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
       target="_blank"
       rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/weekly?language=Java"
        alt="اتجاه ZSvirt/zsvirt الأسبوعي للغة Java | Trendshift"
        width="250"
        height="55"
      /></a>&nbsp;&nbsp;
  </p>
  <h1 align="center">
    منصة افتراضية مفتوحة المصدر
    <br>
    جاهزة للمؤسسات، يقودها المجتمع
  </h1>

  <p align="center">
    <a href="https://zsvirt.io">
      <img
        src="https://img.shields.io/badge/Website-0F62FE?style=flat-square&logo=googlechrome&logoColor=white"
        alt="موقع ZSvirt"
      >
    </a>
    <a href="https://zsvirt.io/en/docs">
      <img
        src="https://img.shields.io/badge/Docs-7C3AED?style=flat-square&logo=readthedocs&logoColor=white"
        alt="الوثائق"
      >
    </a>
    <a href="https://demo.zsvirt.io/">
      <img
        src="https://img.shields.io/badge/Live%20Demo-16A34A?style=flat-square&logo=internetcomputer&logoColor=white"
        alt="العرض التجريبي المباشر"
      >
    </a>
    <a href="https://zsvirt.io/download">
      <img
        src="https://img.shields.io/badge/Download-F97316?style=flat-square&logo=data:image/svg%2Bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTUgMjBoMTR2LTJINXYyem0xNC05aC00VjNIOXY4SDVsNyA3IDctN3oiLz48L3N2Zz4%3D"
        alt="تنزيل"
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
    <strong>العربية</strong> |
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

<div dir="rtl">

> **جرّبه الآن — لا حاجة إلى أجهزة فعلية.** نزّل [صورة qcow2 أو OVA](https://zsvirt.io/download)، وشغّلها كآلة افتراضية على VMware أو VirtualBox أو KVM أو مضيف سحابي (باستخدام الافتراضية المتداخلة)، ثم عيّن عنوان IP لتصبح عقدة إدارة ZSvirt جاهزة. راجع [تشغيل ZSvirt داخل آلة افتراضية](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node) أو انتقل إلى [البدء السريع](https://zsvirt.io/en/docs/quick-start).

## ما هو ZSvirt

يقدّم ZSvirt محرك الافتراضية ZSphere من [ZStack](https://www.zstack-cloud.com/)، المثبت نجاحه في المؤسسات، إلى عالم المصادر المفتوحة. وبدعم من [ZStack](https://www.zstack-cloud.com/)، الشركة الرائدة ذات الخبرة في البنية التحتية، يشكّل ZSvirt منصة خفيفة وقابلة للتوسّع لتشغيل الآلات الافتراضية وإدارتها من دون التقيد بمورّد واحد.

ثبّت عقدة إدارة، واربط مضيفين يعتمدون على KVM، ثم أدر بيئتك بالكامل — بما فيها الآلات الافتراضية والعناقيد والتخزين والشبكات — من خلال واجهة ويب أو RESTful API (مع Terraform وحزم SDK للغات Go وPython وJava) أو CLI. كما تتضمن المنصة أدوات ترحيل VMware، بما يشمل الترحيل عبر الإنترنت واستيراد OVF ورفع ملفات VMDK.

## جولة في المنتج

<details open>
  <summary>
    <strong>📊 لوحة المعلومات — نظرة موحّدة على العمليات</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png"
        alt="لوحة عمليات ZSvirt الموحّدة"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🗂️ المخزون — إدارة مركزية للبنية التحتية</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png"
        alt="مخزون البنية التحتية المركزي في ZSvirt"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🔄 إدارة الترحيل — ترحيل أحمال العمل</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png"
        alt="إدارة الترحيل في ZSvirt"
        width="100%"
      >
    </a>
  </p>
</details>

## العرض التجريبي المباشر

[العرض التجريبي المباشر لـ ZSvirt](https://demo.zsvirt.io/) هو بيئة مستضافة مجانية لتجربة ZSvirt عبر الإنترنت، من دون تثبيت أو تسجيل. افتح الرابط وانقر على **Demo Login** لبدء استكشاف المنصة فورًا.

## البنية المعمارية

يستخدم ZSvirt بنية معيارية تتمحور حول إدارة موارد الافتراضية ومستوى الإدارة والخدمات التوسعية وأدوات التشغيل.

تشمل القدرات الأساسية ما يلي:

- **افتراضية الحوسبة**: إدارة المضيفين والعناقيد والآلات الافتراضية والصور ودورات حياتها.
- **افتراضية الشبكات**: الشبكات الافتراضية وخدمات الشبكة ومجموعات الأمان والقدرات المرتبطة بها.
- **افتراضية التخزين**: التخزين الأساسي والتخزين الاحتياطي ووحدات التخزين واللقطات وإدارة موارد التخزين.
- **مستوى الإدارة**: إطار API ونموذج الصلاحيات والأحداث والتنبيهات والتدقيق وعمليات النظام.
- **الخدمات التوسعية**: قدرات الترحيل والتعافي من الكوارث والمراقبة وإدارة الحصص والتحكم في الوصول وعمليات المؤسسات.
- **الأدوات وعمليات التكامل**: أدوات التثبيت والتشخيص والترحيل، ونصوص الأتمتة، والوكلاء، وCLI، وعمليات التكامل مع الأنظمة الخارجية.

على مستوى بنية البرمجيات، يركّز ZSvirt على عدم التزامن وانعدام الحالة وقابلية التوسّع والأتمتة:

- **بنية غير متزامنة**: تدعم الرسائل والأساليب واستدعاءات HTTP غير المتزامنة لتقليل الحجب وتحسين إنتاجية النظام.
- **خدمات عديمة الحالة**: لا يعتمد الطلب الواحد على حالة طلبات أخرى، ما يسهّل توسيع الخدمات واستعادتها وتشغيلها.
- **قابلية التوسّع عبر الإضافات**: تتيح التوسّع الأفقي لأنواع الموارد وقدرات الأعمال وقدرات التكامل باستخدام الإضافات.
- **محرك سير العمل**: يدير ترتيب تنفيذ العمليات المعقّدة ويدعم التراجع والاستعادة عند الإخفاق.
- **قدرات الوسوم والاستعلام**: تدعم توسيع خصائص الموارد وتصنيفها والاستعلامات الموحّدة والتنسيق المؤتمت.
- **النشر المؤتمت**: يستخدم أدوات الأتمتة لمعالجة مهام النشر والتهيئة والتشغيل، ما يقلّل تعقيد النشر والصيانة.

<p align="center">
  <img
    src="./assets/zsvirt-architecture.svg"
    alt="بنية ZSvirt المعمارية"
    width="100%"
  >
</p>

## دليل الترحيل من VMware

مع إعادة المؤسسات تقييم استراتيجياتها للافتراضية، أصبح الانتقال من VMware إلى منصات بديلة موضوعًا مهمًا للمؤسسات التي تسعى إلى ضبط التكاليف ومرونة البنية التحتية والاستقرار التشغيلي على المدى الطويل.

يوفّر ZSvirt قدرات وأدوات تشغيل موجّهة للترحيل لمساعدة المستخدمين في تقييم أحمال العمل والتخطيط لها ونقلها من بيئات VMware الحالية إلى بنية افتراضية قائمة على ZSvirt.

- [دليل الترحيل من VMware](https://zsvirt.io/vmware-alternative/)

<p align="center">
  <a href="https://zsvirt.io/vmware-alternative/">
    <img
      src="https://github.com/ZSvirt/.github/blob/main/assets/zvirt-migrate.png?raw=true"
      alt="الترحيل من VMware إلى ZSvirt"
      width="100%"
    >
  </a>
</p>

## البدء السريع

أسرع طريقة لتقييم ZSvirt هي اتباع دليل البدء السريع في وثائق المنتج. يرشدك الدليل خلال إعداد موارد الحوسبة والشبكة والتخزين، وتهيئة خدمة الإدارة، وإنشاء أول آلة افتراضية لك.

هل تفضّل تجربته داخل آلة افتراضية أولًا؟ نزّل [صورة qcow2 أو OVA](https://zsvirt.io/download) وشغّل عقدة الإدارة داخل أي برنامج Hypervisor — راجع [تشغيل ZSvirt داخل آلة افتراضية](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node).

🚀 [البدء السريع](https://zsvirt.io/en/docs/quick-start)<br>
▶️ [الفيديو](https://youtu.be/LsSJlBRUvYw)

## أفضل الممارسات

بفضل اعتماده على محرك المؤسسات نفسه المستخدم في ZSphere، يرث ZSvirt سجل النجاح المثبت لدى العملاء حول العالم أدناه.

<p align="center">
  <img
    src="https://github.com/ZSvirt/.github/blob/main/assets/zsvirt-partner-en.png?raw=true"
    alt="عملاء ZSvirt وشركاؤه حول العالم"
    width="100%"
  >
</p>

## مقارنة منصات الافتراضية

مقارنة Proxmox VE وVMware vSphere وZSvirt

<p align="center">
  <img
    src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-comparison.png"
    alt="مقارنة منصات الافتراضية Proxmox VE وZSvirt"
    width="100%"
  >
</p>

## خارطة الطريق

#### النصف الثاني من 2026

| المجال | العمل المخطط |
|---|---|
| **الأمن** | إضافة تشفير أقراص الجهاز الافتراضي<br>إضافة تشفير ترحيل الجهاز الافتراضي |
| **DRS وترحيل الجهاز الافتراضي** | إضافة خيارات ترحيل أكثر مرونة:<br>• إمكانية ترحيل الأقراص بشكل فردي<br>• دعم الترحيل الساخن والبارد لسيناريو SAN إلى SAN |
| **ZMigrate** | **ZMigrate 2.1**:<br>• إضافة فحص مسبق لتقليل مخاطر الترحيل المعقد<br>• مسارات ترحيل أبسط وأكثر ذكاءً |
| **التشغيل** | إضافة مقاييس زمن CPU Ready للجهاز الافتراضي، وزمن قراءة/كتابة قرص الجهاز الافتراضي، ومعدل فقدان حزم الشبكة، وزمن CPU Ready للمضيف<br>دعم مستويات سجل أكثر تفصيلاً للتمرير إلى syslog الخارجي<br>إضافة مراقبة أجهزة NVMe وFC، بما في ذلك IOPS وزمن الاستجابة<br>تضمين برامج تشغيل QXL في VMTools لتحسين عرض وحدة تحكم الجهاز الافتراضي ودقتها |
| **إدارة المخزون** | توسيع تسجيل التخزين والأجهزة الافتراضية ليشمل التسجيل في الموقع نفسه |

## الحوكمة

يخضع ZSvirt لنموذج حوكمة مفتوح المصدر وخفيف يحدّد كيفية صيانة المشروع واتخاذ القرارات وتعاون المساهمين.

يُعد مستند [GOVERNANCE.md](GOVERNANCE.md) نقطة البداية للتعرّف على أدوار المشروع ومسؤوليات المشرفين وعمليات اتخاذ القرار وإدارة الإصدارات والتعاون المجتمعي.

مع نمو المجتمع، قد يتطور نموذج الحوكمة ليشمل مشرفين متفرغين ومجموعات عمل وعمليات مشروع أكثر رسمية.

## المساهمة

نرحّب بمساهمات المجتمع ونقدّرها. سواء كنت تصلح الأخطاء أو تحسّن الوثائق أو تقترح ميزات أو تضيف اختبارات أو تشارك ممارسات النشر والترحيل والتشغيل، فإن مساهمتك تساعد في تحسين ZSvirt.

إذا كنت جديدًا في المشروع، فيمكنك البدء بتحسين الوثائق أو الإبلاغ عن المشكلات أو التحقق بالاختبارات أو مشاركة خبرات الترحيل أو المشاركة في نقاشات المجتمع. ونرحّب أيضًا بمساهمات المطورين في تحسين الشفرة والأدوات وعمليات التكامل.

قد نكرّم المساهمين النشطين من خلال إشادات المجتمع أو ملاحظات الإصدارات أو قوائم المساهمين أو برامج مجتمعية مستقبلية.

قبل المساهمة، يُرجى قراءة:

- [CONTRIBUTING.md](CONTRIBUTING.md)

## الأمان

توضّح [SECURITY.md](SECURITY.md) عملية الإبلاغ عن الثغرات الأمنية.

يُرجى عدم الإبلاغ عن الثغرات الأمنية عبر GitHub Issues أو Discussions العامة.

## الترخيص

يُرخص ZSvirt بموجب [GNU General Public License v3.0](LICENSE).

قد تتضمن بعض المستودعات أو المكوّنات برامج مفتوحة المصدر تابعة لجهات خارجية وبموجب تراخيص مختلفة. يُرجى مراجعة ملفات `LICENSE` و`NOTICE` والملفات ذات الصلة في كل مستودع للحصول على التفاصيل.

## الموارد

<table>
  <tr>
    <td width="50%">
      <h3>🌐 موقع المجتمع</h3>
      <p>استكشف ميزات ZSvirt وحالات الاستخدام والأخبار وموارد المجتمع.</p>
      <a href="https://zsvirt.io"><strong>زيارة الموقع ←</strong></a>
    </td>
    <td width="50%">
      <h3>▶️ مقاطع الفيديو</h3>
      <p>شاهد تقديم منتج ZSvirt وتعرّف على قدراته الأساسية.</p>
      <a href="https://youtu.be/c6pYmlIoPIU"><strong>مشاهدة فيديو المنتج ←</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>📝 المدونة</h3>
      <p>اكتشف تحديثات الإصدارات وقصص الهندسة ورؤى الافتراضية.</p>
      <a href="https://zsvirt.io/blog"><strong>قراءة المدونة ←</strong></a>
    </td>
    <td width="50%">
      <h3>💬 نقاشات GitHub</h3>
      <p>اطرح الأسئلة وشارك الأفكار وتواصل مع مجتمع ZSvirt.</p>
      <a href="https://github.com/ZSvirt/zsvirt/discussions"><strong>المشاركة في النقاش ←</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>▶️ YouTube</h3>
      <p>تابع قناة ZSvirt للاطلاع على العروض والشروحات وتحديثات المنتج.</p>
      <a href="https://youtube.com/@ZSvirt"><strong>متابعة YouTube ←</strong></a>
    </td>
    <td width="50%">
      <h3>💼 LinkedIn</h3>
      <p>تابع ZSvirt للحصول على أخبار المشروع وأبرز أخبار المجتمع ورؤى الصناعة.</p>
      <a href="https://www.linkedin.com/in/zsvirt-community/"><strong>متابعة LinkedIn ←</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>𝕏 X</h3>
      <p>احصل على أحدث إعلانات ZSvirt وتحديثات المجتمع.</p>
      <a href="https://x.com/ZSvirt"><strong>متابعة X ←</strong></a>
    </td>
    <td width="50%">
      <h3>🎮 Discord</h3>
      <p>انضم إلى مجتمع ZSvirt لطرح الأسئلة ومشاركة الأفكار والتواصل مع المستخدمين الآخرين.</p>
      <a href="https://discord.com/invite/KHsw63z9xA"><strong>الانضمام عبر Discord ←</strong></a>
    </td>
  </tr>
</table>

</div>
