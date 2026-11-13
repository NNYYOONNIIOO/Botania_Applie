package nyonio.item;

import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.TranslationTextComponent;
import org.junit.Test;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class ManaNumberFormatTest {
    @Test
    public void formatsEnglishTooltipWithoutLeavingPlaceholderText() throws Exception {
        assertTooltip("en_us", Locale.US, 973000L, "Stored 973,000 Mana");
    }

    @Test
    public void formatsChineseTooltipWithLongStorageAmounts() throws Exception {
        assertTooltip("zh_cn", Locale.CHINA, Long.MAX_VALUE, "存储了 9,223,372,036,854,775,807 魔力");
    }

    @Test
    public void formatsRussianTooltipWithoutLeavingPlaceholderText() throws Exception {
        assertTooltip("ru_ru", new Locale("ru", "RU"), 973000L, "Хранится 973\u00a0000 маны");
    }

    private void assertTooltip(String localeName, Locale locale, long amount, String expected) throws Exception {
        LanguageMap originalLanguage = LanguageMap.getInstance();
        Locale originalLocale = Locale.getDefault();
        try (InputStream input = getClass().getResourceAsStream(
                "/assets/botania_applie/lang/" + localeName + ".json")) {
            assertNotNull(input);
            Map<String, String> translations = new HashMap<>();
            LanguageMap.func_240593_a_(input, translations::put);
            String key = "botania_applie.tooltip.stored_mana";
            LanguageMap language = mock(LanguageMap.class);
            when(language.func_230503_a_(key)).thenReturn(translations.get(key));
            LanguageMap.func_240594_a_(language);
            Locale.setDefault(locale);
            assertEquals(expected,
                    new TranslationTextComponent(key, ManaNumberFormat.format(amount)).getString());
        } finally {
            Locale.setDefault(originalLocale);
            LanguageMap.func_240594_a_(originalLanguage);
        }
    }
}
