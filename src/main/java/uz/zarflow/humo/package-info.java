/**
 * Humo balance-message reconciliation (docs/PRD.md §7): parses balance messages
 * forwarded from @HUMOcardbot (or pasted), compares each card's bank balance against
 * the linked account's ZarFlow balance as of that time, and offers a correction.
 */
package uz.zarflow.humo;