package com.test.loadshedding.filter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

@Component
public class AppFilter implements Filter {

	private final int MAX_REQUEST_COUNT = 3;

	private AtomicInteger requestCount = new AtomicInteger(0);

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		if (requestCount.get() >= MAX_REQUEST_COUNT) {
			HttpServletResponse httpResponse = (HttpServletResponse) response;
			httpResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			return;
		}
		try {
			requestCount.incrementAndGet();
			chain.doFilter(request, response);
		} finally {
			requestCount.decrementAndGet();
		}
	}

}
