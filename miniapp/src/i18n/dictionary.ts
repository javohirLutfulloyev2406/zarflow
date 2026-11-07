import type { AccountType, Language } from "../api/types";
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
  };
  nav: {
    home: string;
    history: string;
    analytics: string;
    plan: string;
  };
  placeholders: {
    analytics: string;
    plan: string;
  };
  history: {
    title: string;
    empty: string;
    today: string;
    yesterday: string;
    deleted: string;
    deleteConfirm: string;
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
  };
  profile: {
    title: string;
    firstName: string;
    language: string;
    timezone: string;
    timezoneSearchPlaceholder: string;
    expectedMonthlyIncome: string;
    incomePlaceholder: string;
    save: string;
    saved: string;
    conflict: string;
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
  },
  nav: {
    home: "Asosiy",
    history: "Tarix",
    analytics: "Analitika",
    plan: "Reja",
  },
  placeholders: {
    analytics: "Tahlil va hisobotlar tez orada shu yerda ko'rinadi.",
    plan: "Byudjet va rejalashtirish tez orada shu yerda ko'rinadi.",
  },
  history: {
    title: "Tarix",
    empty: "Hali tranzaksiyalar yo'q.",
    today: "Bugun",
    yesterday: "Kecha",
    deleted: "O'chirildi",
    deleteConfirm: "O'chirishni tasdiqlaysizmi?",
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
  },
  profile: {
    title: "Profil",
    firstName: "Ism",
    language: "Til",
    timezone: "Vaqt zonasi",
    timezoneSearchPlaceholder: "Shahar yoki hudud qidiring",
    expectedMonthlyIncome: "Oylik kutilgan daromad",
    incomePlaceholder: "Masalan: 8500000",
    save: "Saqlash",
    saved: "Saqlandi",
    conflict: "Ma'lumotlar boshqa joyda o'zgartirildi. Qayta yuklandi.",
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
  },
  nav: {
    home: "Главная",
    history: "История",
    analytics: "Аналитика",
    plan: "План",
  },
  placeholders: {
    analytics: "Аналитика и отчёты скоро появятся здесь.",
    plan: "Бюджет и планирование скоро появятся здесь.",
  },
  history: {
    title: "История",
    empty: "Пока нет транзакций.",
    today: "Сегодня",
    yesterday: "Вчера",
    deleted: "Удалено",
    deleteConfirm: "Подтвердите удаление",
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
  },
  profile: {
    title: "Профиль",
    firstName: "Имя",
    language: "Язык",
    timezone: "Часовой пояс",
    timezoneSearchPlaceholder: "Найдите город или регион",
    expectedMonthlyIncome: "Ожидаемый доход в месяц",
    incomePlaceholder: "Например: 8500000",
    save: "Сохранить",
    saved: "Сохранено",
    conflict: "Данные были изменены в другом месте. Перезагружено.",
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
