package com.test.loadshedding.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class HelloControllerTest {

	@Value(value = "${local.server.port}")
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void testRateLimitNotExceeded() throws InterruptedException {
		ExecutorService executorService =  Executors.newFixedThreadPool(3);
		CountDownLatch latch = new CountDownLatch(3);
		List<Callable<Integer>> tasks = new ArrayList<Callable<Integer>>();
		tasks.add(createTask(latch));
		tasks.add(createTask(latch));
		tasks.add(createTask(latch));
		List<Future<Integer>> outputList = executorService.invokeAll(tasks);
		latch.await(30, TimeUnit.SECONDS);
		boolean allSuccess = outputList.stream().map(f-> {
			try {
				return f.get();
			} catch (Exception e) {
				return 0;
			}
		}).allMatch(val -> val.intValue() == 200);
		Assertions.assertTrue(allSuccess);
	}

	@Test
	public void testRateLimitExceeded() throws InterruptedException {
		ExecutorService executorService =  Executors.newFixedThreadPool(4);
		CountDownLatch latch = new CountDownLatch(4);
		List<Callable<Integer>> tasks = new ArrayList<Callable<Integer>>();
		tasks.add(createTask(latch));
		tasks.add(createTask(latch));
		tasks.add(createTask(latch));
		tasks.add(createTask(latch));
		List<Future<Integer>> outputList = executorService.invokeAll(tasks);
		latch.await(30, TimeUnit.SECONDS);
		boolean rateLimitExceeded = outputList.stream().map(f-> {
			try {
				return f.get();
			} catch (Exception e) {
				return 0;
			}
		}).anyMatch(val -> val.intValue() == 503);
		Assertions.assertTrue(rateLimitExceeded);
	}

	private Callable<Integer> createTask(CountDownLatch latch) {
		return () -> {
			ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:" + port + "/hello",
					String.class, new Object());

			latch.countDown();
			return response.getStatusCode().value();
		};
	}
}
