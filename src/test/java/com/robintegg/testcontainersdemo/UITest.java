package com.robintegg.testcontainersdemo;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(initializers = { UITest.Initializer.class }, classes = RabbitMqTestConfiguration.class)
public class UITest {

	@LocalServerPort
	private int port;

	// @formatter:off
	@Rule
	public BrowserWebDriverContainer chrome = new BrowserWebDriverContainer()
			.withRecordingMode(VncRecordingMode.RECORD_FAILING, new File("./target/"))
			.withCapabilities(new ChromeOptions());
	// @formatter:on

	@ClassRule
	public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:latest");

	@ClassRule
	public static GenericContainer<?> activeMQContainer = new GenericContainer<>("rmohr/activemq:latest")
			.withExposedPorts(61616);

	@ClassRule
	public static GenericContainer<?> rabbitMQContainer = new GenericContainer<>("rabbitmq:management")
			.withExposedPorts(5672);

	@Test
	public void shouldSuccessfullyPassThisTestUsingTheRemoteDriver() throws InterruptedException {

		RemoteWebDriver driver = chrome.getWebDriver();

		System.out.println("Selenium remote URL is: " + chrome.getSeleniumAddress());
		System.out.println("VNC URL is: " + chrome.getVncAddress());

		String url = "http://host.docker.internal:" + port + "/";
		System.out.println("Spring Boot URL is: " + url);
		driver.get(url);

		List<WebElement> results = new WebDriverWait(driver, 15)
				.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.tagName("h1")));

		assertThat(results.size(), is(1));
		assertThat(results.get(0).getText(), containsString("Notifications"));

	}

	@Test
	public void shouldFailThisTestUsingTheRemoteDriverAndGenerateAVideoRecording() throws InterruptedException {

		RemoteWebDriver driver = chrome.getWebDriver();

		System.out.println("Selenium remote URL is: " + chrome.getSeleniumAddress());
		System.out.println("VNC URL is: " + chrome.getVncAddress());

		String url = "http://host.docker.internal:" + port + "/";
		System.out.println("Spring Boot URL is: " + url);
		driver.get(url);

		// added for effect when viewing the video
		Thread.currentThread().sleep(1000);

		List<WebElement> results = new WebDriverWait(driver, 15)
				.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.tagName("h1")));

		assertThat(results.size(), is(2));

	}

	static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext configurableApplicationContext) {

			DemoApplicationTestPropertyValues.using(postgreSQLContainer, activeMQContainer, rabbitMQContainer)
					.applyTo(configurableApplicationContext.getEnvironment());

		}

	}

}
