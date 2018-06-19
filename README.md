# consul-integration-demo
A simple implementation of integrating springboot application with consul.

### What is consul?
It is a tool for discovering and configuring services in your infrastructure. It provides several key features:
##### Service Discovery: 
Clients of Consul can provide a service, such as api or mysql, and other clients can use Consul to discover providers of a given service. Using either DNS or HTTP, applications can easily find the services they depend upon.

##### Health Checking: 
Consul clients can provide any number of health checks, either associated with a given service ("is the webserver returning 200 OK"), or with the local node ("is memory utilization below 90%"). This information can be used by an operator to monitor cluster health, and it is used by the service discovery components to route traffic away from unhealthy hosts.

##### KV Store: 
Applications can make use of Consul's hierarchical key/value store for any number of purposes, including dynamic configuration, feature flagging, coordination, leader election, and more. The simple HTTP API makes it easy to use.

#### Prerequisites
It’s recommended to take a quick look at Consul and all its features. Install consul and run it in your machine.

For simplicity run the consul agent in deve mode.
**command**

    consul agent -dev

Please verify the consul status by opening browser with http://localhost:8500/

###### Step 1
Download springboot project from spring initializr. 
###### Step 2
Add the spring-cloud-starter-consul-all dependency to pom.xml
*pom.xml*

    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-consul-all</artifactId>
        <version>1.3.0.RELEASE</version>
    </dependency>
###### Step 3
*execute*

    mvn spring-boot:run
    
By default, Spring Boot will try to connect to the Consul agent at localhost:8500. For me I faced runtime error **"Consul service ids must not be empty, must start with a letter, end with a letter or digit, and have as interior characters only letters, digits, and hyphen"**. To resolve this error add the application name to **application.yml** file.
###### Step 4
*update application.yml*

    spring:
        application:
            name: consul-integration-demo
        cloud:
            consul:
                host: localhost
                port: 8500
    
Consul rejection error resolved once application name added to the **application.yml** file. Run the application and check the consul ui(http://localhost:8500/). We can see the **consul-integration-demo** in services tab in consul.

It means our service discovery part is okay.
