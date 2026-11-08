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

---

## TASK 2. Justification of the design patterns

### Factory Method

**Problem.** Opening an account involves more than calling a constructor. Each account type needs its own account number format, its own settings (overdraft limit for checking, interest rate for savings, daily transfer limit) and its own fee strategy. If this setup code is written wherever an account is created, it gets duplicated, and every place has to know about every account type.

**Solution.** `AccountFactory` declares the factory method `createAccount()`, and each concrete factory (`CheckingAccountFactory`, `SavingsAccountFactory`) builds one account type completely and correctly: account number, type-specific limits and the fee strategy the account will use. The rest of the system asks for an account of a given type and receives a ready-to-use `Account`, without knowing which class was instantiated or how it was configured.

| Simpler alternative | Factory Method |
|---------------------|----------------|
| A `switch` on the account type with `new CheckingAccount(...)` / `new SavingsAccount(...)` and setup code | Each account type has one factory that contains all its creation rules |
| Adding a new account type means editing every `switch` that creates accounts | Adding a new account type means adding a new factory; existing code does not change |
| Callers depend on concrete account classes | Callers depend only on `Account` and `AccountFactory` |

### Chain of Responsibility

**Problem.** A transfer must pass several independent checks before any money moves: amount, account existence, authorization, account status, sufficient funds and the daily limit. Banking rules also change over time, so checks are added, removed or reordered.

**Solution.** Each check is a separate handler with a single responsibility. A handler either rejects the transfer with a specific reason or passes it to the next handler. The chain is assembled once, and the transfer service only calls its first handler.

| Step | Check |
|:----:|-------|
| 1 | The amount is positive |
| 2 | Source and target accounts are different |
| 3 | Both accounts exist |
| 4 | The customer is authorized to use the source account |
| 5 | The source account can send and the target account can receive (not frozen or closed) |
| 6 | The source account has sufficient funds to cover the amount plus the fee, according to its own rules |
| 7 | The daily transfer limit of the source account is not exceeded |

| Simpler alternative | Chain of Responsibility |
|---------------------|-------------------------|
| One long validation method with a sequence of `if` statements | Each check is a small class that can be read and tested on its own |
| Adding or reordering a rule means editing that method, risking the other checks | Adding a rule means adding a handler; reordering means changing how the chain is assembled |
| The transfer service is coupled to every validation rule | The transfer service only knows the first handler of the chain |

### Strategy

**Problem.** Transfer fees depend on the source account: a fixed amount for checking accounts, a percentage for savings accounts, or no fee at all when an administrator waives the fees. The fee is needed in two places, when checking that the balance is sufficient and when charging it, and new fee policies may be introduced later.

**Solution.** `FeeStrategy` defines the fee calculation, and each policy is a separate implementation. Every account receives its strategy when it is created and delegates the calculation to it, so the transfer logic simply asks the account for its fee.

| Strategy | Used by | Fee |
|----------|---------|-----|
| `FixedFeeStrategy` | `CheckingAccount` | A fixed amount per transfer |
| `PercentageFeeStrategy` | `SavingsAccount` | A percentage of the transferred amount |
| `NoFeeStrategy` | Accounts whose fees were waived by an administrator | None |

| Simpler alternative | Strategy |
|---------------------|----------|
| `if (type == CHECKING) ... else if (type == SAVINGS) ...` wherever a fee is calculated | The fee calculation exists once per policy, in its own class |
| A new fee policy means finding and editing every fee `if` | A new fee policy means adding a new strategy class |
| The fee policy is fixed by the account type in code | The policy can be replaced at runtime, for example when an administrator waives the fees of an account |

### Observer

**Problem.** Several parts of the system must react to the same events. When a transfer is completed or rejected, the customer must be notified and the operation must be recorded in the audit log. Account events, such as an account being frozen, need the same reactions. More reactions will follow, such as forwarding events to the notification service through a message queue.

**Solution.** A `BankingEventPublisher` keeps a list of `BankingEventObserver`s and notifies all of them when an event occurs. The services that perform transfers and manage accounts only publish events; each observer decides on its own how to react.

| Event source | Events |
|--------------|--------|
| Transactions | Transfer completed, transfer rejected |
| Accounts | Account opened, frozen, unfrozen or closed; access granted or revoked; fees waived; balance below a threshold |

| Simpler alternative | Observer |
|---------------------|----------|
| The transfer service calls the email sender, the audit log and every other reaction directly | The transfer service only publishes an event and does not know who reacts |
| A new reaction means editing the transfer and account services | A new reaction means adding a new observer class |
| A failing notification can break the transfer that triggered it | Each observer is isolated; a failing observer does not stop the transfer or the other observers |
| Moving notifications to a separate service means rewriting every call | Moving notifications to a separate service means adding one observer that sends events to the message queue |

### How the patterns work together

The factories create each account together with its rules and its fee strategy. During a transfer, the chain of responsibility validates the request, using the source account's fee strategy to check that the balance covers the amount plus the fee. If all checks pass, the amount and the fee are applied and the transaction is marked as successful; otherwise it is marked as rejected. In both cases the observers are notified of the result.

> **Factory Method** ➜ **Chain of Responsibility** ➜ **Strategy** ➜ **Observer**
