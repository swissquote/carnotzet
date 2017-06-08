---
title: "Using Carnotzet for “end to end” tests"
url: /user-guide/end-to-end-tests
---

{% include toc %} 

Let's write an "end to end" test for the  [example voting app from docker-compose](https://github.com/docker/example-voting-app) using selenium. 

## Defining the test environment

We define the test environment using Carnotzet, we create a new maven module named e2e-tests-carnotzet, then import (re-use) the 
voting-app environment and selenium-chrome : 


```xml
<dependencies>
	<dependency>
		<groupId>com.github.swissquote.examples</groupId>
		<artifactId>selenium-chrome-carnotzet</artifactId>
		<version>3.4.0</version>
	</dependency>
	<dependency>
		<groupId>com.github.swissquote.examples</groupId>
		<artifactId>voting-all-carnotzet</artifactId>
		<version>1.0.0</version>
	</dependency>
</dependencies>
```

Note that all components of the voting application will be imported transitively, we don't need to know all the architecture details to define our e2e test environment !

Since this module is only aggregating two existing environments, we need to specify in src/main/resource/carnotzet.properties that there is no service (docker image) to run : 

```properties
docker.image=none
```

> The full code for this module is available in the e2e-tests-carnotzet directory

## Managing the environment from Java tests

We will use Junit to run the e2e tests, this is just an example, you can use Carnotzet with any test framework.

In order to interact with the environment (start/stop etc...) we need to import the following java libraries from our test module :

```xml
<dependency>
	<groupId>com.github.swissquote</groupId>
	<artifactId>carnotzet-core</artifactId>
	<version>${carnotzet.version}</version>
	<scope>test</scope>
</dependency>
<dependency>
	<groupId>com.github.swissquote</groupId>
	<artifactId>carnotzet-orchestrator-docker-compose</artifactId>
	<version>${carnotzet.version}</version>
	<scope>test</scope>
</dependency>
```

Our test will simulate users that vote on the voting-app and assert that the voting-result page is updated accordingly :

```java
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
```

First we need to start our test environment before the test executes :

```java
@BeforeClass
public static void setup() throws Throwable {
	CarnotzetConfig config = CarnotzetConfig.builder()
					.topLevelModuleId(fromPom(Paths.get("../e2e-tests-carnotzet/pom.xml")))
					.build();
	Carnotzet carnotzet = new Carnotzet(config);
	runtime = new DockerComposeRuntime(carnotzet);
	runtime.start();
}
```

Let's also cleanup the environment after the tests : 
```java
@AfterClass
public static void cleanup() throws Throwable {
	runtime.stop();
	runtime.clean();
}
```

We have a small problem now... the environment takes some time to start and we should wait until it is ready
to serve clients before actually running the tests.
To fix this, we can wait until a log event is emitted by some services running in the test environment.
Let's change the setup() method to block until the voting-result and voting-worker are connected to the db :

```java
LogEvents logEvents = new LogEvents();
runtime.registerLogListener(logEvents);
runtime.start();
logEvents.waitForEntry("voting-result", "Connected to db", 10000, 50);
logEvents.waitForEntry("voting-worker", "Connected to db", 10000, 50);
```

We can register many log listeners to do different things, for example if we want to output the logs of all 
services to stdout : 
 ```java
runtime.registerLogListener(new StdOutLogPrinter(1000, true));
 ```

## Communicating with services running in the test environment
Now that the environment is started, let's look at how we can communicate with the service running inside.
The urls for the voting and result webapps for example : 
```java
votingApp = "http://" + runtime.getContainer("voting-vote").getIp();
resultApp = "http://" + runtime.getContainer("voting-result").getIp();
```

We can also use this to configure selenium remote driver :
```java
WebDriver driver = new RemoteWebDriver(new URL("http://" + runtime.getContainer("selenium-chrome").getIp()+":4444/wd/hub"),capabilities
```

Cleaning the database between tests : 
```java
@Before
public void resetDb() throws SQLException {
	String postgresIp = runtime.getContainer("postgres").getIp();
	try (Connection conn = DriverManager.getConnection("jdbc:postgresql://"+postgresIp+":5432/postgres","postgres","")) {
		try (Statement statement = conn.createStatement()) {
			statement.execute("TRUNCATE TABLE votes");
		}
	}
}
```

## Final result
That's it ! we now have all the pieces to make our test work here's the final code of our test 
(also available in the e2e-test directory of the project, those tests are actually run by travis on every push to the carnotzet project).

```java
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
		List<String> moduleNames = carnotzet.getModules().stream().map(CarnotzetModule::getName).collect(Collectors.toList());
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

```



## Portability and Isolation 

The only requirements to run those tests are java, maven and docker. All the components of the voting application (C#, python, NodeJS, redis 
and postgres), as well as selenium and chrome are all running as docker containers. This greatly simplifies how you manage testing environments. 
This is specially useful for running complex environments in a CI tools such as Jenkins or Travis.

Note that all services in a carnotzet environment run in an isolated docker network, they can talk to each-other and use name resolution, 
you can even run the test environment multiple times in parallel on the same docker host without running into any collision.
