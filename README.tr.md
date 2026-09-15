<!-- synced-with: README.md@89c5cfc6 -->
<div align="center">
  <a href="https://zsvirt.io">
    <img
      src="https://raw.githubusercontent.com/zsvirt/.github/main/assets/zsvirt-logo.jpg"
      alt="ZSvirt logosu"
      width="180"
    >
  </a>
  <p align="center">
    <a
      href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
      target="_blank"
      rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/daily?language=Java"
        alt="ZSvirt günlük Java – Trendshift"
        width="250"
        height="55"
      /></a> <a href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
       target="_blank"
       rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/weekly?language=Java"
        alt="ZSvirt haftalık Java – Trendshift"
        width="250"
        height="55"
      /></a>&nbsp;&nbsp;
  </p>
  <h1 align="center">
    Açık kaynak sanallaştırma
    <br>
    Kurumsal kullanıma hazır, topluluk tarafından geliştiriliyor
  </h1>

  <p align="center">
    <a href="https://zsvirt.io">
      <img
        src="https://img.shields.io/badge/Web%20sitesi-0F62FE?style=flat-square&logo=googlechrome&logoColor=white"
        alt="Web sitesi"
      >
    </a>
    <a href="https://zsvirt.io/en/docs">
      <img
        src="https://img.shields.io/badge/Belgeler-7C3AED?style=flat-square&logo=readthedocs&logoColor=white"
        alt="Belgeler"
      >
    </a>
    <a href="https://demo.zsvirt.io/">
      <img
        src="https://img.shields.io/badge/Canl%C4%B1%20demo-16A34A?style=flat-square&logo=internetcomputer&logoColor=white"
        alt="Canlı demo"
      >
    </a>
    <a href="https://zsvirt.io/download">
      <img
        src="https://img.shields.io/badge/%C4%B0ndir-F97316?style=flat-square&logo=data:image/svg%2Bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTUgMjBoMTR2LTJINXYyem0xNC05aC00VjNIOXY4SDVsNyA3IDctN3oiLz48L3N2Zz4%3D"
        alt="İndir"
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
    <strong>Türkçe</strong> |
    <a href="./README.uk.md">Українська</a> |
    <a href="./README.bn.md">বাংলা</a> |
    <a href="./README.gr.md">Ελληνικά</a> |
    <a href="./README.vi.md">Tiếng Việt</a>
  </p>
</div>

> **Hemen deneyin — fiziksel donanım gerekmez.** [qcow2 veya OVA imajını](https://zsvirt.io/download) indirin, VMware, VirtualBox, KVM ya da bir bulut sunucusunda (iç içe sanallaştırma) VM olarak başlatın ve IP adresini ayarlayın. ZSvirt yönetim düğümü hazırdır. [ZSvirt’i VM içinde çalıştırma](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node) veya [Hızlı başlangıç](https://zsvirt.io/en/docs/quick-start) kılavuzuna bakın.

## ZSvirt nedir?
ZSvirt, [ZStack](https://www.zstack-cloud.com/)’in kurumsal ortamlarda kanıtlanmış ZSphere sanallaştırma motorunu açık kaynak dünyasına taşır. Köklü altyapı sağlayıcısı [ZStack](https://www.zstack-cloud.com/) tarafından desteklenen ZSvirt, sanal makineleri sağlayıcıya bağımlı olmadan çalıştırmak ve yönetmek için hafif, ölçeklenebilir bir platformdur.

Bir yönetim düğümü kurun, KVM tabanlı sunucuları bağlayın ve VM’leri, kümeleri, depolamayı ve ağları web arayüzü, RESTful API (Terraform ve Go/Python/Java SDK’larıyla) veya CLI üzerinden yönetin. VMware geçiş araçları yerleşiktir: çevrimiçi geçiş, OVF içe aktarma ve VMDK yükleme.

## Ürün turu

<details open>
  <summary>
    <strong>📊 KONTROL PANELİ — Birleşik operasyon görünümü</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png"
        alt="ZSvirt operasyon paneli"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🗂️ ENVANTER — Merkezi altyapı yönetimi</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png"
        alt="ZSvirt altyapı envanteri"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🔄 GEÇİŞ YÖNETİMİ — İş yüklerini taşıma</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png"
        alt="ZSvirt geçiş yönetimi"
        width="100%"
      >
    </a>
  </p>
</details>

## Canlı demo

[ZSvirt canlı demosu](https://demo.zsvirt.io/), kurulum veya kayıt gerektirmeden ZSvirt’i çevrimiçi denemeye yarayan ücretsiz barındırılan bir ortamdır. Bağlantıyı açıp **Demo Login** düğmesine tıklayarak platformu keşfedin.

## Mimari

ZSvirt; sanallaştırma kaynaklarının yönetimi, yönetim düzlemi, genişletme hizmetleri ve operasyon araçları etrafında modüler bir mimari kullanır.

Temel yetenekler:

- **İşlem sanallaştırması**: sunucu, küme, sanal makine, imaj ve yaşam döngüsü yönetimi.
- **Ağ sanallaştırması**: sanal ağlar, ağ hizmetleri, güvenlik grupları ve ilgili yetenekler.
- **Depolama sanallaştırması**: birincil depolama, yedekleme depolaması, birimler, anlık görüntüler ve depolama kaynaklarının yönetimi.
- **Yönetim düzlemi**: API çerçevesi, izin modeli, olaylar, alarmlar, denetim ve sistem operasyonları.
- **Genişletme hizmetleri**: geçiş, felaket kurtarma, izleme, kota yönetimi, erişim denetimi ve kurumsal operasyonlar.
- **Araçlar ve entegrasyonlar**: kurulum, tanılama, geçiş, otomasyon betikleri, ajanlar, CLI ve harici sistem entegrasyonları.

Yazılım mimarisi asenkron çalışma, durumsuzluk, genişletilebilirlik ve otomasyonu öne çıkarır:

- **Asenkron mimari**: asenkron mesajlar, yöntemler ve HTTP çağrıları engellemeyi azaltır, sistemin işlem kapasitesini artırır.
- **Durumsuz hizmetler**: istekler diğer isteklerin durumuna bağlı değildir; ölçekleme, kurtarma ve işletim kolaylaşır.
- **Eklenti tabanlı genişletilebilirlik**: kaynak türleri, iş yetenekleri ve entegrasyonlar eklentilerle yatay olarak genişletilir.
- **İş akışı motoru**: karmaşık işlemlerin sırasını yönetir, hata durumlarında geri alma ve kurtarmayı destekler.
- **Etiketler ve sorgular**: kaynak özniteliklerini genişletme, kaynak sınıflandırma, birleşik sorgular ve otomasyon orkestrasyonu.
- **Otomatik dağıtım**: otomasyon araçları dağıtım, yapılandırma ve operasyonları yürüterek dağıtım ve bakım karmaşıklığını azaltır.

<p align="center">
  <img
    src="./assets/zsvirt-architecture.svg"
    alt="ZSvirt mimarisi"
    width="100%"
  >
</p>

## VMware geçiş kılavuzu

Şirketler sanallaştırma stratejilerini yeniden değerlendirirken VMware’den alternatif platformlara geçiş; maliyet kontrolü, altyapı esnekliği ve uzun vadeli operasyonel istikrar arayan kuruluşlar için önem kazanır.

ZSvirt, mevcut VMware ortamlarından ZSvirt tabanlı sanallaştırma altyapısına iş yükü taşımayı değerlendirmek, planlamak ve gerçekleştirmek için geçiş yetenekleri ve operasyon araçları sunar.

- [VMware geçiş kılavuzu](https://zsvirt.io/vmware-alternative/)

<p align="center">
  <a href="https://zsvirt.io/vmware-alternative/">
    <img
      src="https://github.com/ZSvirt/.github/blob/main/assets/zvirt-migrate.png?raw=true"
      alt="VMware’den ZSvirt’e geçiş"
      width="100%"
    >
  </a>
</p>

## Hızlı başlangıç

ZSvirt’i değerlendirmenin en hızlı yolu, ürün belgelerindeki hızlı başlangıç kılavuzudur. İşlem, ağ ve depolama kaynaklarını hazırlamayı, yönetim hizmetini başlatmayı ve ilk sanal makinenizi oluşturmayı anlatır.

Önce VM içinde denemek ister misiniz? [qcow2 veya OVA imajını](https://zsvirt.io/download) indirin ve yönetim düğümünü bir hipervizör içinde çalıştırın — bkz. [ZSvirt’i VM içinde çalıştırma](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node).

🚀 [Hızlı başlangıç](https://zsvirt.io/en/docs/quick-start)<br>
▶️ [Video](https://youtu.be/LsSJlBRUvYw)

## İyi uygulamalar
ZSvirt, ZSphere ile aynı kurumsal motoru kullanır ve aşağıda yer alan dünya çapındaki müşterilerde kanıtlanmış başarılardan yararlanır.
<p align="center">
  <img
    src="https://github.com/ZSvirt/.github/blob/main/assets/zsvirt-partner-en.png?raw=true"
    alt="ZSvirt küresel müşterileri ve iş ortakları"
    width="100%"
  >
</p>

## Sanallaştırma platformları karşılaştırması

Proxmox VE, VMware vSphere ve ZSvirt karşılaştırması

<p align="center">
  <img
    src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-comparison.png"
    alt="Proxmox VE ve ZSvirt karşılaştırması"
    width="100%"
  >
</p>


## Yönetişim

ZSvirt, projenin bakımını, karar alma süreçlerini ve katkıda bulunanların iş birliğini tanımlayan hafif bir açık kaynak yönetişim modeliyle yönetilir.

[GOVERNANCE.md](GOVERNANCE.md), proje rollerini, bakım sorumluluklarını, karar süreçlerini, sürüm yönetimini ve topluluk iş birliğini açıklar.

Topluluk büyüdükçe model; özel bakım sorumluları, çalışma grupları ve daha resmî süreçler içerebilir.

## Katkıda bulunma

Topluluk katkılarını memnuniyetle karşılıyor ve önemsiyoruz. Hata düzeltmek, belgeleri geliştirmek, özellik önermek, test eklemek veya dağıtım, geçiş ve işletim deneyimlerini paylaşmak ZSvirt’i iyileştirir.

Yeni katılımcılar belgeler, sorun bildirimleri, test doğrulaması, geçiş deneyimleri veya topluluk tartışmalarıyla başlayabilir. Geliştiricilerin kod, araç ve entegrasyon iyileştirmelerini de bekliyoruz.

Aktif katılımcılar; topluluk teşekkürleri, sürüm notları, katkıda bulunanlar listeleri veya gelecekteki topluluk programlarıyla takdir edilebilir.

Katkıda bulunmadan önce okuyun:

- [CONTRIBUTING.md](CONTRIBUTING.md)

## Güvenlik

Güvenlik açığı bildirim süreci [SECURITY.md](SECURITY.md) belgesinde açıklanır.

Güvenlik açıklarını herkese açık GitHub Issues veya Discussions üzerinden bildirmeyin.

## Lisans

ZSvirt, [GNU General Public License v3.0](LICENSE) kapsamında lisanslanır.

Bazı depolar veya bileşenler farklı lisanslı üçüncü taraf açık kaynak yazılımlar içerebilir. Ayrıntılar için her deponun `LICENSE`, `NOTICE` ve ilgili dosyalarını inceleyin.

## Kaynaklar

<table>
  <tr>
    <td width="50%">
      <h3>🌐 Topluluk sitesi</h3>
      <p>ZSvirt özelliklerini, kullanım alanlarını, haberlerini ve topluluk kaynaklarını keşfedin.</p>
      <a href="https://zsvirt.io"><strong>Siteyi ziyaret edin →</strong></a>
    </td>
    <td width="50%">
      <h3>▶️ Videolar</h3>
      <p>ZSvirt tanıtımını izleyin ve temel yeteneklerini öğrenin.</p>
      <a href="https://youtu.be/c6pYmlIoPIU"><strong>Ürün videosunu izleyin →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>📝 Blog</h3>
      <p>Sürüm haberlerini, mühendislik deneyimlerini ve sanallaştırma yazılarını okuyun.</p>
      <a href="https://zsvirt.io/blog"><strong>Blogu okuyun →</strong></a>
    </td>
    <td width="50%">
      <h3>💬 GitHub Discussions</h3>
      <p>Sorular sorun, fikir paylaşın ve ZSvirt topluluğuyla iletişim kurun.</p>
      <a href="https://github.com/ZSvirt/zsvirt/discussions"><strong>Tartışmaya katılın →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>▶️ YouTube</h3>
      <p>Demolar, eğitimler ve ürün güncellemeleri için ZSvirt kanalını takip edin.</p>
      <a href="https://youtube.com/@ZSvirt"><strong>YouTube’da takip edin →</strong></a>
    </td>
    <td width="50%">
      <h3>💼 LinkedIn</h3>
      <p>Proje haberlerini, topluluk gelişmelerini ve sektör analizlerini takip edin.</p>
      <a href="https://www.linkedin.com/in/zsvirt-community/"><strong>LinkedIn’de takip edin →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>𝕏 X</h3>
      <p>En yeni ZSvirt duyurularını ve topluluk haberlerini alın.</p>
      <a href="https://x.com/ZSvirt"><strong>X’te takip edin →</strong></a>
    </td>
    <td width="50%">
      <h3>🎮 Discord</h3>
      <p>Soru sormak, fikir paylaşmak ve diğer kullanıcılarla tanışmak için ZSvirt topluluğuna katılın.</p>
      <a href="https://discord.com/invite/KHsw63z9xA"><strong>Discord’a katılın →</strong></a>
    </td>
  </tr>
</table>
