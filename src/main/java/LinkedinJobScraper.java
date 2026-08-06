import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class LinkedinJobScraper {
    
    static class Config {
        static String username = "<linkedin username>";
        static String password = "<linkedin password>";
        static String[] searchUrls = {
            // United Kingdom
            "https://www.linkedin.com/jobs/search-results/?keywords=Test%20automation%2C%20API%20testing%2C%20Selenium%20Playwright&geoId=101165590&f_TPR=r86400&f_SAL=f_SA_id_225001%3A272001%24f_SA_id_226001%3A274001",
            // Germany
            "https://www.linkedin.com/jobs/search-results/?keywords=Test%20automation%2C%20API%20testing%2C%20Selenium%20Playwright&geoId=101282230&f_TPR=r86400&f_SAL=f_SA_id_225001%3A272001%24f_SA_id_226001%3A274001",
            // Netherlands
            "https://www.linkedin.com/jobs/search-results/?keywords=Test%20automation%2C%20API%20testing%2C%20Selenium%20Playwright&geoId=102890719&f_TPR=r86400&f_SAL=f_SA_id_225001%3A272001%24f_SA_id_226001%3A274001",
            // Ireland
            "https://www.linkedin.com/jobs/search-results/?keywords=Test%20automation%2C%20API%20testing%2C%20Selenium%20Playwright&geoId=104738515&f_TPR=r86400&f_SAL=f_SA_id_225001%3A272001%24f_SA_id_226001%3A274001",
            // Poland
            "https://www.linkedin.com/jobs/search-results/?keywords=Test%20automation%2C%20API%20testing%2C%20Selenium%20Playwright&geoId=105072130&f_TPR=r86400&f_SAL=f_SA_id_225001%3A272001%24f_SA_id_226001%3A274001",
            // Sweden
            "https://www.linkedin.com/jobs/search-results/?keywords=Test%20automation%2C%20API%20testing%2C%20Selenium%20Playwright&geoId=105117694&f_TPR=r86400&f_SAL=f_SA_id_225001%3A272001%24f_SA_id_226001%3A274001",
            // Romania
            "https://www.linkedin.com/jobs/search-results/?keywords=Test%20automation%2C%20API%20testing%2C%20Selenium%20Playwright&geoId=106670623&f_TPR=r86400&f_SAL=f_SA_id_225001%3A272001%24f_SA_id_226001%3A274001",
            // Czech Republic
            "https://www.linkedin.com/jobs/search-results/?keywords=Test%20automation%2C%20API%20testing%2C%20Selenium%20Playwright&geoId=104508036&f_TPR=r86400&f_SAL=f_SA_id_225001%3A272001%24f_SA_id_226001%3A274001",
            // Portugal
            "https://www.linkedin.com/jobs/search-results/?keywords=Test%20automation%2C%20API%20testing%2C%20Selenium%20Playwright&geoId=100364837&f_TPR=r86400&f_SAL=f_SA_id_225001%3A272001%24f_SA_id_226001%3A274001",
            // Spain
            "https://www.linkedin.com/jobs/search-results/?keywords=Test%20automation%2C%20API%20testing%2C%20Selenium%20Playwright&geoId=105646813&f_TPR=r86400&f_SAL=f_SA_id_225001%3A272001%24f_SA_id_226001%3A274001",
            // Australia
            "https://www.linkedin.com/jobs/search-results/?keywords=Test%20automation%2C%20API%20testing%2C%20Selenium%20Playwright&geoId=101452733&f_TPR=r86400&f_SAL=f_SA_id_225001%3A272001%24f_SA_id_226001%3A274001",
            // New Zealand
            "https://www.linkedin.com/jobs/search-results/?keywords=Test%20automation%2C%20API%20testing%2C%20Selenium%20Playwright&geoId=105490917&f_TPR=r86400&f_SAL=f_SA_id_225001%3A272001%24f_SA_id_226001%3A274001",
            // EU
            "https://www.linkedin.com/jobs/search-results/?keywords=Test%20automation%2C%20API%20testing%2C%20Selenium%20Playwright&geoId=91000000&f_TPR=r86400&f_SAL=f_SA_id_225001%3A272001%24f_SA_id_226001%3A274001",
            // Canada
            "https://www.linkedin.com/jobs/search-results/?keywords=Test%20automation%2C%20API%20testing%2C%20Selenium%20Playwright&geoId=101174742&f_TPR=r86400&f_SAL=f_SA_id_225001%3A272001%24f_SA_id_226001%3A274001",
            // USA
            "https://www.linkedin.com/jobs/search-results/?keywords=Test%20automation%2C%20API%20testing%2C%20Selenium%20Playwright&geoId=103644278&f_TPR=r86400&f_SAL=f_SA_id_225001%3A272001%24f_SA_id_226001%3A274001"
        };
        static boolean headless = false;
        static int delayBetweenPages = 3000;
    }

    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(Config.headless)
                .setSlowMo(50));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .setViewportSize(1440, 900));
            Page page = context.newPage();

            try {
                login(page);

                for (int u = 0; u < Config.searchUrls.length; u++) {
                    String currentUrl = Config.searchUrls[u];
                    log("\n=======================================================");
                    log("Loading search URL " + (u + 1) + " of " + Config.searchUrls.length + "...");
                    log("URL: " + currentUrl);
                    log("=======================================================");
                    
                    page.navigate(currentUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(30000));
                    sleep(5000);

                    int pageNum = 1;
                    while (true) {
                        log("\n─── Currently on Page " + pageNum + " (URL " + (u + 1) + ") ───");

                        // Scroll to bottom to ensure pagination controls are loaded in the DOM
                        try {
                            Locator jobList = page.locator(".jobs-search-results-list, .scaffold-layout__list").first();
                            if (jobList.count() > 0) {
                                jobList.evaluate("node => node.scrollTo(0, node.scrollHeight)");
                                sleep(2000);
                            } else {
                                page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
                                sleep(2000);
                            }
                        } catch (Exception e) {}

                        // Print job count
                        Locator jobCards = page.locator("//div[@role='button' and contains(@componentkey, 'job-card-component')]");
                        int jobCount = jobCards.count();
                        log("Found " + jobCount + " job(s) on this page.");

                        // Check and click each job card
                        for (int i = 0; i < jobCount; i++) {
                            try {
                                Locator card = jobCards.nth(i);
                                card.scrollIntoViewIfNeeded();
                                
                                Locator hourlySpanHr = card.locator("//span[contains(normalize-space(.), '/hr')]");
                                Locator hourlySpanHour = card.locator("//span[contains(normalize-space(.), '/hour')]");
                                boolean hasHourlyPricing = hourlySpanHr.count() > 0 || hourlySpanHour.count() > 0;
                                
                                // Always click the card to load the details pane because we need to check for Remote & Contract
                                card.click(new Locator.ClickOptions().setTimeout(2000));
                                sleep(2000); // Wait for the right pane to load
                                
                                boolean shouldSave = hasHourlyPricing;
                                
                                if (shouldSave) {
                                    log("Job " + (i + 1) + " has hourly pricing on card.");
                                } else {
                                    // Check details pane for hourly pricing
                                    Locator detailsHourlyHr = page.locator("//span[@data-testid='expandable-text-box']//li[contains(normalize-space(.), '/hr')]");
                                    Locator detailsHourlyHour = page.locator("//span[@data-testid='expandable-text-box']//li[contains(normalize-space(.), '/hour')]");
                                    
                                    if (detailsHourlyHr.count() > 0 || detailsHourlyHour.count() > 0) {
                                        shouldSave = true;
                                        log("Job " + (i + 1) + " has hourly pricing in details.");
                                    }
                                }
                                
                                // If still not marked to save, check for Remote AND Contract
                                if (!shouldSave) {
                                    Locator remoteBtn = page.locator("//span[text()='Save']/preceding::span[text()='Remote']");
                                    Locator contractBtn = page.locator("//span[text()='Save']/preceding::span[text()='Contract']");
                                    
                                    if (remoteBtn.count() > 0 && contractBtn.count() > 0) {
                                        shouldSave = true;
                                        log("Job " + (i + 1) + " is both Remote and Contract.");
                                    }
                                }
                                
                                // Explicitly exclude On-site jobs even if they match criteria
                                if (shouldSave) {
                                    Locator onSite1 = page.locator("//span[text()='Save']/preceding::span[text()='On-site']");
                                    Locator onSite2 = page.locator("//span[text()='Save']/preceding::span[text()='On-Site']");
                                    Locator onSite3 = page.locator("//span[text()='Save']/preceding::span[text()='On site']");
                                    Locator onSite4 = page.locator("//span[text()='Save']/preceding::span[text()='On Site']");
                                    
                                    if (onSite1.count() > 0 || onSite2.count() > 0 || onSite3.count() > 0 || onSite4.count() > 0) {
                                        shouldSave = false;
                                        log("Job " + (i + 1) + " explicitly skipped (marked as On-site).");
                                    }
                                }
                                
                                if (shouldSave) {
                                    Locator saveBtn = page.locator("//span[normalize-space()='Save']");
                                    if (saveBtn.count() > 0 && saveBtn.first().isVisible()) {
                                        saveBtn.first().click();
                                        log("  ✅ Saved!");
                                        sleep(1000);
                                    } else {
                                        log("  ⚠️ Save button not found or already saved.");
                                    }
                                } else {
                                    log("Job " + (i + 1) + " skipped (didn't meet criteria).");
                                }
                            } catch (Exception e) {
                                log("Error processing job card " + (i + 1) + ": " + e.getMessage());
                            }
                        }

                        Locator nextBtn = page.locator("//button[@data-testid='pagination-controls-next-button-visible']");

                        if (nextBtn.count() > 0 && nextBtn.first().isVisible()) {
                            log("Found Next button. Navigating to page " + (pageNum + 1) + "...");
                            nextBtn.first().click();
                            pageNum++;
                            sleep(Config.delayBetweenPages);
                        } else {
                            log("Next button is not present. Reached the last page for this URL!");
                            break;
                        }
                    }
                }
            } catch (Exception err) {
                log("ERROR: " + err.getMessage());
                err.printStackTrace();
            } finally {
                log("\n═══════════════════════════════════════");
                log("Script finished.");
                log("═══════════════════════════════════════");

                try {
                    log("Logging out before closing browser...");
                    page.navigate("https://www.linkedin.com/m/logout", new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(10000));
                    sleep(2000);
                } catch (Exception e) {
                    log("Failed to logout cleanly.");
                }
            }
        }
    }

    private static void login(Page page) {
        log("Navigating to LinkedIn login...");
        page.navigate("https://www.linkedin.com/login", new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Email or phone")).fill(Config.username);
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Password")).fill(Config.password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in").setExact(true)).click();
        
        log("Waiting for login to complete...");
        try {
            page.waitForURL("**/feed/**", new Page.WaitForURLOptions().setTimeout(15000));
        } catch (PlaywrightException err) {
            String currentUrl = page.url();
            if (currentUrl.contains("/login") || currentUrl.contains("/checkpoint") || currentUrl.contains("/challenge")) {
                log("⚠️ Automated login was blocked or requires manual verification.");
                log("⚠️ Please complete the login/captcha manually in the browser window...");
                try {
                    page.waitForURL("**/feed/**", new Page.WaitForURLOptions().setTimeout(120000));
                } catch (PlaywrightException e) {
                    // Manual login failed or timed out
                }
            }
        }
        sleep(3000);
        log("Logged in successfully");
    }

    private static void log(String msg) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a"));
        System.out.println("[" + ts + "] " + msg);
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
