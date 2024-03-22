## Introduction
[Spring Boot](https://spring.io/projects/spring-boot) +
[Reactive](https://spring.io/reactive) +
[Spring Data](https://spring.io/projects/spring-data) +
[MongoDB](https://www.mongodb.com/docs/). Putting these four technologies together can be a challenge especially if you are just starting out.
Without getting into details of each of these technologies, this tutorial aims to help you get a jump start on a working code base based on this technology stack.
This tutorial features:
- interacting with MongoDB using ReactiveMongoRepositories
- interacting with MongoDB using ReactiveMongoTemplate
- wrapping queries in a [multi-document ACID transaction](https://www.mongodb.com/products/capabilities/transactions)




This simplified cash balance application allows you to make REST API calls to:
- Create or fetch an account
- Perform transactions on one account or between two accounts


## GitHub Repository
Access the [repository README](https://github.com/mongodb-developer/mdb-spring-boot-reactive) for more details on the functional specifications.
The README also contains setup, API usage and testing instructions. To clone the repository:


```shell
git clone git@github.com:mongodb-developer/mdb-spring-boot-reactive.git
```


## Code Walkthrough
Let's do a logical walkthrough of how the code works.
I would include code snippets, but to reduce verbosity, I will exclude lines of codes that are not key to our understanding of how the code works.


### Creating or fetching an account
This section showcases how you can perform Create and Read operations with `ReactiveMongoRepository`.


The API endpoints to create or fetch an account can be found
in [AccountController.java](https://github.com/mongodb-developer/mdb-spring-boot-reactive/blob/main/src/main/java/com/example/mdbspringbootreactive/controller/AccountController.java):


```java
@RestController
public class AccountController {
   //...
   @PostMapping("/account")
   public Mono<Account> createAccount(@RequestBody Account account) {
       return accountRepository.save(account);
   }


   @GetMapping("/account/{accountNum}")
   public Mono<Account> getAccount(@PathVariable String accountNum) {
       return accountRepository.findByAccountNum(accountNum).switchIfEmpty(Mono.error(new AccountNotFoundException()));
   }
   //...
}
```
This snippet shows two endpoints:
- A POST method endpoint that creates an account
- A GET method endpoint that retrieves an account but throws an exception if it cannot be found


They both simply return a `Mono<Account>` from [AccountRepository.java](https://github.com/mongodb-developer/mdb-spring-boot-reactive/blob/main/src/main/java/com/example/mdbspringbootreactive/repository/AccountRepository.java),
a `ReactiveMongoRespository` interface which acts as an abstraction from the underlying
[Reactive Streams Driver](https://www.mongodb.com/docs/drivers/reactive-streams/).
- `.save(...)` method creates a new document in the accounts collection in our MongoDB database
- `.findByAccountNum()` method fetches a document that matches the `accountNum`


```java
public interface AccountRepository extends ReactiveMongoRepository<Account, String> {
  
   @Query("{accountNum:'?0'}")
   Mono<Account> findByAccountNum(String accountNum);
   //...
}
```


The [@Query annotation](https://docs.spring.io/spring-data/mongodb/reference/mongodb/repositories/query-methods.html#mongodb.repositories.queries.json-based)
allows you to specify a MongoDB query with placeholders so that it can be dynamically substituted with values from method arguments.
`?0` would be substituted by the value of the first method argument and `?1` would be substituted by the second, so on and so forth.


The built-in [query builder mechanism](https://docs.spring.io/spring-data/mongodb/reference/repositories/query-methods-details.html)
can actually determine the intended query based on the method's name.
In this case, we could actually exclude the [@Query annotation](https://docs.spring.io/spring-data/mongodb/reference/mongodb/repositories/query-methods.html#mongodb.repositories.queries.json-based)
but I left it there for better clarity and to illustrate the previous point.


Notice that there is no need to declare a `save(...)` method even though we are actually using `accountRepository.save()`
in [AccountController.java](https://github.com/mongodb-developer/mdb-spring-boot-reactive/blob/main/src/main/java/com/example/mdbspringbootreactive/controller/AccountController.java).
The `save(...)` method, and many other base methods, are already declared by interfaces up in the inheritance chain of `ReactiveMongoRepository`.


### Debit, Credit and Transfer
This section showcases:
- Update operations with `ReactiveMongoRepository`
- Create, Read and Update operations with `ReactiveMongoTemplate`


Back to `AccountController.java`:
```java
@RestController
public class AccountController {
   //...
   @PostMapping("/account/{accountNum}/debit")
   public Mono<Txn> debitAccount(@PathVariable String accountNum, @RequestBody Map<String, Object> requestBody) {
       //...
       txn.addEntry(new TxnEntry(accountNum, amount));
       return txnService.saveTransaction(txn).flatMap(txnService::executeTxn);
   }


   @PostMapping("/account/{accountNum}/credit")
   public Mono<Txn> creditAccount(@PathVariable String accountNum, @RequestBody Map<String, Object> requestBody) {
       //...
       txn.addEntry(new TxnEntry(accountNum, -amount));
       return txnService.saveTransaction(txn).flatMap(txnService::executeTxn);
   }


   @PostMapping("/account/{from}/transfer")
   public Mono<Txn> transfer(@PathVariable String from, @RequestBody TransferRequest transferRequest) {
       //...
       txn.addEntry(new TxnEntry(from, -amount));
       txn.addEntry(new TxnEntry(to, amount));
       //save pending transaction then execute
       return txnService.saveTransaction(txn).flatMap(txnService::executeTxn);
   }
   //...
}
```
This snippet shows three endpoints:
- A `.../debit` endpoint that adds to an account balance
- A `.../credit` endpoint that subtracts from an account balance
- A `.../transfer` endpoint that performs a transfer from one account to another


Notice that all three methods look really similar. The main idea is:
- A `Txn` can consist of one to many `TxnEntry`
- A `TxnEntry` is a reflection of a change we are about to make to a single account
- A debit or credit `Txn` will only have one `TxnEntry`
- A transfer `Txn` will have two `TxnEntry`
- In all three operations, we first save one record of the `Txn` we are about to perform,
  and then make the intended changes to the target accounts using the [TxnService.java](https://github.com/mongodb-developer/mdb-spring-boot-reactive/blob/main/src/main/java/com/example/mdbspringbootreactive/service/TxnService.java).


```java
@Service
public class TxnService {
   //...
   public Mono<Txn> saveTransaction(Txn txn) {
       return txnTemplate.save(txn);
   }


   public Mono<Txn> executeTxn(Txn txn) {
       return updateBalances(txn)
               .onErrorResume(DataIntegrityViolationException.class
                       /*lambda expression to handle error*/)
               .onErrorResume(AccountNotFoundException.class
                       /*lambda expression to handle error*/)
               .then(txnTemplate.findAndUpdateStatusById(txn.getId(), TxnStatus.SUCCESS));
   }
  


   public Flux<Long> updateBalances(Txn txn) {
       //read entries to update balances, concatMap maintains the sequence
       Flux<Long> updatedCounts = Flux.fromIterable(txn.getEntries()).concatMap(
               entry -> accountRepository.findAndIncrementBalanceByAccountNum(entry.getAccountNum(), entry.getAmount())
           );
       return updatedCounts.handle(/*...*/);
   }
}
```
The `updateBalances(...)` method is responsible for iterating through each `TxnEntry` and making the corresponding updates to each account.
This is done by calling the `findAndIncrementBalanceByAccountNum(...)` method
in [AccountRespository.java](https://github.com/mongodb-developer/mdb-spring-boot-reactive/blob/main/src/main/java/com/example/mdbspringbootreactive/repository/AccountRepository.java).


```java
public interface AccountRepository extends ReactiveMongoRepository<Account, String> {
   //...
   @Update("{'$inc':{'balance': ?1}}")
   Mono<Long> findAndIncrementBalanceByAccountNum(String accountNum, double increment);
}
```
Similar to declaring `find` methods, you can also declare [Data Manipulation Methods](https://docs.spring.io/spring-data/mongodb/reference/mongodb/repositories/modifying-methods.html)
in the `ReactiveMongoRepository` such as `update` methods.
Once again, the [query builder mechanism](https://docs.spring.io/spring-data/mongodb/reference/repositories/query-methods-details.html)
is able to determine that we are interested in querying by `accountNum` based on the naming of the method, and we define the action of an update using the `@Update` annotation.
In this case, the action is an `$inc` and notice that we used `?1` as a placeholder because we want to substitute it with the value of the second argument of the method.


Moving on, in `TxnService` we also have:
- A `saveTransaction` method that saves a `Txn` document into `transactions` collection
- A `executeTxn` method that calls `updateBalances(...)` then updates the transaction status in the `Txn` document created


Both utilize the `TxnTemplate` that contains a `ReactiveMongoTemplate`.


```java
@Service
public class TxnTemplate {
   //...
   public Mono<Txn> save(Txn txn) {
       return template.save(txn);
   }


   public Mono<Txn> findAndUpdateStatusById(String id, TxnStatus status) {
       Query query = query(where("_id").is(id));
       Update update = update("status", status);
       FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);
       return template.findAndModify(query, update, options, Txn.class);
   }
   //...
}
```
The `ReactiveMongoTemplate` provides us with more customizable ways to interact with MongoDB and is a thinner layer of abstraction compared to `ReactiveMongoRepository`.
In the `findAndUpdateStatusById(...)` method, we are pretty much defining the query logic by code, but we are also able to specify that the update should return the newly updated document.


### Multi-document ACID Transactions
The transfer feature in this application is a perfect use-case for multi-document transactions because the updates across two accounts need to be atomic.


In order for the application to gain access to Spring's transaction support, we first need to add a `ReactiveMongoTransactionManager` bean to our configuration as such:


```java
@Configuration
public class ReactiveMongoConfig extends AbstractReactiveMongoConfiguration {
   //...
   @Bean
   ReactiveMongoTransactionManager transactionManager(ReactiveMongoDatabaseFactory dbFactory) {
       return new ReactiveMongoTransactionManager(dbFactory);
   }
}
```
With this, we can proceed to define the scope of our transactions. We will showcase two methods:

**1. Using _TransactionalOperator_**

The `ReactiveMongoTransactionManager` provides us with a `TransactionOperator`.
We can then define the scope of a transaction by appending `.as(transactionalOperator::transactional)` to the method call.
```java
@Service
public class TxnService {
   //In the actual code we are using constructor injection instead of @Autowired
   //Using @Autowired here to keep code snippet concise
   @Autowired
   private TransactionalOperator transactionalOperator;
   //...
   public Mono<Txn> executeTxn(Txn txn) {
       return updateBalances(txn)
               .onErrorResume(DataIntegrityViolationException.class
                       /*lambda expression to handle error*/)
               .onErrorResume(AccountNotFoundException.class
                       /*lambda expression to handle error*/)
               .then(txnTemplate.findAndUpdateStatusById(txn.getId(), TxnStatus.SUCCESS))
               .as(transactionalOperator::transactional);
   }
   //...
}
```

**2. Using _@Transactional_ annotation**

We can also simply define the scope of our transaction by annotating the method with the `@Transactional` annotation.
```java
public class TxnService {
   //...
   @Transactional
   public Mono<Txn> executeTxn(Txn txn) {
       return updateBalances(txn)
               .onErrorResume(DataIntegrityViolationException.class
                       /*lambda expression to handle error*/)
               .onErrorResume(AccountNotFoundException.class
                       /*lambda expression to handle error*/)
               .then(txnTemplate.findAndUpdateStatusById(txn.getId(), TxnStatus.SUCCESS));
   }
   //...
}
```
Read more about [Transactions and Sessions in Spring Data MongoDB](https://docs.spring.io/spring-data/mongodb/reference/mongodb/client-session-transactions.html) for more information.


## Conclusion
We are done! Hope this post was helpful for you in one way or another. If you have any questions, visit [MongoDB Community](https://www.mongodb.com/community/) website where MongoDB engineers and the community can help you with your next big idea!

Once again, you may access the code from the [GitHub repository](https://github.com/mongodb-developer/mdb-spring-boot-reactive),
and if you are just getting started, it may be worth bookmarking [Spring Data MongoDB](https://docs.spring.io/spring-data/mongodb/reference/index.html).

