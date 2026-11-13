package nyonio.item;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.TranslationTextComponent;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TranslationCoverageTest {
    private static final Pattern ARGUMENT = Pattern.compile("%(?:(\\d+)\\$)?s");

    @Test
    public void allLanguagesHaveTheSameKeysAndPlaceholderOrder() throws Exception {
        Map<String, String> english = load("en_us");
        for (String language : new String[] {"zh_cn", "ru_ru"}) {
            Map<String, String> translations = load(language);
            assertEquals(language + " translation keys", english.keySet(), translations.keySet());
            for (String key : english.keySet()) {
                String translation = translations.get(key);
                assertFalse(language + ": " + key, translation.trim().isEmpty());
                assertEquals(language + ": " + key, arguments(english.get(key)), arguments(translation));
            }
        }
    }

    @Test
    public void everyFormattedRussianMessageRendersWithMinecraftTranslations() throws Exception {
        LanguageMap original = LanguageMap.getInstance();
        Map<String, String> translations = load("ru_ru");
        LanguageMap russian = mock(LanguageMap.class);
        when(russian.func_230503_a_(anyString())).thenAnswer(call -> translations.get(call.getArgument(0)));
        LanguageMap.func_240594_a_(russian);
        try {
            for (Map.Entry<String, String> entry : translations.entrySet()) {
                int count = arguments(entry.getValue()).size();
                if (count == 0) {
                    continue;
                }
                Object[] args = new Object[count];
                for (int i = 0; i < count; i++) {
                    args[i] = "ARG" + i;
                }
                String text = new TranslationTextComponent(entry.getKey(), args).getString();
                assertFalse(entry.getKey(), text.contains("%"));
                for (Object argument : args) {
                    assertTrue(entry.getKey(), text.contains(argument.toString()));
                }
            }
        } finally {
            LanguageMap.func_240594_a_(original);
        }
    }

    private Map<String, String> load(String language) throws Exception {
        try (InputStream input = getClass().getResourceAsStream(
                "/assets/botania_applie/lang/" + language + ".json")) {
            assertNotNull(input);
            Map<String, String> translations = new HashMap<>();
            LanguageMap.func_240593_a_(input, translations::put);
            return translations;
        }
    }

    private List<String> arguments(String value) {
        List<String> arguments = new ArrayList<>();
        Matcher matcher = ARGUMENT.matcher(value);
        while (matcher.find()) {
            arguments.add(matcher.group());
        }
        return arguments;
    }
}
