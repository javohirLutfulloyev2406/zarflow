# PRD — ZarFlow

ZarFlow — Biznes va mahsulot hujjati
Nov 5, 2026 · @Javohir
0. AI uchun ko'rsatma
   Ushbu hujjat ZarFlow loyihasining yagona haqiqat manbai (single source of truth). Uni AI assistentga kontekst sifatida berib, keyingi ishni (kod, dizayn, test, hujjat) shu asosda davom ettirish mumkin.
   AI roli. Sen ZarFlow loyihasida Product Owner, Backend Architect (Java 21 / Spring Boot) va Telegram Mini App frontend dasturchisi rolini bajarasan. Loyiha egasi Java/Spring Boot, PostgreSQL, Docker tajribasiga ega backend dasturchi.
   Qat'iy qoidalar (buzilmaydi):
1. Pul summalari faqat BigDecimal (Java) va NUMERIC(19,2) (PostgreSQL). double/float taqiqlangan.
2. Ichki transfer (hisoblar orasida, jamg'armaga) daromad ham, xarajat ham emas.
3. Kredit to'lovining asosiy qarz qismi iste'mol xarajati emas; foiz va komissiya moliyaviy xarajat.
4. Har bir so'rovda user ownership tekshiriladi; user_id faqat tekshirilgan Telegram initData dan olinadi, hech qachon request body dan emas.
5. Tranzaksiyalar izsiz o'chirilmaydi: soft-delete va audit.
6. Vaqt zonasi default Asia/Tashkent; "kun" chegarasi foydalanuvchi vaqt zonasida hisoblanadi.
7. Ilova foydalanuvchini ayblamaydi va uyaltirmaydi; xabarlar neytral va yordam beruvchi ohangda.
8. Joriy bosqichdan tashqaridagi funksiyani (8-bo'lim) so'ralmasa qo'shma.
   Ish tartibi. Yangi vazifada avval 8-bo'limdan joriy bosqichni aniqla, so'ng 4-bo'limdagi tegishli modulning qabul mezonlariga mos ishla. Hujjatga zid qaror kerak bo'lsa, uni 9-bo'limdagi qarorlar jurnaliga qo'shishni taklif qil.
   Til. UI matnlari: o'zbek (lotin) asosiy, rus tili ikkinchi. Kod, identifikatorlar va commit xabarlari ingliz tilida. Har qanday UI, bot xabari yoki vizual qaror 1.1-bo'limdagi ZarFlow brend qoidalariga (ranglar, shriftlar, ovoz) mos bo'lishi shart; Java paket nomi uz.zarflow.
1. Mahsulot mohiyati
   ZarFlow — insonga moliyaviy holatini nazoratga olish, keraksiz xarajatlarni kamaytirish, jamg'arma shakllantirish va qarzlardan qutulishga yordam beradigan shaxsiy moliyaviy boshqaruv tizimi. Platforma: Telegram Bot + Telegram Mini App. Bozor: O'zbekiston, valyuta UZS.
   Shior: Pul oqimingiz — nazoratingizda. (EN: Take Control of Your Money.)
   Mahsulot javob beradigan uchta savol:
1. Hozir mening moliyaviy holatim qanday? (balans, pul oqimi, qarz, jamg'arma)
2. Pulim aynan qayerga sarflanyapti? (kategoriyalar, dinamika, zarur/ixtiyoriy)
3. Holatimni yaxshilash uchun bugun nima qilishim kerak? (bugungi limit, keyingi to'lov, maslahat)
   Uchinchi savol mahsulotning asosiy farqi. Oddiy expense tracker faqat o'tmishni ko'rsatadi; ZarFlow esa bugungi harakatni ko'rsatadi.
   Muammo. Ko'pchilik kundalik xarajatlarni qayd etmaydi, daromad va xarajat balansini bilmaydi, oylik byudjet tuzmaydi, kredit majburiyatlarini alohida kuzatmaydi. Natijada oy oxirida pul qayerga ketgani noma'lum, keraksiz xarajatlar kech aniqlanadi va kreditlar kutilganidan uzoq cho'ziladi.
   Nima uchun mavjud ilovalar yetarli emas. Ular xarajat kiritishni qiyin qiladi (alohida ilova, ko'p qadam), O'zbekiston kontekstiga (UZS, Humo/Uzcard, naqd pul ulushi, o'zbek tili) moslashmagan va kreditni oddiy xarajat kategoriyasi sifatida ko'radi.
   Mahsulot falsafasi. Ilova foydalanuvchini ayblamaydi, bosim o'tkazmaydi. U tushunishga, ongli qaror qabul qilishga va moliyaviy barqarorlikka erishishga yordam beradi. Yaxshi odatlar mukofotlanadi, xatolar jazolanmaydi.
   1.1. Brend identiteti — ZarFlow
   ZarFlow — premium FinTech brendi: bank darajasidagi ishonch, zamonaviy mahsulot qulayligi va o'zbekona moliyaviy qadriyatlar bir joyda. UI, bot xabarlari va marketing materiallari shu bo'limga bo'ysunadi.
   Nom. Zar — qiymat, boylik va farovonlik; Flow — pul oqimi va boshqaruv. ZarFlow — pulni shunchaki hisoblash emas, uni ongli boshqarish.
   Pozitsiya: shaxsiy moliyani boshqarish uchun zamonaviy, ishonchli va aqlli platforma.
   Brend va'dasi: foydalanuvchi pul qayerdan kelayotganini, qayerga ketayotganini va kelajak uchun qancha ajrata olishini tushunadi.
   Taglines: "Pul oqimingiz — nazoratingizda." (asosiy, uz) · "Take Control of Your Money." (en).
   Xususiyat
   Qanday namoyon bo'ladi
   Premium
   Nafis, ortiqcha bezaksiz
   Ishonchli
   Aniq raqamlar, xavfsizlik
   Aqlli
   Ma'lumotga asoslangan maslahat
   Zamonaviy
   Minimalist texnologik dizayn
   Samimiy
   Sodda, tushunarli kommunikatsiya
   Logo
   • Belgi: geometrik Z, bitta uzluksiz "oqim" chizig'idan. Z harfi + pul oqimi + muvozanat g'oyasi.
   • Variantlar: asosiy (belgi + ZARFLOW yozuvi), app icon (faqat Z, yumaloq kvadrat ichida, Emerald fon + Gold belgi), monoxrom (oq yoki qora).
   • Qoida: 40×40 px Telegram avatarida ham Z aniq o'qilishi kerak; belgi ichida mayda detal va gradient yo'q.
   Ranglar
   Token
   Nomi
   HEX
   Vazifasi
   navy-900
   Midnight Navy
   #0D192B
   Asosiy fon
   navy-800
   Navy Surface
   #15243A
   Kartalar, varaqlar
   emerald-700
   Deep Emerald
   #174D40
   Brend aksenti, hero kartalar, asosiy tugma
   gold-400
   Champagne Gold
   #D7B56D
   Premium urg'u: logo, "+" tugma, faol holat (faqat kichik maydonlar)
   ivory-50
   Soft Ivory
   #F4F1E8
   Asosiy matn (dark), yorug' yuzalar (light)
   slate-400
   Slate Gray
   #9AA7B8
   Ikkinchi darajali matn, metadata
   success-400
   Success Green
   #3CCB95
   Daromad, ijobiy balans, "limit ichida"
   warn-400
   Amber
   #E8A85A
   Limitga yaqinlashish (ayblovsiz ogohlantirish)
   Nisbat: 55% Navy va neytral, 25% yorug' yuzalar va matn, 15% Emerald, 5% Gold. Gold katta maydonda ishlatilmaydi. Daromad, xarajat va qarz faqat rang bilan emas, ishora (+/−), ikonka va yorliq bilan ham farqlanadi.
   Tipografiya
   Element
   Shrift
   Logo, H1
   Manrope Bold, 32–40 px
   H2
   Manrope Semibold, 24 px
   Dashboard raqamlari
   Manrope Bold, tabular raqamlar
   Body
   Inter Regular, 14–16 px
   Summalar ro'yxatda
   Inter Semibold, tabular raqamlar
   Labels
   Inter Medium, 12–13 px
   Summa formati: minglik ajratuvchi bo'shliq, valyuta oxirida — 8 500 000 so'm (UI), 8 500 000 UZS (eksport va hisobotlar). Qisqa ko'rinish: 8,5 mln.
   Brend ovozi
   Ton: professional, xotirjam, ishonchli, sodda. ZarFlow ayblamaydi, tushunishga yordam beradi.
   Noto'g'ri
   ZarFlow uslubi
   Siz juda ko'p pul sarfladingiz!
   Bu oy xarajatlar rejalashtirilgan byudjetdan oshdi.
   Pulni tejang!
   Jamg'arma maqsadingizga yana bir qadam yaqinlashdingiz.
   Qarzlaringiz juda ko'p.
   Keling, to'lovlarni reja asosida boshqaramiz.
   Budjetni buzib qo'ydingiz.
   Byudjet limitining 80% qismi ishlatildi.
   DO: minimalist logo, aniq tipografiya, Navy/Emerald/Gold muvozanati, yagona ikonka uslubi (1.8 px stroke, yumaloq uchlar), aniq summa formati.
   DON'T: ko'p rangli gradientlar, logoga mayda detallar, bir ekranda 2 tadan ortiq shrift, foydalanuvchini moliyaviy holati uchun ayblash.
2. Biznes maqsadlar va vazifalar
   Mahsulot ikki bosqichli maqsadga xizmat qiladi: avval bitta real foydalanuvchini moliyaviy barqarorlikka olib chiqish, so'ng isbotlangan yechimni ommaga chiqarish.
   Birinchi foydalanuvchi — loyiha egasi. U kundalik xarajatlarini nazorat qilish, tejash va kreditni muddatidan oldin yopish maqsadida ilovadan har kuni foydalanadi. Har bir funksiya "men buni har kuni ishlataymanmi?" savoli bilan tekshiriladi. Nazariy chiroyli, lekin ishlatilmaydigan funksiya qo'shilmaydi.
   Maqsadli auditoriya (keyingi bosqich):
   • Kredit yoki muddatli to'lov (rassrochka) majburiyatlari bor insonlar.
   • Oylik maosh bilan yashovchi, "oy oxirida pul qolmaydi" muammosiga duch kelganlar.
   • Pul tejash va jamg'arma yaratishni istovchilar.
   • Telegram'dan faol foydalanuvchi, alohida ilova o'rnatishni istamaydiganlar.
   Biznes maqsadlar va ularni bajaruvchi vazifalar
   Biznes maqsad
   Mahsulot vazifasi
   Modul
   Pul qayerga ketishini ko'rsatish
   Har bir kirim/chiqimni 5 soniyadan kam vaqtda qayd etish
   Tranzaksiyalar, Bot
   Joriy holatni tushuntirish
   Balans, pul oqimi, qarz va jamg'armani bitta ekranda ko'rsatish
   Dashboard
   Ortiqcha sarfning oldini olish
   Byudjet, kategoriya limitlari, bugungi limit
   Byudjet
   Keraksiz xarajatni aniqlash
   Kategoriya tahlili, zarur/ixtiyoriy ajratish, oyma-oy taqqoslash
   Analitika
   Kreditni tezroq yopish
   Qoldiq, to'lov jadvali, qo'shimcha to'lov simulyatsiyasi
   Kredit boshqaruvi
   Kutilmagan xarajatdan himoya
   Zaxira fondi va jamg'arma maqsadlari
   Jamg'arma
   Moliyaviy intizom
   Eslatmalar, kunlik hisobot, odat streak'lari
   Bildirishnomalar, Insights
   Muntazam foydalanish
   Bot orqali tez kiritish, kechki eslatma
   Bot
   Muvaffaqiyat mezonlari (birinchi foydalanuvchi uchun)
   Mezon
   Maqsad qiymat
   O'lchov usuli
   Qayd etish muntazamligi
   Oyning 90% kunida kamida 1 ta yozuv
   Yozuvli kunlar / jami kunlar
   Kiritish tezligi
   O'rtacha 5 soniyadan kam
   Bot xabaridan tasdiqgacha vaqt
   Byudjetga rioya
   Kategoriyalarning 80% i limit ichida
   Oy yakunida hisoblanadi
   Jamg'arma ulushi
   Daromadning kamida 10% i
   Jamg'armaga transferlar / daromad
   Qarz kamayishi
   Asosiy qarz har oy rejadan kam emas kamayadi
   debt_payments.principal yig'indisi
   Ixtiyoriy xarajat ulushi
   Oyma-oy kamayish tendensiyasi
   Ixtiyoriy / jami xarajat
   KPI'lar foydalanuvchining real holatini ko'rsatish uchun. Ularni sun'iy oshirish (masalan, majburlovchi bildirishnomalar orqali) mahsulot maqsadi emas.
   Ommaviy bosqich uchun mezonlar
   • 30 kunlik retention: ro'yxatdan o'tganlarning kamida 30% i.
   • Haftalik faol foydalanuvchilar ulushi o'sishi.
   • 3 oy foydalangan userlarda ixtiyoriy xarajat ulushining pasayishi.
3. Asosiy tushunchalar va moliyaviy qoidalar
   Barcha hisob-kitoblar quyidagi ta'riflarga tayanadi; kod va UI shu atamalarni bir xil ma'noda ishlatadi.
   Atama
   Ta'rif
   Misol
   Joriy balans
   Barcha faol hisoblardagi pul yig'indisi (bir lahza holati)
   Karta 2 mln + naqd 300 ming = 2,3 mln
   Sof pul oqimi
   Davrdagi daromad − davrdagi xarajat (transferlarsiz)
   Noyabr: 8 mln − 9 mln = −1 mln
   Iste'mol xarajati
   EXPENSE turidagi tranzaksiyalar, analitika va byudjetga kiradi
   Tushlik, taksi
   Ichki transfer
   Foydalanuvchining o'z hisoblari orasida pul ko'chishi
   Kartadan naqdga, jamg'armaga
   Kredit to'lovi
   Pul oqimida chiqim; analitikada asosiy qarz va foiz alohida
   2 mln = 1,6 mln asosiy + 0,4 mln foiz
   Majburiy xarajat
   Har oy takrorlanadigan, kamaytirib bo'lmaydigan to'lov
   Ijara, kommunal, kredit
   Ixtiyoriy xarajat
   Kamaytirish yoki voz kechish mumkin bo'lgan xarajat
   Kafe, ko'ngilochar, impuls xarid
   Erkin byudjet
   Oyda majburiyatlar va jamg'armadan keyin qoladigan pul
   Pastdagi formula
   Bugungi limit
   Bugun rejadan chiqmay sarflash mumkin bo'lgan summa
   Pastdagi formula
   Joriy balans va sof pul oqimi aralashtirilmaydi: kartada 2 mln bo'lishi mumkin, lekin oy minusda yopilayotgan bo'lishi mumkin. Dashboard ikkalasini alohida ko'rsatadi.
   Tranzaksiya turlari va hisobga ta'siri
   Tur
   Balansga
   Pul oqimiga
   Xarajat analitikasiga
   Byudjetga
   INCOME
+
Kirim
Yo'q
Yo'q
EXPENSE
−
Chiqim
Ha
Ha
TRANSFER
0 (bir hisobdan −, boshqasiga +)
Yo'q
Yo'q
Yo'q
DEBT_PAYMENT
−
Chiqim
Faqat foiz va komissiya
Majburiy to'lov sifatida
SAVINGS (transfer)
0
Yo'q
Yo'q
Rejalashtirilgan jamg'arma sifatida
Erkin byudjet va bugungi limit formulasi
Bu mahsulotning asosiy hisob-kitobi ("Safe-to-spend").
\text{ErkinByudjet} = \text{KutilganDaromad} - \text{MajburiyTo'lovlar} - \text{KreditTo'lovlari} - \text{RejaJamg'arma}
\text{KunlikRuxsat} = \frac{\text{ErkinByudjet} - \text{BugungachaSarflangan}}{\text{QolganKunlar (bugun bilan)}}
\text{BugungiLimit} = \text{KunlikRuxsat} - \text{BugunSarflangan}
Qoidalar:
• BugungachaSarflangan = oy boshidan kechagacha bo'lgan EXPENSE yig'indisi, majburiy to'lov sifatida belgilanganlardan tashqari.
• Kunlik ruxsat har kuni 00:00 da (Asia/Tashkent) qayta hisoblanadi, kun ichida o'zgarmaydi. Shunda foydalanuvchi bir kunlik aniq raqamga ega bo'ladi.
• Bugun kam sarflansa, ertangi ruxsat avtomatik oshadi; ko'p sarflansa, kamayadi.
• Bugungi limit manfiy bo'lsa, UI 0 va "limitdan X so'm oshdi" ko'rsatadi, ayblovchi ohangsiz.
• KutilganDaromad foydalanuvchi sozlamasidan olinadi; oy davomida haqiqiy daromad kutilgandan oshsa, haqiqiy qiymat ishlatiladi.
• Erkin byudjet manfiy bo'lsa, ilova buni alohida ogohlantiradi: majburiyatlar daromaddan katta.
Kredit to'lovini taqsimlash qoidasi
• To'lov summasi = asosiy qarz + foiz + komissiya.
• Foydalanuvchi bank jadvalidagi taqsimotni kiritadi; kiritilmasa, annuitet formulasi bo'yicha taxminiy taqsimot hisoblanadi va "taxminiy" deb belgilanadi.
• outstanding_principal faqat asosiy qarz qismiga kamayadi.
• Muddatidan oldin qo'shimcha to'lov to'liq asosiy qarzga yo'naltiriladi.
Zarur va ixtiyoriy xarajat
Har bir kategoriyada necessity belgisi bor: ESSENTIAL yoki DISCRETIONARY. Tranzaksiya default qiymatni kategoriyadan oladi, foydalanuvchi uni alohida o'zgartirishi mumkin (masalan, "Food" ichida bozor zarur, kafe ixtiyoriy).
4. Funksional modullar
   Mahsulot 10 ta moduldan iborat. Har bir modul: maqsadi, funksiyalari, biznes qoidalari, qabul mezonlari va qaysi bosqichda qurilishi.
#
Modul
Asosiy savol
Bosqich
M1
Dashboard
Hozir holatim qanday?
1
M2
Tranzaksiyalar
Nima kirdi, nima chiqdi?
1
M3
Hisoblar
Pul qayerda turibdi?
1
M4
Kategoriyalar
Pul nimaga ketdi?
1
M5
Analitika va hisobotlar
Qaysi xarajat ko'p, qaysi oshdi?
1–2
M6
Byudjet va bugungi limit
Bugun qancha sarflay olaman?
2
M7
Doimiy to'lovlar
Oldinda qanday majburiyatlar bor?
2
M8
Kredit boshqaruvi
Kreditni qachon yopaman?
3
M9
Jamg'arma maqsadlari
Kelajak uchun qancha yig'dim?
3
M10
Insights va odatlar
Nimani yaxshilashim mumkin?
2–4
M1. Dashboard
Maqsad: ilova ochilgan zahoti moliyaviy holatni 3 soniyada tushuntirish.
Ko'rsatiladi (yuqoridan pastga, muhimlik tartibida): bugungi limit (2-bosqichdan; 1-bosqichda bugungi xarajat), oy bo'yicha kirim / chiqim / sof pul oqimi, joriy balans (hisoblar bo'yicha yoyiladi), kredit qoldig'i va progress, jamg'arma, yaqin 7 kundagi to'lovlar, oxirgi 5 ta tranzaksiya.
Filtrlar: bugun, shu hafta, shu oy, ixtiyoriy oraliq. Default: shu oy.
Qabul mezonlari:
[ ] Ochilganda joriy oy ma'lumotlari 1 soniyadan kam vaqtda chiqadi.
[ ] Balans va sof pul oqimi alohida kartalarda.
[ ] Transferlar kirim/chiqimga qo'shilmaydi.
[ ] Ma'lumot bo'lmasa, bo'sh holat (empty state) birinchi xarajatni qo'shishga yo'naltiradi.
M2. Tranzaksiyalar
Maqsad: markaziy modul; har bir pul harakatini tez va aniq qayd etish.
Atributlar: id, user_id, account_id, category_id, type, amount, currency, necessity, transaction_date, description, source (BOT, MINI_APP, RECURRING), linked_debt_id, linked_goal_id, transfer_group_id, created_at, updated_at, deleted_at.
Funksiyalar: yaratish, tahrirlash, o'chirish (soft-delete, 5 soniya ichida "bekor qilish"), tarix (kunlar bo'yicha guruhlangan), filtrlar (sana, tur, kategoriya, hisob, summa oralig'i, matn qidiruv), takrorlash ("xuddi shunday yana bitta").
Qoidalar: summa > 0; tur summa ishorasi bilan emas, type bilan belgilanadi. Tahrirlash balans va statistikani qayta hisoblaydi. Kelajak sanasiga tranzaksiya kiritib bo'lmaydi (rejalashtirilganlar M7 da). Har bir o'zgarish audit jadvaliga yoziladi.
Qabul mezonlari:
[ ] Mini App'da xarajat qo'shish 3 ta bosishdan oshmaydi: summa → kategoriya → saqlash.
[ ] Oxirgi ishlatilgan 6 ta kategoriya birinchi ko'rsatiladi.
[ ] O'chirilgan tranzaksiya statistikadan chiqadi, lekin auditda qoladi.
[ ] Boshqa userning tranzaksiya id'si bilan so'rov 404 qaytaradi.
M3. Hisoblar
Turlar: CASH, BANK_CARD (Humo, Uzcard, Visa), BANK_ACCOUNT, SAVINGS, OTHER.
Funksiyalar: yaratish, boshlang'ich balans, nom va rang, default hisob, arxivlash, transfer, har bir hisob balansi va tarixi, balansni tuzatish (haqiqiy balans bilan solishtirib farqni "tuzatish" yozuvi sifatida kiritish).
Qoidalar: MVP'da faqat UZS. Balans = boshlang'ich balans + tranzaksiyalar yig'indisi (saqlangan maydon emas, hisoblanadi yoki tranzaksion yangilanadi). Tranzaksiyasi bor hisob o'chirilmaydi, faqat arxivlanadi. Transfer bitta DB tranzaksiyasida ikkita yozuv (transfer_group_id bilan bog'langan).
Qabul mezonlari:
[ ] Onboardingda kamida bitta hisob yaratiladi.
[ ] Transfer umumiy balansni o'zgartirmaydi.
[ ] Balansni tuzatish funksiyasi haqiqiy karta balansi bilan farqni yopadi.
M4. Kategoriyalar
Default xarajat kategoriyalari: Oziq-ovqat, Kafe va restoran, Transport, Uy va kommunal, Aloqa va internet, Xaridlar, Sog'liq, Ta'lim, Ko'ngilochar, Oila, Shaxsiy parvarish, Sovg'alar, Obunalar, Kredit foizi, Boshqa.
Default daromad kategoriyalari: Maosh, Avans, Bonus, Frilans, Biznes, Sovg'a, Qaytarilgan pul, Boshqa.
Har bir kategoriya: nom (uz/ru), icon, rang, tur, necessity, bot uchun kalit so'zlar ro'yxati.
Qoidalar: foydalanuvchi o'z kategoriyasini yarata oladi. Kategoriya o'chirilmaydi, arxivlanadi; eski tranzaksiyalar bilan bog'liqlik saqlanadi. Kalit so'zlar bot parseri uchun ishlatiladi (5-bo'lim).
M5. Analitika va hisobotlar
Maqsad: raqamlarni emas, javoblarni ko'rsatish.
Javob beriladigan savollar: eng ko'p pul qaysi kategoriyaga ketdi; o'tgan davrga nisbatan nima oshdi/kamaydi; qaysi kunlar ko'p sarflanadi; kunlik o'rtacha xarajat; zarur va ixtiyoriy nisbati; qaysi xarajatni kamaytirish mumkin.
Hisobotlar:
Hisobot
Tarkibi
Qayerda
Kunlik
Kirim, chiqim, sof oqim, tranzaksiyalar, kategoriyalar
Bot (kechqurun), Mini App
Haftalik
Jami, kunlik o'rtacha, eng ko'p sarflangan kun, top-3 kategoriya, o'tgan hafta bilan farq
Bot (yakshanba), Mini App
Oylik
Jami kirim/chiqim, sof oqim, jamg'arma, kredit to'lovlari (asosiy/foiz), kategoriyalar, o'tgan oy bilan taqqoslash
Bot (1-sana), Mini App
Ixtiyoriy oraliq
Tanlangan sana oralig'i bo'yicha hammasi
Mini App
Vizualizatsiya: kategoriyalar donut chart, kunlik bar chart, oy kalendari (kunlar sarf miqdori bo'yicha rang bilan, istalgan kunni bosib tafsilot), oyma-oy line chart.
Qabul mezonlari:
[ ] Kalendardan istalgan kunni tanlab, o'sha kunning tranzaksiyalarini ko'rish mumkin.
[ ] Kategoriyani bosganda shu kategoriya tranzaksiyalari ochiladi.
[ ] Kredit asosiy qarzi xarajat analitikasida ko'rinmaydi.
M6. Byudjet va bugungi limit
Maqsad: pulni sarflashdan oldin rejalashtirish.
Funksiyalar: oylik kutilgan daromad, oylik jamg'arma rejasi, kategoriya limitlari, erkin byudjet va bugungi limit (3-bo'limdagi formula), limitga yaqinlashish ogohlantirishi (80% va 100%), oy oxiri prognozi ("shu tezlikda oy oxirida X so'm ortiqcha sarflanadi").
Qoidalar: byudjetga faqat EXPENSE kiradi. Limitlar har oy avtomatik ko'chiriladi (foydalanuvchi o'zgartirmaguncha). Bitta kategoriyaga bitta oyda bitta limit.
Qabul mezonlari:
[ ] Bugungi limit Dashboard'ning eng yuqorisida.
[ ] 80% va 100% chegarada bot bir martadan xabar yuboradi (takrorlanmaydi).
[ ] Byudjet sozlash ustasi (wizard) 2 daqiqada tugaydi.
M7. Doimiy to'lovlar va majburiyatlar
Maqsad: kelajakdagi majburiy chiqimlarni unutmaslik.
Misollar: ijara, kommunal, internet, telefon, obunalar, kredit to'lovi (M8 bilan bog'liq), muntazam daromad (maosh, avans).
Funksiyalar: davriylik (har oy N-sana, har hafta, har N kun), avtomatik yaratish yoki tasdiqlash bilan yaratish, to'lov kalendari, to'lovdan 1 kun oldin va o'sha kuni eslatma, "to'landi" tugmasi botda.
Qabul mezonlari:
[ ] Eslatmadagi "To'landi" tugmasi bir bosishda tranzaksiya yaratadi.
[ ] Majburiy to'lovlar yig'indisi erkin byudjet hisobiga kiradi.
M8. Kredit boshqaruvi
Maqsad: kreditni alohida moliyaviy obyekt sifatida boshqarish va yopilish sanasini yaqinlashtirish.
Turlar: BANK_LOAN, INSTALLMENT (rassrochka), CREDIT_CARD, PERSONAL_DEBT (tanishdan qarz), OTHER.
Atributlar: nom, kreditor, boshlang'ich summa, joriy asosiy qarz, yillik foiz stavkasi, oylik to'lov, boshlanish va tugash sanasi, to'lov kuni, status (ACTIVE, CLOSED).
Funksiyalar:
• Kredit qo'shish va to'lov jadvalini kiritish (qo'lda yoki annuitet bo'yicha avtomatik).
• To'lovni qayd etish: asosiy qarz, foiz, komissiya, bog'liq tranzaksiya.
• Progress: to'langan foizi, qolgan oylar, taxminiy yopilish sanasi, jami to'lanadigan foiz.
• Qo'shimcha to'lov simulyatori: "har oy +X so'm to'lasam, kredit N oy oldin yopiladi va Y so'm foiz tejayman".
• Bir nechta qarz bo'lsa strategiyalar: Avalanche (eng yuqori foizli birinchi) va Snowball (eng kichik qoldiqli birinchi), ikkalasining natijasini taqqoslash.
• Tejalgan pulni kreditga yo'naltirish taklifi (oy oxirida erkin byudjet qoldig'i).
Qoidalar: asosiy qarz iste'mol xarajati emas. Simulyator natijalari taxminiy ekani UI'da yoziladi; aniq raqamlar bank jadvalidan. Kredit to'lovi M7 orqali eslatiladi.
Qabul mezonlari:
[ ] To'lov qayd etilganda outstanding_principal faqat asosiy qism miqdoriga kamayadi.
[ ] Simulyator natijasi annuitet formulasi bo'yicha unit-testlar bilan tekshirilgan.
[ ] Qoldiq 0 bo'lganda kredit CLOSED bo'ladi va tabrik xabari chiqadi.
M9. Jamg'arma maqsadlari
Maqsad: kutilmagan xarajat yangi qarzga olib kelmasligi va kelajak uchun pul yig'ish.
Tavsiya etilgan birinchi maqsad: zaxira fondi (kamida 1 oylik majburiy xarajat). Onboardingda taklif qilinadi.
Funksiyalar: maqsad yaratish (nom, summa, muddat), mablag' qo'shish/qaytarish, progress, "maqsadga yetish uchun oyiga/haftasiga qancha kerak" hisobi, muddat bo'yicha prognoz.
Qoidalar: jamg'armaga pul ajratish ichki transfer (SAVINGS hisobiga), xarajat emas. Maqsaddan pul qaytarish ham transfer.
M10. Insights va moliyaviy odatlar
Maqsad: statistikani harakatga aylantirish. MVP'da AI'siz, deterministik qoidalar bilan.
Qoida asosidagi xulosalar (misollar):
• Kategoriya o'tgan haftaga nisbatan 30% dan ko'p oshdi.
• Oylik byudjetning 80% i ishlatildi, oy tugashiga N kun bor.
• Shu tezlikda oy oxirida limitdan X so'm oshiladi.
• Daromadning X% i kredit to'lovlariga ketdi.
• Bu oy ixtiyoriy xarajatlar o'tgan oydan X so'm kam.
• 3 kundan beri xarajat kiritilmadi.
Odatlar (yengil gamification): ketma-ket qayd etish kunlari (streak), limit ichida o'tgan kunlar, birinchi jamg'arma maqsadi, bir oy to'liq hisob. Faqat mukofot, jarima yo'q.
Keyingi bosqich (4): AI yordamchi tarixni tahlil qilib, tushunarli tavsiya beradi, masalan: "Oxirgi 3 oyda kafe va taksi xarajati oshdi; ularni 15% kamaytirsangiz, oyiga taxminan 200 ming so'm tejaladi".
Qoidalar: kuniga ko'pi bilan 1 ta insight xabari botda; qolganlari Mini App'da. Ohang neytral va yordam beruvchi.
5. Telegram Bot va Mini App
   Bot tezkor kiritish va bildirishnomalar uchun, Mini App ko'rish, tahlil va boshqaruv uchun. Ular bir-birini almashtirmaydi; ikkalasi bitta backend API bilan ishlaydi.
   Vazifa
   Bot
   Mini App
   Tez xarajat/daromad kiritish
   Asosiy kanal
   Ha (forma)
   Tahrirlash, o'chirish
   Faqat oxirgi yozuvni bekor qilish
   To'liq
   Dashboard, grafiklar, kalendar
   Yo'q (qisqa matnli hisobot)
   Ha
   Byudjet, kredit, jamg'arma sozlash
   Yo'q
   Ha
   Eslatmalar va ogohlantirishlar
   Ha
   Yo'q
   Onboarding
   /start va Mini App'ni ochish
   To'liq sozlash
   Bot buyruqlari
   Buyruq
   Natija
   /start
   Ro'yxatdan o'tish, salomlashish, Mini App tugmasi
   /today
   Bugungi kirim, chiqim va bugungi limit
   /week
   Haftalik qisqa hisobot
   /month
   Oylik qisqa hisobot
   /balance
   Hisoblar bo'yicha balans
   /debt
   Kreditlar holati va keyingi to'lov
   /undo
   Oxirgi yozuvni bekor qilish
   /app
   Mini App'ni ochish
   /help
   Kiritish formatlari va misollar
   Bot menyu tugmasi (Menu Button) har doim Mini App'ni ochadi.
   Tez kiritish parseri qoidalari
   Format: <summa> <izoh> yoki <izoh> <summa>. Misollar: 25000 tushlik, taksi 15k, +8500000 oylik, kredit 2 mln.
1. Summa: raqamlar, bo'shliq va nuqta bilan ajratilgan (25 000, 25.000); qisqartmalar k/ming = ×1 000, mln/m = ×1 000 000. Summa topilmasa, bot formatni eslatadi.
2. Tur: default EXPENSE. + belgisi yoki daromad kategoriyasi kalit so'zi (oylik, maosh, avans, bonus) INCOME qiladi.
3. Kategoriya: izohdagi so'zlar kategoriyalarning kalit so'zlari bilan solishtiriladi (uz va ru, lotin va kirill). Foydalanuvchi oldin shu izohni qaysi kategoriyaga qo'ygan bo'lsa, o'shani oladi (shaxsiy o'rganish).
4. Hisob: default hisob; izohda naqd/karta bo'lsa, mos hisob.
5. Kredit: kredit so'zi bo'lsa va faol kredit bitta bo'lsa, DEBT_PAYMENT taklif qilinadi; bir nechta bo'lsa, tanlash tugmalari.
6. Noaniqlik: kategoriya aniqlanmasa, eng ko'p ishlatiladigan 6 ta kategoriya inline tugmalarda ko'rsatiladi.
7. Tasdiq: ishonch yuqori bo'lsa, darhol saqlanadi va javobda "Bekor qilish" va "O'zgartirish" tugmalari bo'ladi. Shunda har bir yozuv 1 ta xabar bilan tugaydi.
8. Bir nechta qator: bitta xabarda har qator alohida tranzaksiya.
   Bot javobi namunasi: Saqlandi: 25 000 so'm — Kafe (Karta). Bugungi limitdan qoldi: 73 000 so'm.
   Mini App talablari
   • Asosiy mavzu: ZarFlow premium dark (Midnight Navy fon, Emerald aksent, Champagne Gold kichik urg'u). Telegram light mavzusida Soft Ivory yuzalarga o'tadi; ranglar 1.1-bo'limdagi tokenlardan olinadi.
   • BackButton, MainButton va HapticFeedback Telegram SDK orqali.
   • Mobile-first, 360–430 px kenglik uchun optimallashtirilgan.
   • Pastki navigatsiya: Asosiy, Tarix, Analitika, Reja (byudjet, kredit, jamg'arma), Profil.
   • Markazda "+" tugmasi: tezkor qo'shish varag'i (bottom sheet) raqamli klaviatura bilan.
   • Birinchi ochilish 2 soniyadan kam; ma'lumot yuklanayotganda skeleton.
6. Asosiy foydalanuvchi ssenariylari
   Har bir ssenariy oxiridan-oxirigacha (end-to-end) test sifatida ham ishlatiladi.
   S1. Birinchi ro'yxatdan o'tish (maqsad: 2 daqiqada tayyor)
1. Foydalanuvchi botga /start yuboradi; bot salomlashadi va "Ilovani ochish" tugmasini beradi.
2. Mini App ochiladi; backend initData imzosini tekshiradi va userni yaratadi.
3. Onboarding 3 qadam: asosiy hisob va boshlang'ich balans; oylik kutilgan daromad; kredit bormi (ha bo'lsa, qisqa forma).
4. Dashboard ochiladi; bo'sh holatda "Birinchi xarajatni botga yozib ko'ring: 25000 tushlik".
   S2. Bot orqali xarajat qo'shish (maqsad: 5 soniya)
1. Foydalanuvchi 25000 tushlik yuboradi.
2. Parser summa, tur, kategoriya va hisobni aniqlaydi.
3. Ishonch yuqori: saqlanadi, javobda bugungi qolgan limit va "Bekor qilish" tugmasi.
4. Ishonch past: kategoriya tugmalari; foydalanuvchi bittasini bosadi va saqlanadi.
   S3. Kunlik nazorat
1. Kun davomida yozuvlar kiritiladi.
2. Foydalanuvchi tanlagan vaqtda (default 21:00) bot kunlik hisobot yuboradi: kirim, chiqim, limitga nisbatan natija, streak.
3. Bugun yozuv bo'lmasa: "Bugun xarajat bo'lmadimi yoki kiritish esdan chiqdimi?" va "Xarajat yo'q edi" tugmasi.
4. Xato bo'lsa, foydalanuvchi Mini App'da tuzatadi.
   S4. Oylik tahlil va keyingi oy rejasi
1. Oyning 1-sanasida bot oylik hisobot va Mini App havolasini yuboradi.
2. Analitikada oy tanlanadi: jami, kategoriyalar, zarur/ixtiyoriy, o'tgan oy bilan farq.
3. Kalendardan istalgan kun ochiladi.
4. Insights: "Kafe xarajati 25% oshdi".
5. Foydalanuvchi keyingi oy limitlarini yangilaydi.
   S5. Kredit to'lovi
1. To'lovdan 1 kun oldin bot eslatadi: summa, sana, kredit nomi.
2. To'lov kuni "To'landi" tugmasi bosiladi; asosiy qarz va foiz jadval bo'yicha taqsimlanadi.
3. Kredit progressi yangilanadi va qolgan oylar ko'rsatiladi.
   S6. Qo'shimcha to'lovni rejalashtirish
1. Oy oxirida erkin byudjetdan qoldiq bor.
2. Ilova taklif qiladi: "Qoldiqning bir qismini zaxira fondiga yoki kreditga yo'naltirasizmi?"
3. Simulyator kreditga yo'naltirilsa yopilish sanasi qancha yaqinlashishini ko'rsatadi.
4. Foydalanuvchi tanlaydi; tranzaksiya yoki transfer yaratiladi.
7. Texnik arxitektura
   MVP: bitta Spring Boot ilovasi ichida modular monolith. Microservices ishlatilmaydi; modullar aniq chegaralar bilan ajratiladi, kerak bo'lsa keyinchalik ajratish mumkin.
   Texnologiyalar
   Qatlam
   Texnologiya
   Backend
   Java 21, Spring Boot 3.5.x
   ORM
   Spring Data JPA / Hibernate
   Ma'lumotlar bazasi
   PostgreSQL 16+
   Migratsiya
   Flyway
   Modul chegaralari
   Spring Modulith (modullararo bog'liqlikni test bilan tekshirish)
   Telegram bot
   TelegramBots (Java), webhook rejimi
   Frontend
   React + TypeScript + Vite, Tailwind CSS, @telegram-apps/sdk
   Grafiklar
   Recharts
   Server holati (frontend)
   TanStack Query
   API
   REST, OpenAPI / Swagger
   Rejalashtirilgan ishlar
   Spring @Scheduled + ShedLock
   Konteyner
   Docker, Docker Compose, Nginx (HTTPS)
   Test
   JUnit 5, Mockito, Testcontainers
   Komponentlar
   Telegram  ──>  Bot (webhook)  ──┐
   ├──>  Spring Boot API  ──>  PostgreSQL
   Telegram  ──>  Mini App (React) ┘          │
   └──>  Scheduler  ──>  Bot notifications
   Backend paket tuzilmasi
   uz.zarflow
   ├── identity        # initData tekshirish, sessiya/JWT
   ├── user            # profil, sozlamalar, vaqt zonasi
   ├── account
   ├── transaction
   ├── category
   ├── budget          # limitlar, erkin byudjet, bugungi limit
   ├── recurring       # doimiy to'lovlar
   ├── debt            # kreditlar, to'lovlar, simulyator
   ├── savings
   ├── analytics       # faqat o'qish uchun so'rovlar
   ├── insights        # qoida asosidagi xulosalar
   ├── notification
   ├── telegram        # bot handlerlari, parser
   └── shared          # config, exception, security, common (Money, DateRange)
   Har bir modulda controller, service, repository, entity va dto. Modullar bir-birining repository'siga murojaat qilmaydi, faqat public service interfeysi yoki domen eventlari (TransactionCreated, DebtPaymentRecorded) orqali.
   Ma'lumotlar modeli
   Jadval
   Vazifasi
   Muhim maydonlar
   users
   Foydalanuvchi
   telegram_id BIGINT UNIQUE, language, timezone, expected_monthly_income
   accounts
   Pul hisoblari
   type, initial_balance, is_default, archived_at
   categories
   Kategoriyalar
   type, necessity, icon, color, keywords[], archived_at
   transactions
   Barcha pul harakatlari
   type, amount, necessity, source, transfer_group_id, linked_debt_id, linked_goal_id, deleted_at
   transaction_audit
   O'zgarishlar tarixi
   transaction_id, action, before JSONB, after JSONB
   budgets
   Oylik byudjet
   month, planned_savings
   budget_limits
   Kategoriya limitlari
   budget_id, category_id, amount, notified_80, notified_100
   recurring_rules
   Doimiy to'lovlar
   schedule, next_run_date, auto_create
   debts
   Kreditlar
   type, original_amount, outstanding_principal, annual_rate, monthly_payment, payment_day, status
   debt_schedule
   To'lov jadvali
   debt_id, due_date, principal, interest
   debt_payments
   Qilingan to'lovlar
   principal, interest, fee, transaction_id
   savings_goals
   Jamg'arma maqsadlari
   target_amount, target_date, account_id, status
   notification_settings
   Bildirishnomalar
   daily_report_time, weekly, monthly, reminders
   user_parsing_memory
   Parser o'rganishi
   user_id, phrase, category_id
   Qarorlar: PK UUID; summa NUMERIC(19,2); valyuta VARCHAR(3); vaqt TIMESTAMPTZ; har bir jadvalda user_id va unga indeks; transactions(user_id, transaction_date) kompozit indeks.
   REST API
   Prefiks /api/v1. Resurslar: /auth/telegram, /users/me, /accounts (+ /transfer), /transactions, /categories, /budgets (+ /today), /recurring, /debts (+ /{id}/payments, /{id}/simulate), /savings-goals (+ /{id}/contributions), /analytics/overview|daily|weekly|monthly|categories|calendar|cash-flow, /insights, /notifications/settings.
   Tranzaksiya ro'yxati filtrlari: from, to, type, categoryId, accountId, minAmount, maxAmount, q, page, size. Yaratish so'rovlari Idempotency-Key sarlavhasini qabul qiladi (bot qayta yuborganda dublikat bo'lmasligi uchun).
   Xavfsizlik
1. Mini App initData serverda HMAC-SHA256 bilan tekshiriladi, auth_date 24 soatdan eski bo'lmasligi kerak.
2. Tekshiruvdan keyin qisqa muddatli JWT beriladi; user_id faqat shundan olinadi.
3. Bot token va maxfiy kalitlar faqat serverda (environment variables).
4. Har bir so'rovda ownership tekshiruvi: repository so'rovlari doim user_id bilan.
5. Bot webhook'i maxfiy secret_token sarlavhasi bilan himoyalangan.
6. Input validatsiya (Bean Validation), parametrlangan so'rovlar, XSS himoyasi, rate limiting.
7. HTTPS majburiy; kunlik PostgreSQL backup va tiklash tekshiruvi.
8. Foydalanuvchi ma'lumotlarini eksport qilish (CSV) va akkauntni to'liq o'chirish imkoniyati.
   Nofunksional talablar
   Talab
   Maqsad
   API javob vaqti
   Oddiy so'rovlar p95 < 300 ms
   Mini App birinchi ochilishi
   < 2 s
   Mavjudlik
   MVP 99%
   Test qamrovi
   Moliyaviy hisob-kitob servislari 90%+
   Lokalizatsiya
   uz (lotin) asosiy, ru tayyor
   Vaqt zonasi
   Asia/Tashkent default
8. Development bosqichlari
   Oltita bosqich; har biri oxirida loyiha egasi real foydalanadigan ishlaydigan versiya chiqadi. Keyingi bosqichga o'tish sharti: oldingi bosqichning tayyorlik mezoni bajarilgan va kamida 1 hafta real foydalanilgan.
   Bosqich
   Nomi
   Maqsad
   Taxminiy muddat
   0
   Foundation
   Loyiha poydevori va Telegram auth
   1 hafta
   1
   Financial Tracking (MVP)
   Har kuni xarajat yozish va ko'rish
   2–3 hafta
   2
   Financial Control
   Bugungi limit va byudjet
   2 hafta
   3
   Debt & Savings
   Kredit va jamg'arma
   2 hafta
   4
   Smart Finance
   AI, ovoz, aqlli tavsiyalar
   3–4 hafta
   5
   Public Product
   Ommaga chiqarish
   Doimiy
   Bosqich 0 — Foundation
   [ ] Spring Boot loyihasi, paket tuzilmasi, Spring Modulith testi.
   [ ] Docker Compose: PostgreSQL, ilova, Nginx.
   [ ] Flyway: users, accounts, categories jadvallari va default kategoriyalar seed.
   [ ] Telegram bot ro'yxatdan o'tkazish (BotFather), webhook, /start.
   [ ] initData tekshirish va JWT; /users/me.
   [ ] React + Vite Mini App skeleti, Telegram SDK, theme, pastki navigatsiya.
   [ ] HTTPS bilan serverga deploy (Mini App faqat HTTPS'da ishlaydi).
   Tayyorlik mezoni: botdan Mini App ochiladi va foydalanuvchi ismi ko'rinadi.
   Bosqich 1 — Financial Tracking (MVP)
   [ ] M3 Hisoblar: CRUD, transfer, balansni tuzatish.
   [ ] M4 Kategoriyalar: default va custom, necessity, kalit so'zlar.
   [ ] M2 Tranzaksiyalar: CRUD, soft-delete, audit, filtrlar, tarix.
   [ ] Bot parser (5-bo'lim qoidalari), /today, /balance, /undo.
   [ ] M1 Dashboard (bugungi limitsiz).
   [ ] M5: kunlik va oylik hisobot, kalendar, kategoriya donut chart.
   [ ] Kechki kunlik hisobot botda.
   [ ] Onboarding (S1).
   Tayyorlik mezoni: loyiha egasi 7 kun ketma-ket barcha xarajatlarni faqat shu ilova orqali kiritadi; parser yozuvlarning 80% idan ko'pini qo'shimcha savolsiz to'g'ri kategoriyalaydi.
   Bosqich 2 — Financial Control
   [ ] M6: kutilgan daromad, kategoriya limitlari, erkin byudjet, bugungi limit.
   [ ] M7: doimiy to'lovlar, eslatmalar, "To'landi" tugmasi.
   [ ] 80%/100% ogohlantirishlar, oy oxiri prognozi.
   [ ] Haftalik hisobot, o'tgan davr bilan taqqoslash.
   [ ] M10: birinchi qoida asosidagi insightlar va streak.
   Tayyorlik mezoni: bugungi limit Dashboard'da to'g'ri hisoblanadi (unit-testlar bilan), bir oy davomida byudjet bilan yashab ko'rilgan.
   Bosqich 3 — Debt & Savings
   [ ] M8: kreditlar, to'lov jadvali (annuitet), to'lovlarni taqsimlash, progress.
   [ ] Qo'shimcha to'lov simulyatori, Avalanche/Snowball taqqoslash.
   [ ] M9: zaxira fondi va jamg'arma maqsadlari, progress, prognoz.
   [ ] Oy oxiri qoldig'ini yo'naltirish ssenariysi (S6).
   Tayyorlik mezoni: loyiha egasining real krediti tizimda, bank jadvali bilan bir oylik to'lov farqi 1 000 so'mdan kam.
   Bosqich 4 — Smart Finance
   [ ] AI moliyaviy yordamchi (oylik tahlil va tavsiyalar).
   [ ] AI yordamida avtomatik kategoriyalash (parser ishonchi past bo'lganda).
   [ ] Ovozli xabar orqali kiritish.
   [ ] Bank SMS/bildirishnomalarini forward qilib parse qilish.
   [ ] CSV eksport.
   Bosqich 5 — Public Product
   [ ] Ko'p foydalanuvchi yuklamasi uchun optimizatsiya va monitoring.
   [ ] Rus tili to'liq.
   [ ] Maxfiylik siyosati va foydalanish shartlari.
   [ ] Premium imkoniyatlar modeli.
   [ ] Bank integratsiyalari (qonuniy va texnik imkoniyat bo'lsa).
   [ ] Web dashboard.
   MVPdan tashqarida (ataylab)
   Bank bilan to'g'ridan-to'g'ri integratsiya, ko'p valyuta, oilaviy/umumiy hisoblar, investitsiyalar, premium obuna, alohida web dashboard. Bular faqat 4–5 bosqichda ko'rib chiqiladi.
9. Xavflar, ochiq savollar va qarorlar
   Xavflar
   Xavf
   Ta'siri
   Yechim
   Foydalanuvchi 2–3 haftada kiritishni tashlaydi
   Ma'lumot to'liq emas, analitika noto'g'ri
   Bot orqali 1 xabarli kiritish, kechki eslatma, streak
   Haddan tashqari ko'p funksiya
   MVP kechikadi
   Bosqichlarga qat'iy rioya, 8-bo'lim
   Naqd xarajatlar esdan chiqadi
   Balans haqiqatdan farq qiladi
   Balansni tuzatish funksiyasi
   Kredit hisobi bank jadvalidan farq qiladi
   Ishonch yo'qoladi
   Bank jadvalini kiritish imkoniyati, taxminiy qiymatlar belgilanadi
   Moliyaviy ma'lumotlar sizib chiqishi
   Jiddiy obro' va huquqiy xavf
   7-bo'lim xavfsizlik talablari, ownership testlari
   Ayblovchi ohang
   Foydalanuvchi ilovadan qochadi
   Neytral matnlar, faqat mukofotli gamification
   Ochiq savollar
   [ ] Oylik daromad bir necha qismda keladimi (avans + maosh)? Erkin byudjet hisobiga ta'siri.
   [ ] Kunlik hisobot uchun default vaqt 21:00 mos keladimi?
   [ ] Domen va Telegram bot username: zarflow.uz, @ZarFlowBot band emasligini tekshirish va ro'yxatdan o'tkazish.
   [ ] Hosting: mahalliy VPS yoki xorijiy bulut (shaxsiy ma'lumotlar lokalizatsiyasi talabi)?
   [ ] Ommaviy bosqichda monetizatsiya modeli.
   Qarorlar jurnali
   Sana
   Qaror
   Sabab
   2026-11-05
   Brend nomi: ZarFlow (oldingi ishchi nom MyFinance)
   Mahsulotni brend darajasiga olib chiqish; Zar + Flow ma'nosi
   2026-11-05
   Vizual uslub: Premium dark (Navy + Emerald + Champagne Gold), Manrope + Inter
   Premium FinTech pozitsiyasi
   2026-11-05
   Platforma: Telegram Bot + Mini App
   Alohida ilova o'rnatish shart emas, kiritish tez
   2026-11-05
   Arxitektura: modular monolith
   MVP uchun soddalik, keyin ajratish mumkin
   2026-11-05
   Bugungi limit asosiy ko'rsatkich
   Statistikani bugungi harakatga aylantiradi
   2026-11-05
   Kredit asosiy qarzi xarajat emas
   To'g'ri moliyaviy statistika
   2026-11-05
   MVP'da bank integratsiyasi va AI yo'q
   Tez ishga tushirish, avval odatni shakllantirish
   2026-11-05
   Migratsiya: Flyway
   Loyiha egasining tanlovi
