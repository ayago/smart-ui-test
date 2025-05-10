# Smart UI Test Demo

This project is a lean Spring Boot application designed to automate UI workflows testing based on structured input defined in a simple text file. It supports dynamic form filling, feature flag activation, and field verification using a flexible parsing and execution engine built on Selenium WebDriver.

**This is a Work In-Progress.**

## Features

* 🧠 Smart locator engine: Automatically finds input fields using labels, placeholders, name, id, or title attributes.
* 🔧 Feature flag activation: Enables/disables UI features before test execution using an API.
* ♻️ Cache clearing support after feature changes.
* 📄 Human-readable test flow format (`dashboard-flow.txt`) with support for:

    * Setting expected and given values
    * Triggering buttons
    * Enabling/disabling features
* ✅ Dependency-injected modular architecture using Spring Boot.

## Project Structure

* `SmartUIRunner` – Kicks off the test from the text file.
* `PageFlowParser` – Parses the test flow file.
* `SmartLocatorEngine` – Locates and interacts with form fields.
* `FeatureManagerClient` – Enables features and clears cache via APIs.
* `WebDriverConfig` – Configures Selenium WebDriver.

## Example Test Flow

```text
Host: https://www.google.com

Features:
 - DUMMY_FEATURE:
    enable: false
    on:
       province: N/A
       store: N/A

Page 1
expected:
 - Search: ""
given:
 - Search: chatgpt
action: Google Search
```

## Getting Started

1. Clone the repo.
2. Place your test flow in `src/main/resources/dashboard-flow.txt`.
3. Run the application using:

   ```bash
   mvn spring-boot:run
   ```

## Requirements

* Java 11+
* Maven
* ChromeDriver or another compatible Selenium WebDriver
* Internet access for UI pages
