# Pix Transaction Simulator

> Plataforma backend **event-driven** para simulação de pagamentos Pix, criada como projeto de portfólio com foco em Java, Spring Boot, microserviços, mensageria, idempotência, persistência, testes automatizados, CI/CD e práticas DevSecOps.

<p>
  <img src="https://img.shields.io/badge/Java-17-blue?style=for-the-badge">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.x-brightgreen?style=for-the-badge">
  <img src="https://img.shields.io/badge/Architecture-Hexagonal-purple?style=for-the-badge">
</p>

---

## 🎯 Objetivo do projeto

O **Pix Transaction Simulator** simula uma plataforma backend de processamento de pagamentos Pix, com foco nas boas práticas de sistemas transacionais.

A proposta é construir um projeto simples o suficiente para ser estudado e explicado em detalhes, mas completo o bastante para demonstrar conhecimentos de uma vaga **Backend Java Pleno**: Java moderno, Spring Boot 3, arquitetura hexagonal, microserviços, idempotência, testes automatizados, CI/CD e DevSecOps.

> ⚠️ Este projeto **simula** um fluxo de pagamento. Ele **não** é uma integração real com o Pix/Bacen.

---

## 🛠️ Stack atual

| Categoria | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.3.x |
| Build | Maven multi-module |
| API REST | `spring-boot-starter-web` |
| Validação | `spring-boot-starter-validation` (Jakarta Validation) |
| Geração de ID | `com.github.f4b6a3:uuid-creator` (UUIDv7), isolada em adapter |
| Persistência | Em memória (`ConcurrentHashMap`) — temporária |
| Testes | JUnit 5, Mockito, Spring Boot Test (MockMvc) |
| Arquitetura | Hexagonal (ports & adapters) |

---

## 🔗 Repositório

[github.com/PedroQueiroz1/Simulador-de-pix](https://github.com/PedroQueiroz1/Simulador-de-pix.git)
