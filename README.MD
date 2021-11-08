Wallet Application
---
To compile the application, it is recommended to use the following command:

`mvn clean install docker:build`

To deploy the application and database in docker using docker-compose you can use the script `start.sh`

The following users can be used for authorization: User1, User2, User3. They all have the same password - 1.

Task description
---
Implement e-wallet with REST API to create it, top it up, check its balance and withdraw funds using Spring (preferably
Spring Boot). Funds on any wallet should not go below zero. The project should be built with Maven or Gradle and shipped
via public VCS repository (preferably GitHub).

Key points:

· implement REST API 
· implement database interaction with the domain model 
· simple React UI with the list of wallets and their balances with the option to add / withdraw money from wallet

Bonus points for any of:

· wallet to wallet transaction 
· running within Docker infrastructure (Dockerfile, docker-compose)
· backend test coverage · basic endpoint authorization (1 account for all)

In case of doubt, you can make any assumptions in line with this task description.
