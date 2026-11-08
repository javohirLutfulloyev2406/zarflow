import type {
  AccountType,
  AchievementCode,
  DebtInterestMethod,
  DebtType,
  Language,
  RecurringKind,
  RecurringSchedule,
} from "../api/types";
import { useLanguage } from "./useLanguage";

export interface Dictionary {
  greeting: (firstName: string) => string;
  home: {
    emptyState: string;
    todayLimitLabel: string;
    balanceLabel: string;
    comingSoon: string;
    todaySpent: string;
    noExpensesToday: string;
    monthIncome: string;
    monthExpense: string;
    monthNet: string;
    balancesTitle: string;
    streakLabel: (days: number) => string;
  };
  nav: {
    home: string;
    history: string;
    analytics: string;
    plan: string;
  };
  placeholders: {
    plan: string;
  };
  analytics: {
    title: string;
    incomeLabel: string;
    expenseLabel: string;
    netLabel: string;
    vsLastMonthLabel: string;
    essentialLabel: string;
    discretionaryLabel: string;
    dailyAverageLabel: string;
    empty: string;
    calendarWeekdays: string[];
  };
  history: {
    title: string;
    empty: string;
    today: string;
    yesterday: string;
    deleted: string;
    deleteConfirm: string;
    clearFilter: string;
    dayDetailEmpty: string;
    closeDayDetail: string;
  };
  accountTypes: Record<AccountType, string>;
  onboarding: {
    step1Title: string;
    accountNameLabel: string;
    accountNamePlaceholder: string;
    accountTypeLabel: string;
    initialBalanceLabel: string;
    initialBalancePlaceholder: string;
    next: string;
    step2Title: string;
    incomeLabel: string;
    incomePlaceholder: string;
    finish: string;
  };
  quickAdd: {
    title: string;
    tabExpense: string;
    tabIncome: string;
    tabTransfer: string;
    amountLabel: string;
    categoriesTitle: string;
    other: string;
    accountLabel: string;
    fromAccountLabel: string;
    toAccountLabel: string;
    noteLabel: string;
    notePlaceholder: string;
    save: string;
    delete: string;
    savedToast: string;
    undo: string;
    fixedToggleLabel: string;
  };
  profile: {
    title: string;
    firstName: string;
    displayName: string;
    displayNamePlaceholder: string;
    language: string;
    timezone: string;
    timezoneSearchPlaceholder: string;
    expectedMonthlyIncome: string;
    incomePlaceholder: string;
    save: string;
    saved: string;
    conflict: string;
  };
  namePrompt: {
    title: string;
    placeholder: string;
    save: string;
  };
  notifications: {
    dailyReportTitle: string;
    dailyReportEnabledLabel: string;
    dailyReportTimeLabel: string;
    weeklyReportLabel: string;
    monthlyReportLabel: string;
  };
  insights: {
    sectionTitle: string;
    empty: string;
    dismissLabel: string;
    periodWeek: string;
    periodMonth: string;
    comparedToPreviousPeriod: string;
  };
  achievements: {
    title: string;
    locked: string;
    codeLabel: Record<AchievementCode, string>;
    codeDescription: Record<AchievementCode, string>;
  };
  budget: {
    dailyAllowanceLabel: string;
    todaySpentTile: string;
    tomorrowTile: string;
    overspentLine: (amount: string) => string;
    setupTitle: string;
    setupCta: string;
    forecastOverage: (amount: string) => string;
    expectedIncomeLabel: string;
    fixedPaymentsLabel: string;
    loanPaymentsLabel: string;
    plannedSavingsLabel: string;
    freeBudgetLabel: string;
    negativeFreeBudgetWarning: string;
    limitsTitle: string;
    limitAmountPlaceholder: string;
    save: string;
    saved: string;
    autoLabel: string;
    useManual: string;
    useAuto: string;
  };
  recurring: {
    sectionTitle: string;
    addButton: string;
    emptyState: string;
    nextDueLabel: (date: string) => string;
    archived: string;
    kindLabel: Record<RecurringKind, string>;
    scheduleLabel: Record<RecurringSchedule, string>;
    formTitleNew: string;
    formTitleEdit: string;
    nameLabel: string;
    namePlaceholder: string;
    amountLabel: string;
    categoryLabel: string;
    accountLabel: string;
    scheduleFieldLabel: string;
    dayOfMonthLabel: string;
    dayOfWeekLabel: string;
    everyNDaysLabel: string;
    startDateLabel: string;
    autoCreateLabel: string;
    remindDaysBeforeLabel: string;
    save: string;
    archive: string;
    weekdayShort: string[];
    upcomingTitle: string;
    upcomingEmpty: string;
    dueToday: string;
    dueTomorrow: string;
    payButton: string;
    skipButton: string;
    statusPaid: string;
    statusSkipped: string;
    homeCardTitle: string;
    homeCardEmpty: string;
  };
  debts: {
    sectionTitle: string;
    addButton: string;
    emptyState: string;
    typeLabel: Record<DebtType, string>;
    interestMethodLabel: Record<DebtInterestMethod, string>;
    formTitleNew: string;
    formTitleEdit: string;
    nameLabel: string;
    namePlaceholder: string;
    lenderLabel: string;
    typeFieldLabel: string;
    originalAmountLabel: string;
    annualRateLabel: string;
    startDateLabel: string;
    termMonthsLabel: string;
    paymentDayLabel: string;
    interestMethodFieldLabel: string;
    save: string;
    manualScheduleButton: string;
    manualScheduleTitle: string;
    manualScheduleAddRow: string;
    estimatedLabel: string;
    outstandingLabel: string;
    paidPercentLabel: string;
    monthlyPaymentLabel: string;
    monthsLeftLabel: string;
    closeDateLabel: string;
    nextPaymentTitle: string;
    noNextPayment: string;
    payButton: string;
    payExtraToggle: string;
    payAmountLabel: string;
    payDateLabel: string;
    payAccountLabel: string;
    simulatorTitle: string;
    simulatorCustomPlaceholder: string;
    simulatorResultSaved: (months: number, amount: string) => string;
    simulatorResultNone: string;
    historyTitle: string;
    historyEmpty: string;
    strategyTitle: string;
    strategyAvalanche: string;
    strategySnowball: string;
    strategyRecommended: string;
    strategyInterestLabel: string;
    strategyCloseDateLabel: string;
    closedBadge: string;
    activeBadge: string;
    homeCardTitle: string;
    debtClosedCelebration: string;
  };
  error: {
    generic: string;
    retry: string;
  };
}

const uz: Dictionary = {
  greeting: (firstName) => `Xayrli kun, ${firstName}`,
  home: {
    emptyState: "Birinchi xarajatni botga yozib ko'ring: 25000 tushlik",
    todayLimitLabel: "Bugungi limit",
    balanceLabel: "Balans",
    comingSoon: "tez orada",
    todaySpent: "Bugun sarflandi",
    noExpensesToday: "Bugun hali xarajat yo'q",
    monthIncome: "Daromad",
    monthExpense: "Xarajat",
    monthNet: "Sof",
    balancesTitle: "Hisoblar",
    streakLabel: (days) => `${days} kun ketma-ket`,
  },
  nav: {
    home: "Asosiy",
    history: "Tarix",
    analytics: "Analitika",
    plan: "Reja",
  },
  placeholders: {
    plan: "Byudjet va rejalashtirish tez orada shu yerda ko'rinadi.",
  },
  analytics: {
    title: "Analitika",
    incomeLabel: "Daromad",
    expenseLabel: "Xarajat",
    netLabel: "Sof",
    vsLastMonthLabel: "O'tgan oyga nisbatan",
    essentialLabel: "Zarur",
    discretionaryLabel: "Ixtiyoriy",
    dailyAverageLabel: "Kuniga o'rtacha xarajat",
    empty: "Bu oyda hali ma'lumot yo'q.",
    calendarWeekdays: ["Du", "Se", "Cho", "Pa", "Ju", "Sh", "Ya"],
  },
  history: {
    title: "Tarix",
    empty: "Hali tranzaksiyalar yo'q.",
    today: "Bugun",
    yesterday: "Kecha",
    deleted: "O'chirildi",
    deleteConfirm: "O'chirishni tasdiqlaysizmi?",
    clearFilter: "Filtrni tozalash",
    dayDetailEmpty: "Bu kunda yozuvlar yo'q.",
    closeDayDetail: "Yopish",
  },
  accountTypes: {
    CASH: "Naqd pul",
    BANK_CARD: "Bank kartasi",
    BANK_ACCOUNT: "Bank hisobi",
    SAVINGS: "Jamg'arma",
    OTHER: "Boshqa",
  },
  onboarding: {
    step1Title: "Birinchi hisobingizni qo'shamiz",
    accountNameLabel: "Hisob nomi",
    accountNamePlaceholder: "Masalan: Hamyon",
    accountTypeLabel: "Turi",
    initialBalanceLabel: "Boshlang'ich balans",
    initialBalancePlaceholder: "Masalan: 500000",
    next: "Keyingi",
    step2Title: "Oylik kutilgan daromadingiz qancha?",
    incomeLabel: "Oylik daromad (ixtiyoriy)",
    incomePlaceholder: "Masalan: 8500000",
    finish: "Boshlash",
  },
  quickAdd: {
    title: "Qo'shish",
    tabExpense: "Xarajat",
    tabIncome: "Daromad",
    tabTransfer: "O'tkazma",
    amountLabel: "Summa",
    categoriesTitle: "Kategoriya",
    other: "Boshqa",
    accountLabel: "Hisob",
    fromAccountLabel: "Qayerdan",
    toAccountLabel: "Qayerga",
    noteLabel: "Izoh",
    notePlaceholder: "Ixtiyoriy",
    save: "Saqlash",
    delete: "O'chirish",
    savedToast: "Saqlandi",
    undo: "Bekor qilish",
    fixedToggleLabel: "Majburiy to'lov",
  },
  profile: {
    title: "Profil",
    firstName: "Telegramdagi ism",
    displayName: "Ilovadagi ismingiz",
    displayNamePlaceholder: "Sizni qanday chaqirsak?",
    language: "Til",
    timezone: "Vaqt zonasi",
    timezoneSearchPlaceholder: "Shahar yoki hudud qidiring",
    expectedMonthlyIncome: "Oylik kutilgan daromad",
    incomePlaceholder: "Masalan: 8500000",
    save: "Saqlash",
    saved: "Saqlandi",
    conflict: "Ma'lumotlar boshqa joyda o'zgartirildi. Qayta yuklandi.",
  },
  namePrompt: {
    title: "Sizni ilovada qanday ism bilan chaqiramiz?",
    placeholder: "Ismingiz",
    save: "Davom etish",
  },
  notifications: {
    dailyReportTitle: "Kunlik hisobot",
    dailyReportEnabledLabel: "Har kuni hisobot yuborilsin",
    dailyReportTimeLabel: "Vaqti",
    weeklyReportLabel: "Haftalik hisobot (yakshanba kuni)",
    monthlyReportLabel: "Oylik hisobot (oy boshida)",
  },
  insights: {
    sectionTitle: "Maslahatlar",
    empty: "Hozircha maslahatlar yo'q.",
    dismissLabel: "Yopish",
    periodWeek: "Hafta",
    periodMonth: "Oy",
    comparedToPreviousPeriod: "Oldingi davrga nisbatan",
  },
  achievements: {
    title: "Yutuqlar",
    locked: "Ochilmagan",
    codeLabel: {
      STREAK_7: "7 kunlik odat",
      STREAK_30: "30 kunlik odat",
      LIMIT_WEEK: "Haftalik limit ustasi",
      FULL_MONTH: "Butun oy",
    },
    codeDescription: {
      STREAK_7: "7 kun ketma-ket yozib boring",
      STREAK_30: "30 kun ketma-ket yozib boring",
      LIMIT_WEEK: "7 kun davomida kunlik limitdan oshmang",
      FULL_MONTH: "Butun oy davomida har kuni yozib boring",
    },
  },
  budget: {
    dailyAllowanceLabel: "Kunlik ruxsat",
    todaySpentTile: "Bugun sarflandi",
    tomorrowTile: "Ertangi ruxsat",
    overspentLine: (amount) => `0 so'm · limitdan ${amount} oshdi`,
    setupTitle: "Byudjetni sozlang",
    setupCta: "Boshlash",
    forecastOverage: (amount) => `Shu tezlikda oy oxirida ${amount} oshadi`,
    expectedIncomeLabel: "① Kutilgan oylik daromad",
    fixedPaymentsLabel: "② Doimiy to'lovlar (ijara, kommunal...)",
    loanPaymentsLabel: "③ Kredit to'lovlari",
    plannedSavingsLabel: "④ Jamg'arish rejasi",
    freeBudgetLabel: "Erkin byudjet",
    negativeFreeBudgetWarning: "Majburiyatlar daromaddan oshib ketdi.",
    limitsTitle: "Kategoriya limitlari",
    limitAmountPlaceholder: "Limit",
    save: "Saqlash",
    saved: "Saqlandi",
    autoLabel: "avtomatik",
    useManual: "Qo'lda kiritish",
    useAuto: "Avtomatik hisoblash",
  },
  recurring: {
    sectionTitle: "Doimiy to'lovlar",
    addButton: "+ Qo'shish",
    emptyState: "Hali doimiy to'lovlar yo'q.",
    nextDueLabel: (date) => `Keyingi: ${date}`,
    archived: "Arxivlangan",
    kindLabel: { REGULAR: "Doimiy xarajat", LOAN: "Kredit", INCOME: "Daromad" },
    scheduleLabel: { MONTHLY: "Oylik", WEEKLY: "Haftalik", EVERY_N_DAYS: "N kunda" },
    formTitleNew: "Yangi doimiy to'lov",
    formTitleEdit: "To'lovni tahrirlash",
    nameLabel: "Nomi",
    namePlaceholder: "Masalan: Ijara",
    amountLabel: "Summa",
    categoryLabel: "Kategoriya",
    accountLabel: "Hisob",
    scheduleFieldLabel: "Davriylik",
    dayOfMonthLabel: "Oyning kuni",
    dayOfWeekLabel: "Hafta kuni",
    everyNDaysLabel: "Necha kunda",
    startDateLabel: "Boshlanish sanasi",
    autoCreateLabel: "Avtomatik yozib qo'yilsin",
    remindDaysBeforeLabel: "Necha kun oldin eslatish",
    save: "Saqlash",
    archive: "Arxivlash",
    weekdayShort: ["Du", "Se", "Cho", "Pa", "Ju", "Sh", "Ya"],
    upcomingTitle: "Keyingi 30 kun",
    upcomingEmpty: "Yaqin 30 kunda to'lovlar yo'q.",
    dueToday: "Bugun",
    dueTomorrow: "Ertaga",
    payButton: "To'landi",
    skipButton: "O'tkazib yuborish",
    statusPaid: "To'landi ✓",
    statusSkipped: "O'tkazib yuborildi",
    homeCardTitle: "Yaqin 7 kundagi to'lovlar",
    homeCardEmpty: "Yaqin 7 kunda to'lov yo'q",
  },
  debts: {
    sectionTitle: "Kreditlar",
    addButton: "+ Qo'shish",
    emptyState: "Hali qarzlar yo'q.",
    typeLabel: {
      BANK_LOAN: "Bank krediti",
      INSTALLMENT: "Rassrochka",
      CREDIT_CARD: "Kredit karta",
      PERSONAL_DEBT: "Shaxsiy qarz",
      OTHER: "Boshqa",
    },
    interestMethodLabel: {
      ANNUITY_MONTHLY: "Annuitet (oylik)",
      ACTUAL_365: "Kundalik hisob (365)",
    },
    formTitleNew: "Yangi kredit",
    formTitleEdit: "Kreditni tahrirlash",
    nameLabel: "Nomi",
    namePlaceholder: "Masalan: Avtokredit",
    lenderLabel: "Bank / beruvchi",
    typeFieldLabel: "Turi",
    originalAmountLabel: "Summa",
    annualRateLabel: "Yillik foiz stavkasi (%)",
    startDateLabel: "Boshlanish sanasi",
    termMonthsLabel: "Muddati (oy)",
    paymentDayLabel: "To'lov kuni",
    interestMethodFieldLabel: "Foiz hisoblash usuli",
    save: "Saqlash",
    manualScheduleButton: "Bank jadvalini kiritish",
    manualScheduleTitle: "Jadvalni qo'lda kiriting",
    manualScheduleAddRow: "+ Qator qo'shish",
    estimatedLabel: "taxminiy",
    outstandingLabel: "Qoldiq",
    paidPercentLabel: "To'langan",
    monthlyPaymentLabel: "Oylik to'lov",
    monthsLeftLabel: "Qolgan oy",
    closeDateLabel: "Yopilish sanasi",
    nextPaymentTitle: "Keyingi to'lov",
    noNextPayment: "Rejalashtirilgan to'lov yo'q",
    payButton: "To'landi",
    payExtraToggle: "Qo'shimcha to'lov (hammasi asosiy qarzga)",
    payAmountLabel: "Summa",
    payDateLabel: "Sana",
    payAccountLabel: "Hisob",
    simulatorTitle: "Tezroq yopish",
    simulatorCustomPlaceholder: "Boshqa summa",
    simulatorResultSaved: (months, amount) => `${months} oy tezroq, ${amount} tejaladi`,
    simulatorResultNone: "Qo'shimcha summani tanlang",
    historyTitle: "To'lovlar tarixi",
    historyEmpty: "Hali to'lovlar yo'q.",
    strategyTitle: "Qaysi strategiya yaxshiroq?",
    strategyAvalanche: "Avalanche (yuqori foizdan)",
    strategySnowball: "Snowball (kichik qoldiqdan)",
    strategyRecommended: "Tavsiya etiladi",
    strategyInterestLabel: "Jami foiz",
    strategyCloseDateLabel: "Yopilish sanasi",
    closedBadge: "Yopilgan",
    activeBadge: "Faol",
    homeCardTitle: "Qarzlar",
    debtClosedCelebration: "🎉 Qarz to'liq yopildi!",
  },
  error: {
    generic: "Kutilmagan xatolik yuz berdi. Ilovani qayta ochib ko'ring.",
    retry: "Qayta urinish",
  },
};

const ru: Dictionary = {
  greeting: (firstName) => `Добрый день, ${firstName}`,
  home: {
    emptyState: "Запишите первый расход боту: 25000 обед",
    todayLimitLabel: "Лимит на сегодня",
    balanceLabel: "Баланс",
    comingSoon: "скоро",
    todaySpent: "Потрачено сегодня",
    noExpensesToday: "Сегодня расходов ещё нет",
    monthIncome: "Доход",
    monthExpense: "Расход",
    monthNet: "Чистыми",
    balancesTitle: "Счета",
    streakLabel: (days) => `${days} дней подряд`,
  },
  nav: {
    home: "Главная",
    history: "История",
    analytics: "Аналитика",
    plan: "План",
  },
  placeholders: {
    plan: "Бюджет и планирование скоро появятся здесь.",
  },
  analytics: {
    title: "Аналитика",
    incomeLabel: "Доход",
    expenseLabel: "Расход",
    netLabel: "Чистыми",
    vsLastMonthLabel: "По сравнению с прошлым месяцем",
    essentialLabel: "Необходимое",
    discretionaryLabel: "Необязательное",
    dailyAverageLabel: "Среднее в день",
    empty: "В этом месяце пока нет данных.",
    calendarWeekdays: ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд"],
  },
  history: {
    title: "История",
    empty: "Пока нет транзакций.",
    today: "Сегодня",
    yesterday: "Вчера",
    deleted: "Удалено",
    deleteConfirm: "Подтвердите удаление",
    clearFilter: "Очистить фильтр",
    dayDetailEmpty: "В этот день нет записей.",
    closeDayDetail: "Закрыть",
  },
  accountTypes: {
    CASH: "Наличные",
    BANK_CARD: "Банковская карта",
    BANK_ACCOUNT: "Банковский счёт",
    SAVINGS: "Сбережения",
    OTHER: "Другое",
  },
  onboarding: {
    step1Title: "Добавим ваш первый счёт",
    accountNameLabel: "Название счёта",
    accountNamePlaceholder: "Например: Кошелёк",
    accountTypeLabel: "Тип",
    initialBalanceLabel: "Начальный баланс",
    initialBalancePlaceholder: "Например: 500000",
    next: "Далее",
    step2Title: "Какой у вас ожидаемый доход в месяц?",
    incomeLabel: "Доход в месяц (необязательно)",
    incomePlaceholder: "Например: 8500000",
    finish: "Начать",
  },
  quickAdd: {
    title: "Добавить",
    tabExpense: "Расход",
    tabIncome: "Доход",
    tabTransfer: "Перевод",
    amountLabel: "Сумма",
    categoriesTitle: "Категория",
    other: "Другое",
    accountLabel: "Счёт",
    fromAccountLabel: "Откуда",
    toAccountLabel: "Куда",
    noteLabel: "Заметка",
    notePlaceholder: "Необязательно",
    save: "Сохранить",
    delete: "Удалить",
    savedToast: "Сохранено",
    undo: "Отменить",
    fixedToggleLabel: "Обязательный платёж",
  },
  profile: {
    title: "Профиль",
    firstName: "Имя в Telegram",
    displayName: "Ваше имя в приложении",
    displayNamePlaceholder: "Как вас называть?",
    language: "Язык",
    timezone: "Часовой пояс",
    timezoneSearchPlaceholder: "Найдите город или регион",
    expectedMonthlyIncome: "Ожидаемый доход в месяц",
    incomePlaceholder: "Например: 8500000",
    save: "Сохранить",
    saved: "Сохранено",
    conflict: "Данные были изменены в другом месте. Перезагружено.",
  },
  namePrompt: {
    title: "Как вас называть в приложении?",
    placeholder: "Ваше имя",
    save: "Продолжить",
  },
  notifications: {
    dailyReportTitle: "Ежедневный отчёт",
    dailyReportEnabledLabel: "Отправлять отчёт каждый день",
    dailyReportTimeLabel: "Время",
    weeklyReportLabel: "Недельный отчёт (по воскресеньям)",
    monthlyReportLabel: "Месячный отчёт (в начале месяца)",
  },
  insights: {
    sectionTitle: "Советы",
    empty: "Пока нет советов.",
    dismissLabel: "Закрыть",
    periodWeek: "Неделя",
    periodMonth: "Месяц",
    comparedToPreviousPeriod: "По сравнению с прошлым периодом",
  },
  achievements: {
    title: "Достижения",
    locked: "Не открыто",
    codeLabel: {
      STREAK_7: "7-дневная привычка",
      STREAK_30: "30-дневная привычка",
      LIMIT_WEEK: "Мастер недельного лимита",
      FULL_MONTH: "Весь месяц",
    },
    codeDescription: {
      STREAK_7: "Записывайте 7 дней подряд",
      STREAK_30: "Записывайте 30 дней подряд",
      LIMIT_WEEK: "7 дней подряд в рамках дневного лимита",
      FULL_MONTH: "Записывайте каждый день весь месяц",
    },
  },
  budget: {
    dailyAllowanceLabel: "Дневной лимит",
    todaySpentTile: "Потрачено сегодня",
    tomorrowTile: "Лимит на завтра",
    overspentLine: (amount) => `0 сум · превышено на ${amount}`,
    setupTitle: "Настройте бюджет",
    setupCta: "Начать",
    forecastOverage: (amount) => `В таком темпе к концу месяца превышение составит ${amount}`,
    expectedIncomeLabel: "① Ожидаемый доход в месяц",
    fixedPaymentsLabel: "② Постоянные платежи (аренда, коммуналка...)",
    loanPaymentsLabel: "③ Платежи по кредиту",
    plannedSavingsLabel: "④ План накоплений",
    freeBudgetLabel: "Свободный бюджет",
    negativeFreeBudgetWarning: "Обязательства превышают доход.",
    limitsTitle: "Лимиты по категориям",
    limitAmountPlaceholder: "Лимит",
    save: "Сохранить",
    saved: "Сохранено",
    autoLabel: "автоматически",
    useManual: "Ввести вручную",
    useAuto: "Считать автоматически",
  },
  recurring: {
    sectionTitle: "Постоянные платежи",
    addButton: "+ Добавить",
    emptyState: "Постоянных платежей пока нет.",
    nextDueLabel: (date) => `Следующий: ${date}`,
    archived: "В архиве",
    kindLabel: { REGULAR: "Постоянный расход", LOAN: "Кредит", INCOME: "Доход" },
    scheduleLabel: { MONTHLY: "Ежемесячно", WEEKLY: "Еженедельно", EVERY_N_DAYS: "Каждые N дней" },
    formTitleNew: "Новый постоянный платёж",
    formTitleEdit: "Редактировать платёж",
    nameLabel: "Название",
    namePlaceholder: "Например: Аренда",
    amountLabel: "Сумма",
    categoryLabel: "Категория",
    accountLabel: "Счёт",
    scheduleFieldLabel: "Периодичность",
    dayOfMonthLabel: "День месяца",
    dayOfWeekLabel: "День недели",
    everyNDaysLabel: "Каждые сколько дней",
    startDateLabel: "Дата начала",
    autoCreateLabel: "Записывать автоматически",
    remindDaysBeforeLabel: "За сколько дней напомнить",
    save: "Сохранить",
    archive: "В архив",
    weekdayShort: ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд"],
    upcomingTitle: "Следующие 30 дней",
    upcomingEmpty: "В ближайшие 30 дней платежей нет.",
    dueToday: "Сегодня",
    dueTomorrow: "Завтра",
    payButton: "Оплачено",
    skipButton: "Пропустить",
    statusPaid: "Оплачено ✓",
    statusSkipped: "Пропущено",
    homeCardTitle: "Платежи в ближайшие 7 дней",
    homeCardEmpty: "В ближайшие 7 дней платежей нет",
  },
  debts: {
    sectionTitle: "Кредиты",
    addButton: "+ Добавить",
    emptyState: "Пока нет долгов.",
    typeLabel: {
      BANK_LOAN: "Банковский кредит",
      INSTALLMENT: "Рассрочка",
      CREDIT_CARD: "Кредитная карта",
      PERSONAL_DEBT: "Личный долг",
      OTHER: "Другое",
    },
    interestMethodLabel: {
      ANNUITY_MONTHLY: "Аннуитет (ежемесячно)",
      ACTUAL_365: "По факту дней (365)",
    },
    formTitleNew: "Новый кредит",
    formTitleEdit: "Редактировать кредит",
    nameLabel: "Название",
    namePlaceholder: "Например: Автокредит",
    lenderLabel: "Банк / кредитор",
    typeFieldLabel: "Тип",
    originalAmountLabel: "Сумма",
    annualRateLabel: "Годовая ставка (%)",
    startDateLabel: "Дата начала",
    termMonthsLabel: "Срок (мес.)",
    paymentDayLabel: "День платежа",
    interestMethodFieldLabel: "Способ расчёта процентов",
    save: "Сохранить",
    manualScheduleButton: "Ввести график банка",
    manualScheduleTitle: "Ввести график вручную",
    manualScheduleAddRow: "+ Добавить строку",
    estimatedLabel: "примерно",
    outstandingLabel: "Остаток",
    paidPercentLabel: "Погашено",
    monthlyPaymentLabel: "Ежемесячный платёж",
    monthsLeftLabel: "Осталось месяцев",
    closeDateLabel: "Дата погашения",
    nextPaymentTitle: "Следующий платёж",
    noNextPayment: "Нет запланированного платежа",
    payButton: "Оплачено",
    payExtraToggle: "Досрочное погашение (всё в основной долг)",
    payAmountLabel: "Сумма",
    payDateLabel: "Дата",
    payAccountLabel: "Счёт",
    simulatorTitle: "Погасить быстрее",
    simulatorCustomPlaceholder: "Другая сумма",
    simulatorResultSaved: (months, amount) => `На ${months} мес. раньше, экономия ${amount}`,
    simulatorResultNone: "Выберите дополнительную сумму",
    historyTitle: "История платежей",
    historyEmpty: "Платежей пока нет.",
    strategyTitle: "Какая стратегия лучше?",
    strategyAvalanche: "Avalanche (с высокой ставки)",
    strategySnowball: "Snowball (с малого остатка)",
    strategyRecommended: "Рекомендуется",
    strategyInterestLabel: "Всего процентов",
    strategyCloseDateLabel: "Дата погашения",
    closedBadge: "Закрыт",
    activeBadge: "Активен",
    homeCardTitle: "Долги",
    debtClosedCelebration: "🎉 Долг полностью погашен!",
  },
  error: {
    generic: "Произошла непредвиденная ошибка. Откройте приложение ещё раз.",
    retry: "Повторить попытку",
  },
};

const dictionaries: Record<Language, Dictionary> = { uz, ru };

export function useDictionary(): Dictionary {
  const language = useLanguage();
  return dictionaries[language];
}
