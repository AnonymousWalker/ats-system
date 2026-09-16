import { expect, test } from '@playwright/test'

test('sample resume/JD journey reaches a scored result', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('heading', { name: /Resume–JD/i })).toBeVisible()

  await page.getByRole('button', { name: 'Use sample pair' }).click()
  await page.getByRole('button', { name: 'Extract skills' }).click()

  await expect(page).toHaveURL(/\/analyses\/[0-9a-f-]+/i, { timeout: 30_000 })
  await expect(page.getByRole('heading', { name: 'Correct extracted skills' })).toBeVisible()

  await expect(page.getByRole('heading', { name: 'Resume skills' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Job description skills' })).toBeVisible()

  await page.getByRole('button', { name: 'Calculate score' }).click()

  await expect(page.getByRole('heading', { name: 'Results' })).toBeVisible({ timeout: 15_000 })
  await expect(page.locator('.score-value')).toBeVisible()
  await expect(page.getByText(/Match score/i)).toBeVisible()
  await expect(page.getByText(/score = round/i)).toBeVisible()
})
