package nyonio.item;

import java.text.NumberFormat;

final class ManaNumberFormat {
    private ManaNumberFormat() {
    }

    static String format(long amount) {
        return NumberFormat.getIntegerInstance().format(amount);
    }
}
