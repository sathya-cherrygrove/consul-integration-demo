/**
 * @author gramcha
 * 19-Jun-2018 1:21:59 PM
 * 
 */
package com.gramcha.controller;

import java.net.URI;
import java.util.Optional;

import javax.naming.ServiceUnavailableException;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.sun.jersey.core.util.MultivaluedMapImpl;

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
    @GetMapping("/app-health-check")
    public String myCustomCheck() {
        return "I am okay.";
    }
}
