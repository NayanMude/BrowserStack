package assignment;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.*;
import java.net.URL;
import java.time.Duration;
import java.util.*;

public class ElPaisScraper {
    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        try {
            driver.get("https://elpais.com/");
            Thread.sleep(5000);
            driver.findElement(By.xpath("//span[text()='Accept']")).click();
    		Thread.sleep(2000);

            // Accept cookie consent if shown
            // Validate if content is in Spanish
            String pageText = driver.findElement(By.tagName("body")).getText();
            if (pageText.contains("Suscríbete") || pageText.contains("España") || pageText.contains("Internacional")) {
                System.out.println("The content appears to be in Spanish.\n");
            } else {
                System.out.println("Spanish content not detected.\n");
            }

            // Navigate to 'Opinion' section
            try {
                WebElement opinionLink = wait.until(ExpectedConditions.elementToBeClickable(By.partialLinkText("Opinión")));
                opinionLink.click();
                Thread.sleep(3000); // Let the page load
            } catch (Exception e) {
                System.out.println("Failed to navigate to Opinion section.");
                driver.quit();
                return;
            }

            // Get first 5 articles
            List<WebElement> articles = driver.findElements(By.cssSelector("article a"));
            Set<String> articleLinks = new LinkedHashSet<>();
            for (WebElement article : articles) {
                String link = article.getAttribute("href");
                if (link != null && link.contains("/opinion/")) {
                    articleLinks.add(link);
                }
                if (articleLinks.size() == 5) break;
            }

            int count = 1;
            for (String link : articleLinks) {
                driver.get(link);
                Thread.sleep(2000); // Let article load

                String title = "";
                String content = "";

                try {
                    WebElement titleElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("h1")));
                    title = titleElement.getText();
                } catch (Exception e) {
                    title = "(No Title Found)";
                }

                try {
                    List<WebElement> paragraphs = driver.findElements(By.cssSelector("article p"));
                    StringBuilder contentBuilder = new StringBuilder();
                    for (WebElement para : paragraphs) {
                        contentBuilder.append(para.getText()).append("\n");
                    }
                    content = contentBuilder.toString().trim();
                } catch (Exception e) {
                    content = "(No Content Found)";
                }

                System.out.println("\n--- Article " + count + " ---");
                System.out.println("Title: " + title);
                System.out.println("Content:\n" + (content.isEmpty() ? "(Empty)" : content));

                // Download cover image
                try {
                    WebElement imgElement = driver.findElement(By.cssSelector("article img"));
                    String imgUrl = imgElement.getAttribute("src");
                    if (imgUrl != null && !imgUrl.isEmpty()) {
                        downloadImage(imgUrl, "cover_image_" + count + ".jpg");
                        System.out.println("Cover image downloaded.\n");
                    }
                } catch (Exception e) {
                    System.out.println("No cover image found.");
                }

                count++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private static void downloadImage(String imageUrl, String fileName) {
        try (InputStream in = new URL(imageUrl).openStream();
             FileOutputStream out = new FileOutputStream(fileName)) {
            byte[] buffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            System.out.println("Failed to download image: " + imageUrl);
        }
    }
}