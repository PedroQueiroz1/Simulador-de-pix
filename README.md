# Pix Transaction Simulator

> Plataforma backend **event-driven** para simulação de pagamentos Pix, criada como projeto de portfólio com foco em Java, Spring Boot, microserviços, mensageria, idempotência, persistência, testes automatizados, CI/CD e práticas DevSecOps.

<p>
  <img src="https://img.shields.io/badge/Status-Em%20desenvolvimento-orange?style=for-the-badge">
  <img src="https://img.shields.io/badge/Java-17-blue?style=for-the-badge">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=for-the-badge">
  <img src="https://img.shields.io/badge/Architecture-Hexagonal-purple?style=for-the-badge">
</p>

---

## 🎯 Objetivo do projeto

O **Pix Transaction Simulator** tem como objetivo simular uma plataforma backend de processamento de pagamentos Pix, com foco em boas práticas utilizadas em sistemas transacionais.

A proposta é construir um projeto simples o suficiente para ser estudado e explicado em detalhes, mas completo o bastante para demonstrar conhecimentos importantes para uma vaga Backend Java Pleno, como:

- Java moderno;
- Spring Boot 3;
- arquitetura hexagonal;
- microserviços;
- mensageria com Kafka;
- idempotência em fluxos de pagamento;
- Redis;
- SQL Server;
- MongoDB;
- testes automatizados;
- CI/CD;
- DevSecOps;
- Docker;
- Kubernetes;
- Argo CD.

---

## 🧠 Contexto

Em sistemas de pagamento, uma mesma requisição pode ser enviada mais de uma vez por falhas de rede, timeout, retry do cliente ou instabilidade temporária entre serviços.

Por isso, este projeto tratará **idempotência** como uma decisão central desde o início.

A ideia é garantir que uma mesma intenção de pagamento não gere múltiplas transações indevidas.

Exemplo de problema que o projeto pretende simular:

1. O cliente solicita um pagamento Pix.
2. O backend processa a requisição.
3. A resposta falha por timeout ou queda de rede.
4. O cliente tenta novamente.
5. O sistema precisa reconhecer que aquela tentativa já existia e não deve criar um pagamento duplicado.

---

## 🛠️ Stack planejada

| Categoria | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.x |
| Build | Maven |
| API | Spring Web |
| Validação | Jakarta Validation |
| Persistência transacional | SQL Server |
| Cache / Idempotência | Redis |
| Mensageria | Kafka |
| Auditoria / Histórico | MongoDB |
| Testes | JUnit 5, Mockito, Testcontainers |
| Cobertura | JaCoCo |
| Containerização | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| DevSecOps | OWASP Dependency Check / análise de dependências |
| Orquestração | Kubernetes |
| GitOps | Argo CD |
| Frontend demonstrativo | HTML, CSS e JavaScript puro |

---

## 🔗 Repositório

[github.com/PedroQueiroz1/Simulador-de-pix](https://github.com/PedroQueiroz1/Simulador-de-pix.git)
