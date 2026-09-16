<!-- synced-with: README.md@e9235837 -->
<div align="center">
  <a href="https://zsvirt.io">
    <img
      src="https://raw.githubusercontent.com/zsvirt/.github/main/assets/zsvirt-logo.jpg"
      alt="Logo do ZSvirt"
      width="180"
    >
  </a>
  <p align="center">
    <a
      href="https://trendshift.io/repositories/156451?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-156451"
      target="_blank"
      rel="noopener noreferrer"><img
        src="https://trendshift.io/api/badge/trendshift/repositories/156451/daily?language=Java"
        alt="ZSvirt Java diário – Trendshift"
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
    Virtualização de código aberto
    <br>
    Pronta para empresas, movida pela comunidade
  </h1>

  <p align="center">
    <a href="https://zsvirt.io">
      <img
        src="https://img.shields.io/badge/Site-0F62FE?style=flat-square&logo=googlechrome&logoColor=white"
        alt="Site"
      >
    </a>
    <a href="https://zsvirt.io/en/docs">
      <img
        src="https://img.shields.io/badge/Documenta%C3%A7%C3%A3o-7C3AED?style=flat-square&logo=readthedocs&logoColor=white"
        alt="Documentação"
      >
    </a>
    <a href="https://demo.zsvirt.io/">
      <img
        src="https://img.shields.io/badge/Demo%20online-16A34A?style=flat-square&logo=internetcomputer&logoColor=white"
        alt="Demo online"
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
    <strong>Português (Brasil)</strong> |
    <a href="./README.th.md">ไทย</a> |
    <a href="./README.tr.md">Türkçe</a> |
    <a href="./README.uk.md">Українська</a> |
    <a href="./README.bn.md">বাংলা</a> |
    <a href="./README.gr.md">Ελληνικά</a> |
    <a href="./README.vi.md">Tiếng Việt</a>
  </p>
</div>

> **Experimente agora, sem hardware físico.** Baixe a [imagem qcow2 ou OVA](https://zsvirt.io/download), inicie-a como VM no VMware, VirtualBox, KVM ou em um host na nuvem (virtualização aninhada) e configure um IP. O nó de gerenciamento do ZSvirt estará pronto. Consulte [Executar o ZSvirt dentro de uma VM](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node) ou o [Início rápido](https://zsvirt.io/en/docs/quick-start).

## O que é o ZSvirt?
O ZSvirt traz ao mundo de código aberto o mecanismo de virtualização ZSphere da [ZStack](https://www.zstack-cloud.com/), comprovado em ambientes empresariais. Com o apoio da [ZStack](https://www.zstack-cloud.com/), uma fornecedora consolidada de infraestrutura, o ZSvirt é uma plataforma leve e escalável para executar e gerenciar máquinas virtuais sem dependência de fornecedor.

Instale um nó de gerenciamento, conecte hosts KVM e gerencie VMs, clusters, armazenamento e redes por uma interface web, API RESTful (com Terraform e SDKs Go/Python/Java) ou CLI. As ferramentas de migração do VMware são integradas: migração online, importação de OVF e upload de VMDK.

## Tour do produto

<details open>
  <summary>
    <strong>📊 PAINEL — Visão unificada das operações</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-dashboard.png"
        alt="Painel de operações do ZSvirt"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🗂️ INVENTÁRIO — Gerenciamento centralizado da infraestrutura</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-inventory.png"
        alt="Inventário de infraestrutura do ZSvirt"
        width="100%"
      >
    </a>
  </p>
</details>

<br>

<details>
  <summary>
    <strong>🔄 GERENCIAMENTO DE MIGRAÇÕES — Migração de cargas de trabalho</strong>
  </summary>

  <br>

  <p align="center">
    <a href="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png">
      <img
        src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-migration-management.png"
        alt="Gerenciamento de migrações do ZSvirt"
        width="100%"
      >
    </a>
  </p>
</details>

## Demo online

A [demo do ZSvirt](https://demo.zsvirt.io/) é um ambiente hospedado gratuito para experimentar o ZSvirt online, sem instalação ou cadastro. Abra o link e clique em **Demo Login** para explorar a plataforma.

## Arquitetura

O ZSvirt adota uma arquitetura modular organizada em gerenciamento de recursos de virtualização, plano de gerenciamento, serviços de extensão e ferramentas operacionais.

As principais capacidades incluem:

- **Virtualização de computação**: gerenciamento de hosts, clusters, máquinas virtuais, imagens e ciclos de vida.
- **Virtualização de rede**: redes virtuais, serviços de rede, grupos de segurança e capacidades relacionadas.
- **Virtualização de armazenamento**: armazenamento primário e de backup, volumes, snapshots e gerenciamento de recursos de armazenamento.
- **Plano de gerenciamento**: framework de API, modelo de permissões, eventos, alarmes, auditoria e operação do sistema.
- **Serviços de extensão**: migração, recuperação de desastres, monitoramento, cotas, controle de acesso e operações empresariais.
- **Ferramentas e integrações**: instalação, diagnóstico, migração, scripts de automação, agentes, CLI e integração com sistemas externos.

Na arquitetura de software, o ZSvirt enfatiza assincronia, ausência de estado, extensibilidade e automação:

- **Arquitetura assíncrona**: mensagens, métodos e chamadas HTTP assíncronos reduzem bloqueios e aumentam a capacidade de processamento do sistema.
- **Serviços sem estado**: cada requisição independe do estado de outras requisições, facilitando escalabilidade, recuperação e operação.
- **Extensibilidade por plugins**: extensão horizontal de tipos de recursos, funções de negócio e integrações por meio de plugins.
- **Mecanismo de workflows**: controla a ordem de operações complexas e permite reversão e recuperação em caso de falhas.
- **Tags e consultas**: extensão de atributos, classificação de recursos, consultas unificadas e orquestração automatizada.
- **Implantação automatizada**: ferramentas automatizam implantação, configuração e operação, reduzindo a complexidade da implantação e da manutenção.

<p align="center">
  <img
    src="./assets/zsvirt-architecture.svg"
    alt="Arquitetura do ZSvirt"
    width="100%"
  >
</p>

## Guia de migração do VMware

Com a reavaliação das estratégias de virtualização, a migração do VMware para outras plataformas se tornou importante para organizações que buscam controlar custos, flexibilizar a infraestrutura e manter estabilidade operacional no longo prazo.

O ZSvirt oferece capacidades de migração e ferramentas operacionais para avaliar, planejar e transferir cargas de trabalho de ambientes VMware existentes para uma infraestrutura virtualizada baseada no ZSvirt.

- [Guia de migração do VMware](https://zsvirt.io/vmware-alternative/)

<p align="center">
  <a href="https://zsvirt.io/vmware-alternative/">
    <img
      src="https://github.com/ZSvirt/.github/blob/main/assets/zvirt-migrate.png?raw=true"
      alt="Migrar do VMware para o ZSvirt"
      width="100%"
    >
  </a>
</p>

## Início rápido

A forma mais rápida de avaliar o ZSvirt é seguir o início rápido da documentação. Ele explica como preparar recursos de computação, rede e armazenamento, inicializar o serviço de gerenciamento e criar sua primeira máquina virtual.

Prefere começar em uma VM? Baixe a [imagem qcow2 ou OVA](https://zsvirt.io/download) e execute o nó de gerenciamento dentro de um hipervisor — veja [Executar o ZSvirt dentro de uma VM](https://zsvirt.io/en/docs/quick-start/nested-virtualization-management-node).

🚀 [Início rápido](https://zsvirt.io/en/docs/quick-start)<br>
▶️ [Vídeo](https://youtu.be/LsSJlBRUvYw)

## Boas práticas
O ZSvirt utiliza o mesmo mecanismo empresarial do ZSphere e herda os resultados comprovados entre os clientes globais apresentados abaixo.
<p align="center">
  <img
    src="https://github.com/ZSvirt/.github/blob/main/assets/zsvirt-partner-en.png?raw=true"
    alt="Clientes e parceiros globais do ZSvirt"
    width="100%"
  >
</p>

## Comparação de plataformas de virtualização

Proxmox VE comparado ao VMware vSphere e ao ZSvirt

<p align="center">
  <img
    src="https://raw.githubusercontent.com/ZSvirt/.github/main/assets/zsvirt-comparison.png"
    alt="Comparação entre Proxmox VE e ZSvirt"
    width="100%"
  >
</p>

## Roteiro

#### 2º semestre de 2026

| Área | Trabalho planejado |
|---|---|
| **Segurança** | Adicionar criptografia de disco de VM<br>Adicionar criptografia de migração de VM |
| **DRS e migração de VM** | Adicionar opções de migração mais flexíveis:<br>• Poder migrar discos individualmente<br>• Suportar migração a quente e a frio para o cenário SAN para SAN |
| **ZMigrate** | **ZMigrate 2.1**:<br>• Adicionar pré-verificação para reduzir riscos em migrações complexas<br>• Fluxos de migração mais simples e inteligentes |
| **Operações** | Adicionar métricas de tempo de CPU Ready da VM, latência de leitura/gravação do disco da VM, taxa de perda de pacotes de rede e tempo de CPU Ready do host<br>Suportar níveis de log mais granulares no encaminhamento para syslog externo<br>Adicionar monitoramento de dispositivos NVMe e FC, incluindo IOPS e latência<br>Adicionar drivers QXL integrados ao VMTools para melhorar a exibição e a resolução do console da VM |
| **Gerenciamento de inventário** | Ampliar o registro de armazenamento e de VMs para cobrir o registro no mesmo site |

## Governança

O ZSvirt segue um modelo leve de governança de código aberto que define a manutenção do projeto, a tomada de decisões e a colaboração entre participantes.

O documento [GOVERNANCE.md](GOVERNANCE.md) apresenta os papéis do projeto, as responsabilidades dos mantenedores, os processos decisórios, o gerenciamento de versões e a colaboração comunitária.

À medida que a comunidade cresce, o modelo pode incluir mantenedores dedicados, grupos de trabalho e processos mais formais.

## Como contribuir

Valorizamos e agradecemos as contribuições da comunidade. Corrigir bugs, melhorar a documentação, propor funcionalidades, adicionar testes ou compartilhar práticas de implantação, migração e operação ajuda a melhorar o ZSvirt.

Quem está começando pode contribuir com documentação, relatos de problemas, verificação de testes, experiências de migração ou discussões. Desenvolvedores também podem melhorar o código, as ferramentas e as integrações.

Participantes ativos poderão receber reconhecimento em agradecimentos, notas de versão, listas de colaboradores ou futuros programas comunitários.

Antes de contribuir, leia:

- [CONTRIBUTING.md](CONTRIBUTING.md)

## Segurança

O processo de comunicação de vulnerabilidades está descrito em [SECURITY.md](SECURITY.md).

Não relate vulnerabilidades por GitHub Issues ou Discussions públicos.

## Licença

O ZSvirt é licenciado sob a [GNU General Public License v3.0](LICENSE).

Alguns repositórios ou componentes podem incluir software de código aberto de terceiros com outras licenças. Consulte `LICENSE`, `NOTICE` e os arquivos relacionados em cada repositório.

## Recursos

<table>
  <tr>
    <td width="50%">
      <h3>🌐 Site da comunidade</h3>
      <p>Conheça funcionalidades, casos de uso, notícias e recursos da comunidade ZSvirt.</p>
      <a href="https://zsvirt.io"><strong>Visitar o site →</strong></a>
    </td>
    <td width="50%">
      <h3>▶️ Vídeos</h3>
      <p>Assista à apresentação do ZSvirt e conheça suas principais capacidades.</p>
      <a href="https://youtu.be/c6pYmlIoPIU"><strong>Assistir ao vídeo do produto →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>📝 Blog</h3>
      <p>Leia novidades de versões, relatos de engenharia e análises sobre virtualização.</p>
      <a href="https://zsvirt.io/blog"><strong>Ler o blog →</strong></a>
    </td>
    <td width="50%">
      <h3>💬 GitHub Discussions</h3>
      <p>Faça perguntas, compartilhe ideias e converse com a comunidade ZSvirt.</p>
      <a href="https://github.com/ZSvirt/zsvirt/discussions"><strong>Participar da discussão →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>▶️ YouTube</h3>
      <p>Siga o canal do ZSvirt para demonstrações, tutoriais e atualizações do produto.</p>
      <a href="https://youtube.com/@ZSvirt"><strong>Seguir no YouTube →</strong></a>
    </td>
    <td width="50%">
      <h3>💼 LinkedIn</h3>
      <p>Acompanhe notícias do projeto, destaques da comunidade e análises do setor.</p>
      <a href="https://www.linkedin.com/in/zsvirt-community/"><strong>Seguir no LinkedIn →</strong></a>
    </td>
  </tr>

  <tr>
    <td width="50%">
      <h3>𝕏 X</h3>
      <p>Receba os últimos anúncios do ZSvirt e novidades da comunidade.</p>
      <a href="https://x.com/ZSvirt"><strong>Seguir no X →</strong></a>
    </td>
    <td width="50%">
      <h3>🎮 Discord</h3>
      <p>Entre na comunidade ZSvirt para perguntar, compartilhar ideias e conhecer outros usuários.</p>
      <a href="https://discord.com/invite/KHsw63z9xA"><strong>Entrar no Discord →</strong></a>
    </td>
  </tr>
</table>
