import { PlaceholderScreen } from "../components/PlaceholderScreen";
import { useDictionary } from "../i18n/dictionary";

export function Analytics() {
  const dictionary = useDictionary();
  return <PlaceholderScreen text={dictionary.placeholders.analytics} />;
}
