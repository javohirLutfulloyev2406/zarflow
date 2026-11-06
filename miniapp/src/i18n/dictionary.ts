import type { Language } from "../api/types";
import { useLanguage } from "./useLanguage";

export interface Dictionary {
  greeting: (firstName: string) => string;
  home: {
    emptyState: string;
    todayLimitLabel: string;
    balanceLabel: string;
    comingSoon: string;
  };
  nav: {
    home: string;
    history: string;
    analytics: string;
    plan: string;
  };
  placeholders: {
    history: string;
    analytics: string;
    plan: string;
  };
  quickAdd: {
    title: string;
    button: string;
  };
  error: {
    generic: string;
  };
}

const uz: Dictionary = {
  greeting: (firstName) => `Xayrli kun, ${firstName}`,
  home: {
    emptyState: "Birinchi xarajatni botga yozib ko'ring: 25000 tushlik",
    todayLimitLabel: "Bugungi limit",
    balanceLabel: "Balans",
    comingSoon: "tez orada",
  },
  nav: {
    home: "Asosiy",
    history: "Tarix",
    analytics: "Analitika",
    plan: "Reja",
  },
  placeholders: {
    history: "Tranzaksiyalar tarixi tez orada shu yerda ko'rinadi.",
    analytics: "Tahlil va hisobotlar tez orada shu yerda ko'rinadi.",
    plan: "Byudjet va rejalashtirish tez orada shu yerda ko'rinadi.",
  },
  quickAdd: {
    title: "Tez kiritish tez orada. Hozircha botga yozing: 25000 tushlik",
    button: "Botga o'tish",
  },
  error: {
    generic: "Kutilmagan xatolik yuz berdi. Ilovani qayta ochib ko'ring.",
  },
};

const ru: Dictionary = {
  greeting: (firstName) => `Добрый день, ${firstName}`,
  home: {
    emptyState: "Запишите первый расход боту: 25000 обед",
    todayLimitLabel: "Лимит на сегодня",
    balanceLabel: "Баланс",
    comingSoon: "скоро",
  },
  nav: {
    home: "Главная",
    history: "История",
    analytics: "Аналитика",
    plan: "План",
  },
  placeholders: {
    history: "История транзакций скоро появится здесь.",
    analytics: "Аналитика и отчёты скоро появятся здесь.",
    plan: "Бюджет и планирование скоро появятся здесь.",
  },
  quickAdd: {
    title: "Быстрый ввод скоро появится. Пока пишите боту: 25000 обед",
    button: "Перейти к боту",
  },
  error: {
    generic: "Произошла непредвиденная ошибка. Откройте приложение ещё раз.",
  },
};

const dictionaries: Record<Language, Dictionary> = { uz, ru };

export function useDictionary(): Dictionary {
  const language = useLanguage();
  return dictionaries[language];
}
