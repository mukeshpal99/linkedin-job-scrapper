import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class LinkedinJobScraper {
    
    static class Config {
        static String username = "<linkedin username>";
        static String password = "<linkedin password>";
        static String googleSheetUrl = "https://docs.google.com/spreadsheets/d/1mdbdiHK7wDyZtb402Ug3Vc6rUTyDfD6zJMGKm2zWE-c/edit?gid=9286295#gid=9286295";
        static boolean headless = false;
        static int delayBetweenPages = 3000;
    }

    static class SearchQuery {
        String country;
        String role;
        String url;

        SearchQuery(String country, String role, String url) {
            this.country = country;
            this.role = role;
            this.url = url;
        }
    }

    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setChannel("chrome")
                .setHeadless(Config.headless)
                .setSlowMo(50));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .setViewportSize(1440, 900));
            Page page = context.newPage();

            try {
                login(page);

                List<SearchQuery> searchQueries = fetchQueriesFromGoogleSheet(Config.googleSheetUrl);
                if (searchQueries.isEmpty()) {
                    log("⚠️ No queries loaded from Google Sheet. Exiting.");
                    return;
                }

                for (int u = 0; u < searchQueries.size(); u++) {
                    SearchQuery query = searchQueries.get(u);
                    String currentUrl = query.url;
                    String currentCountry = query.country;
                    String currentRole = query.role;
                    log("\n=======================================================");
                    log("Loading search URL " + (u + 1) + " of " + searchQueries.size() + "...");
                    log("URL: " + currentUrl + " (Country: " + currentCountry + ", Role: " + currentRole + ")");
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
                                
                                // Always click the card to load the details pane because we need to check for Remote & Contract
                                card.click(new Locator.ClickOptions().setTimeout(3000));
                                sleep(2000); // Wait for the right pane to load
                                
                                boolean shouldSave = false;

                                Locator remoteBtn = page.locator("//span[text()='Save']/preceding::span[text()='Remote']");
                                Locator contractBtn = page.locator("//span[text()='Save']/preceding::span[text()='Contract']");
                                Locator temporaryBtn = page.locator("//span[text()='Save']/preceding::span[text()='Temporary']");
                                
                                boolean isRemote = remoteBtn.count() > 0;
                                boolean isContractOrTemp = contractBtn.count() > 0 || temporaryBtn.count() > 0;

                                boolean isIndiaLeadership = currentCountry != null && currentRole != null && currentCountry.equalsIgnoreCase("India") && currentRole.equalsIgnoreCase("Leadership");

                                if (currentCountry != null && currentRole != null && currentCountry.equalsIgnoreCase("India") && currentRole.equalsIgnoreCase("Engineer")) {
                                    if (isRemote) {
                                        shouldSave = true;
                                        log("Job " + (i + 1) + " is Remote (India Engineer criteria).");
                                    }
                                } else if (isIndiaLeadership) {
                                    Locator managerLocator = card.locator("//span[contains(text(),'Manager')]");
                                    Locator directorLocator = card.locator("//span[contains(text(),'Director')]");
                                    if (managerLocator.count() > 0 || directorLocator.count() > 0) {
                                        shouldSave = true;
                                        log("Job " + (i + 1) + " is Manager or Director (India Leadership criteria).");
                                    }
                                } else {
                                    if (isRemote && isContractOrTemp) {
                                        shouldSave = true;
                                        log("Job " + (i + 1) + " is both Remote and Contract or Temporary.");
                                    }
                                }
                                
                                
                                // Explicitly exclude On-site jobs even if they match criteria
                                if (shouldSave && !isIndiaLeadership) {
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

    private static List<SearchQuery> fetchQueriesFromGoogleSheet(String sheetUrl) {
        List<SearchQuery> queries = new ArrayList<>();
        try {
            String exportUrl = sheetUrl;
            if (sheetUrl.contains("/edit")) {
                exportUrl = sheetUrl.substring(0, sheetUrl.indexOf("/edit")) + "/export?format=csv";
                if (sheetUrl.contains("gid=")) {
                    String gid = sheetUrl.substring(sheetUrl.indexOf("gid="));
                    if (gid.contains("#")) {
                        gid = gid.substring(0, gid.indexOf("#"));
                    }
                    exportUrl += "&" + gid;
                }
            }

            log("Fetching data from Google Sheet export URL: " + exportUrl);
            URL url = new URL(exportUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                int linkColIndex = 2; // default to 3rd column
                int countryColIndex = 0; // default to 1st column
                int roleColIndex = 1; // default to 2nd column
                boolean isHeader = true;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                    
                    String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    if (isHeader) {
                        isHeader = false;
                        for (int i = 0; i < parts.length; i++) {
                            String headerCell = parts[i].trim();
                            if (headerCell.startsWith("\"") && headerCell.endsWith("\"")) {
                                headerCell = headerCell.substring(1, headerCell.length() - 1).trim();
                            }
                            if (headerCell.equalsIgnoreCase("Filter Link") || headerCell.equalsIgnoreCase("Recruiters Page Link") || headerCell.equalsIgnoreCase("Page Posts Link")) {
                                linkColIndex = i;
                            } else if (headerCell.equalsIgnoreCase("Country")) {
                                countryColIndex = i;
                            } else if (headerCell.equalsIgnoreCase("Role")) {
                                roleColIndex = i;
                            }
                        }
                        continue;
                    }
                    
                    if (parts.length > linkColIndex) {
                        String cell = parts[linkColIndex].trim();
                        if (cell.startsWith("\"") && cell.endsWith("\"")) {
                            cell = cell.substring(1, cell.length() - 1).trim();
                        }
                        if (cell.startsWith("http://") || cell.startsWith("https://")) {
                            String country = parts.length > countryColIndex ? parts[countryColIndex].trim() : "";
                            String role = parts.length > roleColIndex ? parts[roleColIndex].trim() : "";
                            
                            if (country.startsWith("\"") && country.endsWith("\"")) { country = country.substring(1, country.length() - 1).trim(); }
                            if (role.startsWith("\"") && role.endsWith("\"")) { role = role.substring(1, role.length() - 1).trim(); }
                            
                            queries.add(new SearchQuery(country, role, cell));
                        }
                    }
                }
            }
            log("Successfully loaded " + queries.size() + " query(s) from Google Sheet.");
        } catch (Exception e) {
            log("Error fetching data from Google Sheet: " + e.getMessage());
            e.printStackTrace();
        }
        return queries;
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
