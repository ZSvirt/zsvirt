<!-- synced-with: README.md@e9235837 -->
<div align="center">
  <a href="https://zsvirt.io">
    <img
      src="https://raw.githubusercontent.com/zsvirt/.github/main/assets/zsvirt-logo.jpg"
      alt="โลโก้ ZSvirt"
      width="180"
    >
  </a>
  <p align="center">
    <a
      href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
      target="_blank"
      rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/daily?language=Java"
        alt="ZSvirt Java รายวัน – Trendshift"
        width="250"
        height="55"
      /></a> <a href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
       target="_blank"
       rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/weekly?language=Java"
        alt="ZSvirt Java รายสัปดาห์ – Trendshift"
        width="250"
        height="55"
      /></a>&nbsp;&nbsp;
  </p>
  <h1 align="center">
    ระบบเวอร์ชวลไลเซชันแบบโอเพนซอร์ส
    <br>
    พร้อมสำหรับองค์กร ขับเคลื่อนโดยชุมชน
  </h1>

  <p align="center">
    <a href="https://zsvirt.io">
      <img
        src="https://img.shields.io/badge/%E0%B9%80%E0%B8%A7%E0%B9%87%E0%B8%9A%E0%B9%84%E0%B8%8B%E0%B8%95%E0%B9%8C-0F62FE?style=flat-square&logo=googlechrome&logoColor=white"
        alt="เว็บไซต์"
      >
    </a>
    <a href="https://zsvirt.io/en/docs">
      <img
        src="https://img.shields.io/badge/%E0%B9%80%E0%B8%AD%E0%B8%81%E0%B8%AA%E0%B8%B2%E0%B8%A3-7C3AED?style=flat-square&logo=readthedocs&logoColor=white"
        alt="เอกสาร"
      >
    </a>
    <a href="https://demo.zsvirt.io/">
      <img
        src="https://img.shields.io/badge/%E0%B9%80%E0%B8%94%E0%B9%82%E0%B8%A1%E0%B8%AD%E0%B8%AD%E0%B8%99%E0%B9%84%E0%B8%A5%E0%B8%99%E0%B9%8C-16A34A?style=flat-square&logo=internetcomputer&logoColor=white"
        alt="เดโมออนไลน์"
      >
    </a>
    <a href="https://zsvirt.io/download">
      <img
        src="https://img.shields.io/badge/%E0%B8%94%E0%B8%B2%E0%B8%A7%E0%B8%99%E0%B9%8C%E0%B9%82%E0%B8%AB%E0%B8%A5%E0%B8%94-F97316?style=flat-square&logo=data:image/svg%2Bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTUgMjBoMTR2LTJINXYyem0xNC05aC00VjNIOXY4SDVsNyA3IDctN3oiLz48L3N2Zz4%3D"
        alt="ดาวน์โหลด"
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
    <strong>ไทย</strong> |
    <a href="./README.tr.md">Türkçe</a> |
    <a href="./README.uk.md">Українська</a> |
    <a href="./README.bn.md">বাংলা</a> |
    <a href="./README.gr.md">Ελληνικά</a> |
    <a href="./README.vi.md">Tiếng Việt</a>
  </p>
</div>

> **ทดลองได้ทันที โดยไม่ต้องมีฮาร์ดแวร์จริง** ดาวน์โหลด[อิมเมจ qcow2 หรือ OVA](https://zsvirt.io/download) แล้วบูตเป็น VM บน VMware, VirtualBox, KVM หรือโฮสต์คลาวด์ (การจำลองเสมือนแบบซ้อน) ตั้งค่า IP แล้วโหนดจัดการ ZSvirt ก็พร้อมใช้งาน ดู[การรัน ZSvirt ภายใน VM](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node) หรือ[เริ่มต้นอย่างรวดเร็ว](https://zsvirt.io/en/docs/quick-start)

## ZSvirt คืออะไร
ZSvirt นำเอนจินเวอร์ชวลไลเซชัน ZSphere ของ [ZStack](https://www.zstack-cloud.com/) ซึ่งผ่านการใช้งานจริงในองค์กรมาสู่โลกโอเพนซอร์ส ด้วยการสนับสนุนจาก [ZStack](https://www.zstack-cloud.com/) ผู้ให้บริการโครงสร้างพื้นฐานที่มีประสบการณ์ ZSvirt เป็นแพลตฟอร์มขนาดเบาที่ขยายระบบได้ สำหรับรันและจัดการเครื่องเสมือนโดยไม่ผูกติดกับผู้ขาย

ติดตั้งโหนดจัดการ เชื่อมต่อโฮสต์ KVM แล้วจัดการ VM คลัสเตอร์ พื้นที่จัดเก็บ และเครือข่ายผ่านเว็บ UI, RESTful API (พร้อม Terraform และ SDK สำหรับ Go/Python/Java) หรือ CLI มีเครื่องมือย้ายจาก VMware ในตัว ได้แก่ การย้ายออนไลน์ การนำเข้า OVF และการอัปโหลด VMDK

## สำรวจผลิตภัณฑ์

<details open>
  <summary>
    <strong>📊 แดชบอร์ด — ภาพรวมการดำเนินงานแบบรวมศูนย์</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png"
        alt="แดชบอร์ดการดำเนินงาน ZSvirt"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🗂️ รายการทรัพยากร — จัดการโครงสร้างพื้นฐานจากส่วนกลาง</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png"
        alt="รายการโครงสร้างพื้นฐาน ZSvirt"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🔄 การจัดการย้ายระบบ — ย้ายเวิร์กโหลด</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png"
        alt="การจัดการย้ายระบบ ZSvirt"
        width="100%"
      >
    </a>
  </p>
</details>

## เดโมออนไลน์

[เดโม ZSvirt](https://demo.zsvirt.io/) เป็นสภาพแวดล้อมที่โฮสต์ไว้ให้ทดลอง ZSvirt ออนไลน์ฟรี ไม่ต้องติดตั้งหรือลงทะเบียน เปิดลิงก์แล้วคลิก **Demo Login** เพื่อสำรวจแพลตฟอร์ม

## สถาปัตยกรรม

ZSvirt ใช้สถาปัตยกรรมแบบโมดูล ครอบคลุมการจัดการทรัพยากรเสมือน ส่วนการจัดการ บริการส่วนขยาย และเครื่องมือปฏิบัติการ

ความสามารถหลักประกอบด้วย:

- **การจำลองเสมือนด้านการประมวลผล**: จัดการโฮสต์ คลัสเตอร์ เครื่องเสมือน อิมเมจ และวงจรชีวิต
- **การจำลองเสมือนเครือข่าย**: เครือข่ายเสมือน บริการเครือข่าย กลุ่มความปลอดภัย และความสามารถที่เกี่ยวข้อง
- **การจำลองเสมือนพื้นที่จัดเก็บ**: พื้นที่จัดเก็บหลัก พื้นที่สำรองข้อมูล โวลุ่ม สแนปช็อต และการจัดการทรัพยากรจัดเก็บ
- **ส่วนการจัดการ**: เฟรมเวิร์ก API โมเดลสิทธิ์ เหตุการณ์ การแจ้งเตือน การตรวจสอบ และการดำเนินงานระบบ
- **บริการส่วนขยาย**: การย้ายระบบ การกู้คืนจากภัยพิบัติ การติดตามตรวจสอบ โควตา การควบคุมการเข้าถึง และการดำเนินงานองค์กร
- **เครื่องมือและการเชื่อมต่อ**: เครื่องมือติดตั้ง วินิจฉัย ย้ายระบบ สคริปต์อัตโนมัติ เอเจนต์ CLI และการเชื่อมต่อระบบภายนอก

สถาปัตยกรรมซอฟต์แวร์เน้นการทำงานแบบอะซิงโครนัส การไม่เก็บสถานะ การขยายความสามารถ และระบบอัตโนมัติ:

- **สถาปัตยกรรมอะซิงโครนัส**: รองรับข้อความ เมธอด และการเรียก HTTP แบบอะซิงโครนัส เพื่อลดการบล็อกและเพิ่มปริมาณงานที่ระบบประมวลผลได้
- **บริการที่ไม่เก็บสถานะ**: แต่ละคำขอไม่ขึ้นกับสถานะของคำขออื่น จึงขยายระบบ กู้คืน และดูแลได้ง่ายขึ้น
- **การขยายผ่านปลั๊กอิน**: ขยายประเภททรัพยากร ความสามารถทางธุรกิจ และการเชื่อมต่อในแนวนอนผ่านปลั๊กอิน
- **เอนจินเวิร์กโฟลว์**: จัดลำดับการดำเนินการที่ซับซ้อน พร้อมรองรับการย้อนกลับและกู้คืนเมื่อเกิดข้อผิดพลาด
- **แท็กและการสืบค้น**: ขยายแอตทริบิวต์ จำแนกทรัพยากร สืบค้นแบบรวม และประสานงานอัตโนมัติ
- **การติดตั้งใช้งานอัตโนมัติ**: ใช้เครื่องมืออัตโนมัติจัดการการติดตั้ง การกำหนดค่า และงานปฏิบัติการ เพื่อลดความซับซ้อนในการติดตั้งและบำรุงรักษา

<p align="center">
  <img
    src="./assets/zsvirt-architecture.svg"
    alt="สถาปัตยกรรม ZSvirt"
    width="100%"
  >
</p>

## คู่มือย้ายจาก VMware

เมื่อองค์กรทบทวนกลยุทธ์เวอร์ชวลไลเซชัน การย้ายจาก VMware ไปยังแพลตฟอร์มอื่นจึงสำคัญต่อการควบคุมต้นทุน ความยืดหยุ่นของโครงสร้างพื้นฐาน และความมั่นคงในการดำเนินงานระยะยาว

ZSvirt มีความสามารถด้านการย้ายระบบและเครื่องมือปฏิบัติการ เพื่อช่วยประเมิน วางแผน และย้ายเวิร์กโหลดจากสภาพแวดล้อม VMware ที่มีอยู่ไปยังโครงสร้างพื้นฐานเสมือนที่ใช้ ZSvirt

- [คู่มือย้ายจาก VMware](https://zsvirt.io/vmware-alternative/)

<p align="center">
  <a href="https://zsvirt.io/vmware-alternative/">
    <img
      src="https://github.com/ZSvirt/.github/blob/main/assets/zvirt-migrate.png?raw=true"
      alt="ย้ายจาก VMware ไปยัง ZSvirt"
      width="100%"
    >
  </a>
</p>

## เริ่มต้นอย่างรวดเร็ว

วิธีประเมิน ZSvirt ที่เร็วที่สุดคือทำตามคู่มือเริ่มต้นในเอกสารผลิตภัณฑ์ คู่มืออธิบายการเตรียมทรัพยากรประมวลผล เครือข่าย และพื้นที่จัดเก็บ การเริ่มบริการจัดการ และการสร้างเครื่องเสมือนเครื่องแรก

อยากทดลองใน VM ก่อนหรือไม่ ดาวน์โหลด[อิมเมจ qcow2 หรือ OVA](https://zsvirt.io/download) แล้วรันโหนดจัดการภายในไฮเปอร์ไวเซอร์ ดู[การรัน ZSvirt ภายใน VM](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node)

🚀 [เริ่มต้นอย่างรวดเร็ว](https://zsvirt.io/en/docs/quick-start)<br>
▶️ [วิดีโอ](https://youtu.be/LsSJlBRUvYw)

## แนวปฏิบัติที่ดี
ZSvirt ใช้เอนจินระดับองค์กรเดียวกับ ZSphere และสืบทอดผลสำเร็จที่พิสูจน์แล้วจากลูกค้าทั่วโลกที่แสดงด้านล่าง
<p align="center">
  <img
    src="https://github.com/ZSvirt/.github/blob/main/assets/zsvirt-partner-en.png?raw=true"
    alt="ลูกค้าและพันธมิตร ZSvirt ทั่วโลก"
    width="100%"
  >
</p>

## เปรียบเทียบแพลตฟอร์มเวอร์ชวลไลเซชัน

เปรียบเทียบ Proxmox VE, VMware vSphere และ ZSvirt

<p align="center">
  <img
    src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-comparison.png"
    alt="เปรียบเทียบ Proxmox VE กับ ZSvirt"
    width="100%"
  >
</p>

## แผนการดำเนินงาน

#### ครึ่งหลังของปี 2026

| ด้าน | งานที่วางแผนไว้ |
|---|---|
| **ความปลอดภัย** | เพิ่มการเข้ารหัสดิสก์ของ VM<br>เพิ่มการเข้ารหัสการย้าย VM |
| **DRS และการย้าย VM** | เพิ่มตัวเลือกการย้ายที่ยืดหยุ่นขึ้น:<br>• สามารถย้ายดิสก์แยกทีละลูก<br>• รองรับการย้ายแบบ hot และ cold สำหรับสถานการณ์ SAN ไป SAN |
| **ZMigrate** | **ZMigrate 2.1**:<br>• เพิ่มการตรวจสอบล่วงหน้าเพื่อลดความเสี่ยงในการย้ายที่ซับซ้อน<br>• ขั้นตอนการย้ายที่ง่ายและฉลาดขึ้น |
| **การดำเนินงาน** | เพิ่มตัวชี้วัดเวลา CPU Ready ของ VM ความหน่วงในการอ่าน/เขียนดิสก์ของ VM อัตราการสูญหายของแพ็กเก็ตเครือข่าย และเวลา CPU Ready ของโฮสต์<br>รองรับระดับบันทึกที่ละเอียดขึ้นสำหรับการส่งต่อไปยัง syslog ภายนอก<br>เพิ่มการมอนิเตอร์อุปกรณ์ NVMe และ FC รวมถึง IOPS และความหน่วง<br>เพิ่มไดรเวอร์ QXL ในตัวใน VMTools เพื่อปรับปรุงการแสดงผลและความละเอียดของคอนโซล VM |
| **การจัดการสินค้าคงคลัง** | ขยายความสามารถในการลงทะเบียนสตอเรจและ VM ให้ครอบคลุมการลงทะเบียนในไซต์เดียวกัน |

## การกำกับดูแลโครงการ

ZSvirt ใช้รูปแบบการกำกับดูแลโอเพนซอร์สที่เรียบง่าย เพื่อกำหนดการบำรุงรักษาโครงการ การตัดสินใจ และการทำงานร่วมกันของผู้มีส่วนร่วม

[GOVERNANCE.md](GOVERNANCE.md) อธิบายบทบาทในโครงการ หน้าที่ของผู้ดูแล กระบวนการตัดสินใจ การจัดการรุ่นเผยแพร่ และการทำงานร่วมกันของชุมชน

เมื่อชุมชนเติบโต รูปแบบนี้อาจเพิ่มผู้ดูแลเฉพาะด้าน คณะทำงาน และกระบวนการที่เป็นทางการมากขึ้น

## การมีส่วนร่วม

เรายินดีและขอบคุณทุกการมีส่วนร่วมจากชุมชน ทั้งการแก้บั๊ก ปรับปรุงเอกสาร เสนอฟีเจอร์ เพิ่มการทดสอบ หรือแบ่งปันแนวปฏิบัติในการติดตั้ง ย้ายระบบ และดูแลระบบ ซึ่งช่วยพัฒนา ZSvirt

ผู้เริ่มต้นอาจช่วยด้านเอกสาร รายงานปัญหา ตรวจสอบการทดสอบ แบ่งปันประสบการณ์ย้ายระบบ หรือร่วมสนทนา นักพัฒนาสามารถปรับปรุงโค้ด เครื่องมือ และการเชื่อมต่อได้เช่นกัน

ผู้มีส่วนร่วมอย่างต่อเนื่องอาจได้รับการยกย่องผ่านคำขอบคุณของชุมชน บันทึกประจำรุ่น รายชื่อผู้มีส่วนร่วม หรือโครงการชุมชนในอนาคต

ก่อนมีส่วนร่วม โปรดอ่าน:

- [CONTRIBUTING.md](CONTRIBUTING.md)

## ความปลอดภัย

ขั้นตอนรายงานช่องโหว่อธิบายไว้ใน [SECURITY.md](SECURITY.md)

โปรดอย่ารายงานช่องโหว่ด้านความปลอดภัยผ่าน GitHub Issues หรือ Discussions สาธารณะ

## สัญญาอนุญาต

ZSvirt เผยแพร่ภายใต้ [GNU General Public License v3.0](LICENSE)

บางคลังโค้ดหรือส่วนประกอบอาจมีซอฟต์แวร์โอเพนซอร์สของบุคคลที่สามภายใต้สัญญาอนุญาตอื่น โปรดดู `LICENSE`, `NOTICE` และไฟล์ที่เกี่ยวข้องในแต่ละคลัง

## แหล่งข้อมูล

<table>
  <tr>
    <td width="50%">
      <h3>🌐 เว็บไซต์ชุมชน</h3>
      <p>สำรวจฟีเจอร์ กรณีใช้งาน ข่าวสาร และแหล่งข้อมูลของชุมชน ZSvirt</p>
      <a href="https://zsvirt.io"><strong>เยี่ยมชมเว็บไซต์ →</strong></a>
    </td>
    <td width="50%">
      <h3>▶️ วิดีโอ</h3>
      <p>ชมวิดีโอแนะนำ ZSvirt และทำความรู้จักความสามารถหลัก</p>
      <a href="https://youtu.be/c6pYmlIoPIU"><strong>ชมวิดีโอผลิตภัณฑ์ →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>📝 บล็อก</h3>
      <p>อ่านข่าวรุ่นใหม่ เรื่องราวทางวิศวกรรม และความรู้ด้านเวอร์ชวลไลเซชัน</p>
      <a href="https://zsvirt.io/blog"><strong>อ่านบล็อก →</strong></a>
    </td>
    <td width="50%">
      <h3>💬 GitHub Discussions</h3>
      <p>ถามคำถาม แบ่งปันแนวคิด และพูดคุยกับชุมชน ZSvirt</p>
      <a href="https://github.com/ZSvirt/zsvirt/discussions"><strong>ร่วมสนทนา →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>▶️ YouTube</h3>
      <p>ติดตามช่อง ZSvirt เพื่อชมการสาธิต บทเรียน และข่าวผลิตภัณฑ์</p>
      <a href="https://youtube.com/@ZSvirt"><strong>ติดตามบน YouTube →</strong></a>
    </td>
    <td width="50%">
      <h3>💼 LinkedIn</h3>
      <p>ติดตามข่าวโครงการ กิจกรรมเด่นของชุมชน และข้อมูลเชิงลึกของอุตสาหกรรม</p>
      <a href="https://www.linkedin.com/in/zsvirt-community/"><strong>ติดตามบน LinkedIn →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>𝕏 X</h3>
      <p>รับประกาศล่าสุดของ ZSvirt และข่าวสารชุมชน</p>
      <a href="https://x.com/ZSvirt"><strong>ติดตามบน X →</strong></a>
    </td>
    <td width="50%">
      <h3>🎮 Discord</h3>
      <p>เข้าร่วมชุมชน ZSvirt เพื่อถามคำถาม แบ่งปันแนวคิด และพบผู้ใช้อื่น</p>
      <a href="https://discord.com/invite/KHsw63z9xA"><strong>เข้าร่วม Discord →</strong></a>
    </td>
  </tr>
</table>
