package com.swissquote.github.carnotzet.e2e.test;

import static com.github.swissquote.carnotzet.core.maven.CarnotzetModuleCoordinates.fromPom;
import static org.hamcrest.core.Is.is;
import static org.openqa.selenium.By.className;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.logging.Logs;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.github.swissquote.carnotzet.core.Carnotzet;
import com.github.swissquote.carnotzet.core.runtime.log.LogEvents;
import com.github.swissquote.carnotzet.core.runtime.log.StdOutLogPrinter;
import com.github.swissquote.carnotzet.runtime.docker.compose.DockerComposeRuntime;

public class ExamplesTest {

	private static LogEvents logEvents;
	private static DockerComposeRuntime runtime;
	private static String votingApp;
	private static String resultApp;

	@BeforeClass
	public static void setup() throws Throwable {
		Carnotzet carnotzet = new Carnotzet(fromPom(Paths.get("../e2e-tests-carnotzet/pom.xml")));
		runtime = new DockerComposeRuntime(carnotzet);

		if (runtime.isRunning()){
			runtime.stop();
			runtime.clean();
		}

		logEvents = new LogEvents();
		runtime.registerLogListener(logEvents);
		runtime.registerLogListener(new StdOutLogPrinter()); // print the environment logs in the test console
		runtime.start();

		votingApp = "http://" + runtime.getContainer("voting-vote").getIp();
		votingApp = "http://" + runtime.getContainer("voting-result").getIp();

		// wait for apps to become ready
		logEvents.waitForEntry("voting-result", "Connected to db", 10000, 50);
		logEvents.waitForEntry("voting-worker", "Connected to db", 10000, 50);
		logEvents.waitForEntry("voting-worker", "Connected to redis", 10000, 50);
	}

	@AfterClass
	public static void cleanup() throws Throwable {
		runtime.stop();
		runtime.clean();
	}

	@Test
	public void test_result_app_is_updated_on_new_votes() throws IOException, InterruptedException {

		WebDriver driver = selenium();

		driver.get(votingApp);
		File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
		FileUtils.copyFile(scrFile, new File("target/selenium/screenshot_voting_app.png"));
		driver.findElement(className("a")).click();

		driver.get(resultApp);
		String displayedPercentForCats = driver.findElement(className("cats")).findElement(className("stat")).getText();
		Assert.assertThat(displayedPercentForCats, is("100.0%"));
		Assert.assertThat(driver.findElement(By.id("result")).getText(), is("1 vote"));
		driver.manage().deleteAllCookies();

	}

	@Test
	public void test_user_can_change_his_vote() throws IOException {
		// TODO
	}

	private void resetDb() {
		runtime.stop("postgres");
		runtime.clean("postgres");
		runtime.start("postgres");
	}

	private WebDriver selenium() throws MalformedURLException {
		DesiredCapabilities capabilities = DesiredCapabilities.chrome();
		capabilities.setJavascriptEnabled(true);
		LoggingPreferences logPreferences = new LoggingPreferences();
		logPreferences.enable(LogType.BROWSER, Level.ALL);
		capabilities.setCapability(CapabilityType.LOGGING_PREFS, logPreferences);

		return new RemoteWebDriver(
				new URL("http://" + runtime.getContainer("selenium-chrome").getIp()+":4444/wd/hub"),
				capabilities
		);
	}

}
