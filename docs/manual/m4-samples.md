# M4: handwriting samples

All 40 numbered pages (5 per language, `docs/plan.md`, task 1) were delivered on 2026-11-28 and checked: each has its full text, on the requested paper. Since 2026-11-28 they are fixtures, with `he-school`, an extra page beside the numbered set. How a page gets in, its rules, consent and every difference from the asked text: `engine/src/test/resources/fixtures/handwriting/README.md`.

| Languages | Made by | Still open |
|---|---|---|
| English, Russian, Hebrew | owner, handwritten; the owner consents to publishing them as test fixtures (2026-11-28) | nothing (verified by the owner, 2026-11-28) |
| German, French, Spanish, Chinese, Japanese | Gemini in "Auto" mode, exact submodel unknown (owner, 2026-11-28) | nothing (proofread by an AI agent) |

## Pages as requested

| Page | Writing | Paper |
|---|---|---|
| 01 | print-like | lined |
| 02 | print-like | plain |
| 03 | cursive | lined |
| 04 | cursive | plain |
| 05 | cursive, quick and untidy | lined |

The handwritten images are crops around the text, not whole pages; the owner accepts them as they are (2026-11-28). The texts they were copied from are outside the repository. An AI agent can draft their expected text (an AI draft of `he-school` had five differences).

## Verification of the handwritten pages

An AI agent drafted the 15 texts from the images; the owner, who wrote the pages, verified them (2026-11-28). The places where the draft departed from the asked text, or was least sure:

| Page | In the draft | Asked | Owner's result | Date |
|---|---|---|---|---|
| `en-01` | "if you have one" | "if you own one" | confirmed | 2026-11-28 |
| `en-01` | "seeds of young plants" | "seeds or young plants" | confirmed | 2026-11-28 |
| `ru-01` | "семена и рассаду" | "семена или рассаду" | confirmed | 2026-11-28 |
| `ru-02` | "Часы работы:" | no colon | confirmed | 2026-11-28 |
| `ru-05` | "резко вырос" (hard to read) | the same | confirmed | 2026-11-28 |
| `ru-05` | "две больших бочки" | "две большие бочки" | confirmed | 2026-11-28 |
| `he-01` | final kaf ending the second word | vav: הביאו (havi'u) | wrong: a vav that looks like a final kaf; text corrected | 2026-11-28 |
| `he-02` | colon after the heading | no colon | confirmed | 2026-11-28 |
| `he-03` | no full stop after the three rules | full stops | confirmed | 2026-11-28 |
| `he-05` | חבילות (havilot) | חביות (haviot) | confirmed: the writer's mistake | 2026-11-28 |

The owner also decided (2026-11-28) that either letter case is right on `ru-02`, as on `ru-01`: both are in block capitals.

## AI-drawn pages

Drawn with Gemini in "Auto" mode; the originals are outside the repository, and their metadata says "Created by Google Generative AI". The prompt, with the language, paper and writing of the page filled in:

> A photograph, taken straight from above, of one sheet of [lined notebook paper | plain white paper] with [neat print-like handwriting | cursive handwriting | quick, untidy handwriting] in [blue ballpoint pen | black pen | pencil], in [language]. The sheet shows exactly the following text, line by line, and nothing else:

Paper and writing per page: 01 print-like on lined, 02 print-like on plain, 03 cursive on lined, 04 cursive on plain, 05 quick and untidy on lined. For Chinese and Japanese, "cursive" means a running hand. Save each image as `de-01.png` and so on, outside the repository, and note which model drew it. A drawing that differs a little from the text is fine (the expected text follows the image); draw again if it is unreadable or lines are missing.

### German

```
[de-01]
Einkaufsliste für Samstag:
zwei Liter Milch, Brötchen,
Äpfel, Käse und Möhren.
Bitte auch Spülmittel mitbringen!

[de-02]
Liebe Nachbarn,
am Freitag feiern wir im Hof.
Es kann etwas lauter werden.
Kommt gern auf ein Glas vorbei.

[de-03]
Heute hat es den ganzen Tag geregnet.
Nach dem Mittagessen haben wir
Brettspiele gespielt und Tee getrunken.
Morgen soll endlich die Sonne scheinen.

[de-04]
Termin beim Zahnarzt: 14. März, 9:30 Uhr.
Versichertenkarte nicht vergessen.
Danach zur Bücherei und die
drei überfälligen Bücher zurückgeben.

[de-05]
Rezept für Pfannkuchen:
250 g Mehl, 3 Eier, 1/2 l Milch,
eine Prise Salz. Alles gut verrühren,
dann in heißer Butter goldgelb backen.
```

### French

```
[fr-01]
Liste des courses :
du pain, du lait, des œufs,
six pommes et un peu de fromage.
N'oublie pas le café !

[fr-02]
Chers voisins,
nous fêtons samedi soir dans la cour.
Il y aura peut-être un peu de bruit.
Venez boire un verre avec nous.

[fr-03]
Il a plu toute la journée.
Après le déjeuner, nous avons joué
aux échecs et préparé un gâteau.
Demain, le soleil devrait revenir.

[fr-04]
Rendez-vous chez le dentiste le 14 mars
à 9 h 30. Apporter la carte vitale.
Ensuite, passer à la bibliothèque
pour rendre les trois livres en retard.

[fr-05]
Recette de crêpes :
250 g de farine, 3 œufs, un demi-litre
de lait et une pincée de sel. Bien mélanger,
puis cuire dans une poêle beurrée.
```

### Spanish

```
[es-01]
Lista de la compra:
pan, leche, huevos,
seis manzanas y un poco de queso.
¡No olvides el café!

[es-02]
Queridos vecinos:
el sábado haremos una fiesta en el patio.
Quizá haya un poco de ruido.
¿Os apetece pasar a tomar algo?

[es-03]
Ha llovido durante todo el día.
Después de comer jugamos al ajedrez
y preparamos un pastel de limón.
Mañana debería volver el sol.

[es-04]
Cita con el dentista: 14 de marzo, 9:30.
Llevar la tarjeta sanitaria.
Después, pasar por la biblioteca
y devolver los tres libros atrasados.

[es-05]
Receta de tortitas:
250 g de harina, 3 huevos, medio litro
de leche y una pizca de sal. Mezclar bien
y cocinar en una sartén con mantequilla.
```

### Chinese

```
[zh-01]
购物清单：
牛奶两瓶、鸡蛋一盒、
苹果六个，还有面包。
别忘了买茶叶！

[zh-02]
各位邻居：
星期六晚上我们在院子里聚会，
可能会有点吵，请多包涵。
欢迎大家过来喝杯茶。

[zh-03]
今天下了一整天的雨。
吃过午饭，我们在家下象棋，
还一起包了饺子。
听说明天会出太阳。

[zh-04]
三月十四日上午九点半看牙医，
记得带上医保卡。
然后去图书馆，
把过期的三本书还了。

[zh-05]
西红柿炒鸡蛋：
先把三个鸡蛋打散，炒熟盛出。
再炒两个西红柿，加一点盐和糖，
最后倒入鸡蛋，翻炒均匀。
```

### Japanese

```
[ja-01]
買い物リスト：
牛乳を二本、卵を一パック、
りんごを六個とパン。
お茶も忘れずに！

[ja-02]
ご近所の皆様へ
土曜日の夜、中庭で集まりを開きます。
少し騒がしくなるかもしれません。
どうぞお気軽にお立ち寄りください。

[ja-03]
今日は一日中雨でした。
昼ご飯のあと、家で将棋をして、
それからケーキを焼きました。
明日は晴れるそうです。

[ja-04]
三月十四日の午前九時半に歯医者。
保険証を持って行くこと。
そのあと図書館に寄って、
期限を過ぎた本を三冊返す。

[ja-05]
卵焼きの作り方：
卵を三つ溶いて、砂糖と塩を少し入れる。
フライパンに油をひき、
弱火で何度かに分けて巻く。
```
