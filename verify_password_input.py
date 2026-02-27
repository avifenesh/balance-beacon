import asyncio
from playwright.async_api import async_playwright

async def verify_password_input():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page()

        try:
            # Navigate to the login page (or any page where the password input is used)
            # Assuming the app is running on localhost:3000
            await page.goto("http://localhost:3000/login")

            # Wait for the password input to be visible
            await page.wait_for_selector('input[type="password"]')

            # Find the password input field
            password_input = page.locator('input[type="password"]')

            # Type a password
            await password_input.fill("mysecretpassword")

            # Find the toggle button (it should be an icon button inside the input wrapper)
            # The button has aria-label="Show password" initially
            toggle_button = page.locator('button[aria-label="Show password"]')

            # Take a screenshot before clicking (password should be hidden)
            await page.screenshot(path="password_hidden.png")
            print("Screenshot taken: password_hidden.png")

            # Click the toggle button
            await toggle_button.click()

            # Wait for a brief moment for state update
            await page.wait_for_timeout(500)

            # Verify the input type changed to "text"
            is_text = await page.locator('input[type="text"][value="mysecretpassword"]').is_visible()
            print(f"Password visible: {is_text}")

            # Take a screenshot after clicking (password should be visible)
            await page.screenshot(path="password_visible.png")
            print("Screenshot taken: password_visible.png")

            # Click the toggle button again to hide
            await page.locator('button[aria-label="Hide password"]').click()

             # Wait for a brief moment for state update
            await page.wait_for_timeout(500)

            # Verify the input type changed back to "password"
            is_password = await page.locator('input[type="password"][value="mysecretpassword"]').is_visible()
            print(f"Password hidden again: {is_password}")

             # Take a screenshot after clicking (password should be hidden again)
            await page.screenshot(path="password_hidden_again.png")
            print("Screenshot taken: password_hidden_again.png")

        except Exception as e:
            print(f"Error: {e}")
        finally:
            await browser.close()

if __name__ == "__main__":
    asyncio.run(verify_password_input())
