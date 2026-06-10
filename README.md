# Pix Transaction Simulator

> Plataforma backend **event-driven** para simulação de pagamentos Pix, criada como projeto de portfólio com foco em Java, Spring Boot, microserviços, mensageria, idempotência, persistência, testes automatizados, CI/CD e práticas DevSecOps.

<p>
  <img src="https://img.shields.io/badge/Java-17-blue?style=for-the-badge">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.x-brightgreen?style=for-the-badge">
  <img src="https://img.shields.io/badge/Architecture-Hexagonal-purple?style=for-the-badge">
  <img src="https://img.shields.io/badge/Event--Driven-Kafka-orange?style=for-the-badge">
  <img src="https://img.shields.io/badge/DevSecOps-CI%2FCD-red?style=for-the-badge">
</p>

---

## 🎯 Objetivo do projeto

O **Pix Transaction Simulator** simula uma plataforma backend de processamento de pagamentos Pix, com foco nas boas práticas de sistemas transacionais, arquitetura event-driven, idempotência, persistência confiável, mensageria, rastreabilidade e práticas DevSecOps.

A proposta é construir um projeto simples o suficiente para ser estudado e explicado em detalhes, mas completo o bastante para demonstrar conhecimentos de uma vaga **Backend Java Pleno**: Java moderno, Spring Boot 3, arquitetura hexagonal, microserviços, Redis, Kafka, SQL Server, MongoDB, testes automatizados, CI/CD, Docker, Kubernetes, Argo CD e boas práticas de segurança.

> ⚠️ Este projeto **simula** um fluxo de pagamento. Ele **não** é uma integração real com o Pix/Bacen.

---

## 🛠️ Stack atual

| Categoria                        | Tecnologia                                                                |
| -------------------------------- | ------------------------------------------------------------------------- |
| Linguagem                        | Java 17                                                                   |
| Framework                        | Spring Boot 3.3.x                                                         |
| Build                            | Maven multi-module                                                        |
| API REST                         | `spring-boot-starter-web`                                                 |
| Validação                        | `spring-boot-starter-validation` — Jakarta Validation                     |
| Geração de ID                    | `com.github.f4b6a3:uuid-creator` — UUIDv7 isolado em adapter              |
| Arquitetura                      | Hexagonal — Ports & Adapters                                              |
| Persistência transacional        | SQL Server                                                                |
| ORM / Persistência Java          | Spring Data JPA                                                           |
| Versionamento de banco           | Flyway                                                                    |
| Idempotência                     | Redis + `Idempotency-Key` + fingerprint SHA-256 do payload                |
| Mensageria                       | Apache Kafka                                                              |
| Publicação confiável de eventos  | Transactional Outbox Pattern                                              |
| Auditoria / histórico de eventos | MongoDB                                                                   |
| Worker assíncrono                | `pix-notification-worker` com Kafka Consumer                              |
| Ledger transacional              | Ledger append-only com lançamentos DEBIT/CREDIT e fechamento em zero      |
| Observabilidade básica           | Correlation ID, Request ID, MDC, logs rastreáveis e Spring Boot Actuator  |
| Testes                           | JUnit 5, Mockito, Spring Boot Test, MockMvc e Testcontainers              |
| Cobertura de testes              | JaCoCo                                                                    |
| DevSecOps                        | OWASP Dependency Check, `.env` ignorado e `.env.example` com placeholders |
| Containerização                  | Docker e Docker Compose                                                   |
| CI/CD                            | GitHub Actions                                                            |
| Orquestração demonstrativa       | Kubernetes manifests                                                      |
| GitOps demonstrativo             | Argo CD Application                                                       |
| Frontend demonstrativo           | HTML, CSS e JavaScript puro                                               |

---

## 🔗 Repositório

[github.com/PedroQueiroz1/Simulador-de-pix](https://github.com/PedroQueiroz1/Simulador-de-pix.git)
