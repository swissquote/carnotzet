package com.swissquote.github.carnotzet.e2e.test;

import static com.github.swissquote.carnotzet.core.maven.CarnotzetModuleCoordinates.fromPom;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.id;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.github.swissquote.carnotzet.core.Carnotzet;
import com.github.swissquote.carnotzet.core.CarnotzetConfig;
import com.github.swissquote.carnotzet.core.CarnotzetModule;
import com.github.swissquote.carnotzet.core.runtime.DefaultCommandRunner;
import com.github.swissquote.carnotzet.core.runtime.log.LogEvents;
import com.github.swissquote.carnotzet.core.runtime.log.StdOutLogPrinter;
import com.github.swissquote.carnotzet.runtime.docker.compose.DockerComposeRuntime;

public class ExamplesTest {

	private static WebDriver driver;
	private static DockerComposeRuntime runtime;
	private static String votingApp;
	private static String resultApp;

	@BeforeClass
	public static void setup() throws Throwable {
		CarnotzetConfig config = CarnotzetConfig.builder()
				.topLevelModuleId(fromPom(Paths.get("../e2e-tests-carnotzet/pom.xml")))
				.build();
		Carnotzet carnotzet = new Carnotzet(config);
		runtime = new DockerComposeRuntime(carnotzet);

		if (runtime.isRunning()) {
			runtime.stop();
			runtime.clean();
		}

		LogEvents logEvents = new LogEvents();
		runtime.registerLogListener(logEvents);

		// print the environment logs in the test console, with consistent colors
		List<String> moduleNames = carnotzet.getModules().stream().map(CarnotzetModule::getServiceId).collect(Collectors.toList());
		runtime.registerLogListener(new StdOutLogPrinter(moduleNames, 1000, true));
		runtime.start();

		votingApp = "http://" + runtime.getContainer("voting-vote").getIp();
		resultApp = "http://" + runtime.getContainer("voting-result").getIp();

		driver = createBrowserSession();

		// wait for apps to become ready
		logEvents.waitForEntry("voting-result", "Connected to db", 10000, 50);
		logEvents.waitForEntry("voting-worker", "Connected to db", 10000, 50);
		logEvents.waitForEntry("voting-worker", "Connecting to redis", 10000, 50);
	}

	@AfterClass
	public static void cleanup() throws Throwable {
		runtime.stop();
		runtime.clean();
	}

	@Before
	public void resetDb() throws SQLException {
		String postgresIp = runtime.getContainer("postgres").getIp();
		try (Connection conn = DriverManager.getConnection("jdbc:postgresql://" + postgresIp + ":5432/postgres", "postgres", "")) {
			try (Statement statement = conn.createStatement()) {
				statement.execute("TRUNCATE TABLE votes");
			}
		}
	}

	@Test
	public void test_result_app_is_updated_on_new_votes() throws IOException, InterruptedException {
		vote("a", "voter_1");
		assertResultPage("100.0%", "0.0%", "1 vote");

		vote("b", "voter_2");
		assertResultPage("50.0%", "50.0%", "2 votes");

		vote("b", "voter_3");
		assertResultPage("33.0%", "67.0%", "3 votes");
	}

	@Test
	public void test_user_can_change_his_vote() throws IOException, InterruptedException {
		vote("a", "voter_1");
		assertResultPage("100.0%", "0.0%", "1 vote");

		vote("b", "voter_1");
		assertResultPage("0.0%", "100.0%", "1 vote");
	}

	@Test
	public void test_transitive_file_config() {
		String commandResult = DefaultCommandRunner.INSTANCE
				.runCommandAndCaptureOutput("docker", "exec", "-t", runtime.getContainer("redis").getId(), "cat", "/testfile");
		assertThat(commandResult, is("injected_file_content_into_redis"));
	}

	@Test
	public void test_transitive_env_config() {
		String commandResult = DefaultCommandRunner.INSTANCE
				.runCommandAndCaptureOutput("docker", "exec", "-t", runtime.getContainer("redis").getId(), "env");
		assertTrue(commandResult.contains("CARNOTZET_TEST=test_value"));
	}

	private void vote(String choice, String voterId) throws MalformedURLException {
		setVoterId(driver, voterId);
		driver.get(votingApp);
		driver.findElement(className(choice)).click();
	}

	private void assertResultPage(String catsPercent, String dogsPercent, String numVotes) throws InterruptedException {
		driver.get(resultApp);
		Thread.sleep(1000); // let some time for the page to update from DB polling.
		String displayedPercentForCats = driver.findElement(className("cats")).findElement(className("stat")).getText();
		String displayedPercentForDogs = driver.findElement(className("dogs")).findElement(className("stat")).getText();
		assertThat(displayedPercentForCats, is(catsPercent));
		assertThat(displayedPercentForDogs, is(dogsPercent));
		assertThat(driver.findElement(id("result")).getText(), is(numVotes));
	}

	private static WebDriver createBrowserSession() throws MalformedURLException {
		DesiredCapabilities capabilities = DesiredCapabilities.chrome();
		capabilities.setJavascriptEnabled(true);
		LoggingPreferences logPreferences = new LoggingPreferences();
		logPreferences.enable(LogType.BROWSER, Level.ALL);
		capabilities.setCapability(CapabilityType.LOGGING_PREFS, logPreferences);
		return new RemoteWebDriver(
				new URL("http://" + runtime.getContainer("selenium-chrome").getIp() + ":4444/wd/hub"),
				capabilities
		);
	}

	private static void setVoterId(WebDriver driver, String voterId) {
		driver.get(votingApp); // needed by selenium to set the cookie
		driver.manage().deleteCookieNamed("voter_id");
		driver.manage().addCookie(new Cookie.Builder("voter_id", voterId).build());
	}

}
