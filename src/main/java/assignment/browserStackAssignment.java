package assignment;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import javax.imageio.ImageIO;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.MutableCapabilities;

public class browserStackAssignment {

    public static final String USERNAME = "nayan_Nq5EOu";
    public static final String ACCESS_KEY = "Ha8yXiqvszwzGnYfxdNT";
    public static final String HUB_URL = "https://" + USERNAME + ":" + ACCESS_KEY + "@hub-cloud.browserstack.com/wd/hub";
    
    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(5);

        List<Map<String, String>> browserConfigs = List.of(
            Map.of("device", "", "os", "Windows", "osVersion", "10", "browserName", "Chrome", "browserVersion", "latest"),
            Map.of("device", "", "os", "OS X", "osVersion", "Monterey", "browserName", "Safari", "browserVersion", "latest"),
            Map.of("device", "", "os", "Windows", "osVersion", "11", "browserName", "Edge", "browserVersion", "latest"),
            Map.of("device", "iPhone 13", "realMobile", "true", "osVersion", "15", "browserName", "iPhone", "mobile", "true"),
            Map.of("device", "Samsung Galaxy S22", "realMobile", "true", "osVersion", "12.0", "browserName", "Android", "mobile", "true")
        );

        for (int i = 0; i < browserConfigs.size(); i++) {
            Map<String, String> config = browserConfigs.get(i);
            int threadNum = i + 1;
            executor.execute(() -> runTest(config, threadNum));
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.MINUTES);
    }

    public static void runTest(Map<String, String> config, int threadNum) {
        WebDriver driver = null;

        try {
            MutableCapabilities caps = new MutableCapabilities();
            MutableCapabilities bstackOptions = new MutableCapabilities();

            if (config.containsKey("mobile")) {
                bstackOptions.setCapability("deviceName", config.get("device"));
                bstackOptions.setCapability("osVersion", config.get("osVersion"));
                bstackOptions.setCapability("realMobile", "true");
                caps.setCapability("browserName", config.get("browserName"));
            } else {
                caps.setCapability("browserName", config.get("browserName"));
                caps.setCapability("browserVersion", config.get("browserVersion"));
                bstackOptions.setCapability("os", config.get("os"));
                bstackOptions.setCapability("osVersion", config.get("osVersion"));
            }

            bstackOptions.setCapability("buildName", "Java Assignment Parallel");
            bstackOptions.setCapability("sessionName", "Thread " + threadNum + " - " + config.get("browserName"));
            caps.setCapability("bstack:options", bstackOptions);

            driver = new RemoteWebDriver(new URL(HUB_URL), caps);
            runArticleTranslationTest(driver, threadNum);

        } catch (Exception e) {
            System.err.println("Thread " + threadNum + " failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) driver.quit();
        }
    }

    public static void runArticleTranslationTest(WebDriver driver, int threadNum) throws Exception {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));
        driver.get("https://elpais.com/");
        try {
            WebElement alert = driver.findElement(By.xpath("(//button[contains(@class,'didomi-components-button didomi-button')]//span)[2]"));
            alert.click();
        } catch (Exception ignored) {}

        String lang = driver.findElement(By.tagName("html")).getAttribute("lang");
        System.out.println("Thread " + threadNum + ": Detected Language: " + lang);

        Thread.sleep(2000);
        driver.get("https://elpais.com/opinion/");

        List<WebElement> articles = driver.findElements(By.xpath("//div[@class='z z-hi']//article"));
        int count = Math.min(articles.size(), 5);
        ArrayList<String> translatedTitles = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            List<WebElement> refreshedArticles = driver.findElements(By.xpath("//div[@class='z z-hi']//article"));
            WebElement article = refreshedArticles.get(i);
            WebElement titleElement = article.findElement(By.tagName("h2"));
            String title = titleElement.getText();
            System.out.println("Thread " + threadNum + " - Article " + (i + 1) + ": " + title);

            String translated = translate(title);
            System.out.println("Thread " + threadNum + " - Translated Title: " + translated);
            translatedTitles.add(translated);

            titleElement.click();
            Thread.sleep(2000);

            String content = driver.findElement(By.xpath("//article[@id='main-content']/div[2]")).getText();
            System.out.println("Thread " + threadNum + " - Content for Article " + (i + 1) + ":\n" + content);

            List<WebElement> images = driver.findElements(By.xpath("//article[@id='main-content']//img"));
            int imageIndex = 1;
            for (WebElement img : images) {
                String src = img.getAttribute("src");
                if (src != null && !src.isEmpty()) {
                    try {
                        URL imageURL = new URL(src);
                        BufferedImage image = ImageIO.read(imageURL);
                        if (image != null) {
                            String fileName = "thread" + threadNum + "_article" + (i + 1) + "_img" + imageIndex + ".jpg";
                            ImageIO.write(image, "jpg", new File(fileName));
                            System.out.println("Thread " + threadNum + ": Downloaded image " + fileName);
                            imageIndex++;
                        }
                    } catch (Exception ignored) {}
                }
            }

            driver.navigate().back();
            Thread.sleep(2000);
        }
    }

    public static String translate(String title) throws Exception {
        String apiKey = "AIzaSyBe4thcCEORr8wRsbI4RjASrQqDlit2Yhk"; // Google Translate API key
        String urlStr = "https://translation.googleapis.com/language/translate/v2?key=" + apiKey +
                "&q=" + URLEncoder.encode(title, "UTF-8") +
                "&source=es&target=en";
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JSONObject jsonResponse = new JSONObject(response.toString());
        String translatedText = jsonResponse.getJSONObject("data")
                                            .getJSONArray("translations")
                                            .getJSONObject(0)
                                            .getString("translatedText");

        return StringEscapeUtils.unescapeHtml4(translatedText);
    }

}
