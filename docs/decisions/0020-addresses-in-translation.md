# 0020: E-mail addresses and web links in translation

Date: 2026-11-25. Status: proposed (M3 task 11; the owner accepts by merging the pull request). Follows 0015 (translator).

- **Problem.** The translation models rewrite addresses. Measured with the 13 directions of the app on four addresses and one bare one: `archive@example.org` came out as `архив@ample.org`, `archive@ejemplo.org` or `913 (E) 030503`, links lost letters or vanished. From Russian, Japanese and Hebrew sources no or almost no sentence kept its address. A wrong address looks right, so the user does not notice. Repairing the output is not possible: often nothing address-like is left to repair.

## Decision

1. **The model never sees an address** (`AddressShield`, used by `DirectionTranslator`, so at every step of a route). An address is an e-mail address or a link that begins with `http://`, `https://` or `www.`, in ASCII. Bare domains (`example.org`) are not recognised: too close to ordinary text with a missing space.
2. **A stand-in takes its place**, `Z19`, `Z29` and so on (a number the sentence does not contain already). Of twelve forms tried in four sentences per direction, this one came out exactly once in 50 of 52 cases; the next best (`(1)`, `www.x1.com`) in 44, `№1` in none.
3. **Fallback.** When the output does not hold every stand-in exactly once, the text between the addresses is translated piece by piece and the addresses are put between the pieces. The punctuation that ties a piece to an address stays as it is. A sentence with no letters outside its addresses is not translated.
4. **Not chosen.** Always cutting around the address: the guarantee without a stand-in, but every such sentence would be translated in fragments, and a fragment invites invented text (the phone number of hand-test finding 3).

## Result

With the shield all 65 probe sentences kept their address exactly (13 directions, 5 sentences each). The wording around an address is still the model's: it may place it oddly (Chinese to English wrote "room archive@example.org ff 12"). The fallback translates fragments out of context.
