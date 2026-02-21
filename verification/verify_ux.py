from playwright.sync_api import Page, expect, sync_playwright
import time

def test_ux_improvement(page: Page):
    print("Navigating to login page...")
    page.goto("http://localhost:3000/login")

    # Login
    print("Logging in...")
    page.fill("input[name='email']", "user1@example.com") # Trying default user from seed? Or create one?
    # The fixtures say e2e-user1@test.local
    page.fill("input[name='email']", "e2e-user1@test.local")
    page.fill("input[name='password']", "TestPassword123!")
    page.click("button[type='submit']")

    # Wait for dashboard
    print("Waiting for dashboard...")
    try:
        page.wait_for_url("**/", timeout=10000)
    except:
        print("Login failed or took too long. Checking for error message...")
        if page.is_visible("text=Invalid email or password"):
            print("Invalid credentials. Trying to register instead...")
            page.goto("http://localhost:3000/register")
            page.fill("input[name='email']", "newuser@test.local")
            page.fill("input[name='password']", "TestPassword123!")
            page.fill("input[name='confirmPassword']", "TestPassword123!")
            page.click("button[type='submit']")
            page.wait_for_url("**/onboarding", timeout=10000)
            # Skip onboarding if possible
            if page.is_visible("button:has-text('Skip')"):
                page.click("button:has-text('Skip')")
            page.wait_for_url("**/", timeout=10000)
        else:
            print("Unknown error. Taking screenshot.")
            page.screenshot(path="verification/login_error.png")
            raise

    print("Logged in successfully.")

    # Navigate to Transactions tab
    print("Navigating to Transactions tab...")
    page.click("button:has-text('Transactions')")

    # Check if transaction exists, if not create one
    print("Checking for transactions...")
    time.sleep(2) # Wait for transactions to load

    if not page.is_visible("button[aria-label='Share expense']"):
        print("No transaction found. Creating one...")
        # Fill transaction form
        page.select_option("select[name='categoryId']", index=1) # Select first available category
        page.fill("input[name='amount']", "50.00")
        page.fill("input[name='date']", "2024-05-22")
        page.fill("textarea[name='description']", "Test Transaction")
        page.click("button:has-text('Save transaction')")
        print("Transaction created.")
        time.sleep(2)

    # Verify Share button accessibility
    print("Verifying Share button accessibility...")
    share_button = page.locator("button[aria-label='Share expense']").first
    expect(share_button).to_be_visible()
    print("Share button has correct aria-label.")

    # Open Share Modal
    print("Opening Share modal...")
    share_button.click()

    # Verify Modal Accessibility
    print("Verifying Modal accessibility...")
    modal = page.locator("div[role='dialog']")
    expect(modal).to_be_visible()
    expect(modal).to_have_attribute("aria-modal", "true")
    expect(modal).to_have_attribute("aria-labelledby", "share-expense-title")
    print("Modal attributes correct.")

    # Verify Close button
    close_button = page.locator("button[aria-label='Close']")
    expect(close_button).to_be_visible()
    print("Close button visible and accessible.")

    # Verify Add Participant input
    add_participant_input = page.locator("input[aria-label='Participant email address']")
    expect(add_participant_input).to_be_visible()
    print("Add participant input accessible.")

    # Take screenshot
    print("Taking screenshot...")
    page.screenshot(path="verification/verification.png")
    print("Screenshot saved to verification/verification.png")

if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        try:
            test_ux_improvement(page)
        except Exception as e:
            print(f"Error: {e}")
            page.screenshot(path="verification/error.png")
        finally:
            browser.close()
