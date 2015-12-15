ACMS - Application Configuration Management Service
===

How to use
---
* Import into IntelliJ (Maven project)
* Add Spring feature to module, and register AcmsService as Spring Context (it should autodetect and suggest this)
* Open application.properties and point spring.cloud.config.server.git.uri=file:/home/niki/Projects/config-git-repo/ to something you have locally
* Open class AcmsService and run/debug as Spring Boot app
* 

Todo
---
- Make the service itself configurable (just add profile specific application.yml files and pass profiles to cmd line --spring.active.profiles=dev,test,foo)
- Fix security configuration (again, depending on profile)
- Create proper keypair for encrypt/decrypt and figure out how to secure this
- define and document the actual GIT repo and process for managing config files.

Docker
---
Tried running the RabbitMQ in docker, but couldn't seem to connect (see docker-compose.yml) file...
