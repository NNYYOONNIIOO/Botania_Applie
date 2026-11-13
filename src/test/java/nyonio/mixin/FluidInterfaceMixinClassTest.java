package nyonio.mixin;

import java.io.InputStream;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;
import static org.junit.Assert.*;

public class FluidInterfaceMixinClassTest {
    @Test
    public void doesNotGenerateInnerClassesInAe2sSignedPackage() throws Exception {
        try (InputStream input = getClass().getResourceAsStream(
                "/nyonio/mixin/MixinFluidManaUpgrades.class")) {
            assertNotNull(input);
            ClassNode mixin = new ClassNode();
            new ClassReader(input).accept(mixin, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
            for (InnerClassNode inner : mixin.innerClasses) {
                assertFalse("An inner class here would be relocated into AE2's signed package: " + inner.name,
                        inner.name.startsWith(mixin.name + "$"));
            }
        }
    }
}
