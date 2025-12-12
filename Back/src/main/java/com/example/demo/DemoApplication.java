package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;

@SpringBootApplication
public class DemoApplication implements ApplicationListener<WebServerInitializedEvent> {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Override
	public void onApplicationEvent(WebServerInitializedEvent event) {
		int port = event.getWebServer().getPort();
		System.out.println("Application started and listening on port: " + port);
	}

}
