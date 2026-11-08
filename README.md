<div align="center">

# Digital Banking System

SDT team project: a simplified Digital Banking System where customers manage bank accounts, transfer funds, view their transaction history and receive notifications, while administrators manage accounts and monitor transactions.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4-6DB33F?logo=springboot&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-build-C71A36?logo=apachemaven&logoColor=white)

</div>

### Functionalities

| Area | Features |
|------|----------|
| **Customer Management** | Create and manage customer profiles<br>Associate customers with one or more bank accounts<br>Manage account access |
| **Account Management** | Create and manage bank accounts<br>View account balances<br>View account details and status |
| **Transaction Management** | Transfer funds between accounts<br>Validate available balances before transactions<br>Maintain transaction history |
| **Transaction Status and Monitoring** | Track pending, successful, and rejected transactions<br>Allow administrators to inspect transactions<br>Maintain consistent account balances after transactions |
| **Notifications** | Notify customers about completed transactions<br>Notify users about failed transactions or important account activity |

### Team

| # | Name | Group |
|---|------|-------|
| 1 | KISELAR Vladyslav | 1241EB |
| 2 | BASIUK Yelyzaveta | 1241EB |
| 3 | KUCHERENKO Illia | 1241EB |

---

## TASK 1. Design patterns


| # | Pattern | Category | Where it is used | Functionality served |
|---|---------|----------|------------------|----------------------|
| 1 | [**Factory Method**](#factory-method) | Creational | Creating bank accounts of different types (`CheckingAccount`, `SavingsAccount`) | Account Management, Customer Management |
| 2 | [**Chain of Responsibility**](#chain-of-responsibility) | Behavioral | Validating a transfer through a sequence of independent checks | Transaction Management, Customer Management |
| 3 | [**Strategy**](#strategy) | Behavioral | Calculating the transfer fee according to the rules of the source account | Transaction Management |
| 4 | [**Observer**](#observer) | Behavioral | Reacting to transaction and account events (customer notifications, audit log) | Notifications |

### Factory Method

`Creational`

Each customer can own one or more accounts, and account types follow different rules: a checking account allows an overdraft up to a limit, while a savings account has an interest rate and does not allow the balance to go below zero. An abstract `AccountFactory` declares the factory method, and the concrete creators `CheckingAccountFactory` and `SavingsAccountFactory` decide which account type is created and how it is initialized: account number, type-specific limits and the fee strategy the account will use.

### Chain of Responsibility

`Behavioral`

Before a transfer is executed, it passes through a chain of validation handlers. Each handler performs one check and either rejects the transfer or passes it to the next handler:

| Step | Check |
|:----:|-------|
| 1 | The customer is authorized to use the source account |
| 2 | Both accounts exist |
| 3 | The source account can send and the target account can receive (not frozen or closed) |
| 4 | Source and target accounts are different |
| 5 | The amount is positive |
| 6 | The source account has sufficient funds to cover the amount plus the fee, according to its own rules |
| 7 | The daily transfer limit is not exceeded |

### Strategy

`Behavioral`

Each account type charges transfers differently. The fee calculation is defined by a common `FeeStrategy` interface with interchangeable implementations, and every account holds the strategy it was given when it was created:

| Strategy | Used by | Fee |
|----------|---------|-----|
| `FixedFeeStrategy` | `CheckingAccount` | A fixed amount per transfer |
| `PercentageFeeStrategy` | `SavingsAccount` | A percentage of the transferred amount |
| `NoFeeStrategy` | Accounts without transfer fees | None |

### Observer

`Behavioral`

When a transaction is completed or rejected, or when important activity happens on an account, a publisher notifies all registered observers. Observers such as the customer notification sender and the audit log react independently, and the transfer logic does not depend on any of them.

| Event source | Events |
|--------------|--------|
| Transactions | Transfer completed, transfer failed |
| Accounts | Account opened, frozen, unfrozen or closed; access granted or revoked; balance below a threshold |

### How the patterns work together

The factories create each account together with its rules and its fee strategy. During a transfer, the chain of responsibility validates the request, using the source account's fee strategy to check that the balance covers the amount plus the fee. The fee is then charged, the transaction is marked as successful or rejected, and the observers are notified of the result.

> **Factory Method** ➜ **Chain of Responsibility** ➜ **Strategy** ➜ **Observer**
