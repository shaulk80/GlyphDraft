# M3 task 6: Hebrew read by a native speaker

Date of the outputs: 2026-11-23, `FixtureTranslationTest` (30 sentences, written by Claude, not checked by a native speaker; the Hebrew side of the reference set is Claude's translation too).

How to fill in: for each sentence put OK, small error (still understood) or wrong meaning after `Verdict:`. At the end, answer the three questions below. The result decides whether the Hebrew directions keep the "experimental" mark (decision 0016).

Display note: Hebrew is right to left, so a line may show with its punctuation on the wrong side in a preview; that is a display matter, not a finding.

Questions at the end:

1. Is Hebrew to English good enough to rely on for the meaning of a document?
2. Is English to Hebrew (two models) and Russian to Hebrew readable, and are the grammar (gender, plural, verb forms) and the spelling acceptable?
3. Is anything in the output offensive or invented?

## Result (owner, 2026-11-23)

66 of 90 outputs plain OK, the rest OK with a more precise reference or a stylistic fault; one wrong meaning (en to he, sentence 30); times written as "9: 00" with a space, once with the wrong hour ("2: 30" for half past three). Answers: 1) hobby translation yes, sensitive data no; 2) acceptable for the current needs; 3) nothing offensive, nothing invented out of the blue, meaning shifts slightly now and then.

## Hebrew to English

[1] הפגישה נדחתה ליום חמישי בשלוש וחצי.  
Output:    The meeting was postponed to Thursday at 3:30.  
Reference: The meeting has been moved to Thursday at half past three.  
Verdict: OK

[2] אנא שמרו את המסמך הזה במקום בטוח.  
Output:    Please keep this document in a safe place.  
Reference: Please keep this document in a safe place.  
Verdict: OK

[3] הרכבת לברלין יוצאת מרציף ארבע.  
Output:    The train to Berlin departs from Platform Four.  
Reference: The train to Berlin leaves from platform four.  
Verdict: OK

[4] הייתי רוצה להזמין שני קפה ופרוסת עוגה.  
Output:    I'd like to order two coffees and a slice of cake.  
Reference: I would like to order two coffees and a piece of cake.  
Verdict: OK

[5] המוזיאון סגור בימי שני, אבל הגן פתוח בכל יום.  
Output:    The museum is closed on Mondays, but the garden is open every day.  
Reference: The museum is closed on Mondays, but the garden is open every day.  
Verdict: OK

[6] לטלפון החדש שלנו יש מצלמה טובה יותר וסוללה שמחזיקה מעמד זמן רב יותר.  
Output:    Our new phone has a better camera and a battery that lasts longer.  
Reference: Our new phone has a better camera and a longer battery life.  
Verdict: OK

[7] אם ירד גשם מחר, נישאר בבית ונקרא.  
Output:    If it rains tomorrow, we'll stay home and read.  
Reference: If it rains tomorrow, we will stay at home and read.  
Verdict: OK

[8] הרופא אמר לו לשתות יותר מים ולישון שמונה שעות.  
Output:    The doctor told him to drink more water and sleep for eight hours.  
Reference: The doctor told him to drink more water and sleep eight hours.  
Verdict: OK

[9] המוצר אינו מתאים לילדים מתחת לגיל שלוש.  
Output:    The product is not suitable for children under the age of three.  
Reference: This product is not suitable for children under three years.  
Verdict: OK

[10] איפה בית המרקחת הקרוב ביותר?  
Output:    Where is the nearest pharmacy?  
Reference: Where is the nearest pharmacy?  
Verdict: OK

[11] החברה דיווחה על רווח קטן ברבעון השלישי.  
Output:    The company reported a small profit in the third quarter.  
Reference: The company reported a small profit in the third quarter.  
Verdict: OK

[12] היא גרה בעיר הזאת יותר מעשרים שנה.  
Output:    She's lived in this town for over 20 years.  
Reference: She has lived in this city for more than twenty years.  
Verdict: OK

[13] אנא כבו את הטלפון הנייד לפני שהסרט מתחיל.  
Output:    Please turn off the cell phone before the movie starts.  
Reference: Please turn off your mobile phone before the film starts.  
Verdict: OK

[14] יש לשלם את החשבונית תוך ארבעה עשר ימים.  
Output:    The invoice must be paid within fourteen days.  
Reference: The invoice must be paid within fourteen days.  
Verdict: OK

[15] אנחנו מחפשים דירה שקטה קרוב לתחנה.  
Output:    We're looking for a quiet apartment near the station.  
Reference: We are looking for a quiet apartment close to the station.  
Verdict: OK

[16] הילדים משחקים בפארק שמאחורי בית הספר.  
Output:    The kids are playing in the park behind the school.  
Reference: The children are playing in the park behind the school.  
Verdict: OK

[17] במקרה של שריפה אין להשתמש במעלית.  
Output:    In case of fire, the elevator should not be used.  
Reference: Do not use the lift in case of fire.  
Verdict: OK ("do not use the lift" is more precise translation, but the meaning is preserved anyway)

[18] הוא שכח את המטרייה שלו באוטובוס והתרטב.  
Output:    He forgot his umbrella on the bus and got wet.  
Reference: He forgot his umbrella on the bus and got wet.  
Verdict: OK

[19] הספרייה החדשה תפתח את שעריה באביב הבא.  
Output:    The new library will open next spring.  
Reference: The new library will open its doors next spring.  
Verdict: OK ("its doors" is missing, but still clear)

[20] תודה רבה על העזרה, אנחנו באמת מעריכים זאת.  
Output:    Thank you so much for your help, we really appreciate it.  
Reference: Thank you very much for your help, we really appreciate it.  
Verdict: OK

[21] המרק צריך עוד מעט מלח וקצת עשבי תיבול טריים.  
Output:    The soup needs some salt and some fresh herbs.  
Reference: The soup needs a little more salt and some fresh herbs.  
Verdict: OK (reference more precise)

[22] על הנוסעים להציג את הכרטיסים שלהם לפקח.  
Output:    Passengers must display their tickets to monitor.  
Reference: Passengers must show their tickets to the inspector.  
Verdict: Both "display" and "monitor" are wrong, meaning not preserved

[23] הדוח מסביר כיצד נמדדו התוצאות.  
Output:    The report explains how the results were measured.  
Reference: The report explains how the results were measured.  
Verdict: OK

[24] סבתא שלי מספרת סיפורים נפלאים על ילדותה בהרים.  
Output:    My grandmother tells wonderful stories about her childhood in the mountains.  
Reference: My grandmother tells wonderful stories about her childhood in the mountains.  
Verdict: OK

[25] החנות נפתחת בתשע ונסגרת בשש.  
Output:    The store opens at 9:00 and closes at 6:00.  
Reference: The shop opens at nine o'clock and closes at six.  
Verdict: OK

[26] אנא שלחו לי את הגרסה הסופית של החוזה עד יום שישי.  
Output:    Please send me the final version of the contract by Friday.  
Reference: Please send me the final version of the contract by Friday.  
Verdict: OK

[27] זה היה מסע ארוך, אבל הנוף מהפסגה היה שווה את זה.  
Output:    It was a long trip, but the view from the summit was worth it.  
Reference: It was a long journey, but the view from the top was worth it.  
Verdict: OK

[28] שטפו את הידיים בסבון לפני הכנת אוכל.  
Output:    Wash your hands with soap before preparing food.  
Reference: Wash your hands with soap before you prepare food.  
Verdict: OK

[29] הממשלה הודיעה על כללים חדשים לכלי רכב חשמליים.  
Output:    The government has announced new rules for electric vehicles.  
Reference: The government has announced new rules for electric cars.  
Verdict: OK

[30] אני מצטער, אבל אני לא מבין למה אתה מתכוון.  
Output:    I'm sorry, but I don't know what you mean.  
Reference: I am sorry, but I do not understand what you mean.  
Verdict: OK (reference is better)

## Russian to Hebrew

[1] Встреча перенесена на четверг, на половину четвёртого.  
Output:    הפגישה נקבעה ליום חמישי, ברבע הרביעי.  
Reference: הפגישה נדחתה ליום חמישי בשלוש וחצי.  
Verdict: Ok-ish, "נדחתה" is more precise

[2] Пожалуйста, храните этот документ в надёжном месте.  
Output:    אנא שמרו על המסמך הזה במקום בטוח.  
Reference: אנא שמרו את המסמך הזה במקום בטוח.  
Verdict: OK

[3] Поезд до Берлина отправляется с четвёртой платформы.  
Output:    הרכבת לברלין יוצאת מרציף 4.  
Reference: הרכבת לברלין יוצאת מרציף ארבע.  
Verdict: OK

[4] Я хотел бы заказать два кофе и кусок торта.  
Output:    אני רוצה להזמין שתי כוסות קפה וחתיכת עוגה.  
Reference: הייתי רוצה להזמין שני קפה ופרוסת עוגה.  
Verdict: OK (the difference are synonyms)

[5] Музей закрыт по понедельникам, но сад открыт каждый день.  
Output:    המוזיאון סגור בימי שני, אבל הגינה פתוחה כל יום.  
Reference: המוזיאון סגור בימי שני, אבל הגן פתוח בכל יום.  
Verdict: OK (the difference are synonyms)

[6] У нашего нового телефона камера лучше, а аккумулятор работает дольше.  
Output:    הטלפון החדש שלנו יש מצלמה טובה יותר, והסוללה שלנו עובדת יותר.  
Reference: לטלפון החדש שלנו יש מצלמה טובה יותר וסוללה שמחזיקה מעמד זמן רב יותר.  
Verdict: It's understandable, but stylistically wrong

[7] Если завтра пойдёт дождь, мы останемся дома и будем читать.  
Output:    אם ירד גשם מחר, נישאר בבית ונקרא.  
Reference: אם ירד גשם מחר, נישאר בבית ונקרא.  
Verdict: OK

[8] Врач сказал ему пить больше воды и спать восемь часов.  
Output:    הרופא אמר לו לשתות יותר מים ולישון שמונה שעות.  
Reference: הרופא אמר לו לשתות יותר מים ולישון שמונה שעות.  
Verdict: OK

[9] Этот продукт не подходит для детей младше трёх лет.  
Output:    זה לא מוצר מתאים לילדים מתחת לגיל שלוש.  
Reference: המוצר אינו מתאים לילדים מתחת לגיל שלוש.  
Verdict: OK

[10] Где ближайшая аптека?  
Output:    איפה בית המרקחת הקרוב ביותר?  
Reference: איפה בית המרקחת הקרוב ביותר?  
Verdict: OK

[11] В третьем квартале компания получила небольшую прибыль.  
Output:    ברבעון השלישי, החברה הרוויחה קצת כסף.  
Reference: החברה דיווחה על רווח קטן ברבעון השלישי.  
Verdict: OK

[12] Она живёт в этом городе уже больше двадцати лет.  
Output:    היא גרה בעיר הזאת יותר מעשרים שנה.  
Reference: היא גרה בעיר הזאת יותר מעשרים שנה.  
Verdict: OK

[13] Пожалуйста, выключите мобильный телефон до начала фильма.  
Output:    בבקשה, כבה את הטלפון הנייד לפני שהסרט מתחיל.  
Reference: אנא כבו את הטלפון הנייד לפני שהסרט מתחיל.  
Verdict: OK

[14] Счёт необходимо оплатить в течение четырнадцати дней.  
Output:    החשבון הזה צריך להיות משולם ב-14 ימים.  
Reference: יש לשלם את החשבונית תוך ארבעה עשר ימים.  
Verdict: OK-ish, reference is better

[15] Мы ищем тихую квартиру недалеко от вокзала.  
Output:    אנחנו מחפשים דירה שקטה ליד תחנת הרכבת.  
Reference: אנחנו מחפשים דירה שקטה קרוב לתחנה.  
Verdict: OK

[16] Дети играют в парке за школой.  
Output:    הילדים משחקים בפארק מאחורי בית הספר.  
Reference: הילדים משחקים בפארק שמאחורי בית הספר.  
Verdict: OK

[17] В случае пожара не пользуйтесь лифтом.  
Output:    אל תשתמשו במעלית במקרה של שריפה.  
Reference: במקרה של שריפה אין להשתמש במעלית.  
Verdict: OK

[18] Он забыл зонт в автобусе и промок.  
Output:    הוא שכח את המטריה באוטובוס ונרטב.  
Reference: הוא שכח את המטרייה שלו באוטובוס והתרטב.  
Verdict: OK

[19] Новая библиотека откроет свои двери следующей весной.  
Output:    הספרייה החדשה תפתח את הדלתות באביב הבא.  
Reference: הספרייה החדשה תפתח את שעריה באביב הבא.  
Verdict: OK

[20] Большое спасибо за вашу помощь, мы очень ценим это.  
Output:    תודה רבה על עזרתך, אנחנו מעריכים את זה.  
Reference: תודה רבה על העזרה, אנחנו באמת מעריכים זאת.  
Verdict: OK

[21] В суп нужно добавить ещё немного соли и свежей зелени.  
Output:    המרק צריך עוד מלח וירוק טרי.  
Reference: המרק צריך עוד מעט מלח וקצת עשבי תיבול טריים.  
Verdict: OK-ish, ref is better

[22] Пассажиры должны предъявлять билеты контролёру.  
Output:    הנוסעים חייבים להציג כרטיסים למפקח.  
Reference: על הנוסעים להציג את הכרטיסים שלהם לפקח.  
Verdict: OK

[23] В отчёте объясняется, как измерялись результаты.  
Output:    הדו"ח מסביר איך המדידות היו.  
Reference: הדוח מסביר כיצד נמדדו התוצאות.  
Verdict: Understandable, but not precise

[24] Моя бабушка рассказывает чудесные истории о своём детстве в горах.  
Output:    סבתא שלי מספרת סיפורים נפלאים על ילדותה בהרים.  
Reference: סבתא שלי מספרת סיפורים נפלאים על ילדותה בהרים.  
Verdict: OK

[25] Магазин открывается в девять часов и закрывается в шесть.  
Output:    החנות נפתחת ב-9: 00 וסגר בשש.  
Reference: החנות נפתחת בתשע ונסגרת בשש.  
Verdict: Understandable, but "וסגר" is wrong, also, extra space within 9:00 causes wrong reading

[26] Пожалуйста, пришлите мне окончательный вариант договора до пятницы.  
Output:    בבקשה, שלח לי את הגרסה הסופית של ההסכם עד יום שישי.  
Reference: אנא שלחו לי את הגרסה הסופית של החוזה עד יום שישי.  
Verdict: OK

[27] Путь был долгим, но вид с вершины того стоил.  
Output:    זה היה מסע ארוך, אבל הנוף מהפסגה היה שווה את זה.  
Reference: זה היה מסע ארוך, אבל הנוף מהפסגה היה שווה את זה.  
Verdict: OK

[28] Перед приготовлением пищи мойте руки с мылом.  
Output:    לפני שתכין אוכל, שטוף את ידיך בסבון.  
Reference: שטפו את הידיים בסבון לפני הכנת אוכל.  
Verdict: OK

[29] Правительство объявило новые правила для электромобилей.  
Output:    הממשלה קבעה חוקים חדשים לרכבים חשמליים.  
Reference: הממשלה הודיעה על כללים חדשים לכלי רכב חשמליים.  
Verdict: "קבעה" is less precise than "הודיעה" in ref; other than that, OK 

[30] Извините, но я не понимаю, что вы имеете в виду.  
Output:    אני מצטער, אבל אני לא מבין למה אתה מתכוון.  
Reference: אני מצטער, אבל אני לא מבין למה אתה מתכוון.  
Verdict: OK

## English to Hebrew (through Russian)

[1] The meeting has been moved to Thursday at half past three.  
Output:    הישיבה נדחתה ליום חמישי בשעה 2: 30.  
Reference: הפגישה נדחתה ליום חמישי בשלוש וחצי.  
Verdict: extra space in time makes wrong reading, otherwise OK

[2] Please keep this document in a safe place.  
Output:    אנא שמור את המסמך הזה במקום בטוח.  
Reference: אנא שמרו את המסמך הזה במקום בטוח.  
Verdict: OK

[3] The train to Berlin leaves from platform four.  
Output:    הרכבת לברלין יוצאת מרציף 4.  
Reference: הרכבת לברלין יוצאת מרציף ארבע.  
Verdict: OK

[4] I would like to order two coffees and a piece of cake.  
Output:    אני רוצה להזמין שתי כוסות קפה וחתיכת עוגה.  
Reference: הייתי רוצה להזמין שני קפה ופרוסת עוגה.  
Verdict: Ok

[5] The museum is closed on Mondays, but the garden is open every day.  
Output:    המוזיאון סגור בימי שני, אבל הגינה פתוחה כל יום.  
Reference: המוזיאון סגור בימי שני, אבל הגן פתוח בכל יום.  
Verdict: OK

[6] Our new phone has a better camera and a longer battery life.  
Output:    לטלפון החדש שלנו יש מצלמה טובה יותר וזמן ארוך יותר של סוללה.  
Reference: לטלפון החדש שלנו יש מצלמה טובה יותר וסוללה שמחזיקה מעמד זמן רב יותר.  
Verdict: OK

[7] If it rains tomorrow, we will stay at home and read.  
Output:    אם ירד גשם מחר, נישאר בבית ונקריא.  
Reference: אם ירד גשם מחר, נישאר בבית ונקרא.  
Verdict: OK

[8] The doctor told him to drink more water and sleep eight hours.  
Output:    הרופא אמר לו לשתות יותר מים ולישון שמונה שעות.  
Reference: הרופא אמר לו לשתות יותר מים ולישון שמונה שעות.  
Verdict: OK

[9] This product is not suitable for children under three years.  
Output:    זה לא מוצר מתאים לילדים מתחת לגיל שלוש.  
Reference: המוצר אינו מתאים לילדים מתחת לגיל שלוש.  
Verdict: OK

[10] Where is the nearest pharmacy?  
Output:    איפה בית המרקחת הקרוב ביותר?  
Reference: איפה בית המרקחת הקרוב ביותר?  
Verdict: OK

[11] The company reported a small profit in the third quarter.  
Output:    החברה דיווחה על רווח קטן ברבעון השלישי.  
Reference: החברה דיווחה על רווח קטן ברבעון השלישי.  
Verdict: OK

[12] She has lived in this city for more than twenty years.  
Output:    היא גרה בעיר הזאת יותר מעשרים שנה.  
Reference: היא גרה בעיר הזאת יותר מעשרים שנה.  
Verdict: OK

[13] Please turn off your mobile phone before the film starts.  
Output:    בבקשה, כבה את הטלפון הנייד לפני שהסרט מתחיל.  
Reference: אנא כבו את הטלפון הנייד לפני שהסרט מתחיל.  
Verdict: OK

[14] The invoice must be paid within fourteen days.  
Output:    החשבון חייב להיות משולם ב-14 ימים.  
Reference: יש לשלם את החשבונית תוך ארבעה עשר ימים.  
Verdict: OK, ref is slightly better

[15] We are looking for a quiet apartment close to the station.  
Output:    אנחנו מחפשים דירה שקטה ליד התחנה.  
Reference: אנחנו מחפשים דירה שקטה קרוב לתחנה.  
Verdict: OK

[16] The children are playing in the park behind the school.  
Output:    הילדים משחקים בפארק מאחורי בית הספר.  
Reference: הילדים משחקים בפארק שמאחורי בית הספר.  
Verdict: OK

[17] Do not use the lift in case of fire.  
Output:    אל תשתמשו במעלית במקרה של שריפה.  
Reference: במקרה של שריפה אין להשתמש במעלית.  
Verdict: OK

[18] He forgot his umbrella on the bus and got wet.  
Output:    הוא השאיר מטריה באוטובוס ונרטב.  
Reference: הוא שכח את המטרייה שלו באוטובוס והתרטב.  
Verdict: OK (ref is more precise)

[19] The new library will open its doors next spring.  
Output:    הספרייה החדשה תפתח את הדלתות באביב הבא.  
Reference: הספרייה החדשה תפתח את שעריה באביב הבא.  
Verdict: OK

[20] Thank you very much for your help, we really appreciate it.  
Output:    תודה רבה על עזרתך, אנחנו מעריכים את זה.  
Reference: תודה רבה על העזרה, אנחנו באמת מעריכים זאת.  
Verdict: OK

[21] The soup needs a little more salt and some fresh herbs.  
Output:    המרק צריך קצת יותר מלח ועשבים טריים.  
Reference: המרק צריך עוד מעט מלח וקצת עשבי תיבול טריים.  
Verdict: OK

[22] Passengers must show their tickets to the inspector.  
Output:    הנוסעים צריכים להראות למפקח את הכרטיסים שלהם.  
Reference: על הנוסעים להציג את הכרטיסים שלהם לפקח.  
Verdict: OK

[23] The report explains how the results were measured.  
Output:    הדו"ח מסביר איך המדידות של התוצאות.  
Reference: הדוח מסביר כיצד נמדדו התוצאות.  
Verdict: Understandable, but stylistically wrong

[24] My grandmother tells wonderful stories about her childhood in the mountains.  
Output:    סבתא שלי מספרת סיפורים נפלאים על ילדותה בהרים.  
Reference: סבתא שלי מספרת סיפורים נפלאים על ילדותה בהרים.  
Verdict: OK

[25] The shop opens at nine o'clock and closes at six.  
Output:    החנות נפתחת ב-9: 00 וסגר בשש.  
Reference: החנות נפתחת בתשע ונסגרת בשש.  
Verdict: "וסגר" wrong, also extra space in time causes wrong reading

[26] Please send me the final version of the contract by Friday.  
Output:    בבקשה תשלח לי את החוזה הסופי עד יום שישי.  
Reference: אנא שלחו לי את הגרסה הסופית של החוזה עד יום שישי.  
Verdict: OK, but ref is more precise

[27] It was a long journey, but the view from the top was worth it.  
Output:    זה היה מסע ארוך, אבל המראה מלמעלה היה שווה את זה.  
Reference: זה היה מסע ארוך, אבל הנוף מהפסגה היה שווה את זה.  
Verdict: OK

[28] Wash your hands with soap before you prepare food.  
Output:    תשטוף את הידיים שלך במים לפני שתבשל.  
Reference: שטפו את הידיים בסבון לפני הכנת אוכל.  
Verdict: "במים" - wrong, soap is missing here, otherwise understandable

[29] The government has announced new rules for electric cars.  
Output:    הממשלה קבעה חוקים חדשים לרכבים חשמליים.  
Reference: הממשלה הודיעה על כללים חדשים לכלי רכב חשמליים.  
Verdict: "קבעה" is less precise than "הודיעה" in ref; other than that, OK 

[30] I am sorry, but I do not understand what you mean.  
Output:    אני מצטער, אבל אני לא יודע על מה אתה מדבר.  
Reference: אני מצטער, אבל אני לא מבין למה אתה מתכוון.  
Verdict: Wrong, sense is lost here - the output sounds like "Sorry, I don't know what you are talking about"

## Answers

1. Is Hebrew to English good enough to rely on for the meaning of a document?
for hobby translation (our current needs) - yes; for sensitive data - no
2. Is English to Hebrew (two models) and Russian to Hebrew readable, and are the grammar (gender, plural, verb forms) and the spelling acceptable?
acceptable for our current needs
3. Is anything in the output offensive or invented?  
offensive - no; invented - kinda, not like "invented out of the blue", but more like "shifted sense slightly"
