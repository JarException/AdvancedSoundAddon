package de.jarexception.advancedsoundaddon.signal;

import org.junit.Test;

import static org.junit.Assert.assertSame;

public class ModuleListBuilderCompatTest {
    @Test
    public void supportsLegacyPhysicsModuleParameter() {
        LegacyBuilder builder = new LegacyBuilder();
        LegacyModule module = new LegacyModule();

        ModuleListBuilderCompat.add(builder, module);

        assertSame(module, builder.module);
    }

    @Test
    public void supportsModernBaseModuleParameter() {
        ModernBuilder builder = new ModernBuilder();
        ModernModule module = new ModernModule();

        ModuleListBuilderCompat.add(builder, module);

        assertSame(module, builder.module);
    }

    private static final class LegacyModule {
    }

    public static final class LegacyBuilder {
        private LegacyModule module;

        public void add(LegacyModule module) {
            this.module = module;
        }
    }

    private static class ModernBaseModule {
    }

    private static final class ModernModule extends ModernBaseModule {
    }

    public static final class ModernBuilder {
        private ModernBaseModule module;

        public void add(ModernBaseModule module) {
            this.module = module;
        }
    }
}
