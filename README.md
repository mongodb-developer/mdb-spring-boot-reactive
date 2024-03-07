# Reactive Java Spring Boot with Spring Data MongoDB 

## About
A simplified cash balance application built with reactive Java Spring Boot and Spring Data MongoDB. This project showcases:
- Create, Read and Updates with ReactiveMongoRepository
- Create, Read and Updates with ReactiveMongoTemplate
- Wrapping queries in a multi-document transaction

## Supported Versions
- Java 21
- Spring Boot Starter Webflux 3.2.3 
- Spring Boot Starter Reactive Data Mongodb 3.2.3

## How it should work
1. A bank account can be created with a unique accountNum, and it always starts with a balance of $0.
2. Accounts and their balances are saved in the "accounts" collection.
3. A debit operation adds to the balance of the account.
4. A credit operation deducts from the balance of the account.
5. A transfer operation deducts from the balance of one account and adds to another.
6. A successful transaction flow is as follows:
   1. Debit/Credit/Transfer operations are first saved in the "transactions" collection with status "PENDING"
   2. The balances of each account is updated accordingly
   3. The transaction status is then updated to "SUCCESS"
7. If there is insufficient balance for deduction in an account, the transaction is rolled back and the status of the transaction is updated to "FAILED"
8. If the account number cannot be found, the transaction is rolled back and the status of the transaction is updated to "FAILED"

## Initialization and Setup
1. Ensure you have access to a MongoDB cluster
2. Run the following to set up schema validation. This creates a constraint such that the "balance" should never be less than 0.
```shell
mongosh "<MongoDB connection string>" --file setup.js
```
3. Create application.properties file in resources and add the following lines 
```properties
spring.data.mongodb.uri=<MongoDB connection string>
spring.data.mongodb.database=txn-demo
 ```
4. Run `mvn clean compile` to compile
5. Run `mvn spring-boot:run` to run the application

## API Usage

### Create account
POST /account \
Request Body:
```
{
  accountNum: <String>,
  balance: <Number>
}
```
Example:
```shell
curl --location 'localhost:8080/account' \
--header 'Content-Type: application/json' \
--data '{
    "accountNum": "111111"
}'
```

### Get account
GET /account/{accountNum}
Example:
```shell
curl --location 'localhost:8080/account/111111'
```

### Debit to account
POST /account/{accountNum}/debit \
Request Body:
```
{
  amount: <Number>
}
```
Example:
```shell
curl --location 'localhost:8080/account/111111/debit' \
--header 'Content-Type: application/json' \
--data '{
    "amount": 1000
}'
```

### Credit from account
POST /account/{accountNum}/credit \
Request Body:
```
{
  amount: <Number>
}
```
Example:
```shell
curl --location 'localhost:8080/account/111111/credit' \
--header 'Content-Type: application/json' \
--data '{
    "amount":10000
}'
```

### Transfer to another account
POST /account/{accountNum}/transfer \
Request Body:
```
{
  to: <String>
  amount: <Number>
}
```
Example:
```shell
curl --location 'localhost:8080/account/123456/transfer' \
--header 'Content-Type: application/json' \
--data '{
    "to": "1111111",
    "amount": 500
}'
```

## Postman Test Collection
The Postman Test Collection is meant for a high-level functional test to ensure that the APIs are still functionally correct.
The Postman calls are meant to be run in sequence and the database needs to be in a clean slate (see step 3).
To run the test:
1. Import the collection into Postman
2. In Postman, add environment variables "host" and "port"
3. In command line, run `mongosh "<MongoDB connection string>" --file setup.js` to reset the database.
4. Use Postman to "Run collection" to automatically run all the Postman calls in sequence and see that the tests are passing.

