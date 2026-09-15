<!-- synced-with: README.md@89c5cfc6 -->
<div align="center">
  <a href="https://zsvirt.io">
    <img
      src="https://raw.githubusercontent.com/zsvirt/.github/main/assets/zsvirt-logo.jpg"
      alt="Logotipo de ZSvirt"
      width="180"
    >
  </a>
  <p align="center">
    <a
      href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
      target="_blank"
      rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/daily?language=Java"
        alt="ZSvirt Java diario – Trendshift"
        width="250"
        height="55"
      /></a> <a href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
       target="_blank"
       rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/weekly?language=Java"
        alt="ZSvirt Java semanal – Trendshift"
        width="250"
        height="55"
      /></a>&nbsp;&nbsp;
  </p>
  <h1 align="center">
    Virtualización de código abierto
    <br>
    Preparada para empresas, impulsada por la comunidad
  </h1>

  <p align="center">
    <a href="https://zsvirt.io">
      <img
        src="https://img.shields.io/badge/Sitio%20web-0F62FE?style=flat-square&logo=googlechrome&logoColor=white"
        alt="Sitio web"
      >
    </a>
    <a href="https://zsvirt.io/en/docs">
      <img
        src="https://img.shields.io/badge/Documentaci%C3%B3n-7C3AED?style=flat-square&logo=readthedocs&logoColor=white"
        alt="Documentación"
      >
    </a>
    <a href="https://demo.zsvirt.io/">
      <img
        src="https://img.shields.io/badge/Demo%20en%20l%C3%ADnea-16A34A?style=flat-square&logo=internetcomputer&logoColor=white"
        alt="Demo en línea"
      >
    </a>
    <a href="https://zsvirt.io/download">
      <img
        src="https://img.shields.io/badge/Descargas-F97316?style=flat-square&logo=data:image/svg%2Bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTUgMjBoMTR2LTJINXYyem0xNC05aC00VjNIOXY4SDVsNyA3IDctN3oiLz48L3N2Zz4%3D"
        alt="Descargas"
      >
    </a>
  </p>

  <p align="center" dir="ltr">
    <a href="./README.md">English</a> |
    <a href="./README_zh.md">简体中文</a> |
    <a href="./README.zht.md">繁體中文</a> |
    <a href="./README.ko.md">한국어</a> |
    <a href="./README.de.md">Deutsch</a> |
    <strong>Español</strong> |
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

> **Pruébalo ahora, sin hardware físico.** Descarga la [imagen qcow2 u OVA](https://zsvirt.io/download), iníciala como VM en VMware, VirtualBox, KVM o un host en la nube (virtualización anidada) y configura una IP: el nodo de gestión de ZSvirt estará listo. Consulta [Ejecutar ZSvirt dentro de una VM](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node) o el [Inicio rápido](https://zsvirt.io/en/docs/quick-start).

## ¿Qué es ZSvirt?
ZSvirt lleva al mundo del código abierto el motor de virtualización ZSphere de [ZStack](https://www.zstack-cloud.com/), probado en entornos empresariales. Con el respaldo de [ZStack](https://www.zstack-cloud.com/), un proveedor consolidado de infraestructura, ZSvirt ofrece una plataforma ligera y escalable para ejecutar y gestionar máquinas virtuales sin dependencia de un proveedor.

Instala un nodo de gestión, conecta hosts KVM y administra máquinas virtuales, clústeres, almacenamiento y redes mediante una interfaz web, una API RESTful (con Terraform y SDK de Go/Python/Java) o una CLI. Incluye herramientas de migración de VMware: migración en línea, importación de OVF y carga de VMDK.

## Recorrido por el producto

<details open>
  <summary>
    <strong>📊 PANEL — Vista unificada de operaciones</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png"
        alt="Panel de operaciones de ZSvirt"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🗂️ INVENTARIO — Gestión centralizada de infraestructura</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png"
        alt="Inventario de infraestructura de ZSvirt"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🔄 GESTIÓN DE MIGRACIONES — Migración de cargas de trabajo</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png"
        alt="Gestión de migraciones de ZSvirt"
        width="100%"
      >
    </a>
  </p>
</details>

## Demo en línea

La [demo de ZSvirt](https://demo.zsvirt.io/) es un entorno alojado gratuito para probar ZSvirt en línea, sin instalar nada ni registrarse. Abre el enlace y pulsa **Demo Login** para explorar la plataforma.

## Arquitectura

ZSvirt utiliza una arquitectura modular centrada en la gestión de recursos de virtualización, el plano de gestión, los servicios de extensión y las herramientas operativas.

Sus capacidades principales incluyen:

- **Virtualización de cómputo**: gestión de hosts, clústeres, máquinas virtuales, imágenes y ciclos de vida.
- **Virtualización de red**: redes virtuales, servicios de red, grupos de seguridad y funciones relacionadas.
- **Virtualización de almacenamiento**: almacenamiento primario y de respaldo, volúmenes, instantáneas y gestión de recursos de almacenamiento.
- **Plano de gestión**: framework de API, modelo de permisos, eventos, alarmas, auditoría y operación del sistema.
- **Servicios de extensión**: migración, recuperación ante desastres, monitorización, cuotas, control de acceso y operaciones empresariales.
- **Herramientas e integraciones**: instalación, diagnóstico, migración, scripts de automatización, agentes, CLI e integración con sistemas externos.

La arquitectura de software prioriza la asincronía, la ausencia de estado, la extensibilidad y la automatización:

- **Arquitectura asíncrona**: mensajes, métodos y llamadas HTTP asíncronos para reducir bloqueos y mejorar la capacidad de procesamiento.
- **Servicios sin estado**: cada solicitud es independiente del estado de otras, lo que facilita escalar, recuperar y operar los servicios.
- **Extensibilidad mediante plugins**: ampliación horizontal de tipos de recursos, funciones de negocio e integraciones mediante plugins.
- **Motor de flujos de trabajo**: controla el orden de operaciones complejas y permite revertir cambios y recuperarse ante fallos.
- **Etiquetas y consultas**: ampliación de atributos, clasificación de recursos, consultas unificadas y orquestación automatizada.
- **Despliegue automatizado**: herramientas que automatizan despliegue, configuración y operación para reducir la complejidad del despliegue y el mantenimiento.

<p align="center">
  <img
    src="./assets/zsvirt-architecture.svg"
    alt="Arquitectura de ZSvirt"
    width="100%"
  >
</p>

## Guía de migración de VMware

Al reevaluar sus estrategias de virtualización, las empresas consideran migrar de VMware a otras plataformas para controlar costes, flexibilizar la infraestructura y mantener la estabilidad operativa a largo plazo.

ZSvirt ofrece funciones de migración y herramientas operativas para evaluar, planificar y trasladar cargas de trabajo desde entornos VMware existentes a infraestructura de virtualización basada en ZSvirt.

- [Guía de migración de VMware](https://zsvirt.io/vmware-alternative/)

<p align="center">
  <a href="https://zsvirt.io/vmware-alternative/">
    <img
      src="https://github.com/ZSvirt/.github/blob/main/assets/zvirt-migrate.png?raw=true"
      alt="Migrar de VMware a ZSvirt"
      width="100%"
    >
  </a>
</p>

## Inicio rápido

La forma más rápida de evaluar ZSvirt es seguir el inicio rápido de la documentación. Te guía para preparar recursos de cómputo, red y almacenamiento, inicializar el servicio de gestión y crear tu primera máquina virtual.

¿Prefieres probarlo primero en una VM? Descarga la [imagen qcow2 u OVA](https://zsvirt.io/download) y ejecuta el nodo de gestión en un hipervisor; consulta [Ejecutar ZSvirt dentro de una VM](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node).

🚀 [Inicio rápido](https://zsvirt.io/en/docs/quick-start)<br>
▶️ [Vídeo](https://youtu.be/LsSJlBRUvYw)

## Buenas prácticas
ZSvirt utiliza el mismo motor empresarial que ZSphere y hereda los resultados probados entre los clientes de todo el mundo que se muestran a continuación.
<p align="center">
  <img
    src="https://github.com/ZSvirt/.github/blob/main/assets/zsvirt-partner-en.png?raw=true"
    alt="Clientes y socios de ZSvirt en el mundo"
    width="100%"
  >
</p>

## Comparación de plataformas de virtualización

Proxmox VE frente a VMware vSphere y ZSvirt

<p align="center">
  <img
    src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-comparison.png"
    alt="Comparación de Proxmox VE y ZSvirt"
    width="100%"
  >
</p>


## Gobernanza

ZSvirt sigue un modelo ligero de gobernanza de código abierto que define el mantenimiento del proyecto, la toma de decisiones y la colaboración entre participantes.

[GOVERNANCE.md](GOVERNANCE.md) describe los roles del proyecto, las responsabilidades de sus mantenedores, los procesos de decisión, la gestión de versiones y la colaboración comunitaria.

A medida que crezca la comunidad, el modelo podrá incorporar mantenedores dedicados, grupos de trabajo y procesos más formales.

## Contribuir

Agradecemos las contribuciones de la comunidad. Corregir errores, mejorar la documentación, proponer funciones, añadir pruebas o compartir prácticas de despliegue, migración y operación ayuda a mejorar ZSvirt.

Si eres nuevo, puedes empezar por la documentación, los informes de problemas, la verificación de pruebas, las experiencias de migración o las discusiones comunitarias. También son bienvenidas las mejoras de código, herramientas e integraciones.

Los participantes activos podrán recibir reconocimiento en agradecimientos, notas de versión, listas de colaboradores o futuros programas comunitarios.

Antes de contribuir, lee:

- [CONTRIBUTING.md](CONTRIBUTING.md)

## Seguridad

El procedimiento para comunicar vulnerabilidades se describe en [SECURITY.md](SECURITY.md).

No comuniques vulnerabilidades mediante GitHub Issues o Discussions públicos.

## Licencia

ZSvirt se distribuye bajo la [GNU General Public License v3.0](LICENSE).

Algunos repositorios o componentes pueden incluir software de código abierto de terceros bajo otras licencias. Consulta `LICENSE`, `NOTICE` y los archivos relacionados de cada repositorio.

## Recursos

<table>
  <tr>
    <td width="50%">
      <h3>🌐 Sitio de la comunidad</h3>
      <p>Descubre las funciones, casos de uso, noticias y recursos comunitarios de ZSvirt.</p>
      <a href="https://zsvirt.io"><strong>Visitar el sitio →</strong></a>
    </td>
    <td width="50%">
      <h3>▶️ Vídeos</h3>
      <p>Mira la presentación de ZSvirt y conoce sus capacidades principales.</p>
      <a href="https://youtu.be/c6pYmlIoPIU"><strong>Ver vídeo del producto →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>📝 Blog</h3>
      <p>Lee novedades de versiones, experiencias de ingeniería y análisis sobre virtualización.</p>
      <a href="https://zsvirt.io/blog"><strong>Leer el blog →</strong></a>
    </td>
    <td width="50%">
      <h3>💬 GitHub Discussions</h3>
      <p>Haz preguntas, comparte ideas y conecta con la comunidad ZSvirt.</p>
      <a href="https://github.com/ZSvirt/zsvirt/discussions"><strong>Participar en la discusión →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>▶️ YouTube</h3>
      <p>Sigue el canal de ZSvirt para ver demostraciones, tutoriales y novedades del producto.</p>
      <a href="https://youtube.com/@ZSvirt"><strong>Seguir en YouTube →</strong></a>
    </td>
    <td width="50%">
      <h3>💼 LinkedIn</h3>
      <p>Sigue las noticias del proyecto, los hitos de la comunidad y los análisis del sector.</p>
      <a href="https://www.linkedin.com/in/zsvirt-community/"><strong>Seguir en LinkedIn →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>𝕏 X</h3>
      <p>Recibe los últimos anuncios de ZSvirt y las novedades de la comunidad.</p>
      <a href="https://x.com/ZSvirt"><strong>Seguir en X →</strong></a>
    </td>
    <td width="50%">
      <h3>🎮 Discord</h3>
      <p>Únete a la comunidad ZSvirt para preguntar, compartir ideas y conectar con otros usuarios.</p>
      <a href="https://discord.com/invite/KHsw63z9xA"><strong>Unirse a Discord →</strong></a>
    </td>
  </tr>
</table>
