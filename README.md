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

#### Finding services using Consul
We already have our application registered in Consul, but how can clients find the service endpoints? We need a discovery client service to get a running and available service from Consul. Spring provides a *DiscoveryClient* API for this, which we can enable with the **@EnableDiscoveryClient** annotation:

*update application class with **@EnableDiscoveryClient***

    @SpringBootApplication
    @EnableDiscoveryClient
    @ComponentScan(basePackages= {"com.gramcha.*"})
    public class ConsulIntegrationDemoApplication {
    	public static void main(String[] args) {
    		SpringApplication.run(ConsulIntegrationDemoApplication.class, args);
    	}
    }

We are going to discover our service through consul in our service itself. This is to simulate finding URL of some service using consul. Let's create RestController class with two URL mappings "/discoveryClient","/ping".

In *"/discoveryClient"* mapping we are going to query the consul with application name **"consul-integration-demo"** and adding *"/ping"* to the URL returned by consul and make REST GET call.

Inject the DiscoveryClient bean into our restcontroller class to make query to consul.
*restcontroller and discoveryclient bean*

    @RestController
    public class DiscoveryClientController {
    	@Autowired
        private DiscoveryClient discoveryClient;
     
        public Optional<URI> serviceUrl() {
            return discoveryClient.getInstances("consul-integration-demo")
              .stream()
              .map(si -> si.getUri())
              .findFirst();
        }
    }

*discoveryClient.getInstances("consul-integration-demo")* will return list of URL with port number. For example: http://10.40.100.44:8080

Let's define our application endpoints
*endpoints*

    @RequestMapping("/discoveryClient")
    public String discoveryPing() throws RestClientException, 
      ServiceUnavailableException {
    		RestTemplate restTemplate = new RestTemplate();
    		System.out.println(serviceUrl());
        URI service = serviceUrl()
          .map(s -> s.resolve("/ping"))
          .orElseThrow(ServiceUnavailableException::new);
        return restTemplate.getForEntity(service, String.class)
          .getBody();
    }
     
    @RequestMapping("/ping")
    public String ping() {
        return "pong";
    }

Now open browser with localhost:8080/discoveryClient URL. */discoveryClient* endpoint will query the consul with "consul-integration-demo" application name. The Consul will returns list of URL's of that application. We are adding "/ping" to consul returned URL and making REST GET call and returning the result. Indirectly we are calling "/ping" endpoint through "/discoveryClient" endpoint.

#### Adding health check
Consul checks the health of the service endpoints periodically.

By default, Spring implements the health endpoint to return 200 OK if the app is up. If we want to customize the endpoint we have to update the **application.yml**
*application.yml*

    spring:
      cloud:
        consul:
          discovery:
            healthCheckPath: /app-health-check
            healthCheckInterval: 20s

As a result, Consul will poll the “/app-health-check” endpoint every 20 seconds.
Let’s define our custom health check service endpoint
*Health check service endpoint*

    @GetMapping("/app-health-check")
    public String myCustomCheck() {
        return "I am okay.";
    }

If we go to the Consul agent site, we’ll see that our application is passing with *"I am okay."* health check message.

#### Distributed Configuration
This consul feature allows synchronizing the configuration among all the services. Consul will watch for any configuration changes and then trigger the update of all the services.
First, we need to add the spring-cloud-starter-consul-config dependency to our **pom.xml**.
*pom.xml*

    <dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-consul-config</artifactId>
			<version>2.0.0.RELEASE</version>
	</dependency>

We also need to move the settings of Consul and Spring application name from the **application.yml** file to the **bootstrap.yml** file which Spring loads first.

Then, we need to **enable** Spring Cloud Consul **Config**:
*bootstrap.yml*

    spring:
      application:
        name: consul-integration-demo
      cloud:
        consul:
          host: localhost
          port: 8500
          discovery:
            healthCheckPath: /app-health-check
            healthCheckInterval: 20s
          config:
            enabled: true

Spring Cloud Consul Config will look for the properties in Consul at “/config/consul-integration-demo”. So if we have a property called “env.welcomeMessage”, we would need to create this property in the Consul agent site.

We can create the property by going to the “KEY/VALUE” section, then entering “/config/consul-integration-demo/env/welcomeMessage” in the “Create Key” form and “Hello World” as value. Finally, click the “Create” button.

let’s inject config properties from consul to our restcontroller class **DiscoveryClientController** and expose an endpoint to get that property.

*DiscoveryClientController*

    @Value("${env.welcomeMessage}")
    String welcomeMessage;
    
    @RequestMapping("/welcomeMessage")
    public String welcomeMessage() {
        return welcomeMessage;
    }
    
Now open browser with http://localhost:8080/welcomeMessage and you should see the “Hello World” from consul config.

#### Updating the Configuration at consul
We can update the configuration consul and no need to restart the Spring Boot application if we have added the **@RefreshScope** annotation to the **DiscoveryClientController** class where configuration used.

*DiscoveryClientController*
    @RestController
    @RefreshScope
    public class DiscoveryClientController {
        ...
        @Value("${env.welcomeMessage}")
        String welcomeMessage;
        ...
    }
    
