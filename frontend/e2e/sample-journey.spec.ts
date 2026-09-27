import { expect, test } from '@playwright/test'

test('sample resume/JD journey reaches a scored result', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('heading', { name: /Score a resume/i })).toBeVisible()

  await page.getByRole('button', { name: 'Use sample' }).click()
  await page.getByRole('button', { name: 'Run scoring' }).click()

  await expect(page.getByRole('img', { name: /Score \d+/ })).toBeVisible({ timeout: 30_000 })
  await expect(page.getByText(/score = round/i)).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Job skills' })).toBeVisible()

  await page.getByRole('button', { name: /^AWS/ }).click()
  await page.getByRole('button', { name: 'Rescore' }).click()
  await expect(page.getByRole('button', { name: 'Rescore' })).toBeHidden({ timeout: 15_000 })
})
